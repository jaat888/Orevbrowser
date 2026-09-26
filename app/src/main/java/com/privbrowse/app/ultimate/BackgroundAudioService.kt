package com.privbrowse.app.ultimate

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class BackgroundAudioService : Service() {
    companion object {
        const val ACTION_PAUSE = "com.privbrowse.app.audio.PAUSE"
        const val ACTION_RESUME = "com.privbrowse.app.audio.RESUME"
        const val ACTION_STOP = "com.privbrowse.app.audio.STOP"
        private const val CHANNEL = "audio"
        private const val NOTIFICATION_ID = 7301
    }

    private var player: MediaPlayer? = null
    private var currentUrl: String = ""

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                NotificationChannel(CHANNEL, "Background audio", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> { player?.pause(); publishNotification("Paused") }
            ACTION_RESUME -> { player?.start(); publishNotification("Playing page audio") }
            ACTION_STOP -> { stopSelf(); return START_NOT_STICKY }
            else -> {
                val url = intent?.getStringExtra("url") ?: return START_NOT_STICKY
                currentUrl = url
                player?.release()
                player = MediaPlayer().apply {
                    setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build())
                    setDataSource(url)
                    setOnPreparedListener { it.start(); publishNotification("Playing page audio") }
                    setOnCompletionListener { stopSelf() }
                    setOnErrorListener { _, _, _ -> stopSelf(); true }
                    prepareAsync()
                }
                startForeground(NOTIFICATION_ID, notification("Loading audio…", false))
            }
        }
        return START_STICKY
    }

    private fun actionPending(action: String): PendingIntent = PendingIntent.getService(
        this,
        action.hashCode(),
        Intent(this, BackgroundAudioService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
    )

    private fun notification(text: String, paused: Boolean): android.app.Notification {
        val toggleAction = if (paused) ACTION_RESUME else ACTION_PAUSE
        val toggleLabel = if (paused) "Resume" else "Pause"
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(com.privbrowse.app.R.drawable.ic_tools)
            .setContentTitle("PrivBrowse background audio")
            .setContentText(text)
            .setOngoing(true)
            .addAction(NotificationCompat.Action(com.privbrowse.app.R.drawable.ic_tools, toggleLabel, actionPending(toggleAction)))
            .addAction(NotificationCompat.Action(com.privbrowse.app.R.drawable.ic_tools, "Stop", actionPending(ACTION_STOP)))
            .build()
    }

    private fun publishNotification(text: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification(text, player?.isPlaying != true))
    }

    override fun onDestroy() {
        player?.runCatching { stop() }
        player?.release()
        player = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
