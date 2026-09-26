package com.privbrowse.app.ultimate

import android.content.Context

object BookmarkTodoStore {
    private const val PREFS = "bookmark_todos"
    fun get(context: Context, url: String): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(url, "").orEmpty()
    fun put(context: Context, url: String, value: String) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(url, value).apply()
    fun remove(context: Context, url: String) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(url).apply()
}
