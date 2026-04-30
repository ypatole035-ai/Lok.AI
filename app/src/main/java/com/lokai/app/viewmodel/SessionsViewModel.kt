package com.lokai.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lokai.app.data.session.SessionRepository
import com.lokai.app.model.ChatSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the sessions history list.
 * Exposes regular ChatSessions; AgentSessions are exposed by AgentViewModel.
 */
class SessionsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SessionRepository(application)

    val sessions: StateFlow<List<ChatSession>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteSession(sessionId: String) {
        viewModelScope.launch { repo.delete(sessionId) }
    }

    fun deleteAllSessions() {
        viewModelScope.launch { repo.deleteAll() }
    }
}
