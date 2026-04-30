package com.lokai.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lokai.app.model.AgentSession
import com.lokai.app.model.ChatSession
import com.lokai.app.viewmodel.AgentViewModel
import com.lokai.app.viewmodel.SessionsViewModel
import java.text.SimpleDateFormat
import java.util.*

private val BgPage  = Color(0xFF0D0D0D)
private val BgCard  = Color(0xFF181818)
private val Amber   = Color(0xFFF5A623)
private val TextMain= Color(0xFFEEEEEE)
private val TextSub = Color(0xFF777777)

/** Unified entry for display: either a regular chat session or an agent session */
sealed class SessionEntry {
    data class Regular(val session: ChatSession) : SessionEntry() {
        val id:        String get() = session.id
        val updatedAt: Long   get() = session.updatedAt
    }
    data class Agent(val session: AgentSession) : SessionEntry() {
        val id:        String get() = session.id
        val updatedAt: Long   get() = session.updatedAt
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsListScreen(
    onOpenRegular: (ChatSession)  -> Unit = {},
    onOpenAgent:   (AgentSession) -> Unit = {}
) {
    val sessVm:  SessionsViewModel = viewModel()
    val agentVm: AgentViewModel    = viewModel()

    val regularSessions by sessVm.sessions.collectAsStateWithLifecycle()
    val agentSessions   by agentVm.allAgentSessions.collectAsStateWithLifecycle()

    // Merge and sort by updatedAt descending
    val allEntries: List<SessionEntry> = remember(regularSessions, agentSessions) {
        val combined = mutableListOf<SessionEntry>()
        regularSessions.forEach { combined.add(SessionEntry.Regular(it)) }
        agentSessions.forEach   { combined.add(SessionEntry.Agent(it)) }
        combined.sortedByDescending { when (it) {
            is SessionEntry.Regular -> it.updatedAt
            is SessionEntry.Agent   -> it.updatedAt
        }}
    }

    Scaffold(
        containerColor = BgPage,
        topBar = {
            TopAppBar(
                title  = { Text("History", color = TextMain, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPage),
                actions = {
                    if (allEntries.isNotEmpty()) {
                        IconButton(onClick = { sessVm.deleteAllSessions() }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear all",
                                 tint = TextSub)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (allEntries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💬", fontSize = 40.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("No conversations yet", color = TextMain, fontSize = 17.sp,
                         fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("Start a chat or open an agent to see history here.",
                         color = TextSub, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier       = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allEntries, key = { when (it) {
                    is SessionEntry.Regular -> "r_${it.id}"
                    is SessionEntry.Agent   -> "a_${it.id}"
                }}) { entry ->
                    when (entry) {
                        is SessionEntry.Regular -> RegularSessionCard(
                            session  = entry.session,
                            onClick  = { onOpenRegular(entry.session) },
                            onDelete = { sessVm.deleteSession(entry.session.id) }
                        )
                        is SessionEntry.Agent -> AgentSessionCard(
                            session  = entry.session,
                            onClick  = { onOpenAgent(entry.session) },
                            onDelete = { agentVm.deleteAgentSession(entry.session.id) }
                        )
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun RegularSessionCard(
    session:  ChatSession,
    onClick:  () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val lastMsg = session.messages.lastOrNull()?.content?.take(80) ?: "No messages"

    Card(
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF1F1A00), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) { Text("💬", fontSize = 18.sp) }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = session.modelName,
                        color      = TextMain,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text     = session.inferenceMode.label,
                        color    = Color(0xFF555555),
                        fontSize = 10.sp
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = lastMsg,
                    color    = TextSub,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = sdf.format(Date(session.updatedAt)),
                    color    = Color(0xFF444444),
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFF444444))
            }
        }
    }
}

@Composable
private fun AgentSessionCard(
    session:  AgentSession,
    onClick:  () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val lastMsg = session.messages.lastOrNull()?.content?.take(80) ?: "No messages"

    Card(
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF1A1500), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) { Text(session.category.emoji, fontSize = 18.sp) }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = session.agentName,
                        color      = TextMain,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text     = "Agent",
                        color    = Amber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = lastMsg,
                    color    = TextSub,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = sdf.format(Date(session.updatedAt)),
                    color    = Color(0xFF444444),
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFF444444))
            }
        }
    }
}
