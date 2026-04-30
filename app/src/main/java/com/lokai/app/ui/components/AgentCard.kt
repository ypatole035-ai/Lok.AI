package com.lokai.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lokai.app.model.AgentProfile
import java.text.SimpleDateFormat
import java.util.*

private val BgCard     = Color(0xFF181818)
private val BgPage     = Color(0xFF0D0D0D)
private val Amber      = Color(0xFFF5A623)
private val TextMain   = Color(0xFFEEEEEE)
private val TextSubtle = Color(0xFF777777)
private val DividerCol = Color(0xFF252525)

@Composable
fun AgentCard(
    agent:    AgentProfile,
    onClick:  () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Agent?", color = TextMain) },
            text  = { Text("\"${agent.name}\" and all its indexed data will be removed. Chat history is kept.", color = TextSubtle) },
            containerColor = BgCard,
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Delete", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSubtle)
                }
            }
        )
    }

    Card(
        modifier  = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category emoji badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF222222), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(agent.category.emoji, fontSize = 22.sp)
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = agent.name,
                    color      = TextMain,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text     = agent.category.displayName,
                        color    = Amber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (agent.fileName != null) {
                        Text(" · ", color = TextSubtle, fontSize = 11.sp)
                        Text(
                            text     = agent.fileName,
                            color    = TextSubtle,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                val sdf = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
                Text(
                    text     = "Last used: ${sdf.format(Date(agent.lastUsedAt))}",
                    color    = TextSubtle,
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = TextSubtle)
            }
        }
    }
}

@Composable
fun CategoryBadge(
    emoji: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg    = if (selected) Color(0xFF2A2000) else Color(0xFF181818)
    val border = if (selected) Amber else Color(0xFF333333)
    val textCol= if (selected) Amber else TextSubtle

    Surface(
        modifier  = modifier.clickable(onClick = onClick),
        shape     = RoundedCornerShape(10.dp),
        color     = bg,
        border    = androidx.compose.foundation.BorderStroke(1.dp, border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(label, color = textCol, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}
