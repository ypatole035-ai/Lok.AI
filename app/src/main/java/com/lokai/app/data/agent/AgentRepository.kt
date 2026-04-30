package com.lokai.app.data.agent

import android.content.Context
import android.util.Log
import com.lokai.app.data.session.LokaiDatabase
import com.lokai.app.data.session.toEntity
import com.lokai.app.data.session.toDomain
import com.lokai.app.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private const val TAG = "AgentRepository"

/**
 * CRUD for [AgentProfile]s, [FileChunk]s, and [AgentSession]s.
 *
 * Also handles the background file-indexing pipeline:
 * 1. Read file via [FileProcessor]
 * 2. Chunk via [FileChunker]
 * 3. Index via [TfIdfEngine]
 * 4. Persist chunks to Room DB
 *
 * Progress is reported via a lambda to drive UI updates.
 */
class AgentRepository(private val context: Context) {

    private val db get() = LokaiDatabase.getInstance(context)

    // ─── Agent profiles ───────────────────────────────────────────────────────

    fun observeAll(): Flow<List<AgentProfile>> =
        db.agentDao().observeAll().map { it.map { e -> e.toDomain() } }

    suspend fun getById(id: String): AgentProfile? =
        db.agentDao().getById(id)?.toDomain()

    suspend fun save(agent: AgentProfile) =
        db.agentDao().upsert(agent.toEntity())

    suspend fun delete(agentId: String) {
        db.agentDao().delete(agentId)
        db.chunkDao().deleteByAgent(agentId)
        // Agent sessions are kept for history but lose live context
    }

    suspend fun deleteAll() {
        db.agentDao().deleteAll()
    }

    suspend fun touchLastUsed(agentId: String) =
        db.agentDao().updateLastUsed(agentId, System.currentTimeMillis())

    // ─── File indexing pipeline ───────────────────────────────────────────────

    /**
     * Process and index the file attached to [agent].
     *
     * Reports progress via [onProgress] with a message describing each step.
     * Returns null on success, or an error message string on failure.
     */
    suspend fun indexAgentFile(
        agent:      AgentProfile,
        onProgress: suspend (String) -> Unit
    ): String? = withContext(Dispatchers.IO) {
        val filePath = agent.filePath ?: return@withContext null  // no file → success

        onProgress("Reading ${agent.fileName ?: "file"}…")
        Log.d(TAG, "Indexing file for agent ${agent.id}: $filePath")

        val processor = FileProcessor(context)
        val result    = processor.process(filePath)
        if (result is FileProcessor.Result.Error) {
            Log.e(TAG, "File processing failed: ${result.message}")
            return@withContext result.message
        }

        val (text, estimatedTokens) = result as FileProcessor.Result.Success
        onProgress("File read — ~$estimatedTokens tokens. Chunking…")

        val chunked = FileChunker.chunk(
            agentId    = agent.id,
            text       = text,
            category   = agent.category,
            customWordSize = agent.customChunkSize
        )
        onProgress("${chunked.totalCount} chunks created. Computing TF-IDF vectors…")

        val indexedBody = TfIdfEngine.index(chunked.bodyChunks)
        onProgress("Indexed ${indexedBody.size} retrievable sections. Saving…")

        val allChunks = (chunked.skeletonChunks + indexedBody).map { it.toEntity() }

        // Clear previous chunks for this agent (e.g. re-indexing)
        db.chunkDao().deleteByAgent(agent.id)
        db.chunkDao().insertAll(allChunks)

        onProgress("Done — agent ready.")
        Log.d(TAG, "Indexing complete: ${allChunks.size} chunks stored")
        null  // success
    }

    // ─── Chunk retrieval ──────────────────────────────────────────────────────

    suspend fun getChunks(agentId: String): List<FileChunk> =
        db.chunkDao().getByAgent(agentId).map { it.toDomain() }

    // ─── Agent sessions ───────────────────────────────────────────────────────

    fun observeAllSessions(): Flow<List<AgentSession>> =
        db.agentSessionDao().observeAll().map { it.map { e -> e.toDomain() } }

    suspend fun getLatestSession(agentId: String): AgentSession? =
        db.agentSessionDao().getLatestForAgent(agentId)?.toDomain()

    suspend fun saveSession(session: AgentSession) =
        db.agentSessionDao().upsert(session.toEntity())

    suspend fun deleteSession(sessionId: String) =
        db.agentSessionDao().delete(sessionId)

    suspend fun deleteAllSessions() =
        db.agentSessionDao().deleteAll()
}

// ─── Missing toEntity/toDomain for AgentSession ───────────────────────────────

private fun com.lokai.app.data.session.AgentSessionEntity.toDomain() =
    AgentSession(
        id = id, agentId = agentId, agentName = agentName, category = category,
        modelId = modelId, modelName = modelName, inferenceMode = inferenceMode,
        messages = messages, createdAt = createdAt, updatedAt = updatedAt
    )

private fun AgentSession.toEntity() =
    com.lokai.app.data.session.AgentSessionEntity(
        id = id, agentId = agentId, agentName = agentName, category = category,
        modelId = modelId, modelName = modelName, inferenceMode = inferenceMode,
        messages = messages, createdAt = createdAt, updatedAt = updatedAt
    )
