package com.lokai.app.data.agent.strategies

import com.lokai.app.data.agent.ContextBuilder
import com.lokai.app.data.agent.TfIdfEngine
import com.lokai.app.model.*

/** Story Agent — Skeleton-First + On-Demand Deep Dive */
object StoryStrategy {
    fun build(
        agent:       AgentProfile,
        fileChunks:  List<FileChunk>,
        history:     List<ChatMessage>,
        userMessage: String,
        maxTokens:   Int
    ): ContextBuilder.BuiltContext {
        val skeleton  = fileChunks.filter { it.isSkeleton }
        val retrieved = TfIdfEngine.retrieve(userMessage, fileChunks, topN = 3)
        return ContextBuilder.build(
            agent           = agent,
            skeletonChunks  = skeleton,
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
        val skeleton   = fileChunks.filter { it.isSkeleton }
        val broader    = TfIdfEngine.retrieveBroader(userMessage, fileChunks, topN = 5)
        val combined   = (alreadyRetrieved + broader).distinctBy { it.id }.take(5)
        return ContextBuilder.build(
            agent           = agent,
            skeletonChunks  = skeleton,
            retrievedChunks = combined,
            history         = history,
            userMessage     = userMessage,
            maxTokens       = maxTokens
        )
    }
}
