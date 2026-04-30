package com.lokai.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Amber   = Color(0xFFF5A623)
private val Subtle  = Color(0xFF555555)
private val OnSurf  = Color(0xFFE0E0E0)
private val Surface = Color(0xFF1A1A1A)

// ─── Generic empty state ──────────────────────────────────────────────────────

@Composable
fun EmptyState(
    icon:    ImageVector,
    title:   String,
    body:    String,
    action:  String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = Subtle,
            modifier           = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text       = title,
            color      = OnSurf,
            fontSize   = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = body,
            color     = Subtle,
            fontSize  = 14.sp,
            textAlign = TextAlign.Center
        )
        if (action != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onAction,
                colors  = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Color.Black),
                shape   = RoundedCornerShape(10.dp)
            ) {
                Text(action, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Preset empty states ──────────────────────────────────────────────────────

@Composable
fun NoModelsEmpty(onBrowse: () -> Unit) = EmptyState(
    icon     = Icons.Filled.Inventory2,
    title    = "No models downloaded yet",
    body     = "Browse the model catalog and download one that fits your device.",
    action   = "Browse models",
    onAction = onBrowse
)

@Composable
fun NoAgentsEmpty(onCreate: () -> Unit) = EmptyState(
    icon     = Icons.Filled.SmartToy,
    title    = "No agents yet",
    body     = "Create a named agent and attach a file to unlock focused AI assistance.",
    action   = "Create agent",
    onAction = onCreate
)

@Composable
fun NoSessionsEmpty() = EmptyState(
    icon  = Icons.Filled.ChatBubbleOutline,
    title = "No conversations yet",
    body  = "Start a chat or open an agent to begin. Sessions appear here automatically."
)

@Composable
fun HFSearchEmpty(query: String) = EmptyState(
    icon  = Icons.Filled.SearchOff,
    title = "No results for "$query"",
    body  = "Try a different keyword or search for a model name like \"llama\" or \"qwen\"."
)

// ─── Error states ─────────────────────────────────────────────────────────────

@Composable
fun ErrorState(
    icon:    ImageVector = Icons.Filled.ErrorOutline,
    title:   String,
    body:    String,
    action:  String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = Color(0xFFEF9A9A),
            modifier           = Modifier.size(52.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text       = title,
            color      = OnSurf,
            fontSize   = 17.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = body,
            color     = Subtle,
            fontSize  = 13.sp,
            textAlign = TextAlign.Center
        )
        if (action != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = onAction,
                colors  = ButtonDefaults.outlinedButtonColors(contentColor = Amber),
                border  = ButtonDefaults.outlinedButtonBorder,
                shape   = RoundedCornerShape(10.dp)
            ) {
                Text(action)
            }
        }
    }
}

@Composable
fun DownloadFailedError(onRetry: () -> Unit) = ErrorState(
    title    = "Download failed",
    body     = "The file could not be downloaded. Check your connection and try again.",
    action   = "Retry",
    onAction = onRetry
)

@Composable
fun ModelLoadFailedError(modelName: String, onDismiss: () -> Unit) = ErrorState(
    title    = "Could not load $modelName",
    body     = "The model file may be corrupted or incompatible. Try re-downloading it.",
    action   = "Dismiss",
    onAction = onDismiss
)

@Composable
fun PdfNoTextError() = ErrorState(
    icon  = Icons.Filled.PictureAsPdf,
    title = "PDF has no text layer",
    body  = "This PDF appears to be scanned or image-based. OCR is not supported in v1.\nTry converting it to a text file first."
)

@Composable
fun FileTooLargeError(fileSizeMb: Int, onDismiss: () -> Unit) = ErrorState(
    title    = "File too large",
    body     = "This file is ${fileSizeMb}MB. Try using a smaller file or split it into sections.",
    action   = "Dismiss",
    onAction = onDismiss
)

@Composable
fun NotEnoughRamError(requiredGb: Float, availableGb: Float) = ErrorState(
    icon  = Icons.Filled.Memory,
    title = "Not enough RAM",
    body  = "This model needs %.1f GB but only %.1f GB is available right now.\nClose background apps and try again.".format(requiredGb, availableGb)
)

// ─── Skeleton shimmer (loading state) ─────────────────────────────────────────

@Composable
fun ShimmerBlock(
    modifier: Modifier = Modifier,
    height: Int = 80
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 1000f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF222222),
            Color(0xFF2E2E2E),
            Color(0xFF222222)
        ),
        start = Offset(shimmerTranslate - 400f, 0f),
        end   = Offset(shimmerTranslate, 0f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(shimmerBrush)
    )
}

@Composable
fun CatalogLoadingShimmer() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(5) {
            ShimmerBlock(height = 90)
        }
    }
}
