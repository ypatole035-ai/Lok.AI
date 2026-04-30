package com.lokai.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lokai.app.model.DownloadState
import com.lokai.app.model.ModelEntry

private val BgCard      = Color(0xFF1A1A1A)
private val BgIncompat  = Color(0xFF141414)
private val AccentAmber = Color(0xFFF5A623)
private val TextPrimary = Color(0xFFE0E0E0)
private val TextSubtle  = Color(0xFF888888)
private val TextDisabled= Color(0xFF555555)

@Composable
fun ModelCard(
    model: ModelEntry,
    availableRamGb: Float,
    compatible: Boolean,
    downloadState: DownloadState = DownloadState.Idle,
    onDownloadClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cardBg    = if (compatible) BgCard else BgIncompat
    val nameColor = if (compatible) TextPrimary else TextDisabled
    val bodyColor = if (compatible) TextSubtle  else TextDisabled

    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(10.dp),
        colors   = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // ── Header row ────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = model.name,
                        color      = nameColor,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text    = "${model.family} · ${model.params}",
                        color   = bodyColor,
                        fontSize= 12.sp
                    )
                }

                Spacer(Modifier.width(8.dp))

                if (model.thinkingTrained) ThinkingBadge() else GeneralBadge()
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text    = model.bestFor,
                color   = bodyColor,
                fontSize= 12.sp,
                maxLines= 2,
                overflow= TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            // ── Stats row ─────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatChip("Min RAM", "%.1f GB".format(model.minRamGb), compatible)
                StatChip("Best at", "%.1f GB".format(model.recommendedRamGb), compatible)
                if (model.benchmarkNote.isNotBlank() && compatible) {
                    StatChip("Speed", model.benchmarkNote.substringBefore(" on"), true)
                }
            }

            // ── Incompatible reason ───────────────────────────────────────────
            if (!compatible) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text    = model.incompatibleReason(availableRamGb),
                    color   = Color(0xFFEF9A9A).copy(alpha = 0.7f),
                    fontSize= 11.sp
                )
            }

            // ── Download button (compatible models only) ──────────────────────
            if (compatible) {
                Spacer(Modifier.height(10.dp))
                when (downloadState) {
                    is DownloadState.Completed -> {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint     = Color(0xFF81C784),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text    = "Downloaded",
                                color   = Color(0xFF81C784),
                                fontSize= 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    else -> {
                        OutlinedButton(
                            onClick        = onDownloadClick,
                            modifier       = Modifier.fillMaxWidth().height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            colors         = ButtonDefaults.outlinedButtonColors(
                                contentColor = AccentAmber
                            )
                        ) {
                            Icon(
                                Icons.Filled.Download,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text     = "Download  ·  ${model.bestVariantFor(availableRamGb)?.displayLabel ?: ""}",
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThinkingBadge() {
    Row(
        modifier          = Modifier
            .background(Color(0xFF2A1F00), RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("⚡", fontSize = 11.sp)
        Text("Thinking", color = AccentAmber, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GeneralBadge() {
    Row(
        modifier          = Modifier
            .background(Color(0xFF1E1E1E), RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("💬", fontSize = 11.sp)
        Text("General", color = TextSubtle, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatChip(label: String, value: String, compatible: Boolean) {
    val textColor = if (compatible) AccentAmber else TextDisabled
    val bgColor   = if (compatible) Color(0xFF222222) else Color(0xFF1A1A1A)
    Column(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, color = TextSubtle.copy(alpha = if (compatible) 1f else 0.5f), fontSize = 9.sp)
        Text(value, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
