package com.lokai.app.data.session

import androidx.room.*
import com.lokai.app.data.inference.InferenceMode
import com.lokai.app.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

// ─── Serializable helpers ─────────────────────────────────────────────────────

@Serializable
private data class SerializableAgentSession(
    val id: String,
    val agentId: String,
    val agentName: String,
    val category: String,
    val modelId: String,
    val modelName: String,
    val inferenceMode: String,
    val messages: List<SerializableMessage2>,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
private data class SerializableMessage2(
    val id: String,
    val role: String,
    val content: String,
    val thinkingLog: List<SerializableLog2>,
    val thinkingMs: Long,
    val timestampMs: Long
)

@Serializable
private data class SerializableLog2(val timestampMs: Long, val message: String)

private val agentJson = Json { ignoreUnknownKeys = true }

// ─── Type Converter for AgentConverters ──────────────────────────────────────

class AgentConverters {

    @TypeConverter
    fun fromCategory(c: AgentCategory): String = c.name

    @TypeConverter
    fun toCategory(s: String): AgentCategory = AgentCategory.valueOf(s)
}

// ─── AgentProfile Entity ──────────────────────────────────────────────────────

@Entity(tableName = "agent_profiles")
@TypeConverters(AgentConverters::class)
data class AgentProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: AgentCategory,
    val modelId: String,
    val modelName: String,
    val filePath: String?,
    val fileName: String?,
    val systemPrompt: String,
    val inferenceMode: InferenceMode,
    val customChunkSize: Int,
    val customChunksRetrieved: Int,
    val customFallback: Boolean,
    val customTemperature: Float,
    val customMaxTokens: Int,
    val customContextSize: Int,
    val customStrategy: String,
    val createdAt: Long,
    val lastUsedAt: Long
)

fun AgentProfileEntity.toDomain() = AgentProfile(
    id = id, name = name, category = category, modelId = modelId, modelName = modelName,
    filePath = filePath, fileName = fileName, systemPrompt = systemPrompt,
    inferenceMode = inferenceMode, customChunkSize = customChunkSize,
    customChunksRetrieved = customChunksRetrieved, customFallback = customFallback,
    customTemperature = customTemperature, customMaxTokens = customMaxTokens,
    customContextSize = customContextSize, customStrategy = customStrategy,
    createdAt = createdAt, lastUsedAt = lastUsedAt
)

fun AgentProfile.toEntity() = AgentProfileEntity(
    id = id, name = name, category = category, modelId = modelId, modelName = modelName,
    filePath = filePath, fileName = fileName, systemPrompt = systemPrompt,
    inferenceMode = inferenceMode, customChunkSize = customChunkSize,
    customChunksRetrieved = customChunksRetrieved, customFallback = customFallback,
    customTemperature = customTemperature, customMaxTokens = customMaxTokens,
    customContextSize = customContextSize, customStrategy = customStrategy,
    createdAt = createdAt, lastUsedAt = lastUsedAt
)

// ─── FileChunk Entity ─────────────────────────────────────────────────────────

@Entity(tableName = "file_chunks")
data class FileChunkEntity(
    @PrimaryKey val id: String,
    val agentId: String,
    val index: Int,
    val text: String,
    val tfidfJson: String,
    val isSkeleton: Boolean
)

fun FileChunkEntity.toDomain() =
    FileChunk(id = id, agentId = agentId, index = index,
              text = text, tfidfJson = tfidfJson, isSkeleton = isSkeleton)

fun FileChunk.toEntity() =
    FileChunkEntity(id = id, agentId = agentId, index = index,
                    text = text, tfidfJson = tfidfJson, isSkeleton = isSkeleton)

// ─── AgentSession Entity ──────────────────────────────────────────────────────

@Entity(tableName = "agent_sessions")
@TypeConverters(AgentConverters::class)
data class AgentSessionEntity(
    @PrimaryKey val id: String,
    val agentId: String,
    val agentName: String,
    val category: AgentCategory,
    val modelId: String,
    val modelName: String,
    val inferenceMode: InferenceMode,
    val messages: List<ChatMessage>,
    val createdAt: Long,
    val updatedAt: Long
)

fun AgentSessionEntity.toDomain() = AgentSession(
    id = id, agentId = agentId, agentName = agentName, category = category,
    modelId = modelId, modelName = modelName, inferenceMode = inferenceMode,
    messages = messages, createdAt = createdAt, updatedAt = updatedAt
)

fun AgentSession.toEntity() = AgentSessionEntity(
    id = id, agentId = agentId, agentName = agentName, category = category,
    modelId = modelId, modelName = modelName, inferenceMode = inferenceMode,
    messages = messages, createdAt = createdAt, updatedAt = updatedAt
)

// ─── DAOs ─────────────────────────────────────────────────────────────────────

@Dao
interface AgentDao {

    // Agent profiles
    @Query("SELECT * FROM agent_profiles ORDER BY lastUsedAt DESC")
    fun observeAll(): Flow<List<AgentProfileEntity>>

    @Query("SELECT * FROM agent_profiles ORDER BY lastUsedAt DESC")
    suspend fun getAll(): List<AgentProfileEntity>

    @Query("SELECT * FROM agent_profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AgentProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AgentProfileEntity)

    @Query("DELETE FROM agent_profiles WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM agent_profiles")
    suspend fun deleteAll()

    @Query("UPDATE agent_profiles SET lastUsedAt = :ts WHERE id = :id")
    suspend fun updateLastUsed(id: String, ts: Long)
}

@Dao
interface ChunkDao {

    @Query("SELECT * FROM file_chunks WHERE agentId = :agentId ORDER BY `index` ASC")
    suspend fun getByAgent(agentId: String): List<FileChunkEntity>

    @Query("SELECT * FROM file_chunks WHERE agentId = :agentId AND isSkeleton = 1 ORDER BY `index` ASC")
    suspend fun getSkeletonByAgent(agentId: String): List<FileChunkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chunks: List<FileChunkEntity>)

    @Query("DELETE FROM file_chunks WHERE agentId = :agentId")
    suspend fun deleteByAgent(agentId: String)
}

@Dao
interface AgentSessionDao {

    @Query("SELECT * FROM agent_sessions ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<AgentSessionEntity>>

    @Query("SELECT * FROM agent_sessions WHERE agentId = :agentId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestForAgent(agentId: String): AgentSessionEntity?

    @Query("SELECT * FROM agent_sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AgentSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AgentSessionEntity)

    @Query("DELETE FROM agent_sessions WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM agent_sessions")
    suspend fun deleteAll()
}
