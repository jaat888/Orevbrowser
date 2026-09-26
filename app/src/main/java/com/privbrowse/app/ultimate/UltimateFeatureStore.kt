package com.privbrowse.app.ultimate

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object UltimateFeatureStore {
    const val PREFS = "privbrowse_ultimate"
    const val KEY_VERTICAL_TABS = "vertical_tabs"
    const val KEY_RANDOM_CLOSE = "random_close_order"
    const val KEY_FLOATING_BUBBLE = "floating_bubble"
    const val KEY_LOW_POWER = "low_power"
    const val KEY_VIDEO_SPEED = "video_speed"
    const val KEY_AUDIO_ONLY = "audio_only"
    const val KEY_AUTO_AD_SKIP = "auto_ad_skip"
    const val KEY_READER_STRIP = "ai_junk_strip"
    const val KEY_OFFLINE_MODE = "offline_mode"
    const val KEY_READING_STREAK = "reading_streak"
    const val KEY_READING_STREAK_DAY = "reading_streak_day"
    const val KEY_ADS_BLOCKED_TOTAL = "ads_blocked_total"
    const val KEY_TRACKERS_BLOCKED_TOTAL = "trackers_blocked_total"
    const val KEY_BADGES = "badges"
    const val KEY_GESTURE_ACTIONS = "gesture_actions"
    const val KEY_CLIPBOARD_SECONDS = "clipboard_seconds"
    const val KEY_PROFILE = "profile"
    const val KEY_GHOST = "ghost"
    const val KEY_WIFI_WARNING = "wifi_warning"
    const val KEY_VPN_KILL_SWITCH = "vpn_kill_switch"
    const val KEY_BACKGROUND_MEDIA = "background_media"
    const val KEY_AUTO_REVOKE_MEDIA = "auto_revoke_media"
    const val KEY_ONE_HANDED = "one_handed_mode"
    const val KEY_SITE_ISOLATION = "site_isolation"
    const val KEY_NETWORK_USAGE = "network_usage"
    const val KEY_THEME_JSON = "theme_json"
    const val KEY_SCHEDULED_CLEAR_HISTORY = "scheduled_clear_history"
    const val KEY_AI_BUBBLE = "ai_page_bubble"

    fun prefs(context: Context): SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun jsonArray(context: Context, key: String): JSONArray = runCatching { JSONArray(prefs(context).getString(key, "[]") ?: "[]") }.getOrElse { JSONArray() }
    fun putArray(context: Context, key: String, value: JSONArray) = prefs(context).edit().putString(key, value.toString()).apply()
    fun jsonObject(context: Context, key: String): JSONObject = runCatching { JSONObject(prefs(context).getString(key, "{}") ?: "{}") }.getOrElse { JSONObject() }
    fun putObject(context: Context, key: String, value: JSONObject) = prefs(context).edit().putString(key, value.toString()).apply()
}
