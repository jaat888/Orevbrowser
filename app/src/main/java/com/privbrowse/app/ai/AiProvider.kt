package com.privbrowse.app.ai

enum class AiProvider(
    val label: String,
    val defaultModel: String,
    val endpoint: String
) {
    GROQ("Groq", "", "https://api.groq.com/openai/v1/chat/completions"),
    OPENROUTER("OpenRouter", "", "https://openrouter.ai/api/v1/chat/completions"),
    OPENAI("OpenAI", "", "https://api.openai.com/v1/chat/completions"),
    GEMINI("Gemini", "", "https://generativelanguage.googleapis.com/v1beta/models/"),
    ANTHROPIC("Anthropic", "", "https://api.anthropic.com/v1/messages")
}
