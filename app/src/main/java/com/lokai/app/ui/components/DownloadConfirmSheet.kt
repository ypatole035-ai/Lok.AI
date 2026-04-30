package com.lokai.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lokai.app.model.ModelEntry
import com.lokai.app.model.ModelVariant

private val BgSheet     = Color(0xFF181818)
private val BgRow       = Color(0xFF222222)
private val AccentAmber = Color(0xFFF5A623)
private val TextPrimary = Color(0xFFE0E0E0)
private val TextSubtle  = Color(0xFF888888)
private val BgDivider   = Color(0xFF2A2A2A)

/**
 * Bottom sheet confirming a model download before it begins.
 *
 * Displays: variant, size, RAM requirement, thinking badge if applicable.
 * Confirm → starts download. Dismiss → closes sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadConfirmSheet(
    model: ModelEntry,
    variant: ModelVariant,
    availableRamGb: Float,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = BgSheet,
        shape            = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Title ─────────────────────────────────────────────────────────
            Text(
                text       = "Download Model",
                color      = TextPrimary,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // ── Model name + badge ────────────────────────────────────────────
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text       = model.name,
                    color      = TextPrimary,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.weight(1f)
                )
                if (model.thinkingTrained) {
                    ThinkingBadgeSmall()
                }
            }

            Text(
                text    = model.bestFor,
                color   = TextSubtle,
                fontSize= 13.sp
            )

            HorizontalDivider(color = BgDivider, thickness = 1.dp)

            // ── Detail rows ───────────────────────────────────────────────────
            DetailRow("Quantization", variant.quant)
            DetailRow("Download size", variant.displayLabel.substringAfter("· ").substringBefore(" · needs"))
            DetailRow("Needs RAM", "%.1f GB".format(variant.ramRequiredGb))
            DetailRow("Your RAM", "%.1f GB available".format(availableRamGb))

            // RAM fit indicator
            val ramOk = availableRamGb >= variant.ramRequiredGb
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .background(
                        if (ramOk) Color(0xFF0A2010) else Color(0xFF200A0A),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(if (ramOk) "✓" else "⚠", fontSize = 14.sp)
                Text(
                    text    = if (ramOk)
                        "This model should load comfortably on your device."
                    else
                        "Your device may have limited RAM for this variant. It might still load.",
                    color   = if (ramOk) Color(0xFF81C784) else Color(0xFFFFB74D),
                    fontSize= 12.sp
                )
            }

            HorizontalDivider(color = BgDivider, thickness = 1.dp)

            // ── Action buttons ────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextSubtle)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick  = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = AccentAmber,
                        contentColor   = Color.Black
                    )
                ) {
                    Text("Download", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .background(BgRow, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSubtle, fontSize = 13.sp)
        Text(text = value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ThinkingBadgeSmall() {
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
