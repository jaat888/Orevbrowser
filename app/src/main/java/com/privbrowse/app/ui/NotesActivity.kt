package com.privbrowse.app.ui

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject

class NotesActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_URL = "url"
        private const val PREFS = "privbrowse_notes"
        private const val NOTES = "notes"
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun color(id: Int) = ContextCompat.getColor(this, id)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        build()
    }

    private fun build() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(color(R.color.background_light)) }
        root.addView(toolbar())
        val scroll = ScrollView(this)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(8), dp(14), dp(28)) }
        box.addView(action("New note", "Create a note and optionally attach the current page." ) { newNote() })
        loadNotes().forEach { box.addView(noteCard(it), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }) }
        scroll.addView(box)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun toolbar(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(4), dp(6), dp(4), dp(4))
        addView(TextView(this@NotesActivity).apply { text = "‹"; textSize = 38f; gravity = Gravity.CENTER; setTextColor(color(R.color.text_light)); setOnClickListener { finish() } }, LinearLayout.LayoutParams(dp(44), dp(50)))
        addView(TextView(this@NotesActivity).apply { text = "Notes"; textSize = 22f; setTextColor(color(R.color.text_light)); setTypeface(typeface, android.graphics.Typeface.BOLD) }, LinearLayout.LayoutParams(0, -2, 1f))
    }

    private fun action(title: String, subtitle: String, listener: () -> Unit): View = card().apply {
        setOnClickListener { listener() }
        val box = LinearLayout(this@NotesActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(12)) }
        box.addView(TextView(this@NotesActivity).apply { text = title; textSize = 15f; setTextColor(color(R.color.text_light)); setTypeface(typeface, android.graphics.Typeface.BOLD) })
        box.addView(TextView(this@NotesActivity).apply { text = subtitle; textSize = 12f; setTextColor(color(R.color.text_muted_light)); setPadding(0, dp(2), 0, 0) })
        addView(box)
    }

    private fun noteCard(note: JSONObject): View = card().apply {
        val box = LinearLayout(this@NotesActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(10)) }
        box.addView(TextView(this@NotesActivity).apply { text = note.optString("title", "Note"); textSize = 15f; setTextColor(color(R.color.text_light)); setTypeface(typeface, android.graphics.Typeface.BOLD) })
        val body = note.optString("body")
        box.addView(TextView(this@NotesActivity).apply { text = body; textSize = 13f; setTextColor(color(R.color.text_light)); setPadding(0, dp(5), 0, 0) })
        val url = note.optString("url")
        if (url.isNotBlank()) box.addView(TextView(this@NotesActivity).apply { text = url; textSize = 10.5f; setTextColor(color(R.color.text_muted_light)); setPadding(0, dp(4), 0, 0); maxLines = 1 })
        box.addView(TextView(this@NotesActivity).apply {
            text = "Delete"; textSize = 11f; setTextColor(color(R.color.accent_dark)); setPadding(0, dp(10), 0, 0)
            setOnClickListener { deleteNote(note.optLong("id")); build() }
        })
        addView(box)
    }

    private fun card() = com.google.android.material.card.MaterialCardView(this).apply {
        radius = dp(14).toFloat(); cardElevation = 0f; strokeWidth = dp(1); strokeColor = color(R.color.divider); setCardBackgroundColor(color(R.color.surface_light))
    }

    private fun newNote() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(4), dp(8), 0) }
        val title = EditText(this).apply { hint = "Title"; setSingleLine(true) }
        val body = EditText(this).apply { hint = "Note"; minLines = 5; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE }
        box.addView(title); box.addView(body, LinearLayout.LayoutParams(-1, dp(130)).apply { topMargin = dp(8) })
        AlertDialog.Builder(this).setTitle("New note").setView(box).setNegativeButton("Cancel", null).setPositiveButton("Save") { _, _ ->
            val arr = loadNotesArray()
            val o = JSONObject().apply {
                put("id", System.currentTimeMillis()); put("title", title.text.toString().trim().ifBlank { "Untitled" }); put("body", body.text.toString()); put("url", intent.getStringExtra(EXTRA_URL).orEmpty()); put("timestamp", System.currentTimeMillis())
            }
            arr.put(o); saveNotes(arr); build()
        }.show()
    }

    private fun loadNotes(): List<JSONObject> {
        val arr = loadNotesArray(); val list = ArrayList<JSONObject>()
        for (i in arr.length() - 1 downTo 0) list += arr.getJSONObject(i)
        return list
    }
    private fun loadNotesArray(): JSONArray = runCatching { JSONArray(getSharedPreferences(PREFS, MODE_PRIVATE).getString(NOTES, "[]") ?: "[]") }.getOrElse { JSONArray() }
    private fun saveNotes(arr: JSONArray) = getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(NOTES, arr.toString()).apply()
    private fun deleteNote(id: Long) { val src = loadNotesArray(); val out = JSONArray(); for (i in 0 until src.length()) if (src.getJSONObject(i).optLong("id") != id) out.put(src.getJSONObject(i)); saveNotes(out) }
}
