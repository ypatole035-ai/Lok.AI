package com.lokai.app.data.agent

import android.util.Log
import com.lokai.app.model.FileChunk
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.math.ln
import kotlin.math.sqrt

private const val TAG = "TfIdfEngine"

/**
 * Fully on-device TF-IDF indexing and similarity retrieval.
 *
 * No embedding model. No server. No internet.
 *
 * ## At agent creation (indexing):
 * 1. Each chunk's text is tokenised and cleaned
 * 2. TF (term frequency) + IDF (inverse document frequency) computed
 * 3. TF-IDF vector serialised as JSON into FileChunk.tfidfJson
 *
 * ## At each user message (retrieval):
 * 1. Query tokenised and cleaned
 * 2. TF-IDF similarity computed against all stored chunk vectors
 * 3. Top N chunks returned, ranked by cosine similarity
 */
object TfIdfEngine {

    private val jsonSer = Json { ignoreUnknownKeys = true }

    // ─── Indexing ─────────────────────────────────────────────────────────────

    /**
     * Compute TF-IDF vectors for all [chunks] and return them updated
     * with populated [FileChunk.tfidfJson].
     */
    fun index(chunks: List<FileChunk>): List<FileChunk> {
        if (chunks.isEmpty()) return chunks
        Log.d(TAG, "Indexing ${chunks.size} chunks")

        val tokenised = chunks.map { tokenize(it.text) }

        // Document frequency: how many chunks contain each term
        val df = mutableMapOf<String, Int>()
        tokenised.forEach { tokens ->
            tokens.toSet().forEach { term -> df[term] = (df[term] ?: 0) + 1 }
        }

        val n = chunks.size.toDouble()
        return chunks.mapIndexed { i, chunk ->
            val tokens = tokenised[i]
            val tf = computeTf(tokens)
            val tfidf = tf.mapValues { (term, tfVal) ->
                val idf = ln((n + 1.0) / ((df[term] ?: 1).toDouble() + 1.0)) + 1.0
                tfVal * idf
            }
            val norm = normalize(tfidf)
            chunk.copy(tfidfJson = jsonSer.encodeToString(norm))
        }
    }

    // ─── Retrieval ────────────────────────────────────────────────────────────

    /**
     * Retrieve the top [topN] chunks from [indexedChunks] most similar to [query].
     * Only searches chunks where [FileChunk.isSkeleton] == false (body chunks).
     * Returns chunks sorted by descending similarity, filtered to score > [minScore].
     */
    fun retrieve(
        query:         String,
        indexedChunks: List<FileChunk>,
        topN:          Int   = 3,
        minScore:      Float = 0.01f
    ): List<FileChunk> {
        val bodyChunks = indexedChunks.filter { !it.isSkeleton }
        if (bodyChunks.isEmpty()) return emptyList()

        val queryTokens = tokenize(query)
        if (queryTokens.isEmpty()) return emptyList()

        val queryTf    = computeTf(queryTokens)
        // Compute IDF from all chunks (body + skeleton for accurate IDF)
        val allTokenised = indexedChunks.map { tokenize(it.text) }
        val df = mutableMapOf<String, Int>()
        allTokenised.forEach { tokens -> tokens.toSet().forEach { df[it] = (df[it] ?: 0) + 1 } }
        val n = indexedChunks.size.toDouble()
        val queryTfIdf = queryTf.mapValues { (term, tfVal) ->
            val idf = ln((n + 1.0) / ((df[term] ?: 1).toDouble() + 1.0)) + 1.0
            tfVal * idf
        }
        val queryNorm = normalize(queryTfIdf)

        return bodyChunks
            .mapNotNull { chunk ->
                if (chunk.tfidfJson.isBlank()) return@mapNotNull null
                val chunkVec = try {
                    jsonSer.decodeFromString<Map<String, Double>>(chunk.tfidfJson)
                } catch (e: Exception) { return@mapNotNull null }
                val score = cosineSimilarity(queryNorm, chunkVec).toFloat()
                Pair(chunk, score)
            }
            .filter { (_, score) -> score >= minScore }
            .sortedByDescending { (_, score) -> score }
            .take(topN)
            .map { (chunk, _) -> chunk }
    }

    /**
     * Broader retrieval with lower threshold — used for fallback when uncertainty
     * signals are detected in the model response.
     */
    fun retrieveBroader(
        query:         String,
        indexedChunks: List<FileChunk>,
        topN:          Int   = 5,
        minScore:      Float = 0.005f
    ): List<FileChunk> = retrieve(query, indexedChunks, topN, minScore)

    // ─── Uncertainty detection ────────────────────────────────────────────────

    private val UNCERTAINTY_SIGNALS = listOf(
        "i don't have", "i'm not sure", "that wasn't in", "i can't find",
        "not mentioned", "i don't know", "no information", "not provided",
        "can't determine", "unclear from", "not available", "no mention of"
    )

    fun containsUncertainty(response: String): Boolean {
        val lower = response.lowercase()
        return UNCERTAINTY_SIGNALS.any { lower.contains(it) }
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    private val STOPWORDS = setOf(
        "a", "an", "the", "and", "or", "but", "in", "on", "at", "to", "for",
        "of", "with", "by", "from", "is", "it", "this", "that", "was", "are",
        "be", "as", "at", "so", "we", "he", "she", "they", "you", "i", "not",
        "do", "did", "have", "has", "had", "will", "would", "could", "should",
        "may", "might", "can", "its", "into", "than", "then", "if", "been"
    )

    internal fun tokenize(text: String): List<String> {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in STOPWORDS }
    }

    private fun computeTf(tokens: List<String>): Map<String, Double> {
        if (tokens.isEmpty()) return emptyMap()
        val counts = mutableMapOf<String, Int>()
        tokens.forEach { counts[it] = (counts[it] ?: 0) + 1 }
        val max = counts.values.maxOrNull()?.toDouble() ?: 1.0
        return counts.mapValues { (_, count) -> count.toDouble() / max }
    }

    private fun normalize(vec: Map<String, Double>): Map<String, Double> {
        val magnitude = sqrt(vec.values.sumOf { it * it })
        return if (magnitude == 0.0) vec
        else vec.mapValues { (_, v) -> v / magnitude }
    }

    private fun cosineSimilarity(a: Map<String, Double>, b: Map<String, Double>): Double {
        var dot = 0.0
        for ((term, aVal) in a) {
            val bVal = b[term] ?: continue
            dot += aVal * bVal
        }
        // Both should already be normalised, but guard against edge cases
        return dot
    }
}
