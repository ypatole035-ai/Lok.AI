package com.lokai.app.data.session

import androidx.room.*
import com.lokai.app.data.inference.InferenceMode
import com.lokai.app.model.ChatMessage
import com.lokai.app.model.ChatSession
import com.lokai.app.model.ThinkingLog
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

// ─── Serializable helpers for JSON columns ────────────────────────────────────

@Serializable
private data class SerializableMessage(
    val id: String,
    val role: String,
    val content: String,
    val thinkingLog: List<SerializableThinkingLog>,
    val thinkingMs: Long,
    val timestampMs: Long
)

@Serializable
private data class SerializableThinkingLog(
    val timestampMs: Long,
    val message: String
)

private val json = Json { ignoreUnknownKeys = true }

// ─── Type Converters ──────────────────────────────────────────────────────────

class ChatConverters {

    @TypeConverter
    fun fromInferenceMode(mode: InferenceMode): String = mode.name

    @TypeConverter
    fun toInferenceMode(name: String): InferenceMode =
        InferenceMode.valueOf(name)

    @TypeConverter
    fun fromMessages(messages: List<ChatMessage>): String {
        val serializable = messages.map { msg ->
            SerializableMessage(
                id          = msg.id,
                role        = msg.role,
                content     = msg.content,
                thinkingLog = msg.thinkingLog.map { l ->
                    SerializableThinkingLog(l.timestampMs, l.message)
                },
                thinkingMs  = msg.thinkingMs,
                timestampMs = msg.timestampMs
            )
        }
        return json.encodeToString(serializable)
    }

    @TypeConverter
    fun toMessages(jsonString: String): List<ChatMessage> {
        if (jsonString.isBlank()) return emptyList()
        return try {
            json.decodeFromString<List<SerializableMessage>>(jsonString).map { s ->
                ChatMessage(
                    id          = s.id,
                    role        = s.role,
                    content     = s.content,
                    thinkingLog = s.thinkingLog.map { l -> ThinkingLog(l.timestampMs, l.message) },
                    thinkingMs  = s.thinkingMs,
                    timestampMs = s.timestampMs
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

// ─── Entity ───────────────────────────────────────────────────────────────────

@Entity(tableName = "chat_sessions")
@TypeConverters(ChatConverters::class)
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val modelId:       String,
    val modelName:     String,
    val inferenceMode: InferenceMode,
    val messages:      List<ChatMessage>,
    val createdAt:     Long,
    val updatedAt:     Long
)

// ─── DAO ──────────────────────────────────────────────────────────────────────

@Dao
interface SessionDao {

    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    suspend fun getAll(): List<ChatSessionEntity>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getById(sessionId: String): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun delete(sessionId: String)

    @Query("DELETE FROM chat_sessions")
    suspend fun deleteAll()
}

// ─── Mappers ──────────────────────────────────────────────────────────────────

fun ChatSessionEntity.toDomain() = ChatSession(
    id            = id,
    modelId       = modelId,
    modelName     = modelName,
    inferenceMode = inferenceMode,
    messages      = messages,
    createdAt     = createdAt,
    updatedAt     = updatedAt
)

fun ChatSession.toEntity() = ChatSessionEntity(
    id            = id,
    modelId       = modelId,
    modelName     = modelName,
    inferenceMode = inferenceMode,
    messages      = messages,
    createdAt     = createdAt,
    updatedAt     = updatedAt
)
