package com.privbrowse.app.ai

enum class AiProvider(
    val label: String,
    val defaultModel: String,
    val endpoint: String,
    val models: List<String>
) {
    GROQ(
        "Groq",
        "openai/gpt-oss-20b",
        "https://api.groq.com/openai/v1/chat/completions",
        listOf(
            "openai/gpt-oss-20b",
            "openai/gpt-oss-120b",
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant",
            "groq/compound-mini"
        )
    ),
    OPENROUTER(
        "OpenRouter",
        "openai/gpt-5.4-mini",
        "https://openrouter.ai/api/v1/chat/completions",
        listOf(
            "openai/gpt-5.4-mini",
            "openai/gpt-5.4",
            "google/gemini-3.8-flash",
            "anthropic/claude-sonnet-4.6",
            "deepseek/deepseek-chat",
            "openrouter/auto"
        )
    ),
    OPENAI(
        "OpenAI",
        "gpt-5.4-mini",
        "https://api.openai.com/v1/chat/completions",
        listOf(
            "gpt-5.4-mini",
            "gpt-5.4",
            "gpt-4.1-mini",
            "gpt-4o-mini"
        )
    ),
    GEMINI(
        "Gemini",
        "gemini-3.8-flash",
        "https://generativelanguage.googleapis.com/v1beta/models/",
        listOf(
            "gemini-3.8-flash",
            "gemini-3.7-flash",
            "gemini-3.1-flash-lite",
            "gemini-3.1-pro-preview",
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-2.5-pro"
        )
    ),
    ANTHROPIC(
        "Anthropic",
        "claude-sonnet-4-6",
        "https://api.anthropic.com/v1/messages",
        listOf(
            "claude-sonnet-4-6",
            "claude-opus-4-8",
            "claude-haiku-4-5-20251001"
        )
    ),
    CUSTOM(
        "Custom OpenAI-compatible",
        "your-model-id",
        "https://example.com/v1/chat/completions",
        emptyList()
    )
}
