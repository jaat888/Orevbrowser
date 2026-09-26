package com.privbrowse.app.ultimate

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class QueueItem(
    val id: Long,
    val url: String,
    val file: String,
    val mime: String,
    val bytes: Long,
    val total: Long,
    val status: String,
    val suspicious: Boolean,
    val attempts: Int
)

object DownloadQueueStore {
    private const val KEY = "download_queue"

    fun list(c: Context): List<QueueItem> {
        val a = UltimateFeatureStore.jsonArray(c, KEY)
        return (0 until a.length()).mapNotNull { i ->
            a.optJSONObject(i)?.let { j ->
                QueueItem(
                    id = j.optLong("id"),
                    url = j.optString("url"),
                    file = j.optString("file"),
                    mime = j.optString("mime"),
                    bytes = j.optLong("bytes"),
                    total = j.optLong("total"),
                    status = j.optString("status"),
                    suspicious = j.optBoolean("suspicious"),
                    attempts = j.optInt("attempts", 0)
                )
            }
        }
    }

    fun add(c: Context, url: String, file: String, mime: String?, suspicious: Boolean = false) {
        val a = UltimateFeatureStore.jsonArray(c, KEY)
        a.put(JSONObject().apply {
            put("id", System.currentTimeMillis())
            put("url", url)
            put("file", file)
            put("mime", mime ?: "application/octet-stream")
            put("bytes", 0)
            put("total", -1)
            put("status", "queued")
            put("suspicious", suspicious)
            put("attempts", 0)
        })
        UltimateFeatureStore.putArray(c, KEY, a)
    }

    fun update(c: Context, id: Long, bytes: Long, total: Long, status: String, attempts: Int? = null, suspicious: Boolean? = null) {
        val a = UltimateFeatureStore.jsonArray(c, KEY)
        for (i in 0 until a.length()) {
            val j = a.optJSONObject(i) ?: continue
            if (j.optLong("id") == id) {
                j.put("bytes", bytes)
                j.put("total", total)
                j.put("status", status)
                if (attempts != null) j.put("attempts", attempts)
                if (suspicious != null) j.put("suspicious", suspicious)
            }
        }
        UltimateFeatureStore.putArray(c, KEY, a)
    }

    fun markAttempt(c: Context, item: QueueItem, nextStatus: String): Int {
        val next = item.attempts + 1
        update(c, item.id, item.bytes, item.total, nextStatus, next)
        return next
    }

    fun remove(c: Context, id: Long) {
        val a = UltimateFeatureStore.jsonArray(c, KEY)
        val o = JSONArray()
        for (i in 0 until a.length()) if (a.optJSONObject(i)?.optLong("id") != id) o.put(a.getJSONObject(i))
        UltimateFeatureStore.putArray(c, KEY, o)
    }
}
