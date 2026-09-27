package com.privbrowse.app.v2ray

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.privbrowse.app.R

class V2RayVpnService : VpnService() {
    companion object {
        const val ACTION_CONNECT = "com.privbrowse.app.v2ray.CONNECT"
        const val ACTION_DISCONNECT = "com.privbrowse.app.v2ray.DISCONNECT"
        const val EXTRA_CONFIG = "config"
        const val EXTRA_SPLIT = "split"
        const val EXTRA_SESSION = "session"
        const val EXTRA_PRESERVE_SESSION = "preserve_session"
        private const val CHANNEL = "v2ray"
        private const val NOTIFICATION_ID = 2601
        private const val TAG = "PrivBrowseV2Ray"
    }

    private var tun: ParcelFileDescriptor? = null
    @Volatile private var running = false
    @Volatile private var generation = 0L
    private val lock = Any()

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification("V2Ray starting"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> startTunnel(
                intent.getStringExtra(EXTRA_CONFIG).orEmpty(),
                intent.getStringArrayListExtra(EXTRA_SPLIT)?.toSet().orEmpty(),
                intent.getLongExtra(EXTRA_SESSION, 0L)
            )
            ACTION_DISCONNECT -> stopTunnel(intent?.getBooleanExtra(EXTRA_PRESERVE_SESSION, false) == true)
        }
        return START_NOT_STICKY
    }

    private fun startTunnel(rawConfig: String, split: Set<String>, session: Long) {
        val myGeneration: Long
        synchronized(lock) {
            generation += 1
            myGeneration = generation
        }
        Thread {
            try {
                if (rawConfig.isBlank()) throw IllegalArgumentException("V2Ray config is empty")
                if (session == 0L || V2RayStore.activeSession(this) != session) return@Thread
                LibXrayBridge.assertSuccess(LibXrayBridge.invokeTest(rawConfig))
                synchronized(lock) { if (generation != myGeneration || V2RayStore.activeSession(this) != session) return@Thread }
                val pfd = Builder()
                    .setSession("PrivBrowse V2Ray")
                    .setMtu(1500)
                    .addAddress("10.245.7.2", 30)
                    .addRoute("0.0.0.0", 0)
                    .addRoute("::", 0)
                    .addAddress("fd00:245:7::2", 128)
                    .addDnsServer("1.1.1.1")
                    .addDnsServer("2606:4700:4700::1111")
                    .addAllowedApplication(packageName)
                    .establish() ?: throw IllegalStateException("Android VPN interface could not be established")
                synchronized(lock) {
                    if (generation != myGeneration) { pfd.close(); return@Thread }
                    tun = pfd
                }
                LibXrayBridge.attachVpn(this)
                if (V2RayStore.activeSession(this) != session) {
                    LibXrayBridge.detachVpn()
                    pfd.close()
                    return@Thread
                }
                val config = XrayConfigBuilder.buildPacketConfig(rawConfig, pfd.fd, split)
                LibXrayBridge.assertSuccess(LibXrayBridge.invokeRun(config))
                synchronized(lock) {
                    if (generation != myGeneration || V2RayStore.activeSession(this) != session) {
                        LibXrayBridge.detachVpn()
                        pfd.close()
                        return@Thread
                    }
                    running = true
                }
                updateNotification("V2Ray packet tunnel active for PrivBrowse")
                V2RayStore.setRuntimeState(this, "packet_connected")
            } catch (e: Exception) {
                Log.e(TAG, "V2Ray packet tunnel failed", e)
                synchronized(lock) {
                    if (generation == myGeneration && (session == 0L || V2RayStore.activeSession(this) == session)) {
                        V2RayStore.setRuntimeState(this, "error: ${e.message ?: "start failed"}")
                    }
                }
                if (session == 0L || V2RayStore.activeSession(this) == session) stopTunnelInternal(myGeneration, preserveState = true)
            }
        }.start()
    }

    private fun stopTunnel(preserveSession: Boolean) {
        synchronized(lock) { generation += 1 }
        if (!preserveSession) V2RayStore.clearSession(this)
        Thread { stopTunnelInternal(null) }.start()
    }

    private fun stopTunnelInternal(expectedGeneration: Long?, preserveState: Boolean = false) {
        synchronized(lock) {
            if (expectedGeneration != null && generation != expectedGeneration) return
        }
        try { LibXrayBridge.invokeStop() } catch (e: Exception) { Log.w(TAG, "Xray stop failed", e) }
        LibXrayBridge.detachVpn()
        running = false
        // Bug fix: tun.close() can throw IOException; unguarded, that skipped
        // setRuntimeState/stopForeground/stopSelf below - leaving a stale foreground
        // notification and a "running" service that never actually stops.
        runCatching { tun?.close() }
        tun = null
        if (!preserveState) V2RayStore.setRuntimeState(this, "stopped")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            (getSystemService(NotificationManager::class.java)).createNotificationChannel(
                NotificationChannel(CHANNEL, "V2Ray tunnel", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun notification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("PrivBrowse V2Ray")
            .setContentText(text)
            .setOngoing(true)
            .build()

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(text))
    }

    override fun onDestroy() {
        if (running) stopTunnelInternal(null)
        else runCatching { tun?.close() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)
}
