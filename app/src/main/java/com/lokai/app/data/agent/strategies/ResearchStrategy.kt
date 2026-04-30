package com.lokai.app.data.agent.strategies

import com.lokai.app.data.agent.ContextBuilder
import com.lokai.app.data.agent.TfIdfEngine
import com.lokai.app.model.*

/** Research Agent — Summary-First + Retrieval */
object ResearchStrategy {
    fun build(
        agent:       AgentProfile,
        fileChunks:  List<FileChunk>,
        history:     List<ChatMessage>,
        userMessage: String,
        maxTokens:   Int
    ): ContextBuilder.BuiltContext {
        val summary   = fileChunks.filter { it.isSkeleton }
        val retrieved = TfIdfEngine.retrieve(userMessage, fileChunks, topN = 4)
        return ContextBuilder.build(
            agent           = agent,
            skeletonChunks  = summary,
            retrievedChunks = retrieved,
            history         = history,
            userMessage     = userMessage,
            maxTokens       = maxTokens
        )
    }

    fun buildFallback(
        agent:       AgentProfile,
        fileChunks:  List<FileChunk>,
        history:     List<ChatMessage>,
        userMessage: String,
        maxTokens:   Int,
        alreadyRetrieved: List<FileChunk>
    ): ContextBuilder.BuiltContext {
        val summary  = fileChunks.filter { it.isSkeleton }
        val broader  = TfIdfEngine.retrieveBroader(userMessage, fileChunks, topN = 6)
        val combined = (alreadyRetrieved + broader).distinctBy { it.id }.take(6)
        return ContextBuilder.build(
            agent           = agent,
            skeletonChunks  = summary,
            retrievedChunks = combined,
            history         = history,
            userMessage     = userMessage,
            maxTokens       = maxTokens
        )
    }
}
