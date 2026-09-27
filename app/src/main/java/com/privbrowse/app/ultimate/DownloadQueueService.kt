package com.privbrowse.app.ultimate

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class DownloadQueueService : Service() {
    private val exec = Executors.newSingleThreadExecutor()
    private val channel = "downloads"
    @Volatile private var stopped = false

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                NotificationChannel(channel, "Downloads", NotificationManager.IMPORTANCE_LOW)
            )
        }
        startForeground(7101, notification("Download queue active"))
        exec.execute { process() }
    }

    private fun notification(text: String) = NotificationCompat.Builder(this, channel)
        .setSmallIcon(com.privbrowse.app.R.drawable.ic_tools)
        .setContentTitle("PrivBrowse downloads")
        .setContentText(text)
        .setOngoing(true)
        .build()

    private fun isStopped() = stopped

    private fun process() {
        while (!isStopped()) {
            val next = DownloadQueueStore.list(this).firstOrNull { it.status == "queued" || it.status == "downloading" } ?: break
            download(next)
        }
        stopSelf()
    }

    private fun download(item: QueueItem) {
        val category = when {
            item.mime.startsWith("image") -> "images"
            item.mime.startsWith("video") -> "video"
            item.mime.startsWith("text") || item.mime.contains("pdf") -> "docs"
            else -> "other"
        }
        val dir = File(getExternalFilesDir("downloads"), "PrivBrowse/$category").apply { mkdirs() }
        val target = File(dir, item.file.ifBlank { "download-${item.id}" })
        var existing = if (target.exists()) target.length() else 0L

        try {
            val connection = URL(item.url).openConnection() as HttpURLConnection
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            if (existing > 0L) connection.setRequestProperty("Range", "bytes=$existing-")
            connection.connect()

            if (existing > 0L && connection.responseCode == 200) {
                existing = 0L
                target.outputStream().use { it.flush() }
            }
            if (connection.responseCode == 416) {
                DownloadQueueStore.update(this, item.id, existing, existing, "complete")
                connection.disconnect()
                return
            }
            if (connection.responseCode !in 200..299) throw java.io.IOException("HTTP ${connection.responseCode}")

            val totalHeader = connection.getHeaderFieldLong("Content-Length", -1L)
            val total = if (totalHeader > 0L) totalHeader + existing else -1L
            DownloadQueueStore.update(this, item.id, existing, total, "downloading")

            connection.inputStream.use { input ->
                RandomAccessFile(target, "rw").use { raf ->
                    raf.seek(existing)
                    val buffer = ByteArray(32 * 1024)
                    var sampleStart = existing
                    var sampleTime = System.currentTimeMillis()
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        raf.write(buffer, 0, read)
                        existing += read
                        val now = System.currentTimeMillis()
                        if (now - sampleTime >= 500L) {
                            DownloadQueueStore.update(this, item.id, existing, total, "downloading")
                            val limit = UltimateFeatureStore.prefs(this).getLong("download_speed_bps", 0L)
                            if (limit > 0L) {
                                val elapsed = now - sampleTime
                                val bytes = existing - sampleStart
                                val expected = (bytes.toDouble() / limit * 1000.0).toLong()
                                if (expected > elapsed) Thread.sleep((expected - elapsed).coerceAtMost(1000L))
                            }
                            sampleStart = existing
                            sampleTime = System.currentTimeMillis()
                        }
                    }
                }
            }
            connection.disconnect()
            DownloadQueueStore.update(this, item.id, existing, total, "complete")
            val safety = DownloadSafetyScanner.inspect(target, item.mime)
            DownloadQueueStore.update(this, item.id, existing, total, "complete", suspicious = safety.suspicious)
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(
                item.id.toInt(),
                NotificationCompat.Builder(this, channel)
                    .setSmallIcon(com.privbrowse.app.R.drawable.ic_tools)
                    .setContentTitle("Download complete")
                    .setContentText(if (safety.suspicious) "Warning: ${safety.reason}" else item.file)
                    .setAutoCancel(true)
                    .build()
            )
        } catch (_: Throwable) {
            val attempts = DownloadQueueStore.markAttempt(this, item, if (item.attempts >= 2) "failed" else "queued")
            // Bug fix: process()'s loop immediately re-picks any "queued" item with no
            // delay, so a persistent failure (e.g. server down) hammered the same URL
            // 3 times back-to-back with zero backoff. A short, attempt-scaled pause
            // gives a transient failure a chance to clear instead of busy-looping.
            if (attempts < 3) runCatching { Thread.sleep(2_000L * attempts) }
            if (attempts >= 3) {
                val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(
                    item.id.toInt(),
                    NotificationCompat.Builder(this, channel)
                        .setSmallIcon(com.privbrowse.app.R.drawable.ic_tools)
                        .setContentTitle("Download failed")
                        .setContentText(item.file)
                        .setAutoCancel(true)
                        .build()
                )
            }
        }
    }

    override fun onDestroy() {
        stopped = true
        exec.shutdownNow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
