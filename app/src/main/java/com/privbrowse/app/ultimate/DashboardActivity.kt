package com.privbrowse.app.ultimate

import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.privbrowse.app.R
import com.privbrowse.app.data.DbHelper
import java.net.URL
import java.util.concurrent.Executors

class DashboardActivity : AppCompatActivity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private val exec = Executors.newSingleThreadExecutor()

    override fun onCreate(b: Bundle?) { super.onCreate(b); build() }

    private fun build() {
        val db = DbHelper(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(getColor(R.color.background_light)); setPadding(dp(14), dp(12), dp(14), dp(22)) }
        root.addView(TextView(this).apply { text = "PrivBrowse Home Dashboard"; textSize = 23f; setTextColor(getColor(R.color.text_light)); setTypeface(typeface, 1) })
        val weather = TextView(this).apply { text = "Weather: loading…"; textSize = 13f; setTextColor(getColor(R.color.text_light)); setPadding(0, dp(12), 0, dp(12)) }
        root.addView(weather)
        exec.execute { val value = runCatching { URL("https://wttr.in/?format=3").readText().trim() }.getOrElse { "Weather unavailable" }; runOnUiThread { weather.text = "Weather  ·  $value" } }
        root.addView(TextView(this).apply { text = "To-do / bookmark notes"; textSize = 16f; setTextColor(getColor(R.color.text_light)); setTypeface(typeface, 1); setPadding(0, dp(8), 0, dp(8)); setOnClickListener { todoDialog() } })
        root.addView(TextView(this).apply { text = "RSS / Atom feed"; textSize = 16f; setTextColor(getColor(R.color.text_light)); setTypeface(typeface, 1); setPadding(0, dp(8), 0, dp(8)); setOnClickListener { rssDialog() } })
        root.addView(TextView(this).apply { text = "Recent + frequent sites"; textSize = 16f; setTextColor(getColor(R.color.text_light)); setTypeface(typeface, 1); setPadding(0, dp(8), 0, dp(8)); setOnClickListener { frequentDialog(db) } })
        val storedTodo = getSharedPreferences("dashboard", 0).getString("todo", "").orEmpty()
        if (storedTodo.isNotBlank()) root.addView(TextView(this).apply { text = "Todo: $storedTodo"; textSize = 13f; setTextColor(getColor(R.color.text_muted_light)); setPadding(0, dp(6), 0, dp(8)) })
        setContentView(root)
    }

    private fun frequentDialog(db: DbHelper) {
        val history = db.getHistory()
        val frequent = history.groupBy { android.net.Uri.parse(it.url).host.orEmpty() }.mapValues { it.value.size }.entries.sortedByDescending { it.value }.take(12)
        val recent = history.take(12)
        val message = buildString {
            append("RECENT\n")
            recent.forEach { append("• ").append(it.title.ifBlank { it.url }).append("\\n") }
            append("\\nFREQUENT\n")
            frequent.forEach { append("• ").append(it.key).append(" ( ").append(it.value).append(" visits)\\n") }
            if (history.isEmpty()) append("\\nNo history yet.")
        }
        AlertDialog.Builder(this).setTitle("Recent + frequently visited").setMessage(message).setPositiveButton("Close", null).show()
    }

    private fun todoDialog() { val e = EditText(this).apply { hint = "Write a local todo"; minLines = 3 }; AlertDialog.Builder(this).setTitle("Dashboard todo").setView(e).setPositiveButton("Save") { _, _ -> getSharedPreferences("dashboard", 0).edit().putString("todo", e.text.toString()).apply(); build() }.setNegativeButton("Cancel", null).show() }
    private fun rssDialog() { val e = EditText(this).apply { hint = "https://example.com/feed.xml"; setSingleLine(true) }; AlertDialog.Builder(this).setTitle("RSS / Atom feed").setView(e).setPositiveButton("Open") { _, _ -> startActivity(android.content.Intent(this, RssActivity::class.java).putExtra("url", e.text.toString())) }.setNegativeButton("Cancel", null).show() }
    override fun onDestroy() { exec.shutdownNow(); super.onDestroy() }
}
