package com.privbrowse.app.ultimate

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.privbrowse.app.R
import org.json.JSONObject

class ThemeActivity : AppCompatActivity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private lateinit var json: EditText

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        val raw = runCatching { contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
        if (raw.isNullOrBlank()) { toast("Theme file is empty"); return@registerForActivityResult }
        json.setText(raw)
    }

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); build() }

    private fun build() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(20)); setBackgroundColor(getColor(R.color.background_light)) }
        root.addView(TextView(this).apply { text = "Theme marketplace / import"; textSize = 22f; setTextColor(getColor(R.color.text_light)); setTypeface(typeface, 1) })
        root.addView(TextView(this).apply { text = "JSON format: background, surface, text, accent. Import a local .json theme or paste it below."; textSize = 12f; setTextColor(getColor(R.color.text_muted_light)); setPadding(0, dp(5), 0, dp(10)) })
        json = EditText(this).apply {
            setText(UltimateFeatureStore.prefs(this@ThemeActivity).getString(UltimateFeatureStore.KEY_THEME_JSON, """{"name":"Custom","background":"#101114","surface":"#181b22","text":"#ffffff","accent":"#00d4a8"}"""))
            minLines = 8; gravity = 48
        }
        root.addView(json, LinearLayout.LayoutParams(-1, dp(190)))
        fun action(label: String, click: () -> Unit) = TextView(this).apply { text = label; textSize = 14f; setTextColor(getColor(R.color.accent_dark)); setPadding(dp(10), dp(13), dp(10), dp(13)); setOnClickListener { click() } }
        root.addView(TextView(this).apply { text = "Bundled theme library"; textSize = 13f; setTextColor(getColor(R.color.text_muted_light)); setPadding(0, dp(8), 0, dp(2)) })
        listOf(
            "Midnight" to """{"name":"Midnight","background":"#0b0d12","surface":"#171b24","text":"#f3f4f6","accent":"#5eead4"}""",
            "Ocean" to """{"name":"Ocean","background":"#071521","surface":"#0e2538","text":"#e6f7ff","accent":"#55c2ff"}""",
            "Forest" to """{"name":"Forest","background":"#0c140f","surface":"#17251a","text":"#eff9f0","accent":"#86efac"}"""
        ).forEach { (name, value) -> root.addView(action(name) { json.setText(value); applyTheme() }) }
        root.addView(action("IMPORT .JSON THEME") { picker.launch(arrayOf("application/json", "text/*")) })
        root.addView(action("PREVIEW / APPLY") { applyTheme() })
        root.addView(action("RESET THEME") { UltimateFeatureStore.prefs(this).edit().remove(UltimateFeatureStore.KEY_THEME_JSON).apply(); finish() })
        setContentView(root)
    }

    private fun applyTheme() {
        val obj = runCatching { JSONObject(json.text.toString()) }.getOrNull()
        if (obj == null) { toast("Invalid JSON"); return }
        val required = listOf("background", "surface", "text", "accent")
        if (required.any { runCatching { Color.parseColor(obj.optString(it)) }.isFailure }) { toast("Theme needs valid background/surface/text/accent colors"); return }
        UltimateFeatureStore.prefs(this).edit().putString(UltimateFeatureStore.KEY_THEME_JSON, obj.toString()).apply()
        toast("Theme applied")
        finish()
    }

    private fun toast(text: String) = android.widget.Toast.makeText(this, text, android.widget.Toast.LENGTH_SHORT).show()
}
