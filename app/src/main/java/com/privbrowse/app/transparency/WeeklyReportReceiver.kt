package com.privbrowse.app.transparency

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.privbrowse.app.R
import com.privbrowse.app.data.DbHelper
import com.privbrowse.app.ui.TransparencyLogActivity

private const val REPORT_INTERVAL_MILLIS = 7L * 24 * 60 * 60 * 1000

/**
 * Phase 3 — fires roughly weekly, tallies what was blocked since the last
 * report from the on-device network log, and shows a summary notification
 * ("47 trackers blocked this week, 12 fingerprinting attempts stopped").
 * Everything here reads from the local SQLite log (DbHelper) — nothing is
 * sent anywhere to produce this (Zero-Cloud Guarantee).
 */
class WeeklyReportReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "weekly_privacy_report"
        private const val NOTIFICATION_ID = 4201
        private const val PREFS = "privbrowse_prefs"
        private const val KEY_LAST_REPORT_TS = "last_weekly_report_ts"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val since = prefs.getLong(KEY_LAST_REPORT_TS, System.currentTimeMillis() - REPORT_INTERVAL_MILLIS)
        val stats = DbHelper(context).getBlockedStatsSince(since)
        prefs.edit().putLong(KEY_LAST_REPORT_TS, System.currentTimeMillis()).apply()

        // Nothing blocked this period — skip the notification rather than nag with a "0/0" report.
        if (stats.trackersBlocked == 0 && stats.fingerprintBlocked == 0) return

        ensureChannel(context)
        val body = context.getString(
            R.string.weekly_report_body,
            stats.trackersBlocked,
            stats.fingerprintBlocked
        )

        val contentIntent = PendingIntent.getActivity(
            context, NOTIFICATION_ID,
            Intent(context, TransparencyLogActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(context.getString(R.string.weekly_report_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted (Android 13+) — fail silently,
            // the in-app Network Transparency Log is still viewable any time.
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.weekly_report_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                channel.description = context.getString(R.string.weekly_report_channel_desc)
                manager.createNotificationChannel(channel)
            }
        }
    }
}
