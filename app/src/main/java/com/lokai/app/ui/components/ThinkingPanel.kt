package com.lokai.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lokai.app.model.ThinkingLog

private val BgThinking  = Color(0xFF161616)
private val BorderColor = Color(0xFF333333)
private val TextDim     = Color(0xFF888888)
private val TextLog     = Color(0xFF9E9E9E)
private val AccentAmber = Color(0xFFF5A623)

/**
 * Collapsible thinking/reasoning panel shown above each assistant message.
 *
 * Behaviour:
 * - While generating: shows live "⚙️ Thinking..." with real-time log entries streaming in.
 * - After generation: collapsed by default, showing "⚙️ Thought for Xs ∨".
 * - Tap ∨ to expand, tap ∧ to collapse.
 *
 * @param isGenerating  True while inference is in progress
 * @param log           Log entries (live during generation, final after)
 * @param thinkingMs    Elapsed milliseconds (0 during generation)
 */
@Composable
fun ThinkingPanel(
    isGenerating: Boolean,
    log:          List<ThinkingLog>,
    thinkingMs:   Long,
    modifier:     Modifier = Modifier
) {
    if (log.isEmpty() && !isGenerating) return

    var expanded by remember { mutableStateOf(false) }

    // Auto-expand while generating, auto-collapse when done
    LaunchedEffect(isGenerating) {
        expanded = isGenerating
    }

    val thinkingSec = thinkingMs / 1000f
    val headerText = when {
        isGenerating -> "⚙️ Thinking..."
        else         -> "⚙️ Thought for %.1fs".format(thinkingSec)
    }
    val toggleChar = if (expanded) "∧" else "∨"

    Column(modifier = modifier) {
        // ── Left-border card ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(BgThinking)
        ) {
            // Left accent border
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(BorderColor)
            )

            Column(modifier = Modifier.fillMaxWidth()) {

                // ── Header row (always visible) ───────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isGenerating) { expanded = !expanded }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text       = headerText,
                        color      = TextDim,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (!isGenerating) {
                        Text(
                            text     = toggleChar,
                            color    = TextDim,
                            fontSize = 13.sp
                        )
                    }
                }

                // ── Expandable log body ───────────────────────────────────────
                AnimatedVisibility(
                    visible = expanded,
                    enter   = expandVertically(),
                    exit    = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier.padding(
                            start  = 12.dp,
                            end    = 12.dp,
                            bottom = 10.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        log.forEach { entry ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment     = Alignment.Top
                            ) {
                                Text(
                                    text     = "•",
                                    color    = TextDim,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text       = entry.message,
                                    color      = TextLog,
                                    fontSize   = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Simplified "live" variant shown inline during generation before the message is committed.
 * Used in the streaming state — shows current log + animated status.
 */
@Composable
fun ThinkingPanelLive(
    log:      List<ThinkingLog>,
    modifier: Modifier = Modifier
) {
    ThinkingPanel(
        isGenerating = true,
        log          = log,
        thinkingMs   = 0L,
        modifier     = modifier
    )
}
