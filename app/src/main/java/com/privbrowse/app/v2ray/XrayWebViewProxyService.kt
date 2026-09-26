package com.privbrowse.app.v2ray

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.privbrowse.app.R

class XrayWebViewProxyService : Service() {
    companion object {
        const val ACTION_CONNECT = "com.privbrowse.app.v2ray.WEBVIEW_CONNECT"
        const val ACTION_DISCONNECT = "com.privbrowse.app.v2ray.WEBVIEW_DISCONNECT"
        const val EXTRA_CONFIG = "config"
        const val EXTRA_SPLIT = "split"
        const val EXTRA_SESSION = "session"
        const val EXTRA_PRESERVE_SESSION = "preserve_session"
        private const val CHANNEL = "v2ray_webview"
        private const val NOTIFICATION_ID = 2602
        private const val TAG = "PrivBrowseWVProxy"
    }

    @Volatile private var running = false
    @Volatile private var generation = 0L

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification("WebView proxy starting"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> startProxy(
                intent.getStringExtra(EXTRA_CONFIG).orEmpty(),
                intent.getStringArrayListExtra(EXTRA_SPLIT)?.toSet().orEmpty(),
                intent.getLongExtra(EXTRA_SESSION, 0L)
            )
            ACTION_DISCONNECT -> stopProxy(intent?.getBooleanExtra(EXTRA_PRESERVE_SESSION, false) == true)
        }
        return START_NOT_STICKY
    }

    private fun startProxy(raw: String, split: Set<String>, session: Long) {
        val myGeneration = ++generation
        Thread {
            try {
                if (raw.isBlank()) throw IllegalArgumentException("V2Ray config is empty")
                if (session == 0L || V2RayStore.activeSession(this) != session) return@Thread
                LibXrayBridge.assertSuccess(LibXrayBridge.invokeTest(raw))
                if (generation != myGeneration || V2RayStore.activeSession(this) != session) return@Thread
                val config = XrayConfigBuilder.buildWebViewConfig(raw, split)
                LibXrayBridge.assertSuccess(LibXrayBridge.invokeRun(config))
                if (generation != myGeneration || V2RayStore.activeSession(this) != session) return@Thread
                running = true
                V2RayStore.setRuntimeState(this, "webview_connected")
                updateNotification("WebView proxy active on 127.0.0.1:${XrayConfigBuilder.WEBVIEW_SOCKS_PORT}")
                WebViewProxy.apply(this)
            } catch (e: Exception) {
                Log.e(TAG, "WebView proxy failed", e)
                WebViewProxy.clear(this)
                if (generation == myGeneration && (session == 0L || V2RayStore.activeSession(this) == session)) {
                    V2RayStore.setRuntimeState(this, "error: ${e.message ?: "start failed"}")
                    stopProxyInternal(preserveState = true)
                }
            }
        }.start()
    }

    private fun stopProxy(preserveSession: Boolean) {
        ++generation
        if (!preserveSession) V2RayStore.clearSession(this)
        Thread { stopProxyInternal() }.start()
    }

    private fun stopProxyInternal(preserveState: Boolean = false) {
        runCatching { WebViewProxy.clear(this) }
        runCatching { LibXrayBridge.invokeStop() }
        running = false
        if (!preserveState) V2RayStore.setRuntimeState(this, "stopped")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "WebView proxy", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun notification(text: String): Notification = NotificationCompat.Builder(this, CHANNEL)
        .setSmallIcon(R.drawable.ic_launcher).setContentTitle("PrivBrowse WebView proxy").setContentText(text).setOngoing(true).build()

    private fun updateNotification(text: String) = getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(text))

    override fun onDestroy() {
        if (running) stopProxyInternal()
        else WebViewProxy.clear(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
