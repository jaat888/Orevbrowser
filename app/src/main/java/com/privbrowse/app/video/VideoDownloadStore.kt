package com.privbrowse.app.video

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class VideoDownloadItem(val id: Long, val url: String, val mimeType: String?, val createdAt: Long)

object VideoDownloadStore {
    private const val PREFS = "privbrowse_video_downloads"
    private const val ITEMS = "items_json"

    fun add(context: Context, url: String, id: Long, mimeType: String?) {
        val items = all(context).toMutableList()
        items.removeAll { it.id == id }
        items.add(VideoDownloadItem(id, url, mimeType, System.currentTimeMillis()))
        save(context, items.sortedByDescending { it.createdAt }.take(100))
    }

    fun all(context: Context): List<VideoDownloadItem> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(ITEMS, null)
        if (raw == null) {
            val legacy = prefs.getStringSet("items", emptySet()).orEmpty().mapNotNull { item ->
                val i = item.indexOf('|')
                if (i <= 0) null else item.substring(0, i).toLongOrNull()?.let { id ->
                    VideoDownloadItem(id, item.substring(i + 1), null, 0L)
                }
            }
            if (legacy.isNotEmpty()) save(context, legacy)
            return legacy.sortedByDescending { it.createdAt }
        }
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                VideoDownloadItem(o.optLong("id"), o.optString("url"), o.optString("mime").takeIf { it.isNotBlank() }, o.optLong("created"))
            }.filter { it.id > 0 && it.url.isNotBlank() }.sortedByDescending { it.createdAt }
        }.getOrElse { emptyList() }
    }

    private fun save(context: Context, items: List<VideoDownloadItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().put("id", item.id).put("url", item.url).put("mime", item.mimeType ?: "").put("created", item.createdAt))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(ITEMS, arr.toString()).apply()
    }
}
