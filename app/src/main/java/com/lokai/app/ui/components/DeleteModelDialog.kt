package com.lokai.app.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

private val BgDialog    = Color(0xFF1E1E1E)
private val TextPrimary = Color(0xFFE0E0E0)
private val TextSubtle  = Color(0xFF888888)
private val ColorRed    = Color(0xFFEF5350)

/**
 * Confirmation dialog before deleting a downloaded model.
 * Shown when user long-presses a MyModelCard.
 */
@Composable
fun DeleteModelDialog(
    modelName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest  = onDismiss,
        containerColor    = BgDialog,
        title = {
            Text(
                text       = "Delete model?",
                color      = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text  = "\"$modelName\" will be permanently deleted from your device. You can re-download it later.",
                color = TextSubtle
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors  = ButtonDefaults.textButtonColors(contentColor = ColorRed)
            ) {
                Text("Delete", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors  = ButtonDefaults.textButtonColors(contentColor = TextSubtle)
            ) {
                Text("Cancel")
            }
        }
    )
}
