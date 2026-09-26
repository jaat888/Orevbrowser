package com.privbrowse.app.ultimate

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object QrExport {
    fun encodeTabs(tabs: List<com.privbrowse.app.ui.BrowserTab>, current: Int): String {
        val eligible = tabs.filter { !it.isIncognito && !it.isVault && it.url.startsWith("http") }
        val currentId = tabs.getOrNull(current)?.id
        val currentExportIndex = eligible.indexOfFirst { it.id == currentId }.takeIf { it >= 0 } ?: 0
        val a = JSONArray()
        eligible.forEach { tab ->
            a.put(JSONObject().apply {
                put("title", tab.title)
                put("url", tab.url)
                put("desktop", tab.isDesktopMode)
                put("pinned", tab.isPinned)
                put("group", tab.groupName)
                put("groupColor", tab.groupColor)
            })
        }
        return JSONObject().put("version", 2).put("current", currentExportIndex.coerceIn(0, (a.length() - 1).coerceAtLeast(0))).put("tabs", a).toString()
    }

    fun settings(context: Context): String {
        val src = context.getSharedPreferences(com.privbrowse.app.ui.MainActivity.PREFS, Context.MODE_PRIVATE).all
        val out = JSONObject()
        src.forEach { (k, v) ->
            when (v) {
                is String -> out.put(k, v)
                is Boolean -> out.put(k, v)
                is Int -> out.put(k, v)
                is Long -> out.put(k, v)
            }
        }
        return out.toString()
    }
}
