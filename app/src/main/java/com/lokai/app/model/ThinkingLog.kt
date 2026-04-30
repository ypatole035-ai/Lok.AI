package com.lokai.app.model

/**
 * A single entry in the thinking/reasoning panel log.
 *
 * Emitted by ChatViewModel during inference to describe what the app
 * is doing at each step — retrieval, context assembly, generation timing, etc.
 *
 * These are app-generated log entries, NOT model internals.
 * They are shown in the collapsible ThinkingPanel above each assistant message.
 */
data class ThinkingLog(
    /** Unix timestamp (ms) when this entry was emitted */
    val timestampMs: Long = System.currentTimeMillis(),
    /** Human-readable log message, e.g. "Context: 847 / 2048 tokens used" */
    val message: String
)
