package com.lokai.app.model

/**
 * A specific quantization variant of a model.
 *
 * e.g. Llama 3.2 1B might have Q4_K_M and Q5_K_M variants
 * with different sizes and RAM requirements.
 */
data class ModelVariant(
    /** Quantization identifier, e.g. "Q4_K_M" */
    val quant: String,
    /** Download size in GB */
    val sizeGb: Float,
    /** Minimum RAM needed to load this variant, in GB */
    val ramRequiredGb: Float,
    /** Direct download URL (HuggingFace or mirror) */
    val downloadUrl: String,
    /** Expected SHA-256 hex checksum (empty string = skip verification) */
    val sha256: String = ""
) {
    /** Display string shown in download confirmation sheet */
    val displayLabel: String get() = "$quant · ${formatSize(sizeGb)} · needs ${formatSize(ramRequiredGb)} RAM"

    private fun formatSize(gb: Float): String =
        if (gb < 1f) "${(gb * 1024).toInt()} MB" else "%.1f GB".format(gb)
}
