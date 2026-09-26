package com.privbrowse.app.privacy

import android.content.Context
import org.json.JSONObject

/** Small on-device per-domain CSS/JS store. Values are user-authored and never synced. */
object SiteCustomizationStore {
    private const val PREFS = "privbrowse_site_customizations"
    private const val CSS = "css"
    private const val JS = "js"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun readMap(context: Context, key: String): JSONObject =
        runCatching { JSONObject(prefs(context).getString(key, "{}") ?: "{}") }.getOrElse { JSONObject() }

    fun getCss(context: Context, host: String): String = readMap(context, CSS).optString(host.lowercase(), "")
    fun getJs(context: Context, host: String): String = readMap(context, JS).optString(host.lowercase(), "")

    fun setCss(context: Context, host: String, value: String) {
        val map = readMap(context, CSS)
        val key = host.trim().lowercase()
        if (key.isBlank()) return
        if (value.isBlank()) map.remove(key) else map.put(key, value)
        prefs(context).edit().putString(CSS, map.toString()).apply()
    }

    fun setJs(context: Context, host: String, value: String) {
        val map = readMap(context, JS)
        val key = host.trim().lowercase()
        if (key.isBlank()) return
        if (value.isBlank()) map.remove(key) else map.put(key, value)
        prefs(context).edit().putString(JS, map.toString()).apply()
    }
}
