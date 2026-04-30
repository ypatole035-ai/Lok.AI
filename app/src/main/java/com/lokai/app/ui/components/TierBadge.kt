package com.lokai.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lokai.app.model.DeviceTier

@Composable
fun TierBadge(tier: DeviceTier, modifier: Modifier = Modifier) {
    val bgColor = Color(tier.badgeColor)
    // Pick dark or light text based on luminance of badge color
    val textColor = if (isLightColor(tier.badgeColor)) Color(0xFF111111) else Color(0xFFEEEEEE)

    Text(
        text = tier.displayName.uppercase(),
        color = textColor,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    )
}

/** Returns true if the color is light enough that dark text should be used. */
private fun isLightColor(argb: Long): Boolean {
    val r = ((argb shr 16) and 0xFF).toInt()
    val g = ((argb shr 8)  and 0xFF).toInt()
    val b = (argb           and 0xFF).toInt()
    // Standard relative luminance approximation
    val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    return luminance > 0.55
}
