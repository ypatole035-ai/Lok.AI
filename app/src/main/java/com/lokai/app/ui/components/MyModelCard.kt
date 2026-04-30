package com.lokai.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lokai.app.model.DownloadedModel

private val BgCard      = Color(0xFF1A1A1A)
private val AccentAmber = Color(0xFFF5A623)
private val TextPrimary = Color(0xFFE0E0E0)
private val TextSubtle  = Color(0xFF888888)

/**
 * Card for a fully downloaded model.
 *
 * - Long-press triggers delete confirmation
 * - Chat and Agent action buttons
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyModelCard(
    model: DownloadedModel,
    onChatClick: (DownloadedModel) -> Unit,
    onAgentClick: (DownloadedModel) -> Unit,
    onLongPress: (DownloadedModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick      = { /* no-op — use buttons */ },
                onLongClick  = { onLongPress(model) }
            ),
        shape  = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
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
                        color      = TextPrimary,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text    = model.displayQuant,
                        color   = TextSubtle,
                        fontSize= 12.sp
                    )
                }

                Spacer(Modifier.width(8.dp))

                if (model.thinkingTrained) {
                    Row(
                        modifier          = Modifier
                            .background(Color(0xFF2A1F00), RoundedCornerShape(5.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("⚡", fontSize = 11.sp)
                        Text(
                            text       = "Thinking",
                            color      = AccentAmber,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Stats row ─────────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill("RAM", "%.1f GB".format(model.ramRequiredGb))
                if (model.avgTokensPerSec > 0f) {
                    StatPill("Speed", "%.0f tok/s".format(model.avgTokensPerSec))
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Action buttons ────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick  = { onAgentClick(model) },
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextSubtle)
                ) {
                    Icon(
                        Icons.Filled.SmartToy,
                        contentDescription = null,
                        modifier           = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("Agent", fontSize = 12.sp)
                }

                Button(
                    onClick  = { onChatClick(model) },
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = AccentAmber,
                        contentColor   = Color.Black
                    )
                ) {
                    Icon(
                        Icons.Filled.Chat,
                        contentDescription = null,
                        modifier           = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ── Long-press hint ───────────────────────────────────────────────
            Spacer(Modifier.height(6.dp))
            Text(
                text    = "Hold to delete",
                color   = Color(0xFF444444),
                fontSize= 10.sp
            )
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Row(
        modifier          = Modifier
            .background(Color(0xFF222222), RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, color = TextSubtle, fontSize = 9.sp)
        Text(text = value, color = AccentAmber, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
