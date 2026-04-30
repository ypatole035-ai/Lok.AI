package com.lokai.app.model

/**
 * Complete hardware profile detected at runtime.
 *
 * Populated by [com.lokai.app.data.device.DeviceDetector].
 */
data class DeviceProfile(
    // ── Memory ────────────────────────────────────────────────────────────────
    /** Total physical RAM in GB */
    val totalRamGb: Float,
    /** Currently available (free) RAM in GB at detection time */
    val availableRamGb: Float,
    /** Swap / zram in GB (weighted at 0.6× for tier calculation) */
    val swapGb: Float,
    /** Effective RAM = totalRamGb + swapGb * 0.6 */
    val effectiveRamGb: Float,

    // ── CPU ───────────────────────────────────────────────────────────────────
    /** Human-readable chip name, e.g. "Snapdragon 888" */
    val chipName: String,
    /** Number of CPU cores */
    val cpuCores: Int,
    /** CPU architecture string, e.g. "aarch64" */
    val cpuArch: String,

    // ── GPU ───────────────────────────────────────────────────────────────────
    /** GPU vendor detected from device files */
    val gpuVendor: GpuVendor,

    // ── Android ───────────────────────────────────────────────────────────────
    /** Device manufacturer + model, e.g. "Samsung Galaxy S23" */
    val deviceName: String,
    /** Android version string, e.g. "14" */
    val androidVersion: String,
    /** Android API level, e.g. 34 */
    val apiLevel: Int,

    // ── Classification ────────────────────────────────────────────────────────
    /** Tier determined from effectiveRamGb */
    val tier: DeviceTier
)

enum class GpuVendor(val displayName: String) {
    ADRENO("Adreno (Qualcomm)"),
    MALI("Mali (ARM)"),
    POWERVR("PowerVR"),
    UNKNOWN("Unknown GPU")
}
