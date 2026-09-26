package com.privbrowse.app.ultimate

import android.content.Context
import android.net.TrafficStats
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * App-UID network accounting using Android TrafficStats snapshots.
 * WebView does not expose reliable per-tab byte counters, so the recorder
 * intentionally reports app-level RX/TX usage for the selected 7-day window.
 */
object NetworkUsageRecorder {
    private const val PREFS = "privbrowse_network_usage"
    private const val KEY_DAYS = "days"

    fun snapshot(context: Context) {
        val rx = TrafficStats.getUidRxBytes(android.os.Process.myUid())
        val tx = TrafficStats.getUidTxBytes(android.os.Process.myUid())
        if (rx < 0L || tx < 0L) return
        val key = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = runCatching { JSONArray(p.getString(KEY_DAYS, "[]") ?: "[]") }.getOrElse { JSONArray() }
        var found = false
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optString("day") == key) {
                o.put("rx", rx).put("tx", tx)
                found = true
                break
            }
        }
        if (!found) arr.put(JSONObject().put("day", key).put("rx", rx).put("tx", tx))
        val cutoff = System.currentTimeMillis() - 8L * 24 * 3600_000L
        val out = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val day = o.optString("day")
            val millis = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(day)?.time ?: 0L }.getOrDefault(0L)
            if (millis >= cutoff || day == key) out.put(o)
        }
        p.edit().putString(KEY_DAYS, out.toString()).apply()
    }

    fun weeklyRxTx(context: Context): Pair<Long, Long> {
        val nowRx = TrafficStats.getUidRxBytes(android.os.Process.myUid()).takeIf { it >= 0L } ?: 0L
        val nowTx = TrafficStats.getUidTxBytes(android.os.Process.myUid()).takeIf { it >= 0L } ?: 0L
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = runCatching { JSONArray(p.getString(KEY_DAYS, "[]") ?: "[]") }.getOrElse { JSONArray() }
        var minRx: Long? = null
        var minTx: Long? = null
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            minRx = listOfNotNull(minRx, o.optLong("rx").takeIf { it >= 0L }).minOrNull()
            minTx = listOfNotNull(minTx, o.optLong("tx").takeIf { it >= 0L }).minOrNull()
        }
        return (nowRx - (minRx ?: nowRx)).coerceAtLeast(0L) to (nowTx - (minTx ?: nowTx)).coerceAtLeast(0L)
    }
}
