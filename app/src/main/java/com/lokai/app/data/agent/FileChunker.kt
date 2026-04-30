package com.lokai.app.data.agent

import com.lokai.app.model.AgentCategory
import com.lokai.app.model.FileChunk

/**
 * Splits cleaned file text into [FileChunk]s according to category strategy.
 *
 * Chunk sizes by default:
 * - Code:      N/A (full load — no chunking)
 * - Story:     250 words per body chunk
 * - Research:  200 words per body chunk
 * - Reference: 300 words per body chunk
 * - Custom:    user-defined
 *
 * Skeleton/summary chunks (isSkeleton=true) are extracted before body chunking
 * for Story and Research strategies.
 */
object FileChunker {

    data class ChunkedFile(
        val skeletonChunks: List<FileChunk>,  // always-loaded layer
        val bodyChunks:     List<FileChunk>   // indexed for retrieval
    ) {
        val allChunks: List<FileChunk> get() = skeletonChunks + bodyChunks
        val totalCount: Int get() = allChunks.size
    }

    fun chunk(
        agentId:      String,
        text:         String,
        category:     AgentCategory,
        customWordSize: Int = 250
    ): ChunkedFile {
        return when (category) {
            AgentCategory.CODE      -> chunkFullLoad(agentId, text)
            AgentCategory.STORY     -> chunkSkeleton(agentId, text, wordSize = 250)
            AgentCategory.RESEARCH  -> chunkSummary(agentId, text, wordSize = 200)
            AgentCategory.REFERENCE -> chunkPureRetrieval(agentId, text, wordSize = 300)
            AgentCategory.CUSTOM    -> chunkPureRetrieval(agentId, text, wordSize = customWordSize)
        }
    }

    // ─── Code: full load, single chunk ───────────────────────────────────────

    private fun chunkFullLoad(agentId: String, text: String): ChunkedFile {
        val single = FileChunk(
            agentId    = agentId,
            index      = 0,
            text       = text,
            isSkeleton = true  // treated as skeleton — always loaded
        )
        return ChunkedFile(skeletonChunks = listOf(single), bodyChunks = emptyList())
    }

    // ─── Story: skeleton (structure) + body chunks ───────────────────────────

    private fun chunkSkeleton(agentId: String, text: String, wordSize: Int): ChunkedFile {
        val lines = text.lines()
        val paragraphs = splitIntoParagraphs(lines)

        // Skeleton: first paragraph + last paragraph + all headings (lines starting with #)
        val headings = lines.filter { it.trimStart().startsWith("#") }
        val firstPara = paragraphs.firstOrNull() ?: ""
        val lastPara  = paragraphs.lastOrNull()  ?: ""

        val skeletonText = buildString {
            if (firstPara.isNotBlank()) { appendLine(firstPara); appendLine() }
            headings.forEach { appendLine(it) }
            if (lastPara != firstPara && lastPara.isNotBlank()) { appendLine(); append(lastPara) }
        }.trim()

        val skeletonChunks = if (skeletonText.isNotBlank()) {
            listOf(FileChunk(agentId = agentId, index = 0, text = skeletonText, isSkeleton = true))
        } else emptyList()

        // Body: everything between headings, split into word-count chunks
        val bodyText = buildBodyText(lines)
        val bodyChunks = splitByWords(bodyText, wordSize).mapIndexed { i, t ->
            FileChunk(agentId = agentId, index = skeletonChunks.size + i, text = t, isSkeleton = false)
        }

        return ChunkedFile(skeletonChunks = skeletonChunks, bodyChunks = bodyChunks)
    }

    // ─── Research: summary layer + body chunks ────────────────────────────────

    private fun chunkSummary(agentId: String, text: String, wordSize: Int): ChunkedFile {
        val lines = text.lines()
        val paragraphs = splitIntoParagraphs(lines)
        val headings = lines.filter { it.trimStart().startsWith("#") }

        // Summary: first 2 paragraphs + headings + last 2 paragraphs
        val first2 = paragraphs.take(2).joinToString("\n\n")
        val last2  = if (paragraphs.size > 2) paragraphs.takeLast(2).joinToString("\n\n") else ""

        val summaryText = buildString {
            if (first2.isNotBlank()) { appendLine(first2); appendLine() }
            headings.forEach { appendLine(it) }
            if (last2.isNotBlank() && last2 != first2) { appendLine(); append(last2) }
        }.trim()

        val skeletonChunks = if (summaryText.isNotBlank()) {
            listOf(FileChunk(agentId = agentId, index = 0, text = summaryText, isSkeleton = true))
        } else emptyList()

        // Body chunks from middle paragraphs
        val bodyParas = if (paragraphs.size > 4) paragraphs.drop(2).dropLast(2) else paragraphs
        val bodyText  = bodyParas.joinToString("\n\n")
        val bodyChunks = splitByWords(bodyText, wordSize).mapIndexed { i, t ->
            FileChunk(agentId = agentId, index = skeletonChunks.size + i, text = t, isSkeleton = false)
        }

        return ChunkedFile(skeletonChunks = skeletonChunks, bodyChunks = bodyChunks)
    }

    // ─── Reference / Custom: pure retrieval, no skeleton ─────────────────────

    private fun chunkPureRetrieval(agentId: String, text: String, wordSize: Int): ChunkedFile {
        val chunks = splitByWords(text, wordSize).mapIndexed { i, t ->
            FileChunk(agentId = agentId, index = i, text = t, isSkeleton = false)
        }
        return ChunkedFile(skeletonChunks = emptyList(), bodyChunks = chunks)
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun splitIntoParagraphs(lines: List<String>): List<String> {
        val paragraphs = mutableListOf<String>()
        val current    = StringBuilder()
        for (line in lines) {
            if (line.isBlank()) {
                val p = current.toString().trim()
                if (p.isNotEmpty()) paragraphs.add(p)
                current.clear()
            } else {
                if (current.isNotEmpty()) current.append(" ")
                current.append(line.trim())
            }
        }
        val last = current.toString().trim()
        if (last.isNotEmpty()) paragraphs.add(last)
        return paragraphs
    }

    private fun buildBodyText(lines: List<String>): String =
        lines.filter { !it.trimStart().startsWith("#") }.joinToString(" ")

    private fun splitByWords(text: String, wordsPerChunk: Int): List<String> {
        if (text.isBlank()) return emptyList()
        val words  = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        return words.chunked(wordsPerChunk).map { it.joinToString(" ") }
    }
}
