package com.lokai.app.data.session

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.lokai.app.data.inference.InferenceMode
import com.lokai.app.model.ChatMessage
import com.lokai.app.model.ChatSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SessionRepository(private val context: Context) {

    private val dao: SessionDao
        get() = LokaiDatabase.getInstance(context).sessionDao()

    // ─── Observe all sessions (for sessions list screen) ─────────────────────

    fun observeAll(): Flow<List<ChatSession>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    // ─── Load / save ──────────────────────────────────────────────────────────

    suspend fun getById(sessionId: String): ChatSession? =
        dao.getById(sessionId)?.toDomain()

    suspend fun save(session: ChatSession) {
        dao.upsert(session.toEntity())
    }

    /** Auto-save: update only the messages + updatedAt timestamp */
    suspend fun appendMessages(sessionId: String, messages: List<ChatMessage>) {
        val existing = dao.getById(sessionId) ?: return
        dao.upsert(
            existing.copy(
                messages  = messages,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /** Update the inference mode stored for a session */
    suspend fun updateMode(sessionId: String, mode: InferenceMode) {
        val existing = dao.getById(sessionId) ?: return
        dao.upsert(existing.copy(inferenceMode = mode))
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    suspend fun delete(sessionId: String) = dao.delete(sessionId)

    suspend fun deleteAll() = dao.deleteAll()

    // ─── Export as Markdown ───────────────────────────────────────────────────

    /**
     * Exports [session] as a `.md` file in the app's cache dir, then
     * returns an Intent ready to launch the Android share sheet.
     *
     * The caller should `startActivity(Intent.createChooser(intent, "Share conversation"))`.
     */
    fun buildExportIntent(session: ChatSession): Intent {
        val sb = StringBuilder()
        sb.appendLine("# Chat with ${session.modelName}")
        sb.appendLine()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        sb.appendLine("*Exported from Lok.AI — ${sdf.format(Date(session.createdAt))}*")
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()

        for (msg in session.messages) {
            if (msg.isUser) {
                sb.appendLine("**You:** ${msg.content}")
            } else {
                sb.appendLine("**${session.modelName}:** ${msg.content}")
                if (msg.thinkingLog.isNotEmpty()) {
                    sb.appendLine()
                    sb.appendLine("> *Thought for %.1fs*".format(msg.thinkingSeconds))
                }
            }
            sb.appendLine()
        }

        val exportDir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val safe      = session.modelName.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file      = File(exportDir, "chat_${safe}_${session.createdAt}.md")
        file.writeText(sb.toString())

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type    = "text/markdown"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
