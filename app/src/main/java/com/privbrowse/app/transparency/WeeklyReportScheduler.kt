package com.privbrowse.app.transparency

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Phase 3 — schedules the recurring Weekly Privacy Report alarm.
 *
 * Uses an inexact repeating alarm (not setExactAndAllowWhileIdle) on
 * purpose: a notification summarizing last week's blocked-tracker count
 * doesn't need to fire at a precise minute, and staying inexact means the
 * app doesn't need the SCHEDULE_EXACT_ALARM special access Android 12+
 * gates behind a separate user grant.
 */
object WeeklyReportScheduler {

    private const val REQUEST_CODE = 4200

    /** Safe to call on every app start — re-registering an identical alarm is a no-op for AlarmManager. */
    fun scheduleIfNeeded(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val existing = pendingIntent(context, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (existing != null) return
        alarmManager.setInexactRepeating(
            AlarmManager.RTC,
            System.currentTimeMillis() + AlarmManager.INTERVAL_DAY,
            AlarmManager.INTERVAL_DAY * 7,
            pendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
        alarmManager.cancel(pi)
        pi.cancel()
    }

    private fun pendingIntent(context: Context, flags: Int): PendingIntent? {
        val intent = Intent(context, WeeklyReportReceiver::class.java)
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }
}
