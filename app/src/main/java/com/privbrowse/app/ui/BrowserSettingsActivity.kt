package com.privbrowse.app.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.privbrowse.app.R
import com.privbrowse.app.adblock.BlockLevel
import com.privbrowse.app.adblock.AdBlocker
import com.privbrowse.app.privacy.BiometricLock

/**
 * Compact browser settings / feature center. It intentionally keeps advanced
 * controls behind grouped cards rather than exposing one giant flat menu.
 */
class BrowserSettingsActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var adBlocker: AdBlocker

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun color(res: Int): Int = ContextCompat.getColor(this, res)

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE)
        super.onCreate(savedInstanceState)
        adBlocker = AdBlocker(this)
        title = getString(R.string.settings_title)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(color(R.color.background_light))
            clipToPadding = false
            setPadding(0, 0, 0, dp(24))
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(10), dp(18), dp(26))
        }
        root.addView(buildToolbar())
        root.addView(buildHeaderCard(), lp().apply { topMargin = dp(8) })

        root.addView(section("Privacy"), lp().apply { topMargin = dp(20) })
        root.addView(actionRow("Ad blocker mode", adBlockLabel()) { cycleAdBlocker() }, lp().apply { topMargin = dp(4) })
        root.addView(toggleRow("Fresh identity per tab", "Randomize browser fingerprint signals for new tabs.", prefs.getBoolean(MainActivity.KEY_FRESH_IDENTITY, true)) { setPref(MainActivity.KEY_FRESH_IDENTITY, it) })
        root.addView(toggleRow("Do Not Track signal", "Advertise navigator.doNotTrack=1 to pages.", prefs.getBoolean(MainActivity.KEY_DNT, true)) { setPref(MainActivity.KEY_DNT, it) })
        root.addView(toggleRow("Global Privacy Control", "Expose the standard privacy opt-out signal to sites.", prefs.getBoolean(MainActivity.KEY_GPC, true)) { setPref(MainActivity.KEY_GPC, it) })
        root.addView(toggleRow("Cookie-banner handling", "Auto-dismiss common consent banners when detectable.", prefs.getBoolean(MainActivity.KEY_COOKIE_BANNER, true)) { setPref(MainActivity.KEY_COOKIE_BANNER, it) })
        root.addView(toggleRow("Privacy badge", "Show the A–F privacy indicator beside the address bar.", prefs.getBoolean(MainActivity.KEY_SHOW_PRIVACY_BADGE, true)) { setPref(MainActivity.KEY_SHOW_PRIVACY_BADGE, it) })
        root.addView(actionRow("Site privacy controls", "Cookies, storage, permissions and per-site cleanup.") { openMainAction(MainActivity.ACTION_OPEN_SITE_CONTROLS) })

        root.addView(section("Browser"), lp().apply { topMargin = dp(20) })
        root.addView(toggleRow("JavaScript", "Sites need this for most modern web apps.", prefs.getBoolean(MainActivity.KEY_JAVASCRIPT, true)) { setPref(MainActivity.KEY_JAVASCRIPT, it) })
        root.addView(toggleRow("Load images", "Turn off heavy images for a lighter data mode.", prefs.getBoolean(MainActivity.KEY_IMAGES, true)) { setPref(MainActivity.KEY_IMAGES, it) })
        root.addView(toggleRow("Safe Browsing", "Use WebView's built-in unsafe-site checks when supported.", prefs.getBoolean(MainActivity.KEY_SAFE_BROWSING, true)) { setPref(MainActivity.KEY_SAFE_BROWSING, it) })
        root.addView(toggleRow("Require a tap to play media", "Reduce unexpected audio/video autoplay.", prefs.getBoolean(MainActivity.KEY_MEDIA_GESTURE, true)) { setPref(MainActivity.KEY_MEDIA_GESTURE, it) })
        root.addView(toggleRow("Restore last session", "Reopen the tabs you had open when PrivBrowse was closed.", prefs.getBoolean(MainActivity.KEY_RESTORE_SESSION, true)) { setPref(MainActivity.KEY_RESTORE_SESSION, it) })
        root.addView(toggleRow("Open typed HTTP links securely", "Upgrade plain http:// input to https:// when possible.", prefs.getBoolean(MainActivity.KEY_HTTPS_FIRST, true)) { setPref(MainActivity.KEY_HTTPS_FIRST, it) })
        root.addView(actionRow("Search engine", prefs.getString(MainActivity.KEY_SEARCH_ENGINE, "DuckDuckGo") ?: "DuckDuckGo") { chooseSearchEngine() })
        root.addView(actionRow("Home page", prefs.getString(MainActivity.KEY_HOME_URL, MainActivity.HOME_URL) ?: MainActivity.HOME_URL) { editHomePage() })
        root.addView(actionRow("Theme", if (prefs.getBoolean(MainActivity.KEY_DARK_MODE, false)) "Dark" else "Light") { toggleTheme() })

        root.addView(section("Tabs & data"), lp().apply { topMargin = dp(20) })
        root.addView(actionRow("Tab manager", "Search tabs, duplicate, close others, restore recently closed.") { openMainAction(MainActivity.ACTION_OPEN_TAB_MANAGER) })
        root.addView(actionRow("Reading list", "Save pages to read later without losing them in history.") { startActivity(Intent(this, ReadingListActivity::class.java)) })
        root.addView(toggleRow("Clear browsing data when backgrounded", "Remove history/cookies when PrivBrowse is sent to the background.", prefs.getBoolean(MainActivity.KEY_CLEAR_ON_EXIT, false)) { setPref(MainActivity.KEY_CLEAR_ON_EXIT, it) })
        root.addView(actionRow("Panic / quick wipe", "Close tabs and erase browsing traces immediately.") { openMainAction(MainActivity.ACTION_PANIC) })
        root.addView(actionRow("Advanced privacy", "Domain auto-incognito, cookie timers, vault and biometric lock.") { startActivity(Intent(this, Phase4Activity::class.java)) })
        root.addView(actionRow("Privacy log", "Transparency events and 30-day local history.") { startActivity(Intent(this, TransparencyLogActivity::class.java)) })

        root.addView(section("Tools"), lp().apply { topMargin = dp(20) })
        root.addView(actionRow("Page tools", "Find, translate, listen, print/PDF, copy, share and more.") { openMainAction(MainActivity.ACTION_OPEN_PAGE_TOOLS) })
        root.addView(actionRow("AI Copilot", "Optional direct provider connections; keys stay in the secure vault.") { startActivity(Intent(this, AiCopilotActivity::class.java)) })
        root.addView(actionRow("Feature Center", "100+ browser, privacy, performance and AI controls.") { startActivity(Intent(this, FeatureCenterActivity::class.java)) })
        root.addView(actionRow("Ultimate Feature Lab", "Tab gestures, live search, notes, compare, per-site scripts and wishlist controls.") { startActivity(Intent(this, FeatureLabActivity::class.java)) })
        root.addView(actionRow("Downloads", "View direct media downloads handled by Android DownloadManager.") { startActivity(Intent(this, VideoDownloadsActivity::class.java)) })
        root.addView(actionRow("Tunnel / VPN", "Configure Xray/V2Ray or inspect VPN Gate discovery.") { startActivity(Intent(this, V2RayActivity::class.java)) })
        root.addView(actionRow("Breach checker", "Optional HIBP account breach lookup.") { startActivity(Intent(this, Phase4Activity::class.java)) })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            root.addView(section("Device"), lp().apply { topMargin = dp(20) })
            root.addView(toggleRow("Biometric app lock", "Ask for fingerprint/face authentication when PrivBrowse resumes.", prefs.getBoolean("biometric_lock", false)) { toggleBiometric(it) })
        }

        val footer = TextView(this).apply {
            text = "PrivBrowse keeps browser state on-device by default. Network features such as AI, translation and VPN only leave the device when you explicitly use them."
            textSize = 12f
            setTextColor(color(R.color.text_muted_light))
            setPadding(dp(2), dp(18), dp(2), 0)
        }
        root.addView(footer)
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun buildToolbar(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(4), 0, dp(4))
        addView(TextView(this@BrowserSettingsActivity).apply {
            text = "‹"
            textSize = 38f
            gravity = Gravity.CENTER
            setTextColor(color(R.color.text_light))
            isClickable = true
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(46), dp(48)))
        addView(TextView(this@BrowserSettingsActivity).apply {
            text = getString(R.string.settings_title)
            textSize = 23f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(color(R.color.text_light))
        }, LinearLayout.LayoutParams(0, -2, 1f))
    }

    private fun buildHeaderCard(): View = MaterialCardView(this).apply {
        radius = dp(18).toFloat()
        cardElevation = dp(1).toFloat()
        strokeWidth = dp(1)
        strokeColor = color(R.color.divider)
        setCardBackgroundColor(color(R.color.surface_light))
        val box = LinearLayout(this@BrowserSettingsActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        box.addView(TextView(this@BrowserSettingsActivity).apply {
            text = "Private controls, without the clutter"
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(color(R.color.text_light))
        })
        box.addView(TextView(this@BrowserSettingsActivity).apply {
            text = "One screen for browser behavior; advanced features stay tucked behind simple actions."
            textSize = 13f
            setTextColor(color(R.color.text_muted_light))
            setPadding(0, dp(4), 0, 0)
        })
        addView(box)
    }

    private fun section(text: String) = TextView(this).apply {
        this.text = text.uppercase()
        textSize = 12f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setTextColor(color(R.color.accent_dark))
        setPadding(dp(2), 0, 0, dp(6))
    }

    private fun toggleRow(title: String, subtitle: String, checked: Boolean, lp: LinearLayout.LayoutParams = lp(), listener: (Boolean) -> Unit): View {
        val card = MaterialCardView(this).apply {
            radius = dp(14).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = color(R.color.divider)
            setCardBackgroundColor(color(R.color.surface_light))
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(11), dp(10), dp(11))
        }
        val copy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        copy.addView(TextView(this).apply {
            text = title
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(color(R.color.text_light))
        })
        copy.addView(TextView(this).apply {
            text = subtitle
            textSize = 12f
            setTextColor(color(R.color.text_muted_light))
            setPadding(0, dp(2), 0, 0)
        })
        val sw = MaterialSwitch(this@BrowserSettingsActivity).apply {
            isChecked = checked
            setOnCheckedChangeListener { _, value -> listener(value) }
        }
        row.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(sw, LinearLayout.LayoutParams(-2, -2))
        card.addView(row)
        return card.apply { layoutParams = lp }
    }

    private fun actionRow(title: String, subtitle: String, action: () -> Unit): View = MaterialCardView(this).apply {
        radius = dp(14).toFloat()
        cardElevation = 0f
        strokeWidth = dp(1)
        strokeColor = color(R.color.divider)
        setCardBackgroundColor(color(R.color.surface_light))
        isClickable = true
        setOnClickListener { action() }
        val row = LinearLayout(this@BrowserSettingsActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        val copy = LinearLayout(this@BrowserSettingsActivity).apply { orientation = LinearLayout.VERTICAL }
        copy.addView(TextView(this@BrowserSettingsActivity).apply {
            text = title
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(color(R.color.text_light))
        })
        copy.addView(TextView(this@BrowserSettingsActivity).apply {
            text = subtitle
            textSize = 12f
            setTextColor(color(R.color.text_muted_light))
            setPadding(0, dp(2), 0, 0)
            maxLines = 2
        })
        row.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(TextView(this@BrowserSettingsActivity).apply {
            text = "›"
            textSize = 25f
            setTextColor(color(R.color.text_muted_light))
        })
        addView(row)
    }

    private fun lp() = LinearLayout.LayoutParams(-1, -2)

    private fun openMainAction(action: String) {
        startActivity(Intent(this, MainActivity::class.java).apply {
            this.action = action
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        })
    }

    private fun setPref(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
        Toast.makeText(this, if (value) "Enabled" else "Disabled", Toast.LENGTH_SHORT).show()
    }

    private fun cycleAdBlocker() {
        adBlocker.cycleLevel()
        Toast.makeText(this, "Ad blocker: ${adBlockLabel()}", Toast.LENGTH_SHORT).show()
        recreate()
    }

    private fun adBlockLabel(): String = when (adBlocker.level) {
        BlockLevel.OFF -> "Off"
        BlockLevel.NORMAL -> "Normal"
        BlockLevel.STRICT -> "Strict"
    }

    private fun chooseSearchEngine() {
        val engines = arrayOf("DuckDuckGo", "Brave Search", "Bing", "Google", "Startpage", "Ecosia")
        val current = engines.indexOf(prefs.getString(MainActivity.KEY_SEARCH_ENGINE, engines[0])).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Search engine")
            .setSingleChoiceItems(engines, current) { dialog, which ->
                prefs.edit().putString(MainActivity.KEY_SEARCH_ENGINE, engines[which]).apply()
                dialog.dismiss()
                recreate()
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun editHomePage() {
        val input = EditText(this).apply {
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setText(prefs.getString(MainActivity.KEY_HOME_URL, MainActivity.HOME_URL))
            selectAll()
        }
        AlertDialog.Builder(this)
            .setTitle("Home page")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val value = input.text.toString().trim()
                if (value.startsWith("http://") || value.startsWith("https://")) {
                    prefs.edit().putString(MainActivity.KEY_HOME_URL, value).apply()
                    recreate()
                } else {
                    Toast.makeText(this, "Enter a valid http/https URL", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun toggleTheme() {
        val dark = !prefs.getBoolean(MainActivity.KEY_DARK_MODE, false)
        prefs.edit().putBoolean(MainActivity.KEY_DARK_MODE, dark).apply()
        AppCompatDelegate.setDefaultNightMode(if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        recreate()
    }

    private fun toggleBiometric(enable: Boolean) {
        if (!enable) {
            prefs.edit().putBoolean("biometric_lock", false).apply()
            return
        }
        BiometricLock.authenticate(this, {
            prefs.edit().putBoolean("biometric_lock", true).apply()
        }, {
            Toast.makeText(this, "Biometric setup cancelled", Toast.LENGTH_SHORT).show()
            recreate()
        })
    }
}
