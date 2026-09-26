package com.privbrowse.app.v2ray

import android.content.Context
import com.privbrowse.app.privacy.SecureStore

object V2RayStore {
    private const val PREFS = "privbrowse_v2ray"
    private const val ENABLED = "enabled"
    private const val SPLIT = "split_domains"
    private const val RUNTIME = "runtime_state"
    private const val SECURE_CONFIG = "v2ray_json"
    private const val SESSION = "active_session"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun getConfig(c: Context): String = SecureStore.get(c, SECURE_CONFIG) ?: ""
    fun setConfig(c: Context, config: String) = SecureStore.put(c, SECURE_CONFIG, config)
    fun isEnabled(c: Context) = prefs(c).getBoolean(ENABLED, false)
    fun setEnabled(c: Context, value: Boolean) = prefs(c).edit().putBoolean(ENABLED, value).apply()
    fun getSplit(c: Context): Set<String> = prefs(c).getStringSet(SPLIT, emptySet()).orEmpty()
    fun setSplit(c: Context, domains: Set<String>) = prefs(c).edit().putStringSet(SPLIT, domains).apply()
    fun runtimeState(c: Context) = prefs(c).getString(RUNTIME, "stopped") ?: "stopped"
    fun setRuntimeState(c: Context, state: String) = prefs(c).edit().putString(RUNTIME, state).apply()
    fun beginSession(c: Context): Long {
        val id = System.currentTimeMillis() xor System.nanoTime()
        prefs(c).edit().putLong(SESSION, id).apply()
        return id
    }
    fun activeSession(c: Context): Long = prefs(c).getLong(SESSION, 0L)
    fun clearSession(c: Context) = prefs(c).edit().remove(SESSION).putBoolean(ENABLED, false).apply()
}
