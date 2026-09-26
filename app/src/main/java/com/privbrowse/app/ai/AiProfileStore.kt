package com.privbrowse.app.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AiProfile(val name: String, val prompt: String, val isDefault: Boolean)

object AiProfileStore {
    private const val PREFS = "privbrowse_ai"
    private const val PROFILES = "profiles"

    private val builtIns = listOf(
        AiProfile("Browser assistant", "You are PrivBrowse AI. Answer clearly and concisely. Use page context when supplied. Say when information is missing; never invent citations.", true),
        AiProfile("Page summarizer", "Summarize the provided page accurately. Preserve important names, dates, numbers and caveats. Use short sections and bullets.", false),
        AiProfile("Simple explainer", "Explain difficult page content in simple language. Define jargon briefly and use small examples when useful.", false),
        AiProfile("Research notes", "Turn the supplied page into structured research notes. Separate direct facts from claims, and keep source links/URLs visible when present.", false),
        AiProfile("Study helper", "Turn the supplied page into clean study material: key ideas, definitions, examples and a brief self-check section. Do not invent facts absent from the page.", false),
        AiProfile("Writing editor", "Rewrite the user's supplied text for clarity and structure while preserving meaning. Do not add unsupported facts.", false),
        AiProfile("Developer helper", "Act as a practical programming assistant. Explain errors simply, provide focused fixes and call out assumptions. Do not pretend to have run code.", false)
    )

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(c: Context, profile: AiProfile) {
        val arr = JSONArray(prefs(c).getString(PROFILES, "[]") ?: "[]")
        for (i in arr.length() - 1 downTo 0) if (arr.optJSONObject(i)?.optString("name") == profile.name) arr.remove(i)
        if (profile.isDefault) for (i in 0 until arr.length()) arr.optJSONObject(i)?.put("default", false)
        arr.put(JSONObject().put("name", profile.name).put("prompt", profile.prompt).put("default", profile.isDefault))
        prefs(c).edit().putString(PROFILES, arr.toString()).apply()
    }

    fun all(c: Context): List<AiProfile> {
        val custom = runCatching {
            val arr = JSONArray(prefs(c).getString(PROFILES, "[]") ?: "[]")
            List(arr.length()) { i ->
                val o = arr.optJSONObject(i) ?: JSONObject()
                AiProfile(o.optString("name"), o.optString("prompt"), o.optBoolean("default"))
            }
        }.getOrElse { emptyList() }
        val customByName = custom.associateBy { it.name }
        val merged = builtIns.map { customByName[it.name] ?: it }.toMutableList()
        custom.filterNot { it.name in merged.map(AiProfile::name) }.forEach(merged::add)
        return merged
    }

    fun default(c: Context): AiProfile = all(c).firstOrNull { it.isDefault } ?: builtIns.first()
}
