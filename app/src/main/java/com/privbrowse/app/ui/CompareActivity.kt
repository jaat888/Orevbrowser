package com.privbrowse.app.ui

import com.privbrowse.app.R
import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Html
import android.view.Gravity
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class CompareActivity : AppCompatActivity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun color(id: Int) = ContextCompat.getColor(this, id)
    private lateinit var left: WebView
    private lateinit var right: WebView
    private lateinit var leftUrl: EditText
    private lateinit var rightUrl: EditText

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(color(R.color.background_light)) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(6), dp(6), dp(6), dp(6)) }
        top.addView(TextView(this).apply { text = "‹"; textSize = 38f; gravity = Gravity.CENTER; setTextColor(color(R.color.text_light)); setOnClickListener { finish() } }, LinearLayout.LayoutParams(dp(44), dp(48)))
        leftUrl = EditText(this).apply { hint = "First URL"; setSingleLine(true); setText(intent.getStringExtra("left")) }
        rightUrl = EditText(this).apply { hint = "Second URL"; setSingleLine(true); setText(intent.getStringExtra("right")) }
        top.addView(leftUrl, LinearLayout.LayoutParams(0, dp(48), 1f)); top.addView(rightUrl, LinearLayout.LayoutParams(0, dp(48), 1f)); root.addView(top)
        val actions = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        fun button(label: String, click: () -> Unit) = TextView(this).apply { text = "  $label  "; textSize = 12f; setTextColor(color(R.color.accent_dark)); setPadding(dp(4), dp(7), dp(4), dp(7)); setOnClickListener { click() } }
        actions.addView(button("Compare") { left.loadUrl(normalize(leftUrl.text.toString())); right.loadUrl(normalize(rightUrl.text.toString())) })
        actions.addView(button("Diff") { showDiff() })
        root.addView(actions)
        val split = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        left = web(); right = web(); split.addView(left, LinearLayout.LayoutParams(0, 0, 1f)); split.addView(right, LinearLayout.LayoutParams(0, 0, 1f)); root.addView(split, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        left.loadUrl(normalize(intent.getStringExtra("left").orEmpty())); right.loadUrl(normalize(intent.getStringExtra("right").orEmpty()))
    }

    private fun showDiff() {
        left.evaluateJavascript("document.body ? document.body.innerText : ''") { aRaw ->
            right.evaluateJavascript("document.body ? document.body.innerText : ''") { bRaw ->
                val a = runCatching { org.json.JSONTokener(aRaw).nextValue().toString() }.getOrElse { aRaw.trim('\\', '"') }
                val b = runCatching { org.json.JSONTokener(bRaw).nextValue().toString() }.getOrElse { bRaw.trim('\\', '"') }
                val linesA = a.lines().take(1000); val linesB = b.lines().take(1000); val setB = linesB.toSet(); val setA = linesA.toSet()
                val out = StringBuilder("Only in first page:\n")
                linesA.filter { it.isNotBlank() && it !in setB }.take(80).forEach { out.append("− ").append(it).append('\n') }
                out.append("\\nOnly in second page:\n")
                linesB.filter { it.isNotBlank() && it !in setA }.take(80).forEach { out.append("+ ").append(it).append('\n') }
                if (out.length < 35) out.append("No text-only differences found in the sampled content.")
                AlertDialog.Builder(this).setTitle("Text diff").setMessage(out.toString()).setPositiveButton("Close", null).show()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled") private fun web() = WebView(this).apply { settings.javaScriptEnabled = true; settings.domStorageEnabled = true; webViewClient = WebViewClient(); setBackgroundColor(color(R.color.background_light)) }
    private fun normalize(value: String): String = if (value.startsWith("http://") || value.startsWith("https://")) value else "https://$value"
}
