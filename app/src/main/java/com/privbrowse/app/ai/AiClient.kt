package com.privbrowse.app.ai

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Small dependency-free HTTP client for the optional PrivBrowse AI providers. */
object AiClient {
    data class Reply(val text: String)

    fun chat(
        provider: AiProvider,
        key: String,
        model: String,
        system: String,
        user: String,
        maxTokens: Int = 2048,
        customEndpoint: String? = null
    ): Reply {
        require(key.isNotBlank()) { "Missing API key" }
        require(model.isNotBlank()) { "Missing model" }
        return when (provider) {
            AiProvider.GEMINI -> gemini(key, model, system, user, maxTokens)
            AiProvider.ANTHROPIC -> anthropic(key, model, system, user, maxTokens)
            AiProvider.OPENAI -> runCatching {
                openAiResponses(key, model, system, user, maxTokens)
            }.getOrElse { firstError ->
                val msg = firstError.message.orEmpty()
                if (msg.contains("HTTP 400") || msg.contains("HTTP 404") || msg.contains("HTTP 422") || msg.contains("HTTP 405")) {
                    openAiCompatible(AiProvider.OPENAI, key, model, system, user, maxTokens, null)
                } else {
                    throw firstError
                }
            }
            AiProvider.CUSTOM -> openAiCompatible(provider, key, model, system, user, maxTokens, customEndpoint)
            else -> openAiCompatible(provider, key, model, system, user, maxTokens, null)
        }
    }

    fun listModels(provider: AiProvider, key: String, customEndpoint: String? = null): List<String> = when (provider) {
        AiProvider.GEMINI -> listGeminiModels(key)
        AiProvider.ANTHROPIC -> provider.models
        AiProvider.CUSTOM -> listOpenAiModels(provider, key, customEndpoint)
        else -> listOpenAiModels(provider, key, null)
    }

    private fun openAiCompatible(
        provider: AiProvider,
        key: String,
        model: String,
        system: String,
        user: String,
        maxTokens: Int,
        customEndpoint: String?
    ): Reply {
        val endpoint = customEndpoint?.trim().takeIf { provider == AiProvider.CUSTOM && !it.isNullOrBlank() }
            ?: provider.endpoint
        val body = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", system))
                put(JSONObject().put("role", "user").put("content", user))
            })
            put("max_tokens", maxTokens.coerceIn(64, 16384))
        }
        val headers = mutableMapOf("Authorization" to "Bearer $key", "Content-Type" to "application/json")
        if (provider == AiProvider.OPENROUTER) {
            headers["HTTP-Referer"] = "https://privbrowse.local"
            headers["X-Title"] = "PrivBrowse"
        }
        val json = JSONObject(post(endpoint, body.toString(), headers))
        val message = json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
        val content = message?.opt("content")
        val text = when (content) {
            is String -> content
            is JSONArray -> buildString {
                for (i in 0 until content.length()) {
                    val item = content.optJSONObject(i) ?: continue
                    append(item.optString("text"))
                }
            }
            else -> null
        }
        return Reply(text?.takeIf { it.isNotBlank() } ?: extractError(json) ?: json.toString(2))
    }

    /** Uses OpenAI's Responses API first, then falls back to Chat Completions for compatible models/endpoints. */
    private fun openAiResponses(key: String, model: String, system: String, user: String, maxTokens: Int): Reply {
        val body = JSONObject().apply {
            put("model", model)
            put("instructions", system)
            put("input", user)
            put("max_output_tokens", maxTokens.coerceIn(64, 16384))
        }
        val json = JSONObject(post("https://api.openai.com/v1/responses", body.toString(), mapOf(
            "Authorization" to "Bearer $key",
            "Content-Type" to "application/json"
        )))
        json.optString("output_text").takeIf { it.isNotBlank() }?.let { return Reply(it) }
        val output = json.optJSONArray("output")
        val text = output?.let { arr ->
            buildString {
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val content = item.optJSONArray("content") ?: continue
                    for (j in 0 until content.length()) {
                        val c = content.optJSONObject(j) ?: continue
                        append(c.optString("text"))
                    }
                }
            }
        }
        return Reply(text?.takeIf { it.isNotBlank() } ?: extractError(json) ?: json.toString(2))
    }

    private fun gemini(key: String, model: String, system: String, user: String, maxTokens: Int): Reply {
        val endpoint = "${AiProvider.GEMINI.endpoint}${model}:generateContent"
        val body = JSONObject().apply {
            put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            put("contents", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", user)))
            }))
            put("generationConfig", JSONObject().put("maxOutputTokens", maxTokens.coerceIn(64, 8192)))
        }
        val json = JSONObject(post(endpoint, body.toString(), mapOf("x-goog-api-key" to key, "Content-Type" to "application/json")))
        val candidates = json.optJSONArray("candidates")
        val parts = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
        val text = parts?.let { arr ->
            buildString {
                for (i in 0 until arr.length()) append(arr.optJSONObject(i)?.optString("text").orEmpty())
            }
        }
        return Reply(text?.takeIf { it.isNotBlank() } ?: extractError(json) ?: json.toString(2))
    }

    private fun anthropic(key: String, model: String, system: String, user: String, maxTokens: Int): Reply {
        val body = JSONObject().apply {
            put("model", model)
            put("max_tokens", maxTokens.coerceIn(64, 8192))
            put("system", system)
            put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", user)))
        }
        val json = JSONObject(post(AiProvider.ANTHROPIC.endpoint, body.toString(), mapOf(
            "x-api-key" to key,
            "anthropic-version" to "2023-06-01",
            "Content-Type" to "application/json"
        )))
        val content = json.optJSONArray("content")
        val text = content?.let { arr ->
            buildString {
                for (i in 0 until arr.length()) append(arr.optJSONObject(i)?.optString("text").orEmpty())
            }
        }
        return Reply(text?.takeIf { it.isNotBlank() } ?: extractError(json) ?: json.toString(2))
    }

    private fun listOpenAiModels(provider: AiProvider, key: String, customEndpoint: String?): List<String> {
        val chatEndpoint = customEndpoint?.trim().takeIf { provider == AiProvider.CUSTOM && !it.isNullOrBlank() }
            ?: provider.endpoint
        val endpoint = chatEndpoint
            .replace("/chat/completions", "/models")
            .replace("/responses", "/models")
            .trimEnd('/')
        val json = JSONObject(get(endpoint, mapOf("Authorization" to "Bearer $key")))
        val data = json.optJSONArray("data") ?: return provider.models
        val models = buildList {
            for (i in 0 until data.length()) {
                val id = data.optJSONObject(i)?.optString("id").orEmpty()
                if (id.isNotBlank()) add(id)
            }
        }.distinct().sorted()
        return models.ifEmpty { provider.models }.take(120)
    }

    private fun listGeminiModels(key: String): List<String> {
        val url = "https://generativelanguage.googleapis.com/v1beta/models?key=${URLEncoder.encode(key, "UTF-8")}"
        val json = JSONObject(get(url, emptyMap()))
        val models = json.optJSONArray("models") ?: return AiProvider.GEMINI.models
        return buildList {
            for (i in 0 until models.length()) {
                val o = models.optJSONObject(i) ?: continue
                val methods = o.optJSONArray("supportedGenerationMethods")
                var supported = false
                if (methods != null) for (j in 0 until methods.length()) if (methods.optString(j) == "generateContent") supported = true
                if (!supported) continue
                val id = o.optString("baseModelId").takeIf { it.isNotBlank() }
                    ?: o.optString("name").removePrefix("models/").takeIf { it.isNotBlank() }
                id?.let(::add)
            }
        }.distinct().sorted().take(120).ifEmpty { AiProvider.GEMINI.models }
    }

    private fun extractError(json: JSONObject): String? {
        val direct = json.optJSONObject("error")?.optString("message").takeIf { !it.isNullOrBlank() }
        if (direct != null) return direct
        val msg = json.optString("message").takeIf { it.isNotBlank() }
        return msg
    }

    private fun get(urlString: String, headers: Map<String, String>): String {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 25000
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "PrivBrowse/1.3.0")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        return readResponse(conn)
    }

    private fun post(urlString: String, body: String, headers: Map<String, String>): String {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 60000
            doOutput = true
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "PrivBrowse/1.3.0")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
        return readResponse(conn)
    }

    private fun readResponse(conn: HttpURLConnection): String {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
        conn.disconnect()
        if (code !in 200..299) throw IllegalStateException("HTTP $code: $text")
        return text
    }
}
