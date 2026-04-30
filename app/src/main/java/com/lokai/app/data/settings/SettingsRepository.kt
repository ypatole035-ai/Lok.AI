package com.lokai.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.lokai.app.data.inference.InferenceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lokai_settings")

data class LokaiSettings(
    val threads:           Int          = 0,          // 0 = auto-detect
    val contextSize:       Int          = 2048,
    val maxTokens:         Int          = 512,
    val temperature:       Float        = 0.7f,
    val defaultMode:       InferenceMode = InferenceMode.NORMAL,
    val customSystemPrompt:String       = "",
    val autoSaveSessions:  Boolean      = true,
    val batteryWarnPercent:Int          = 20,
    val tooltipPreciseSeen:Boolean      = false,
    val tooltipFocusedSeen:Boolean      = false
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THREADS            = intPreferencesKey("threads")
        val CONTEXT_SIZE       = intPreferencesKey("context_size")
        val MAX_TOKENS         = intPreferencesKey("max_tokens")
        val TEMPERATURE        = floatPreferencesKey("temperature")
        val DEFAULT_MODE       = stringPreferencesKey("default_mode")
        val CUSTOM_SYSTEM      = stringPreferencesKey("custom_system_prompt")
        val AUTO_SAVE          = booleanPreferencesKey("auto_save_sessions")
        val BATTERY_WARN       = intPreferencesKey("battery_warn_percent")
        val TOOLTIP_PRECISE    = booleanPreferencesKey("tooltip_precise_seen")
        val TOOLTIP_FOCUSED    = booleanPreferencesKey("tooltip_focused_seen")
    }

    val settings: Flow<LokaiSettings> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences())
            else throw e
        }
        .map { prefs ->
            LokaiSettings(
                threads            = prefs[Keys.THREADS]            ?: 0,
                contextSize        = prefs[Keys.CONTEXT_SIZE]       ?: 2048,
                maxTokens          = prefs[Keys.MAX_TOKENS]         ?: 512,
                temperature        = prefs[Keys.TEMPERATURE]        ?: 0.7f,
                defaultMode        = InferenceMode.valueOf(
                                        prefs[Keys.DEFAULT_MODE]    ?: InferenceMode.NORMAL.name
                                     ),
                customSystemPrompt = prefs[Keys.CUSTOM_SYSTEM]      ?: "",
                autoSaveSessions   = prefs[Keys.AUTO_SAVE]          ?: true,
                batteryWarnPercent = prefs[Keys.BATTERY_WARN]       ?: 20,
                tooltipPreciseSeen = prefs[Keys.TOOLTIP_PRECISE]    ?: false,
                tooltipFocusedSeen = prefs[Keys.TOOLTIP_FOCUSED]    ?: false
            )
        }

    suspend fun setThreads(value: Int)           = update { it[Keys.THREADS]         = value }
    suspend fun setContextSize(value: Int)        = update { it[Keys.CONTEXT_SIZE]    = value }
    suspend fun setMaxTokens(value: Int)          = update { it[Keys.MAX_TOKENS]      = value }
    suspend fun setTemperature(value: Float)      = update { it[Keys.TEMPERATURE]     = value }
    suspend fun setDefaultMode(mode: InferenceMode) = update { it[Keys.DEFAULT_MODE]  = mode.name }
    suspend fun setCustomSystemPrompt(text: String) = update { it[Keys.CUSTOM_SYSTEM] = text }
    suspend fun setAutoSave(value: Boolean)       = update { it[Keys.AUTO_SAVE]       = value }
    suspend fun setBatteryWarnPercent(value: Int) = update { it[Keys.BATTERY_WARN]    = value }
    suspend fun markTooltipPreciseSeen()          = update { it[Keys.TOOLTIP_PRECISE] = true }
    suspend fun markTooltipFocusedSeen()          = update { it[Keys.TOOLTIP_FOCUSED] = true }

    private suspend fun update(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }
}
