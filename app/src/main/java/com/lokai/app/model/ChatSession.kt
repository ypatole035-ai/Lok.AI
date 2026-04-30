package com.lokai.app.model

import com.lokai.app.data.inference.InferenceMode

/**
 * A single chat message in a session.
 *
 * @param id          Unique ID for this message (used as Compose list key)
 * @param role        "user" or "assistant"
 * @param content     The message text content
 * @param thinkingLog Log entries captured during this response (assistant only)
 * @param thinkingMs  How long generation took in ms (assistant only)
 * @param timestampMs When this message was created
 */
data class ChatMessage(
    val id:          String = java.util.UUID.randomUUID().toString(),
    val role:        String,           // "user" | "assistant"
    val content:     String,
    val thinkingLog: List<ThinkingLog> = emptyList(),
    val thinkingMs:  Long = 0L,
    val timestampMs: Long = System.currentTimeMillis()
) {
    val isUser:      Boolean get() = role == "user"
    val isAssistant: Boolean get() = role == "assistant"

    val thinkingSeconds: Float get() = thinkingMs / 1000f
}

/**
 * A complete chat session — a list of messages plus metadata.
 *
 * @param id            Unique session ID
 * @param modelId       The model used in this session
 * @param modelName     Display name of the model (denormalized for quick display)
 * @param inferenceMode The mode this session uses (persisted per session)
 * @param messages      Ordered list of messages, oldest first
 * @param createdAt     When this session started
 * @param updatedAt     When the last message was added
 */
data class ChatSession(
    val id:            String = java.util.UUID.randomUUID().toString(),
    val modelId:       String,
    val modelName:     String,
    val inferenceMode: InferenceMode = InferenceMode.NORMAL,
    val messages:      List<ChatMessage> = emptyList(),
    val createdAt:     Long = System.currentTimeMillis(),
    val updatedAt:     Long = System.currentTimeMillis()
)
