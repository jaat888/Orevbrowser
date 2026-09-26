package com.privbrowse.app.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.privbrowse.app.R
import com.privbrowse.app.privacy.BreachChecker
import com.privbrowse.app.privacy.BiometricLock
import com.privbrowse.app.privacy.DomainPrivacyStore
import com.privbrowse.app.privacy.SecureStore

class Phase4Activity : androidx.appcompat.app.AppCompatActivity() {
    private val pickFilter = 410
    private lateinit var status: TextView
    private lateinit var autoIncognito: EditText
    private lateinit var cookieHost: EditText
    private lateinit var cookieMinutes: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 24, 24, 24) }
        root.addView(TextView(this).apply { text = "Phase 4 — Advanced Privacy Controls"; textSize = 22f; setPadding(0,0,0,16) })
        root.addView(TextView(this).apply { text = "Domain-locked auto-incognito (one domain per line)" })
        autoIncognito = EditText(this).apply {
            minLines = 3
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setText(DomainPrivacyStore.getAutoIncognito(this@Phase4Activity).sorted().joinToString("\n"))
        }
        root.addView(autoIncognito, lp())
        root.addView(button("Save auto-incognito") { saveAutoIncognito() })

        root.addView(TextView(this).apply { text = "Per-domain cookie auto-wipe"; setPadding(0,20,0,0) })
        cookieHost = EditText(this).apply { hint = "example.com" }
        cookieMinutes = EditText(this).apply { hint = "minutes (0 = off)"; inputType = InputType.TYPE_CLASS_NUMBER }
        root.addView(cookieHost, lp()); root.addView(cookieMinutes, lp())
        root.addView(button("Save cookie timer") { saveCookieTimer() })

        root.addView(button("Import custom ad-block host list") { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "text/plain"; addCategory(Intent.CATEGORY_OPENABLE) }, pickFilter) })
        root.addView(button("Reader mode notes") {
            AlertDialog.Builder(this).setTitle("Reader mode").setMessage("Use the main browser menu → Reader mode on an article page. It runs locally in the WebView and does not upload page content.").setPositiveButton(R.string.close, null).show()
        })
        root.addView(button("Check an email for breaches") { showBreachDialog() })
        root.addView(button("Store API/password entry securely") { showVaultDialog() })
        root.addView(button("Enable / disable biometric app lock") { toggleBiometric() })
        status = TextView(this).apply { setPadding(0, 18, 0, 0) }
        root.addView(status)
        root.addView(button("Done") { finish() })
        ScrollView(this).apply { addView(root); setContentView(this) }
    }

    private fun lp() = LinearLayout.LayoutParams(-1, -2).apply { topMargin = 8 }
    private fun button(label: String, action: () -> Unit) = Button(this).apply { text = label; setOnClickListener { action() }; layoutParams = lp() }

    private fun saveAutoIncognito() {
        val hosts = autoIncognito.text.toString().lines().map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        DomainPrivacyStore.setAutoIncognito(this, hosts)
        status.text = "Saved ${hosts.size} auto-incognito domain(s)."
    }

    private fun saveCookieTimer() {
        val host = cookieHost.text.toString().trim()
        val mins = cookieMinutes.text.toString().toLongOrNull() ?: 0L
        if (host.isBlank()) { status.text = "Enter a domain first."; return }
        DomainPrivacyStore.setCookieTimer(this, host, mins)
        status.text = if (mins > 0) "Cookie wipe timer saved for $host." else "Cookie timer disabled for $host."
    }

    private fun showBreachDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 8, 24, 0) }
        val email = EditText(this).apply { hint = "Email"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS }
        val key = EditText(this).apply { hint = "HIBP API key"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        box.addView(email, lp()); box.addView(key, lp())
        AlertDialog.Builder(this).setTitle("Breach checker").setView(box).setPositiveButton("Check") { _, _ ->
            status.text = "Checking…"
            Thread {
                val result = BreachChecker.check(email.text.toString(), key.text.toString())
                runOnUiThread { status.text = result.message }
            }.start()
        }.setNegativeButton(R.string.close, null).show()
    }

    private fun showVaultDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 8, 24, 0) }
        val name = EditText(this).apply { hint = "Entry name" }
        val value = EditText(this).apply { hint = "Secret/value"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        box.addView(name, lp()); box.addView(value, lp())
        AlertDialog.Builder(this).setTitle("Local encrypted vault").setView(box).setPositiveButton("Save") { _, _ ->
            val n = name.text.toString().trim(); if (n.isNotBlank()) { SecureStore.put(this, "vault_$n", value.text.toString()); status.text = "Saved encrypted on-device." }
        }.setNegativeButton(R.string.close, null).show()
    }

    private fun toggleBiometric() {
        val prefs = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE)
        val enabled = !prefs.getBoolean("biometric_lock", false)
        if (enabled && !BiometricLock.isAvailable(this)) { status.text = "Biometric is not available."; return }
        prefs.edit().putBoolean("biometric_lock", enabled).apply()
        status.text = if (enabled) "Biometric app lock enabled." else "Biometric app lock disabled."
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != pickFilter || resultCode != Activity.RESULT_OK || data?.data == null) return
        val uri: Uri = data.data ?: return
        try {
            val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            val hosts = text.lines().mapNotNull { line ->
                val clean = line.substringBefore('#').trim().lowercase()
                when {
                    clean.startsWith("0.0.0.0 ") -> clean.substringAfter(" ").trim().takeIf { it.contains('.') }
                    clean.startsWith("127.0.0.1 ") -> clean.substringAfter(" ").trim().takeIf { it.contains('.') }
                    clean.matches(Regex("[a-z0-9.-]+")) -> clean.takeIf { it.contains('.') }
                    else -> null
                }
            }.toSet()
            getSharedPreferences("privbrowse_custom_filters", MODE_PRIVATE).edit().putStringSet("hosts", hosts).apply()
            status.text = "Imported ${hosts.size} custom host rules. New requests will use them."
        } catch (e: Exception) { status.text = "Import failed: ${e.message}" }
    }
}
