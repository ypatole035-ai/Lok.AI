package com.lokai.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lokai.app.model.AgentProfile
import com.lokai.app.ui.components.AgentCard
import com.lokai.app.viewmodel.AgentViewModel

private val BgPage  = Color(0xFF0D0D0D)
private val Amber   = Color(0xFFF5A623)
private val TextSub = Color(0xFF555555)
private val TextMain= Color(0xFFEEEEEE)

@Composable
fun AgentListScreen(
    onCreateAgent: () -> Unit = {},
    onOpenAgent:   (AgentProfile) -> Unit = {}
) {
    val vm:    AgentViewModel = viewModel()
    val state by vm.listState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BgPage,
        floatingActionButton = {
            FloatingActionButton(
                onClick          = onCreateAgent,
                containerColor   = Amber,
                contentColor     = Color.Black
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Agent")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text       = "Agents",
                    color      = TextMain,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.weight(1f)
                )
                Text(
                    text     = "${state.agents.size} agent${if (state.agents.size != 1) "s" else ""}",
                    color    = TextSub,
                    fontSize = 13.sp
                )
            }

            when {
                state.loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Amber)
                    }
                }
                state.agents.isEmpty() -> {
                    EmptyAgentsState(onCreateAgent = onCreateAgent)
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.agents, key = { it.id }) { agent ->
                            AgentCard(
                                agent    = agent,
                                onClick  = { onOpenAgent(agent) },
                                onDelete = { vm.deleteAgent(agent.id) }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) } // FAB clearance
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAgentsState(onCreateAgent: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🤖", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text       = "No agents yet",
                color      = TextMain,
                fontSize   = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text      = "Create an agent and attach a file.\nAsk questions about your code, documents, or stories.",
                color     = TextSub,
                fontSize  = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onCreateAgent,
                colors  = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Color.Black)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("New Agent", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
