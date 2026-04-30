package com.lokai.app.data.agent.strategies

import com.lokai.app.data.agent.ContextBuilder
import com.lokai.app.data.agent.TfIdfEngine
import com.lokai.app.model.*

/** Reference Agent — Pure Retrieval. Nothing pre-loaded; 5 chunks per query. */
object ReferenceStrategy {
    fun build(
        agent:       AgentProfile,
        fileChunks:  List<FileChunk>,
        history:     List<ChatMessage>,
        userMessage: String,
        maxTokens:   Int
    ): ContextBuilder.BuiltContext {
        val retrieved = TfIdfEngine.retrieve(userMessage, fileChunks, topN = 5)
        return ContextBuilder.build(
            agent           = agent,
            skeletonChunks  = emptyList(),
            retrievedChunks = retrieved,
            history         = history,
            userMessage     = userMessage,
            maxTokens       = maxTokens
        )
    }
    // Reference has no fallback — user asks more specific question
}

/**
 * Custom Agent — fully user-configured.
 *
 * Reading strategy name maps to behaviour:
 * - "Full Load"          → like Code (no retrieval)
 * - "Skeleton+Retrieval" → like Story
 * - "Summary+Retrieval"  → like Research
 * - "Pure Retrieval"     → like Reference
 */
object CustomStrategy {
    fun build(
        agent:       AgentProfile,
        fileChunks:  List<FileChunk>,
        history:     List<ChatMessage>,
        userMessage: String,
        maxTokens:   Int
    ): ContextBuilder.BuiltContext {
        val chunksN  = agent.customChunksRetrieved
        val skeleton = when (agent.customStrategy) {
            "Full Load", "Skeleton+Retrieval", "Summary+Retrieval" ->
                fileChunks.filter { it.isSkeleton }
            else -> emptyList()
        }
        val retrieved = when (agent.customStrategy) {
            "Full Load" -> emptyList()
            else        -> TfIdfEngine.retrieve(userMessage, fileChunks, topN = chunksN)
        }
        return ContextBuilder.build(
            agent           = agent,
            skeletonChunks  = skeleton,
            retrievedChunks = retrieved,
            history         = history,
            userMessage     = userMessage,
            maxTokens       = maxTokens
        )
    }
}
