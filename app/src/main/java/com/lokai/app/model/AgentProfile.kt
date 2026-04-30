package com.lokai.app.model

import com.lokai.app.data.inference.InferenceMode

/**
 * A saved Named Agent profile.
 *
 * An agent is an AI assistant tied to a specific model and optional file,
 * with a context strategy chosen by category. Agents persist across sessions.
 */
data class AgentProfile(
    val id:            String        = java.util.UUID.randomUUID().toString(),
    val name:          String,
    val category:      AgentCategory,
    val modelId:       String,
    val modelName:     String,
    /** Absolute path to the attached file, null if no file attached */
    val filePath:      String?       = null,
    val fileName:      String?       = null,
    /** Auto-generated from category; user may edit */
    val systemPrompt:  String,
    val inferenceMode: InferenceMode = InferenceMode.NORMAL,
    // Custom strategy overrides (only used when category == CUSTOM)
    val customChunkSize:      Int    = 250,
    val customChunksRetrieved:Int    = 3,
    val customFallback:       Boolean= true,
    val customTemperature:    Float  = 0.7f,
    val customMaxTokens:      Int    = 512,
    val customContextSize:    Int    = 2048,
    // Custom reading strategy name when category == CUSTOM
    val customStrategy:       String = "Summary+Retrieval",
    val createdAt:     Long   = System.currentTimeMillis(),
    val lastUsedAt:    Long   = System.currentTimeMillis()
)
