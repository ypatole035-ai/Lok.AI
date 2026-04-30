package com.lokai.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lokai.app.data.inference.InferenceMode
import com.lokai.app.data.settings.LokaiSettings
import com.lokai.app.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SettingsRepository(application)

    val settings: StateFlow<LokaiSettings> = repo.settings.stateIn(
        scope         = viewModelScope,
        started       = SharingStarted.WhileSubscribed(5_000),
        initialValue  = LokaiSettings()
    )

    fun setThreads(value: Int)              = viewModelScope.launch { repo.setThreads(value) }
    fun setContextSize(value: Int)          = viewModelScope.launch { repo.setContextSize(value) }
    fun setMaxTokens(value: Int)            = viewModelScope.launch { repo.setMaxTokens(value) }
    fun setTemperature(value: Float)        = viewModelScope.launch { repo.setTemperature(value) }
    fun setDefaultMode(mode: InferenceMode) = viewModelScope.launch { repo.setDefaultMode(mode) }
    fun setCustomSystemPrompt(text: String) = viewModelScope.launch { repo.setCustomSystemPrompt(text) }
    fun setAutoSave(value: Boolean)         = viewModelScope.launch { repo.setAutoSave(value) }
    fun setBatteryWarnPercent(value: Int)   = viewModelScope.launch { repo.setBatteryWarnPercent(value) }
}
