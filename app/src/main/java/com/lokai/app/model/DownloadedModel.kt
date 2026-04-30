package com.lokai.app.model

/**
 * A model that has been fully downloaded and verified on this device.
 *
 * Stored in Room DB and shown in the "My Models" section.
 */
data class DownloadedModel(
    /** Matches ModelEntry.id from the catalog */
    val modelId: String,
    /** Display name, copied from ModelEntry at download time */
    val name: String,
    /** Quantization used, e.g. "Q4_K_M" */
    val quant: String,
    /** Full absolute path to the .gguf file on device storage */
    val localPath: String,
    /** File size in bytes */
    val sizeBytes: Long,
    /** RAM required in GB for this variant */
    val ramRequiredGb: Float,
    /** Whether this model has thinking/reasoning training */
    val thinkingTrained: Boolean,
    /** Unix timestamp (ms) when download completed */
    val downloadedAt: Long = System.currentTimeMillis(),
    /** Rolling average tokens/sec from last 5 inference runs (0 = no data yet) */
    val avgTokensPerSec: Float = 0f
) {
    val formattedSize: String get() = when {
        sizeBytes >= 1_073_741_824L -> "%.1f GB".format(sizeBytes / 1_073_741_824f)
        sizeBytes >= 1_048_576L     -> "%.0f MB".format(sizeBytes / 1_048_576f)
        else                        -> "${sizeBytes / 1024} KB"
    }

    val displayQuant: String get() = "$quant · $formattedSize"
}
