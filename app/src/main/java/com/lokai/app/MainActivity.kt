package com.lokai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lokai.app.ui.navigation.LokaiNavGraph
import com.lokai.app.ui.screens.OnboardingScreen
import com.lokai.app.ui.theme.LokAITheme
import com.lokai.app.viewmodel.OnboardingViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashReporter(this).install()
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

    // FIX: ViewModel hoisted here so it survives the onFinished() recomposition
    // that sets showOnboarding = false. Previously it was created inside
    // OnboardingScreen and got disposed mid-flight on page 2 → crash.
    val onboardingVm: OnboardingViewModel = viewModel()

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

    if (!resolved) return

    if (showOnboarding) {
        OnboardingScreen(
            onFinished = { showOnboarding = false },
            vm         = onboardingVm   // FIX: pass hoisted VM
        )
    } else {
        LokaiNavGraph()
    }
}
