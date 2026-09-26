package com.privbrowse.app.ultimate

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.privbrowse.app.data.DbHelper
import java.util.Calendar

/** Scheduled local privacy maintenance. No network activity. */
class CleanupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (UltimateFeatureStore.prefs(context).getBoolean(UltimateFeatureStore.KEY_SCHEDULED_CLEAR_HISTORY, false)) {
            runCatching { DbHelper(context).clearHistory() }
        }
        schedule(context)
    }

    companion object {
        private const val REQUEST = 5208

        fun schedule(context: Context) {
            val prefs = UltimateFeatureStore.prefs(context)
            val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = PendingIntent.getBroadcast(
                context,
                REQUEST,
                Intent(context, CleanupReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or if (android.os.Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
            )
            if (!prefs.getBoolean(UltimateFeatureStore.KEY_SCHEDULED_CLEAR_HISTORY, false)) {
                alarm.cancel(pi)
                return
            }
            val next = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 3)
                set(Calendar.MILLISECOND, 0)
            }
            alarm.cancel(pi)
            alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, next.timeInMillis, AlarmManager.INTERVAL_DAY, pi)
        }
    }
}
