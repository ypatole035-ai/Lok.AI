package com.lokai.app.data.download

import android.content.Context
import android.util.Log
import androidx.work.*
import com.lokai.app.model.DownloadState
import com.lokai.app.model.DownloadedModel
import com.lokai.app.model.ModelEntry
import com.lokai.app.model.ModelVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "DownloadManager"

// WorkManager input data keys
const val KEY_MODEL_ID       = "model_id"
const val KEY_DOWNLOAD_URL   = "download_url"
const val KEY_DEST_PATH      = "dest_path"
const val KEY_SHA256         = "sha256"
const val KEY_MODEL_NAME     = "model_name"
const val KEY_QUANT          = "quant"
const val KEY_RAM_REQUIRED   = "ram_required"
const val KEY_THINKING       = "thinking_trained"

// WorkManager progress keys
const val PROGRESS_BYTES_DL  = "bytes_downloaded"
const val PROGRESS_BYTES_TOT = "bytes_total"
const val PROGRESS_RESUMING  = "is_resuming"

/**
 * Central controller for all model downloads.
 *
 * - Owns a per-model [StateFlow<DownloadState>] that the UI observes
 * - Starts/cancels WorkManager workers
 * - Delegates actual HTTP work to [ModelDownloadWorker]
 * - Handles storage path scanning for pre-existing GGUFs
 */
class DownloadManager(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)
    private val verifier    = ChecksumVerifier()

    // modelId → state flow
    private val _states = mutableMapOf<String, MutableStateFlow<DownloadState>>()

    fun stateFor(modelId: String): StateFlow<DownloadState> {
        return _states.getOrPut(modelId) {
            MutableStateFlow(DownloadState.Idle)
        }.asStateFlow()
    }

    /**
     * Picks the best variant for [availableRamGb] and enqueues the download.
     *
     * @return the chosen [ModelVariant], or null if no variant fits
     */
    fun startDownload(
        model: ModelEntry,
        availableRamGb: Float,
        storageDir: File
    ): ModelVariant? {
        val variant = model.bestVariantFor(availableRamGb) ?: run {
            Log.w(TAG, "No fitting variant for ${model.id} at ${availableRamGb} GB RAM")
            return null
        }

        val destFile = File(storageDir, "${model.id}_${variant.quant}.gguf")
        val state    = _states.getOrPut(model.id) { MutableStateFlow(DownloadState.Idle) }
        state.value  = DownloadState.Queued

        val inputData = workDataOf(
            KEY_MODEL_ID     to model.id,
            KEY_DOWNLOAD_URL to variant.downloadUrl,
            KEY_DEST_PATH    to destFile.absolutePath,
            KEY_SHA256       to variant.sha256,
            KEY_MODEL_NAME   to model.name,
            KEY_QUANT        to variant.quant,
            KEY_RAM_REQUIRED to variant.ramRequiredGb,
            KEY_THINKING     to model.thinkingTrained
        )

        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(inputData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag(downloadTag(model.id))
            .build()

        workManager.enqueueUniqueWork(
            downloadTag(model.id),
            ExistingWorkPolicy.KEEP,
            request
        )

        // Observe WorkManager progress and pipe into our state flow
        workManager.getWorkInfosByTagLiveData(downloadTag(model.id))
            .observeForever { infos ->
                val info = infos?.firstOrNull() ?: return@observeForever
                when (info.state) {
                    WorkInfo.State.RUNNING -> {
                        val dl      = info.progress.getLong(PROGRESS_BYTES_DL, 0L)
                        val total   = info.progress.getLong(PROGRESS_BYTES_TOT, -1L)
                        val resume  = info.progress.getBoolean(PROGRESS_RESUMING, false)
                        state.value = DownloadState.Downloading(dl, total, resume)
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        state.value = DownloadState.Completed(destFile.absolutePath)
                    }
                    WorkInfo.State.FAILED -> {
                        state.value = DownloadState.Failed("Download failed")
                    }
                    WorkInfo.State.CANCELLED -> {
                        state.value = DownloadState.Cancelled
                        // Clean up partial file
                        if (destFile.exists()) destFile.delete()
                    }
                    else -> { /* ENQUEUED / BLOCKED — still Queued */ }
                }
            }

        Log.i(TAG, "Enqueued download: ${model.id} ${variant.quant} → ${destFile.absolutePath}")
        return variant
    }

    /** Cancel a running download and clean up partial file. */
    fun cancelDownload(modelId: String, storageDir: File) {
        workManager.cancelUniqueWork(downloadTag(modelId))
        _states[modelId]?.value = DownloadState.Cancelled
        // Partial files are cleaned up by the worker's onStopped / cancel observer above
        storageDir.listFiles()?.filter { it.name.startsWith(modelId) }?.forEach { it.delete() }
        Log.i(TAG, "Cancelled download for $modelId")
    }

    /** Reset state to Idle (used after delete or cancel dismiss) */
    fun resetState(modelId: String) {
        _states[modelId]?.value = DownloadState.Idle
    }

    /**
     * Scans [storageDir] for existing .gguf files that match models in [knownModelIds].
     * Returns a map of modelId → absolute path for any found files.
     *
     * File naming convention: {modelId}_{quant}.gguf
     */
    fun scanExistingGgufs(storageDir: File, knownModelIds: Set<String>): Map<String, String> {
        if (!storageDir.exists()) return emptyMap()
        return storageDir.listFiles()
            ?.filter { it.extension == "gguf" }
            ?.mapNotNull { file ->
                val modelId = knownModelIds.firstOrNull { id -> file.name.startsWith(id) }
                modelId?.let { modelId to file.absolutePath }
            }
            ?.toMap()
            ?: emptyMap()
    }

    private fun downloadTag(modelId: String) = "download_$modelId"
}

// ─── WorkManager Worker ───────────────────────────────────────────────────────

/**
 * Performs the actual HTTP download in a WorkManager coroutine worker.
 *
 * Features:
 * - HTTP Range header resume (skips already-downloaded bytes)
 * - Streams directly to file (no temp copy)
 * - Reports progress via setProgress()
 * - SHA-256 verification after completion
 * - Deletes partial file on failure
 */
class ModelDownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val verifier = ChecksumVerifier()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelId  = inputData.getString(KEY_MODEL_ID)     ?: return@withContext Result.failure()
        val url      = inputData.getString(KEY_DOWNLOAD_URL) ?: return@withContext Result.failure()
        val destPath = inputData.getString(KEY_DEST_PATH)    ?: return@withContext Result.failure()
        val sha256   = inputData.getString(KEY_SHA256)       ?: ""

        val destFile = File(destPath)

        try {
            val existingBytes = if (destFile.exists()) destFile.length() else 0L
            val isResuming    = existingBytes > 0

            Log.i(TAG, "Downloading $modelId | resuming=$isResuming | offset=$existingBytes")

            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout    = 60_000
                setRequestProperty("User-Agent", "LokAI/1.0 Android")
                if (isResuming) {
                    setRequestProperty("Range", "bytes=$existingBytes-")
                }
                connect()
            }

            val responseCode = connection.responseCode
            val isPartial    = responseCode == HttpURLConnection.HTTP_PARTIAL
            val isOk         = responseCode == HttpURLConnection.HTTP_OK

            if (!isOk && !isPartial) {
                Log.e(TAG, "HTTP $responseCode for $modelId")
                return@withContext Result.failure()
            }

            // If server doesn't support range requests, restart from 0
            val startOffset = if (isPartial) existingBytes else 0L
            if (!isPartial && isResuming) {
                destFile.delete()
                Log.i(TAG, "Server doesn't support Range; restarting download for $modelId")
            }

            val contentLength = connection.contentLengthLong
            val totalBytes    = if (isPartial) existingBytes + contentLength else contentLength

            connection.inputStream.use { input ->
                FileOutputStream(destFile, isPartial).use { output ->
                    val buffer  = ByteArray(8192)
                    var written = startOffset
                    var bytes: Int

                    while (input.read(buffer).also { bytes = it } != -1) {
                        if (isStopped) {
                            Log.i(TAG, "Worker stopped — cleaning up $modelId")
                            destFile.delete()
                            return@withContext Result.failure()
                        }
                        output.write(buffer, 0, bytes)
                        written += bytes

                        setProgress(
                            workDataOf(
                                PROGRESS_BYTES_DL  to written,
                                PROGRESS_BYTES_TOT to totalBytes,
                                PROGRESS_RESUMING  to isResuming
                            )
                        )
                    }
                }
            }

            connection.disconnect()
            Log.i(TAG, "Download complete for $modelId (${destFile.length()} bytes)")

            // Verification (emits Verifying state implicitly via Downloading progress)
            val valid = verifier.verify(destFile, sha256)
            if (!valid) {
                Log.e(TAG, "Checksum failed for $modelId — deleting")
                destFile.delete()
                return@withContext Result.failure()
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Download error for $modelId: ${e.message}")
            // Don't delete partial file — allows resume on next attempt
            Result.failure()
        }
    }
}
