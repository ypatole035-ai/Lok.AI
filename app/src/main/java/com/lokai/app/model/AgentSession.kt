package com.lokai.app.model

import com.lokai.app.data.inference.InferenceMode

/**
 * A saved agent chat session — messages exchanged with a specific Named Agent.
 * Stored separately from regular ChatSession so they can be tagged and filtered.
 */
data class AgentSession(
    val id:         String        = java.util.UUID.randomUUID().toString(),
    val agentId:    String,
    val agentName:  String,
    val category:   AgentCategory,
    val modelId:    String,
    val modelName:  String,
    val inferenceMode: InferenceMode = InferenceMode.NORMAL,
    val messages:   List<ChatMessage> = emptyList(),
    val createdAt:  Long = System.currentTimeMillis(),
    val updatedAt:  Long = System.currentTimeMillis()
)
