package com.lokai.app

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lokai.app.ui.navigation.LokaiNavGraph
import com.lokai.app.ui.screens.OnboardingScreen
import com.lokai.app.ui.theme.LokAITheme
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore by preferencesDataStore("onboarding")
private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")

/**
 * Phase 6 — Onboarding, Polish & v1.0 Release
 *
 * Routes first-time users through OnboardingScreen.
 * Subsequent launches go directly to LokaiNavGraph.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LokAITheme {
                LokaiRoot()
            }
        }
    }
}

@Composable
private fun LokaiRoot() {
    val context = LocalContext.current

    var resolved       by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        context.onboardingDataStore.data
            .map { it[KEY_ONBOARDING_DONE] ?: false }
            .catch { emit(false) }
            .collect { done ->
                if (!resolved) {
                    showOnboarding = !done
                    resolved = true
                }
            }
    }

    if (!resolved) return // single blank frame while DataStore loads

    if (showOnboarding) {
        OnboardingScreen(onFinished = { showOnboarding = false })
    } else {
        LokaiNavGraph()
    }
}
