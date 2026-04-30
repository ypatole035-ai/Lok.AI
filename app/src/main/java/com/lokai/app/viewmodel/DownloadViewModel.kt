package com.lokai.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lokai.app.data.device.DeviceDetector
import com.lokai.app.data.download.DownloadRepository
import com.lokai.app.data.models.ModelCatalog
import com.lokai.app.model.DownloadState
import com.lokai.app.model.DownloadedModel
import com.lokai.app.model.ModelEntry
import com.lokai.app.model.ModelVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class DownloadUiState(
    val downloadedModels: List<DownloadedModel> = emptyList(),
    val isLoading: Boolean = true,
    val deleteConfirmModelId: String? = null,   // non-null = show delete confirmation
    val deleteError: String? = null
)

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val repo     = DownloadRepository(application)
    private val catalog  = ModelCatalog(application)
    private val detector = DeviceDetector(application)

    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    /** Live RAM GB — re-read at download time */
    private var liveRamGb: Float = 4f

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            liveRamGb = detector.detect().effectiveRamGb
            val downloaded = repo.getAllDownloaded()
            _uiState.update { it.copy(downloadedModels = downloaded, isLoading = false) }
        }
    }

    // ─── Download state observation ───────────────────────────────────────────

    /** Returns a StateFlow<DownloadState> for [modelId], for live progress observation. */
    fun downloadStateFor(modelId: String): StateFlow<DownloadState> =
        repo.downloadStateFor(modelId)

    // ─── Start download ───────────────────────────────────────────────────────

    /**
     * Initiates download for [model].
     * Uses live RAM reading at the moment of tap (doc requirement: re-read at download time).
     * @return chosen variant or null if nothing fits.
     */
    fun startDownload(model: ModelEntry): ModelVariant? {
        val variant = repo.startDownload(model, liveRamGb) ?: return null

        // Observe completion → persist to DB
        viewModelScope.launch {
            repo.downloadStateFor(model.id)
                .filter { it is DownloadState.Completed }
                .take(1)
                .collect { state ->
                    val completed = state as DownloadState.Completed
                    val file = File(completed.localPath)
                    val downloadedModel = DownloadedModel(
                        modelId        = model.id,
                        name           = model.name,
                        quant          = variant.quant,
                        localPath      = completed.localPath,
                        sizeBytes      = file.length(),
                        ramRequiredGb  = variant.ramRequiredGb,
                        thinkingTrained= model.thinkingTrained
                    )
                    repo.saveDownload(downloadedModel)
                    refresh()
                }
        }
        return variant
    }

    /** Cancel an in-progress download. */
    fun cancelDownload(modelId: String) {
        repo.cancelDownload(modelId)
    }

    // ─── Delete model ─────────────────────────────────────────────────────────

    fun requestDelete(modelId: String) {
        _uiState.update { it.copy(deleteConfirmModelId = modelId) }
    }

    fun dismissDeleteConfirm() {
        _uiState.update { it.copy(deleteConfirmModelId = null) }
    }

    fun confirmDelete(modelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = repo.deleteModel(modelId)
            if (ok) {
                refresh()
            } else {
                _uiState.update { it.copy(deleteError = "Failed to delete model file.") }
            }
            _uiState.update { it.copy(deleteConfirmModelId = null) }
        }
    }

    fun clearDeleteError() {
        _uiState.update { it.copy(deleteError = null) }
    }

    // ─── Storage scan ─────────────────────────────────────────────────────────

    /** Scans device storage for pre-existing GGUFs and imports them if found. */
    fun scanAndImport() {
        viewModelScope.launch(Dispatchers.IO) {
            val knownIds = catalog.allModels().map { it.id }.toSet()
            val found    = repo.scanExistingGgufs(knownIds)
            found.forEach { (modelId, path) ->
                if (!repo.isDownloaded(modelId)) {
                    val entry = catalog.allModels().firstOrNull { it.id == modelId } ?: return@forEach
                    val file  = File(path)
                    // Guess quant from filename
                    val quant = file.nameWithoutExtension.substringAfterLast("_")
                    val variant = entry.variants.firstOrNull { it.quant == quant }
                        ?: entry.variants.firstOrNull()
                        ?: return@forEach
                    repo.saveDownload(
                        DownloadedModel(
                            modelId        = modelId,
                            name           = entry.name,
                            quant          = variant.quant,
                            localPath      = path,
                            sizeBytes      = file.length(),
                            ramRequiredGb  = variant.ramRequiredGb,
                            thinkingTrained= entry.thinkingTrained
                        )
                    )
                }
            }
            refresh()
        }
    }
}
