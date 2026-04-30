package com.lokai.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LokAIDarkColors = darkColorScheme(
    primary         = Color(0xFFF5A623),   // amber accent
    onPrimary       = Color(0xFF000000),
    primaryContainer= Color(0xFF2A1F00),
    onPrimaryContainer = Color(0xFFF5A623),
    secondary       = Color(0xFF888888),
    onSecondary     = Color(0xFF000000),
    background      = Color(0xFF0D0D0D),
    onBackground    = Color(0xFFE0E0E0),
    surface         = Color(0xFF1A1A1A),
    onSurface       = Color(0xFFE0E0E0),
    surfaceVariant  = Color(0xFF222222),
    onSurfaceVariant= Color(0xFF888888),
    outline         = Color(0xFF333333),
    error           = Color(0xFFEF9A9A),
    onError         = Color(0xFF000000)
)

@Composable
fun LokAITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LokAIDarkColors,
        typography  = LokAITypography,
        content     = content
    )
}
