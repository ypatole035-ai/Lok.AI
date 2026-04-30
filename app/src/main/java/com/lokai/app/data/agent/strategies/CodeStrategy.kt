package com.lokai.app.data.agent.strategies

import com.lokai.app.data.agent.ContextBuilder
import com.lokai.app.model.*

/**
 * Code Agent strategy — Full Load.
 *
 * Every line of code matters. The entire file is always in context.
 * If it overflows, the user gets a clear warning with the exact token count.
 */
object CodeStrategy {

    data class Result(
        val context:         ContextBuilder.BuiltContext,
        val overflowWarning: String?  // non-null if file + settings overflow context
    )

    fun build(
        agent:        AgentProfile,
        fileChunks:   List<FileChunk>,
        history:      List<ChatMessage>,
        userMessage:  String,
        maxTokens:    Int
    ): Result {
        // Code strategy: the single full-load chunk is always the skeleton
        val fullChunk  = fileChunks.filter { it.isSkeleton }
        val fileTokens = fullChunk.sumOf { ContextBuilder.estimateTokens(it.text) }

        val overflow: String? = if (fileTokens > (maxTokens * 0.70).toInt()) {
            "⚠️ File is approximately $fileTokens tokens, which uses " +
            "${(fileTokens * 100.0 / maxTokens).toInt()}% of your $maxTokens-token context.\n" +
            "Consider splitting large codebases into one agent per file or per module."
        } else null

        val ctx = ContextBuilder.build(
            agent           = agent,
            skeletonChunks  = fullChunk,
            retrievedChunks = emptyList(),
            history         = history,
            userMessage     = userMessage,
            maxTokens       = maxTokens
        )

        return Result(context = ctx, overflowWarning = overflow)
    }
}
