package com.lokai.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lokai.app.model.DeviceProfile

private val BgCard      = Color(0xFF1A1A1A)
private val AccentAmber = Color(0xFFF5A623)
private val TextPrimary = Color(0xFFE0E0E0)
private val TextSubtle  = Color(0xFF888888)

@Composable
fun DeviceProfileCard(profile: DeviceProfile, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Device name + tier badge ──────────────────────────────────────
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment   = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text      = profile.deviceName,
                        color     = TextPrimary,
                        fontSize  = 16.sp,
                        fontWeight= FontWeight.SemiBold
                    )
                    Text(
                        text    = "Android ${profile.androidVersion} · API ${profile.apiLevel}",
                        color   = TextSubtle,
                        fontSize= 12.sp
                    )
                }
                TierBadge(tier = profile.tier)
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text    = profile.tier.description,
                color   = TextSubtle,
                fontSize= 12.sp
            )

            Divider(
                modifier  = Modifier.padding(vertical = 12.dp),
                color     = Color(0xFF2A2A2A),
                thickness = 1.dp
            )

            // ── Hardware grid ─────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HardwareRow(
                    icon  = "🧠",
                    label = "Chip",
                    value = profile.chipName
                )
                HardwareRow(
                    icon  = "💾",
                    label = "RAM",
                    value = buildRamString(profile)
                )
                HardwareRow(
                    icon  = "⚙️",
                    label = "CPU",
                    value = "${profile.cpuCores} cores · ${profile.cpuArch}"
                )
                HardwareRow(
                    icon  = "🎮",
                    label = "GPU",
                    value = profile.gpuVendor.displayName
                )
            }

            Divider(
                modifier  = Modifier.padding(vertical = 12.dp),
                color     = Color(0xFF2A2A2A),
                thickness = 1.dp
            )

            // ── Effective RAM breakdown ───────────────────────────────────────
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment   = Alignment.CenterVertically
            ) {
                Text(
                    text    = "Effective RAM (used for filtering)",
                    color   = TextSubtle,
                    fontSize= 11.sp
                )
                Text(
                    text      = "%.2f GB".format(profile.effectiveRamGb),
                    color     = AccentAmber,
                    fontSize  = 13.sp,
                    fontWeight= FontWeight.Bold,
                    fontFamily= FontFamily.Monospace
                )
            }
            if (profile.swapGb > 0.05f) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text    = "%.2f GB physical + %.2f GB swap × 0.6".format(
                        profile.totalRamGb, profile.swapGb
                    ),
                    color   = TextSubtle.copy(alpha = 0.7f),
                    fontSize= 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun HardwareRow(icon: String, label: String, value: String) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 14.sp, modifier = Modifier.width(26.dp))
        Text(
            text      = label,
            color     = TextSubtle,
            fontSize  = 12.sp,
            modifier  = Modifier.width(50.dp)
        )
        Text(
            text    = value,
            color   = Color(0xFFE0E0E0),
            fontSize= 13.sp
        )
    }
}

private fun buildRamString(profile: DeviceProfile): String {
    val total = "%.1f GB total".format(profile.totalRamGb)
    val avail = "%.1f GB free".format(profile.availableRamGb)
    return if (profile.swapGb > 0.05f) {
        "$total · $avail · +%.1f GB swap".format(profile.swapGb)
    } else {
        "$total · $avail"
    }
}
