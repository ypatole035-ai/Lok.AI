package com.lokai.app.data.device

import android.content.Context
import android.os.Build
import com.lokai.app.model.DeviceProfile
import com.lokai.app.model.DeviceTier
import com.lokai.app.model.GpuVendor
import java.io.File

/**
 * Reads hardware information from:
 *  - /proc/meminfo   → RAM + swap
 *  - /proc/cpuinfo   → chip name, cores, arch
 *  - Android Build   → device name, Android version, API level
 *  - /dev/kgsl-3d0   → Adreno GPU detection
 *  - /dev/mali0      → Mali GPU detection
 *
 * All reads are synchronous and cheap — run on IO dispatcher.
 */
class DeviceDetector(private val context: Context) {

    fun detect(): DeviceProfile {
        val memInfo   = parseMemInfo()
        val cpuInfo   = parseCpuInfo()
        val gpuVendor = detectGpu()

        val totalRam     = memInfo.totalKb     / (1024f * 1024f)
        val availableRam = memInfo.availableKb / (1024f * 1024f)
        val swap         = memInfo.swapTotalKb / (1024f * 1024f)
        val effectiveRam = totalRam + (swap * 0.6f)

        return DeviceProfile(
            totalRamGb      = totalRam,
            availableRamGb  = availableRam,
            swapGb          = swap,
            effectiveRamGb  = effectiveRam,
            chipName        = cpuInfo.chipName,
            cpuCores        = cpuInfo.cores,
            cpuArch         = cpuInfo.arch,
            gpuVendor       = gpuVendor,
            deviceName      = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            androidVersion  = Build.VERSION.RELEASE,
            apiLevel        = Build.VERSION.SDK_INT,
            tier            = DeviceTier.fromEffectiveRamGb(effectiveRam)
        )
    }

    // ─── /proc/meminfo ────────────────────────────────────────────────────────

    private data class MemInfo(val totalKb: Long, val availableKb: Long, val swapTotalKb: Long)

    private fun parseMemInfo(): MemInfo {
        var total    = 0L
        var available = 0L
        var swapTotal = 0L
        try {
            File("/proc/meminfo").bufferedReader().use { reader ->
                reader.lineSequence().forEach { line ->
                    when {
                        line.startsWith("MemTotal:")     -> total    = extractKb(line)
                        line.startsWith("MemAvailable:") -> available = extractKb(line)
                        line.startsWith("SwapTotal:")    -> swapTotal = extractKb(line)
                    }
                }
            }
        } catch (_: Exception) { /* fall through with zeros */ }
        return MemInfo(total, available, swapTotal)
    }

    private fun extractKb(line: String): Long {
        // e.g. "MemTotal:        7932920 kB"
        return line.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
    }

    // ─── /proc/cpuinfo ────────────────────────────────────────────────────────

    private data class CpuInfo(val chipName: String, val cores: Int, val arch: String)

    private fun parseCpuInfo(): CpuInfo {
        var hardware = ""
        var processor = ""
        var coreCount = 0
        var model     = ""
        try {
            File("/proc/cpuinfo").bufferedReader().use { reader ->
                reader.lineSequence().forEach { line ->
                    val lower = line.lowercase()
                    when {
                        lower.startsWith("hardware")  -> hardware  = line.substringAfter(":").trim()
                        lower.startsWith("processor") -> coreCount++
                        lower.startsWith("model name")-> model     = line.substringAfter(":").trim()
                        lower.startsWith("processor") -> processor = line.substringAfter(":").trim()
                    }
                }
            }
        } catch (_: Exception) { /* ignore */ }

        val rawChip = when {
            hardware.isNotBlank() -> hardware
            model.isNotBlank()    -> model
            else                  -> Build.HARDWARE
        }

        val chipName = mapChipName(rawChip)
        val arch     = System.getProperty("os.arch") ?: Build.CPU_ABI

        return CpuInfo(
            chipName = chipName,
            cores    = if (coreCount > 0) coreCount else Runtime.getRuntime().availableProcessors(),
            arch     = arch
        )
    }

    /**
     * Maps raw hardware/model strings from /proc/cpuinfo to human-readable chip names.
     * Covers Snapdragon SM codes, MediaTek MT codes, Exynos, Kirin, Tensor.
     */
    private fun mapChipName(raw: String): String {
        val r = raw.uppercase()

        // ── Snapdragon SM codes ────────────────────────────────────────────────
        val snapdragonMap = mapOf(
            "SM8750" to "Snapdragon 8 Elite",
            "SM8650" to "Snapdragon 8 Gen 3",
            "SM8550" to "Snapdragon 8 Gen 2",
            "SM8475" to "Snapdragon 8+ Gen 1",
            "SM8450" to "Snapdragon 8 Gen 1",
            "SM8350" to "Snapdragon 888",
            "SM8250" to "Snapdragon 865",
            "SM8150" to "Snapdragon 855",
            "SM8050" to "Snapdragon 8s Gen 3",
            "SM7675" to "Snapdragon 7s Gen 3",
            "SM7550" to "Snapdragon 7 Gen 3",
            "SM7475" to "Snapdragon 7+ Gen 2",
            "SM7450" to "Snapdragon 7 Gen 1",
            "SM7350" to "Snapdragon 778G",
            "SM7325" to "Snapdragon 778G+",
            "SM7250" to "Snapdragon 765G",
            "SM6450" to "Snapdragon 6 Gen 1",
            "SM6375" to "Snapdragon 695",
            "SM6350" to "Snapdragon 690",
            "SM4450" to "Snapdragon 4 Gen 2",
            "SM4375" to "Snapdragon 480+",
            "SM4350" to "Snapdragon 480"
        )
        for ((code, name) in snapdragonMap) {
            if (r.contains(code)) return name
        }
        if (r.contains("SNAPDRAGON") || r.contains("QSD") || r.contains("MSM")) {
            return "Snapdragon ($raw)"
        }

        // ── MediaTek MT codes ─────────────────────────────────────────────────
        val mtkMap = mapOf(
            "MT6991" to "Dimensity 9400",
            "MT6989" to "Dimensity 9300",
            "MT6985" to "Dimensity 9200+",
            "MT6983" to "Dimensity 9200",
            "MT6982" to "Dimensity 9000+",
            "MT6980" to "Dimensity 9000",
            "MT6979" to "Dimensity 8300",
            "MT6977" to "Dimensity 8200",
            "MT6976" to "Dimensity 8100",
            "MT6975" to "Dimensity 1300",
            "MT6891" to "Dimensity 1200",
            "MT6889" to "Dimensity 1000+",
            "MT6886" to "Dimensity 7200",
            "MT6880" to "Dimensity 1000",
            "MT6877" to "Dimensity 900",
            "MT6875" to "Dimensity 820",
            "MT6873" to "Dimensity 800U",
            "MT6853" to "Dimensity 720",
            "MT6833" to "Dimensity 700",
            "MT6769" to "Helio G85",
            "MT6768" to "Helio G80",
            "MT6765" to "Helio G35"
        )
        for ((code, name) in mtkMap) {
            if (r.contains(code)) return name
        }
        if (r.contains("MT") || r.contains("MEDIATEK") || r.contains("DIMENSITY") || r.contains("HELIO")) {
            return "MediaTek ($raw)"
        }

        // ── Samsung Exynos ────────────────────────────────────────────────────
        val exynosMap = mapOf(
            "S5E9945" to "Exynos 2500",
            "S5E9935" to "Exynos 2400",
            "S5E9925" to "Exynos 2200",
            "S5E9845" to "Exynos 990",
            "S5E9840" to "Exynos 880",
            "EXYNOS2500" to "Exynos 2500",
            "EXYNOS2400" to "Exynos 2400",
            "EXYNOS2200" to "Exynos 2200",
            "EXYNOS990" to "Exynos 990",
            "EXYNOS9820" to "Exynos 9820",
            "EXYNOS9810" to "Exynos 9810"
        )
        for ((code, name) in exynosMap) {
            if (r.contains(code)) return name
        }
        if (r.contains("EXYNOS")) return "Samsung Exynos ($raw)"

        // ── Kirin / HiSilicon ─────────────────────────────────────────────────
        val kirinMap = mapOf(
            "KIRIN9000" to "Kirin 9000",
            "KIRIN990" to "Kirin 990",
            "KIRIN980" to "Kirin 980",
            "KIRIN970" to "Kirin 970",
            "KIRIN810" to "Kirin 810",
            "KIRIN710" to "Kirin 710"
        )
        for ((code, name) in kirinMap) {
            if (r.contains(code)) return name
        }
        if (r.contains("KIRIN") || r.contains("HISILICON")) return "HiSilicon Kirin ($raw)"

        // ── Google Tensor ─────────────────────────────────────────────────────
        if (r.contains("TENSOR")) {
            return when {
                r.contains("G4") -> "Google Tensor G4"
                r.contains("G3") -> "Google Tensor G3"
                r.contains("G2") -> "Google Tensor G2"
                else             -> "Google Tensor"
            }
        }

        // ── Fallback ──────────────────────────────────────────────────────────
        return if (raw.isNotBlank()) raw else "Unknown SoC"
    }

    // ─── GPU detection ────────────────────────────────────────────────────────

    private fun detectGpu(): GpuVendor = when {
        File("/dev/kgsl-3d0").exists() -> GpuVendor.ADRENO
        File("/dev/mali0").exists()    -> GpuVendor.MALI
        File("/dev/pvr_sync").exists() -> GpuVendor.POWERVR
        else                           -> GpuVendor.UNKNOWN
    }
}
