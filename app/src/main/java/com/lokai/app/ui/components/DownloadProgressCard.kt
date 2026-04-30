package com.lokai.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lokai.app.model.DownloadState

private val BgCard      = Color(0xFF1A1A1A)
private val AccentAmber = Color(0xFFF5A623)
private val TextPrimary = Color(0xFFE0E0E0)
private val TextSubtle  = Color(0xFF888888)
private val BgBar       = Color(0xFF2A2A2A)

/**
 * In-app download progress card shown while a model is downloading.
 *
 * Shows:
 * - Model name and quant
 * - Progress bar (or indeterminate while verifying/queued)
 * - Bytes downloaded / total
 * - Cancel button
 */
@Composable
fun DownloadProgressCard(
    modelName: String,
    quant: String,
    state: DownloadState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(10.dp),
        colors   = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // ── Header row ────────────────────────────────────────────────────
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment   = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = modelName,
                        color      = TextPrimary,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1
                    )
                    Text(
                        text     = statusLabel(state, quant),
                        color    = AccentAmber,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick  = onCancel,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Filled.Close,
                        contentDescription = "Cancel download",
                        tint               = TextSubtle,
                        modifier           = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Progress bar ──────────────────────────────────────────────────
            when (state) {
                is DownloadState.Downloading -> {
                    val progress = state.progress
                    if (progress >= 0f) {
                        // Determinate
                        LinearProgressIndicator(
                            progress          = { progress },
                            modifier          = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color             = AccentAmber,
                            trackColor        = BgBar
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text    = state.formattedProgress(),
                                color   = TextSubtle,
                                fontSize= 11.sp
                            )
                            Text(
                                text    = "${state.progressPercent}%",
                                color   = AccentAmber,
                                fontSize= 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        // Indeterminate (size unknown)
                        LinearProgressIndicator(
                            modifier   = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color      = AccentAmber,
                            trackColor = BgBar
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text    = state.formattedProgress(),
                            color   = TextSubtle,
                            fontSize= 11.sp
                        )
                    }
                }

                is DownloadState.Verifying,
                is DownloadState.Queued -> {
                    LinearProgressIndicator(
                        modifier   = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color      = AccentAmber,
                        trackColor = BgBar
                    )
                }

                else -> { /* Idle/Completed/Failed/Cancelled handled by parent */ }
            }
        }
    }
}

private fun statusLabel(state: DownloadState, quant: String): String = when (state) {
    is DownloadState.Queued      -> "Queued · $quant"
    is DownloadState.Downloading -> if (state.isResuming) "Resuming · $quant" else "Downloading · $quant"
    is DownloadState.Verifying   -> "Verifying · $quant"
    else                         -> quant
}
