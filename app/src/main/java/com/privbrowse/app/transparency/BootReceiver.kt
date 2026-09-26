package com.privbrowse.app.transparency

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Phase 3 — repeating alarms don't survive a reboot, so this re-arms the
 * Weekly Privacy Report alarm once the device finishes booting.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            WeeklyReportScheduler.scheduleIfNeeded(context)
        }
    }
}
