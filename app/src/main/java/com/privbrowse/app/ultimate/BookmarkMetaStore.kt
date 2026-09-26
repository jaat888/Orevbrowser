package com.privbrowse.app.ultimate

import android.content.Context
import org.json.JSONObject

object BookmarkMetaStore {
    private const val PREFS = "bookmark_meta"
    private const val KEY = "items"

    fun put(context: Context, url: String, title: String, description: String = "") {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val root = runCatching { JSONObject(prefs.getString(KEY, "{}")) }.getOrElse { JSONObject() }
        val words = (title + " " + url).lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 4 }
            .distinct()
            .take(6)
        root.put(url, JSONObject().apply {
            put("description", description.trim().take(300))
            put("tags", words.joinToString(", "))
            put("savedAt", System.currentTimeMillis())
        })
        prefs.edit().putString(KEY, root.toString()).apply()
    }

    fun get(context: Context, url: String): JSONObject? {
        val root = runCatching { JSONObject(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "{}")) }.getOrElse { JSONObject() }
        return root.optJSONObject(url)
    }
}
