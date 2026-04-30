package com.lokai.app.data.agent

import com.lokai.app.model.*

/**
 * Assembles the final prompt sent to llama.cpp for agent inference.
 *
 * Structure:
 *   [system prompt]
 *   [skeleton/summary layer — always present if applicable]
 *   [retrieved chunks — labelled by strategy]
 *   [conversation history — trimmed from oldest if context overflows]
 *   [current user message]
 *
 * Token budget enforced throughout — skeleton is never trimmed.
 */
object ContextBuilder {

    data class BuiltContext(
        val prompt:       String,
        val tokensUsed:   Int,
        val tokensMax:    Int,
        val retrievedCount: Int,
        val historyTurnsIncluded: Int
    )

    /**
     * Build the full prompt for an agent message.
     *
     * @param agent           The agent profile
     * @param skeletonChunks  Always-loaded chunks (skeleton / summary)
     * @param retrievedChunks TF-IDF retrieved body chunks for this query
     * @param history         Full conversation history (oldest first)
     * @param userMessage     The current user input
     * @param maxTokens       Context window size (from settings)
     */
    fun build(
        agent:           AgentProfile,
        skeletonChunks:  List<FileChunk>,
        retrievedChunks: List<FileChunk>,
        history:         List<ChatMessage>,
        userMessage:     String,
        maxTokens:       Int = 2048
    ): BuiltContext {

        val systemPrompt    = agent.systemPrompt.trim()
        val skeletonText    = skeletonChunks.joinToString("\n\n") { it.text }
        val retrievedText   = if (retrievedChunks.isNotEmpty()) {
            "--- Retrieved context ---\n" +
            retrievedChunks.mapIndexed { i, c -> "[Section ${i+1}]\n${c.text}" }.joinToString("\n\n") +
            "\n--- End of retrieved context ---"
        } else ""

        // Reserve tokens for mandatory sections
        val sysTokens      = estimateTokens(systemPrompt)
        val skeletonTokens = estimateTokens(skeletonText)
        val retrievedTokens= estimateTokens(retrievedText)
        val userTokens     = estimateTokens(userMessage)
        // Reserve 20% for the model's response
        val responseReserve = (maxTokens * 0.20).toInt()

        var budget = maxTokens - sysTokens - skeletonTokens - retrievedTokens -
                     userTokens - responseReserve - 50 // formatting overhead

        // Fit as many history turns as budget allows, starting from most recent
        val fittingHistory = mutableListOf<ChatMessage>()
        for (msg in history.asReversed()) {
            val tokens = estimateTokens(msg.content)
            if (budget - tokens < 0) break
            budget -= tokens
            fittingHistory.add(0, msg)
        }

        val sb = StringBuilder()

        if (systemPrompt.isNotBlank()) {
            sb.appendLine("<<SYS>>")
            sb.appendLine(systemPrompt)
            sb.appendLine("<</SYS>>")
            sb.appendLine()
        }

        if (skeletonText.isNotBlank()) {
            sb.appendLine("=== Document Context ===")
            sb.appendLine(skeletonText)
            sb.appendLine("=== End Document Context ===")
            sb.appendLine()
        }

        if (retrievedText.isNotBlank()) {
            sb.appendLine(retrievedText)
            sb.appendLine()
        }

        for (msg in fittingHistory) {
            val role = if (msg.isUser) "User" else "Assistant"
            sb.appendLine("$role: ${msg.content}")
        }

        sb.append("User: $userMessage\nAssistant:")

        val finalPrompt = sb.toString()
        val tokensUsed  = estimateTokens(finalPrompt)

        return BuiltContext(
            prompt                = finalPrompt,
            tokensUsed            = tokensUsed,
            tokensMax             = maxTokens,
            retrievedCount        = retrievedChunks.size,
            historyTurnsIncluded  = fittingHistory.size / 2
        )
    }

    /** 1 token ≈ 4 characters */
    fun estimateTokens(text: String): Int = (text.length / 4).coerceAtLeast(0)
}
