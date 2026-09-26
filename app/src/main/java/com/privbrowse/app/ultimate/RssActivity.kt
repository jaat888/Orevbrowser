package com.privbrowse.app.ultimate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.privbrowse.app.R
import java.net.URL
import java.util.concurrent.Executors
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

class RssActivity : AppCompatActivity() {
    private lateinit var box: LinearLayout
    private val exec = Executors.newSingleThreadExecutor()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        build()
        intent.getStringExtra("url")?.takeIf { it.isNotBlank() }?.let(::load)
    }

    private fun build() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.background_light))
            setPadding(dp(12), dp(10), dp(12), dp(20))
        }
        val url = EditText(this).apply {
            hint = "RSS/Atom URL"
            setSingleLine(true)
            setText(intent.getStringExtra("url"))
        }
        root.addView(url, LinearLayout.LayoutParams(-1, dp(52)))
        root.addView(TextView(this).apply {
            text = "Load feed"
            textSize = 14f
            setTextColor(getColor(R.color.accent_dark))
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener { load(url.text.toString().trim()) }
        })
        box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(box, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun load(feed: String) {
        if (feed.isBlank()) return
        box.removeAllViews()
        box.addView(TextView(this).apply {
            text = "Loading…"
            setTextColor(getColor(R.color.text_muted_light))
        })
        exec.execute {
            val items = runCatching { parse(URL(feed).openStream()) }.getOrElse { emptyList() }
            runOnUiThread {
                box.removeAllViews()
                if (items.isEmpty()) {
                    box.addView(TextView(this).apply {
                        text = "No feed items found"
                        setTextColor(getColor(R.color.text_light))
                    })
                }
                items.take(40).forEach { (title, link) ->
                    val itemView = TextView(this).apply {
                        text = title
                        textSize = 14f
                        setTextColor(getColor(R.color.text_light))
                        setPadding(0, dp(10), 0, dp(10))
                        setOnClickListener {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                        }
                    }
                    box.addView(itemView)
                }
            }
        }
    }

    private fun parse(stream: java.io.InputStream): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        var title = ""
        var link = ""
        var inItem = false
        var current = ""
        val handler = object : DefaultHandler() {
            override fun startElement(uri: String?, localName: String?, qName: String, atts: Attributes?) {
                current = qName
                if (qName.equals("item", true) || qName.equals("entry", true)) {
                    inItem = true
                    title = ""
                    link = ""
                } else if (inItem && qName.equals("link", true) && atts != null) {
                    link = atts.getValue("href").orEmpty()
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (!inItem) return
                val value = String(ch, start, length).trim()
                if (value.isBlank()) return
                when {
                    current.equals("title", true) -> title += value
                    current.equals("link", true) && link.isBlank() -> link += value
                }
            }

            override fun endElement(uri: String?, localName: String?, qName: String) {
                if (qName.equals("item", true) || qName.equals("entry", true)) {
                    if (title.isBlank()) title = "Feed item"
                    if (link.isNotBlank()) out += title.trim() to link.trim()
                    inItem = false
                }
                current = ""
            }
        }
        SAXParserFactory.newInstance().newSAXParser().parse(stream, handler)
        stream.close()
        return out
    }

    override fun onDestroy() {
        exec.shutdownNow()
        super.onDestroy()
    }
}
