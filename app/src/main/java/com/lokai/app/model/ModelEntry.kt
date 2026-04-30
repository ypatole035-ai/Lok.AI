package com.lokai.app.model

/**
 * A single model entry from the bundled models.json catalog.
 *
 * Contains all display metadata and the list of quantization variants
 * available for download.
 */
data class ModelEntry(
    /** Unique identifier used internally, e.g. "llama-3.2-1b-instruct" */
    val id: String,
    /** Display name shown in the model browser, e.g. "Llama 3.2 1B Instruct" */
    val name: String,
    /** Short label for the model family, e.g. "Meta" */
    val family: String,
    /** Parameter count string, e.g. "1B" or "7B" */
    val params: String,
    /** What this model is best at, e.g. "Lightweight chat, quick answers" */
    val bestFor: String,
    /** Minimum RAM needed for the smallest variant (GB) */
    val minRamGb: Float,
    /** Recommended RAM for the best quality variant (GB) */
    val recommendedRamGb: Float,
    /** Whether this model has been trained with thinking/reasoning tokens */
    val thinkingTrained: Boolean,
    /**
     * Short description of benchmark performance, e.g. "~18 tok/s on Snapdragon 888"
     * Empty string if no benchmark data available.
     */
    val benchmarkNote: String = "",
    /** All available quantization variants, ordered best → smallest */
    val variants: List<ModelVariant>
) {
    /**
     * Returns true if this model's minimum variant fits within [availableRamGb].
     */
    fun isCompatibleWith(availableRamGb: Float): Boolean =
        minRamGb <= availableRamGb

    /**
     * Returns the best variant that fits within [availableRamGb],
     * following the preferred quant order: Q5_K_M → Q4_K_M → Q4_K_S → Q3_K_M → IQ3_M → IQ2_M → Q2_K
     */
    fun bestVariantFor(availableRamGb: Float): ModelVariant? {
        val preferredOrder = listOf("Q5_K_M", "Q4_K_M", "Q4_K_S", "Q3_K_M", "IQ3_M", "IQ2_M", "Q2_K")
        val fitting = variants.filter { it.ramRequiredGb <= availableRamGb }
        if (fitting.isEmpty()) return null
        return preferredOrder
            .mapNotNull { quant -> fitting.firstOrNull { it.quant == quant } }
            .firstOrNull() ?: fitting.minByOrNull { it.ramRequiredGb }
    }

    /**
     * Why this model cannot run — only meaningful if [isCompatibleWith] returns false.
     */
    fun incompatibleReason(availableRamGb: Float): String =
        "Needs %.1f GB RAM, you have %.1f GB".format(minRamGb, availableRamGb)
}
