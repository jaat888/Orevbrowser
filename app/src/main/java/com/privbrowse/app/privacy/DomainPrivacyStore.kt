package com.privbrowse.app.privacy

import android.content.Context
import android.net.Uri

/** Small on-device store for domain-scoped privacy preferences. */
object DomainPrivacyStore {
    private const val PREFS = "privbrowse_domain_settings"
    private const val AUTO_INCOGNITO = "auto_incognito"
    private const val COOKIE_TIMERS = "cookie_timers"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun normalizeHost(raw: String): String? {
        var value = raw.trim().lowercase()
        if (value.isBlank()) return null
        if (value.contains("://")) value = Uri.parse(value).host ?: return null
        value = value.substringBefore('/').substringBefore('?').substringBefore('#')
        value = value.removePrefix(".").trimEnd('.')
        if (value.isBlank() || value.contains(" ") || value.contains(":") || !value.contains('.')) return null
        if (value.any { !(it.isLetterOrDigit() || it == '.' || it == '-' || it == '_') }) return null
        return value
    }

    fun getAutoIncognito(context: Context): MutableSet<String> =
        (prefs(context).getStringSet(AUTO_INCOGNITO, emptySet()) ?: emptySet()).toMutableSet()

    fun setAutoIncognito(context: Context, hosts: Set<String>) {
        val normalized = hosts.mapNotNull(::normalizeHost).toSet()
        prefs(context).edit().putStringSet(AUTO_INCOGNITO, normalized).apply()
    }

    fun isAutoIncognito(context: Context, host: String?): Boolean {
        val h = host?.let(::normalizeHost) ?: return false
        return getAutoIncognito(context).any { h == it || h.endsWith(".$it") }
    }

    fun getCookieTimers(context: Context): Map<String, Long> {
        val raw = prefs(context).getStringSet(COOKIE_TIMERS, emptySet()) ?: emptySet()
        return raw.mapNotNull { item ->
            val parts = item.split('=', limit = 2)
            if (parts.size != 2) return@mapNotNull null
            val host = normalizeHost(parts[0]) ?: return@mapNotNull null
            val millis = parts[1].toLongOrNull() ?: return@mapNotNull null
            host to millis
        }.toMap()
    }

    fun timerForHost(context: Context, host: String?): Long? {
        val h = host?.let(::normalizeHost) ?: return null
        val timers = getCookieTimers(context)
        timers[h]?.let { return it }
        return timers.entries
            .filter { h.endsWith(".${it.key}") }
            .maxByOrNull { it.key.length }
            ?.value
    }

    fun setCookieTimer(context: Context, host: String, minutes: Long) {
        val normalized = normalizeHost(host) ?: return
        val safeMinutes = minutes.coerceIn(0L, 365L * 24L * 60L)
        val current = getCookieTimers(context).toMutableMap()
        if (safeMinutes == 0L) current.remove(normalized)
        else current[normalized] = safeMinutes * 60_000L
        val raw = current.map { "${it.key}=${it.value}" }.toSet()
        prefs(context).edit().putStringSet(COOKIE_TIMERS, raw).apply()
    }
}
