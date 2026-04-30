package com.lokai.app.data.download

import android.content.Context
import android.os.Environment
import android.util.Log
import com.lokai.app.data.session.LokaiDatabase
import com.lokai.app.data.session.toDomain
import com.lokai.app.data.session.toEntity
import com.lokai.app.model.DownloadState
import com.lokai.app.model.DownloadedModel
import com.lokai.app.model.ModelEntry
import com.lokai.app.model.ModelVariant
import kotlinx.coroutines.flow.StateFlow
import java.io.File

private const val TAG = "DownloadRepository"

/**
 * Coordinates downloads, database persistence, and storage scanning.
 *
 * Single source of truth for which models are downloaded.
 */
class DownloadRepository(private val context: Context) {

    private val db      = LokaiDatabase.getInstance(context)
    private val dao     = db.downloadedModelDao()
    private val manager = DownloadManager(context)

    /** Returns app-private external storage dir for GGUFs (no permission needed on API 29+) */
    val storageDir: File get() {
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "models"
        )
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // ─── State observation ────────────────────────────────────────────────────

    fun downloadStateFor(modelId: String): StateFlow<DownloadState> =
        manager.stateFor(modelId)

    // ─── Download control ─────────────────────────────────────────────────────

    /**
     * Starts downloading [model], picking the best variant for [availableRamGb].
     * @return the chosen variant, or null if no variant fits.
     */
    fun startDownload(model: ModelEntry, availableRamGb: Float): ModelVariant? =
        manager.startDownload(model, availableRamGb, storageDir)

    /** Cancel the download for [modelId]. */
    fun cancelDownload(modelId: String) =
        manager.cancelDownload(modelId, storageDir)

    fun resetState(modelId: String) =
        manager.resetState(modelId)

    // ─── Persistence ──────────────────────────────────────────────────────────

    /** Save a completed download to the Room DB. */
    suspend fun saveDownload(model: DownloadedModel) {
        dao.insert(model.toEntity())
        Log.i(TAG, "Saved download record: ${model.modelId}")
    }

    /** Load all downloaded models from Room. */
    suspend fun getAllDownloaded(): List<DownloadedModel> =
        dao.getAll().map { it.toDomain() }

    /** Returns true if [modelId] is in the DB (completed download). */
    suspend fun isDownloaded(modelId: String): Boolean =
        dao.getById(modelId) != null

    /** Get a single downloaded model by ID. */
    suspend fun getDownloaded(modelId: String): DownloadedModel? =
        dao.getById(modelId)?.toDomain()

    /**
     * Delete a downloaded model: removes the .gguf file and the DB record.
     */
    suspend fun deleteModel(modelId: String): Boolean {
        val record = dao.getById(modelId) ?: return false
        val file   = File(record.localPath)
        val deleted = if (file.exists()) file.delete() else true
        if (deleted) {
            dao.delete(modelId)
            manager.resetState(modelId)
            Log.i(TAG, "Deleted model: $modelId")
        } else {
            Log.e(TAG, "Failed to delete file: ${record.localPath}")
        }
        return deleted
    }

    // ─── Storage scanner ──────────────────────────────────────────────────────

    /**
     * Scans the storage directory for existing GGUFs and returns a map of
     * modelId → file path for any files matching [knownModelIds].
     *
     * Used at app start to recover from cases where the DB was cleared but files remain.
     */
    fun scanExistingGgufs(knownModelIds: Set<String>): Map<String, String> =
        manager.scanExistingGgufs(storageDir, knownModelIds)
}
