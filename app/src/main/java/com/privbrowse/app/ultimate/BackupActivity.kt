package com.privbrowse.app.ultimate

import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.privbrowse.app.R
import java.io.File

/** Full settings backup export/import UI. Browser databases remain profile-local. */
class BackupActivity : AppCompatActivity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private val exportPicker = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        val backup = runCatching { BackupManager.createLocalBackup(this) }.getOrNull()
        if (backup == null) { toast("Backup creation failed"); return@registerForActivityResult }
        runCatching {
            contentResolver.openOutputStream(uri)?.use { out -> backup.inputStream().use { it.copyTo(out) } }
                ?: error("Unable to open destination")
        }.onSuccess { toast("Backup exported") }
            .onFailure { toast("Export failed: ${it.message}") }
    }

    private val importPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        val ok = runCatching { BackupManager.restoreFromUri(this, uri) }.getOrDefault(false)
        toast(if (ok) "Settings restored. Restart PrivBrowse to apply profile/WebView changes." else "Backup import failed")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        build()
    }

    private fun build() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(24))
            setBackgroundColor(getColor(R.color.background_light))
        }
        root.addView(TextView(this).apply {
            text = "Settings backup & restore"
            textSize = 23f
            setTextColor(getColor(R.color.text_light))
            setTypeface(typeface, 1)
        })
        root.addView(TextView(this).apply {
            text = "Export/import all PrivBrowse preference settings as one compressed file. Bookmarks, history and reading data stay inside the active profile database."
            textSize = 12f
            setTextColor(getColor(R.color.text_muted_light))
            setPadding(0, dp(6), 0, dp(16))
        })
        root.addView(action("EXPORT BACKUP") {
            val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US).format(java.util.Date())
            exportPicker.launch("privbrowse-backup-$stamp.json.gz")
        })
        root.addView(action("IMPORT BACKUP") {
            importPicker.launch(arrayOf("application/gzip", "application/octet-stream", "application/json", "text/*"))
        })
        root.addView(action("CREATE & SHARE BACKUP") {
            if (!BackupManager.shareLatest(this)) toast("Unable to create backup")
        })
        root.addView(action("SHOW BACKUP FOLDER") {
            val dir = File(getExternalFilesDir("Backups") ?: filesDir, "PrivBrowse")
            toast("Saved in: ${dir.absolutePath}")
        })
        setContentView(root)
    }

    private fun action(label: String, click: () -> Unit) = TextView(this).apply {
        text = label
        textSize = 14f
        setTextColor(getColor(R.color.accent_dark))
        setPadding(dp(10), dp(15), dp(10), dp(15))
        setOnClickListener { click() }
    }
}
