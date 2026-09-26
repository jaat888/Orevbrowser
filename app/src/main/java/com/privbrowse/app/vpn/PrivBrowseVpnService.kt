package com.privbrowse.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.privbrowse.app.R
import com.privbrowse.app.ui.MainActivity

/**
 * Phase 5 discovery/selection service.
 *
 * This version intentionally does NOT establish a fake TUN. A TUN without a
 * real OpenVPN/OpenVPN3 engine would black-hole browser traffic. VPN Gate
 * discovery remains available, but connection is rejected until a real
 * OpenVPN engine is bundled.
 */
class PrivBrowseVpnService : android.net.VpnService() {
    companion object {
        const val ACTION_CONNECT = "com.privbrowse.app.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.privbrowse.app.vpn.DISCONNECT"
        const val EXTRA_ROTATION_MINUTES = "rotation_minutes"
        private const val CHANNEL = "vpn_gate"
        private const val NOTIFICATION_ID = 2501
        private const val TAG = "PrivBrowseVpn"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var candidatePool: List<VpnServer> = emptyList()
    private var rotationMinutes = 0

    private val rotationRunnable: Runnable = Runnable {
        // No-op by design until a real OpenVPN engine exists.
        handler.removeCallbacks(rotationRunnable)
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                rotationMinutes = intent.getIntExtra(EXTRA_ROTATION_MINUTES, 0).coerceIn(0, 5)
                Thread { discoverOnly() }.start()
            }
            ACTION_DISCONNECT -> disconnect()
        }
        return START_NOT_STICKY
    }

    private fun discoverOnly() {
        try {
            candidatePool = VpnServerSelector.topCandidates(VpnGateApi.fetchServers())
            Log.i(TAG, "VPN Gate discovery found ${candidatePool.size} candidate(s); connection disabled without OpenVPN engine")
        } catch (e: Exception) {
            Log.e(TAG, "VPN Gate discovery failed", e)
        } finally {
            stopSelf()
        }
    }

    private fun disconnect() {
        handler.removeCallbacks(rotationRunnable)
        stopSelf()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "VPN Gate", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun notification(text: String): Notification = NotificationCompat.Builder(this, CHANNEL)
        .setSmallIcon(R.drawable.ic_launcher)
        .setContentTitle("PrivBrowse VPN Gate")
        .setContentText(text)
        .setOngoing(true)
        .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
        .build()

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
