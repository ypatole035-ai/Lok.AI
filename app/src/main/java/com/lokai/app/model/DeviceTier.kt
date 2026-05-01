package com.lokai.app.model

/**
 * 7-tier device classification based on effective RAM.
 *
 * Effective RAM = total RAM + (swap * 0.6)
 *
 * Tiers determine which models are shown as compatible vs too large.
 */
enum class DeviceTier(
    val displayName: String,
    val description: String,
    val badgeColor: Long,       // hex ARGB for UI
    val minRamGb: Float,
    val maxRamGb: Float         // exclusive upper bound (Float.MAX_VALUE for top tier)
) {
    MICRO(
        displayName  = "Micro",
        description  = "Very limited device. Only the smallest quantized models will fit.",
        badgeColor   = 0xFF9E9E9EL,   // FIX: added L suffix
        minRamGb     = 0f,
        maxRamGb     = 2.5f
    ),
    LOW(
        displayName  = "Low",
        description  = "Entry-level device. Small 1B–2B models at reduced quant will work.",
        badgeColor   = 0xFFEF9A9AL,   // FIX: added L suffix
        minRamGb     = 2.5f,
        maxRamGb     = 3.5f
    ),
    LOW_MID(
        displayName  = "Low-Mid",
        description  = "Budget device. 1B–3B models run well. Some 7B at low quant.",
        badgeColor   = 0xFFFFCC80L,   // FIX: added L suffix
        minRamGb     = 3.5f,
        maxRamGb     = 5f
    ),
    MID(
        displayName  = "Mid",
        description  = "Mid-range device. 7B models at moderate quant. Good for daily use.",
        badgeColor   = 0xFFFFF176L,   // FIX: added L suffix
        minRamGb     = 5f,
        maxRamGb     = 7f
    ),
    HIGH(
        displayName  = "High",
        description  = "Flagship phone. 7B at full quant, some 13B models at lower quant.",
        badgeColor   = 0xFFA5D6A7L,   // FIX: added L suffix
        minRamGb     = 7f,
        maxRamGb     = 12f
    ),
    DESKTOP(
        displayName  = "Desktop",
        description  = "High-RAM device or tablet. 13B models and some 34B at low quant.",
        badgeColor   = 0xFF80DEEAL,   // FIX: added L suffix
        minRamGb     = 12f,
        maxRamGb     = 20f
    ),
    WORKSTATION(
        displayName  = "Workstation",
        description  = "Server-class device. Large models run comfortably.",
        badgeColor   = 0xFFCE93D8L,   // FIX: added L suffix
        minRamGb     = 20f,
        maxRamGb     = Float.MAX_VALUE
    );

    companion object {
        /**
         * Classify a device given its effective RAM in GB.
         * Effective RAM = total + (swap * 0.6)
         */
        fun fromEffectiveRamGb(effectiveRamGb: Float): DeviceTier {
            return entries.firstOrNull { effectiveRamGb >= it.minRamGb && effectiveRamGb < it.maxRamGb }
                ?: MICRO
        }
    }
}
