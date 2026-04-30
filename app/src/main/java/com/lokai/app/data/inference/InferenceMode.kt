package com.lokai.app.data.inference

/**
 * The three inference modes available for every chat session.
 *
 * - NORMAL   — standard inference, no modifications, fastest
 * - PRECISE  — for thinking-trained models: activates <think> tokens, temp 0.4, +20% max tokens
 * - FOCUSED  — for regular models: injects step-by-step system prefix, temp 0.4, +20% max tokens
 */
enum class InferenceMode {
    NORMAL,
    PRECISE,
    FOCUSED;

    val label: String get() = when (this) {
        NORMAL  -> "Normal"
        PRECISE -> "⚡ Precise"
        FOCUSED -> "🎯 Focused"
    }

    val tooltip: String get() = when (this) {
        NORMAL  -> "Standard inference. Fastest, lowest token usage."
        PRECISE -> "This model is trained for deep reasoning. Expect slower but significantly more accurate responses."
        FOCUSED -> "This model isn't trained for reasoning mode, but we'll guide it to be more careful and precise. Mild improvement."
    }
}
