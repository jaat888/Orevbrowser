package com.privbrowse.app.ai

enum class AiProvider(
    val label: String,
    val defaultModel: String,
    val endpoint: String,
    // Common, known-good model IDs for this provider, newest/most useful
    // first. Kept short on purpose — the "+ Add new model" entry in the
    // picker covers anything released after this list was last updated,
    // so users are never stuck waiting for an app update to use a new model.
    val models: List<String>
) {
    GROQ(
        "Groq", "llama-3.3-70b-versatile",
        "https://api.groq.com/openai/v1/chat/completions",
        listOf(
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant",
            "mixtral-8x7b-32768",
            "gemma2-9b-it"
        )
    ),
    OPENROUTER(
        "OpenRouter", "openai/gpt-4o",
        "https://openrouter.ai/api/v1/chat/completions",
        listOf(
            "openai/gpt-4o",
            "openai/gpt-4o-mini",
            "anthropic/claude-3.5-sonnet",
            "google/gemini-pro-1.5",
            "meta-llama/llama-3.1-70b-instruct",
            "deepseek/deepseek-chat"
        )
    ),
    OPENAI(
        "OpenAI", "gpt-4o-mini",
        "https://api.openai.com/v1/chat/completions",
        listOf(
            "gpt-4o",
            "gpt-4o-mini",
            "gpt-4-turbo",
            "o1-mini"
        )
    ),
    GEMINI(
        "Gemini", "gemini-1.5-flash",
        "https://generativelanguage.googleapis.com/v1beta/models/",
        listOf(
            "gemini-2.0-flash",
            "gemini-1.5-pro",
            "gemini-1.5-flash"
        )
    ),
    ANTHROPIC(
        "Anthropic", "claude-3-5-sonnet-20241022",
        "https://api.anthropic.com/v1/messages",
        listOf(
            "claude-3-5-sonnet-20241022",
            "claude-3-5-haiku-20241022",
            "claude-3-opus-20240229"
        )
    )
}
