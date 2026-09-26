package com.privbrowse.app.ai

import android.content.Context
import org.json.JSONArray

/**
 * Model IDs the user typed in themselves via "+ Add new model". This is
 * what lets a brand-new model release work the same day it ships, instead
 * of waiting on an app update to add it to AiProvider's built-in list.
 */
object AiCustomModelStore {
    private const val PREFS = "privbrowse_ai"
    private fun key(provider: AiProvider) = "custom_models_${provider.name.lowercase()}"
    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun all(c: Context, provider: AiProvider): List<String> {
        val raw = prefs(c).getString(key(provider), "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { i -> arr.optString(i) }.filter { it.isNotBlank() }
        }.getOrElse { emptyList() }
    }

    fun add(c: Context, provider: AiProvider, modelId: String) {
        val trimmed = modelId.trim()
        if (trimmed.isBlank()) return
        val existing = all(c, provider)
        if (trimmed in existing) return
        val arr = JSONArray()
        existing.forEach(arr::put)
        arr.put(trimmed)
        prefs(c).edit().putString(key(provider), arr.toString()).apply()
    }
}
