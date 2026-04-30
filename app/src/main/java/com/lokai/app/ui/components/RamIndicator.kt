package com.lokai.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AccentAmber  = Color(0xFFF5A623)
private val TextSubtle   = Color(0xFF888888)
private val BgPill       = Color(0xFF1E1E1E)
private val WarnRed      = Color(0xFFE53935)

/**
 * Small pill widget showing available RAM in the chat top bar.
 * Updates every 2 seconds via ChatViewModel.
 *
 * @param availableRamMb  Currently available RAM in megabytes
 * @param isGenerating    True while inference is running (shows amber accent)
 * @param batteryPct      Current battery percentage (shows red if low)
 * @param isBatteryLow    True if battery is below threshold
 */
@Composable
fun RamIndicator(
    availableRamMb: Long,
    isGenerating:   Boolean,
    batteryPct:     Int,
    isBatteryLow:   Boolean,
    modifier:       Modifier = Modifier
) {
    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // RAM pill
        RamPill(availableRamMb = availableRamMb, isGenerating = isGenerating)

        // Battery warning — only shown when low
        if (isBatteryLow) {
            BatteryPill(batteryPct = batteryPct)
        }
    }
}

@Composable
private fun RamPill(availableRamMb: Long, isGenerating: Boolean) {
    val text  = when {
        availableRamMb >= 1024 -> "%.1f GB free".format(availableRamMb / 1024f)
        else                   -> "${availableRamMb} MB free"
    }
    val color = if (isGenerating) AccentAmber else TextSubtle

    Row(
        modifier          = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(BgPill)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("RAM", color = TextSubtle, fontSize = 9.sp)
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BatteryPill(batteryPct: Int) {
    Row(
        modifier          = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF2A0000))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text("🔋", fontSize = 9.sp)
        Text(
            text       = "$batteryPct%",
            color      = WarnRed,
            fontSize   = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
