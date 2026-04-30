package com.lokai.app.model

/**
 * The five named agent categories, each mapping to a distinct context strategy.
 */
enum class AgentCategory(
    val displayName:  String,
    val emoji:        String,
    val description:  String,
    val strategyName: String
) {
    CODE(
        displayName  = "Code",
        emoji        = "💻",
        description  = "Full file always in context. Every import, function, and class is available. Best for debugging and refactoring.",
        strategyName = "Full Load"
    ),
    STORY(
        displayName  = "Story / Writing",
        emoji        = "📖",
        description  = "Document structure always loaded. Relevant scenes retrieved on demand. Best for long-form writing assistance.",
        strategyName = "Skeleton + Retrieval"
    ),
    RESEARCH(
        displayName  = "Research",
        emoji        = "📄",
        description  = "Abstract and conclusions always loaded. Body sections retrieved per question. Best for papers and documents.",
        strategyName = "Summary + Retrieval"
    ),
    REFERENCE(
        displayName  = "Reference",
        emoji        = "📋",
        description  = "Pure retrieval — nothing pre-loaded. Every question searches the full file. Best for API docs and large references.",
        strategyName = "Pure Retrieval"
    ),
    CUSTOM(
        displayName  = "Custom",
        emoji        = "🎛️",
        description  = "Full control. Configure strategy, chunk size, retrieval count, temperature, and system prompt yourself.",
        strategyName = "Custom"
    );

    val label: String get() = "$emoji $displayName"

    fun defaultSystemPrompt(): String = when (this) {
        CODE      -> "You are a coding assistant. You have the complete source code provided below. Help with debugging, refactoring, explanation, and improvements. Never guess about the code — the full source is available to you."
        STORY     -> "You are a writing assistant focused on this story. You have the structure and key moments loaded. For specific scenes and details, I will retrieve the relevant sections as needed. Focus only on this document."
        RESEARCH  -> "You are a research assistant. You have the document's structure and conclusions loaded. I will retrieve specific findings from the body as you ask questions. Focus only on this document."
        REFERENCE -> "You are a reference assistant. I will retrieve the relevant sections from the document for each question. Answers should be precise and direct."
        CUSTOM    -> ""
    }
}
