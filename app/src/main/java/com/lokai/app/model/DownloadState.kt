package com.lokai.app.model

/**
 * Represents the current download state for a model variant.
 * Emitted by DownloadManager and observed by the UI.
 */
sealed class DownloadState {
    /** No download in progress or queued for this model */
    object Idle : DownloadState()

    /** Download is queued but not yet started */
    object Queued : DownloadState()

    /**
     * Download is actively in progress.
     * @param bytesDownloaded bytes received so far (includes any resumed bytes)
     * @param totalBytes      total file size in bytes (-1 if unknown)
     * @param isResuming      true if this is resuming a partial download
     */
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val isResuming: Boolean = false
    ) : DownloadState() {
        /** Progress fraction 0.0–1.0, or -1 if totalBytes unknown */
        val progress: Float get() =
            if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else -1f

        val progressPercent: Int get() =
            if (totalBytes > 0) ((bytesDownloaded.toFloat() / totalBytes) * 100).toInt() else 0

        fun formattedProgress(): String {
            val dl = formatBytes(bytesDownloaded)
            return if (totalBytes > 0) "$dl / ${formatBytes(totalBytes)}" else dl
        }

        private fun formatBytes(bytes: Long): String = when {
            bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824f)
            bytes >= 1_048_576L     -> "%.0f MB".format(bytes / 1_048_576f)
            else                    -> "${bytes / 1024} KB"
        }
    }

    /** SHA-256 verification in progress after download */
    object Verifying : DownloadState()

    /** Download completed and verified successfully */
    data class Completed(val localPath: String) : DownloadState()

    /** Download failed with a reason */
    data class Failed(val reason: String) : DownloadState()

    /** Download was cancelled by the user */
    object Cancelled : DownloadState()
}
