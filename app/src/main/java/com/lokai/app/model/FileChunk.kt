package com.lokai.app.model

/**
 * A single chunk of text extracted from an agent's attached file.
 *
 * Chunks are created at agent creation time and stored in the Room DB.
 * At query time, TF-IDF similarity is computed against all chunks for
 * the same agent to retrieve the most relevant ones.
 */
data class FileChunk(
    val id:        String = java.util.UUID.randomUUID().toString(),
    val agentId:   String,
    /** Zero-based sequential index within the file */
    val index:     Int,
    /** The raw text content of this chunk */
    val text:      String,
    /**
     * TF-IDF vector stored as a JSON-serialised map of term→weight.
     * Empty string until indexing completes.
     */
    val tfidfJson: String = "",
    /** True if this chunk is part of the skeleton/summary layer (always loaded) */
    val isSkeleton: Boolean = false
)
