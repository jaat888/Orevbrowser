package com.privbrowse.app.ai

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object AiClient {
    data class Reply(val text: String)

    fun chat(provider: AiProvider, key: String, model: String, system: String, user: String): Reply {
        return when (provider) {
            AiProvider.GEMINI -> gemini(key, model, system, user)
            AiProvider.ANTHROPIC -> anthropic(key, model, system, user)
            else -> openAiCompatible(provider, key, model, system, user)
        }
    }

    private fun openAiCompatible(provider: AiProvider, key: String, model: String, system: String, user: String): Reply {
        val body = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", system))
                put(JSONObject().put("role", "user").put("content", user))
            })
        }
        val headers = mutableMapOf("Authorization" to "Bearer $key", "Content-Type" to "application/json")
        if (provider == AiProvider.OPENROUTER) {
            headers["HTTP-Referer"] = "https://privbrowse.local"
            headers["X-Title"] = "PrivBrowse"
        }
        val response = post(provider.endpoint, body.toString(), headers)
        val json = JSONObject(response)
        return Reply(json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: json.toString(2))
    }

    private fun gemini(key: String, model: String, system: String, user: String): Reply {
        val endpoint = "${AiProvider.GEMINI.endpoint}${model}:generateContent"
        val body = JSONObject().apply {
            put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", user)))))
        }
        val json = JSONObject(post(endpoint, body.toString(), mapOf("x-goog-api-key" to key, "Content-Type" to "application/json")))
        val text = json.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")
        return Reply(text ?: json.toString(2))
    }

    private fun anthropic(key: String, model: String, system: String, user: String): Reply {
        val endpoint = AiProvider.ANTHROPIC.endpoint
        val body = JSONObject().apply {
            put("model", model); put("max_tokens", 1024); put("system", system)
            put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", user)))
        }
        val json = JSONObject(post(endpoint, body.toString(), mapOf("x-api-key" to key, "anthropic-version" to "2023-06-01", "Content-Type" to "application/json")))
        return Reply(json.optJSONArray("content")?.optJSONObject(0)?.optString("text") ?: json.toString(2))
    }

    private fun post(urlString: String, body: String, headers: Map<String, String>): String {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod="POST"; connectTimeout=15000; readTimeout=30000; doOutput=true
            headers.forEach { (k,v)-> setRequestProperty(k,v) }
        }
        conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: "HTTP ${conn.responseCode}"
        val code=conn.responseCode; conn.disconnect()
        if(code !in 200..299) throw IllegalStateException("AI provider HTTP $code: $text")
        return text
    }
}
