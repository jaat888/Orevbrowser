package com.privbrowse.app.ultimate

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.privbrowse.app.R
import kotlin.math.max

/**
 * Real downloads screen. The old version rendered the queue once as plain
 * text and never looked at it again, so an in-progress download just sat
 * there with no visible change until you manually reopened the screen —
 * looked exactly like "it doesn't show what's downloading." The actual
 * download engine (DownloadQueueService) was fine; only this screen wasn't
 * watching it. This version polls the same store every 700ms while visible
 * and redraws, with a real progress bar and a clear status per item.
 */
class DownloadQueueActivity : AppCompatActivity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun color(id: Int) = ContextCompat.getColor(this, id)

    private lateinit var listBox: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private val refreshTick = object : Runnable {
        override fun run() {
            renderList()
            handler.postDelayed(this, 700L)
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        build()
    }

    override fun onResume() { super.onResume(); handler.post(refreshTick) }
    override fun onPause() { super.onPause(); handler.removeCallbacks(refreshTick) }

    private fun build() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.background_light))
        }
        root.addView(TextView(this).apply {
            text = "Downloads"
            textSize = 22f
            setTextColor(color(R.color.text_light))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(14), dp(14), dp(14), dp(6))
        })

        val urlRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(14), dp(4), dp(14), dp(8))
        }
        val urlInput = EditText(this).apply {
            hint = "HTTPS URL to download"
            setSingleLine(true)
        }
        urlRow.addView(urlInput, LinearLayout.LayoutParams(0, dp(52), 1f))
        urlRow.addView(TextView(this).apply {
            text = "Queue"
            textSize = 14f
            gravity = android.view.Gravity.CENTER
            setTextColor(color(R.color.accent_dark))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(14), 0, dp(6), 0)
            setOnClickListener {
                val u = urlInput.text.toString().trim()
                if (u.isNotBlank()) {
                    val file = u.substringAfterLast('/').substringBefore('?').ifBlank { "download" }
                    DownloadQueueStore.add(this@DownloadQueueActivity, u, file, null)
                    ContextCompat.startForegroundService(this@DownloadQueueActivity, Intent(this@DownloadQueueActivity, DownloadQueueService::class.java))
                    urlInput.setText("")
                    renderList()
                }
            }
        }, LinearLayout.LayoutParams(-2, -2))
        root.addView(urlRow)

        listBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(4), dp(14), dp(24))
        }
        val scroll = ScrollView(this)
        scroll.addView(listBox, LinearLayout.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        setContentView(root)
        renderList()
    }

    private fun humanSize(bytes: Long): String {
        if (bytes < 0) return "?"
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.0f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        return "%.2f GB".format(mb / 1024.0)
    }

    private fun statusColor(status: String): Int = when (status) {
        "downloading" -> color(R.color.accent)
        "complete" -> Color.parseColor("#2E7D32")
        "failed" -> Color.parseColor("#C62828")
        "queued" -> color(R.color.text_muted_light)
        else -> color(R.color.text_muted_light)
    }

    private fun statusLabel(status: String): String = when (status) {
        "downloading" -> "DOWNLOADING"
        "complete" -> "COMPLETE"
        "failed" -> "FAILED"
        "queued" -> "QUEUED"
        else -> status.uppercase()
    }

    private fun renderList() {
        val items = DownloadQueueStore.list(this)
            .sortedWith(compareBy({ if (it.status == "complete") 1 else 0 }, { -it.id }))
        listBox.removeAllViews()
        if (items.isEmpty()) {
            listBox.addView(TextView(this).apply {
                text = "No downloads yet."
                textSize = 13f
                setTextColor(color(R.color.text_muted_light))
                setPadding(dp(4), dp(20), dp(4), dp(4))
            })
            return
        }
        items.forEach { item ->
            val card = MaterialCardView(this).apply {
                radius = dp(12).toFloat(); cardElevation = 0f; strokeWidth = dp(1)
                strokeColor = color(R.color.divider); setCardBackgroundColor(color(R.color.surface_light))
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }
            }
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }
            box.addView(TextView(this).apply {
                text = item.file
                textSize = 14f
                setTextColor(color(R.color.text_light))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                maxLines = 1
            })
            box.addView(TextView(this).apply {
                text = statusLabel(item.status) + if (item.suspicious) " · flagged" else ""
                textSize = 11f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(statusColor(item.status))
                setPadding(0, dp(4), 0, dp(4))
            })
            if (item.status == "downloading" || item.status == "queued") {
                val determinate = item.total > 0
                box.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                    isIndeterminate = !determinate
                    if (determinate) {
                        max = 1000
                        progress = ((item.bytes.toDouble() / item.total.toDouble()) * 1000).toInt().coerceIn(0, 1000)
                    }
                }, LinearLayout.LayoutParams(-1, dp(6)))
            }
            box.addView(TextView(this).apply {
                text = if (item.total > 0)
                    "${humanSize(item.bytes)} / ${humanSize(item.total)} (${max(0, (item.bytes * 100 / item.total).toInt())}%)"
                else humanSize(item.bytes)
                textSize = 12f
                setTextColor(color(R.color.text_muted_light))
                setPadding(0, dp(4), 0, 0)
            })
            box.setOnLongClickListener {
                DownloadQueueStore.remove(this@DownloadQueueActivity, item.id)
                renderList()
                true
            }
            card.addView(box)
            listBox.addView(card)
        }
        listBox.addView(TextView(this).apply {
            text = "Tap and hold a download to remove it from the list."
            textSize = 11f
            setTextColor(color(R.color.text_muted_light))
            setPadding(dp(4), dp(10), dp(4), dp(4))
        })
    }
}
