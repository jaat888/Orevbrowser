package com.privbrowse.app.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.privbrowse.app.R
import com.privbrowse.app.adblock.AdBlocker
import com.privbrowse.app.data.DbHelper
import com.privbrowse.app.privacy.BiometricLock
import org.json.JSONObject

/**
 * Large searchable feature catalog. The main browser stays minimal while this
 * hub exposes power controls and shortcuts in small, scannable cards.
 *
 * Renders through a RecyclerView: with 200+ rows, building every card as a
 * live View on the main thread in onCreate (the previous implementation)
 * made this screen jank/hang/crash on lower-memory devices. Row data is
 * cheap plain objects; only on-screen cards are ever inflated, and
 * off-screen ViewHolders are recycled as you scroll.
 */
class FeatureCenterActivity : AppCompatActivity() {

    private sealed class Row {
        data class Header(val title: String) : Row()
        data class Action(val title: String, val subtitle: String, val onLabel: Boolean, val onClick: () -> Unit) : Row()
        data class Toggle(val title: String, val subtitle: String, val key: String, val default: Boolean) : Row()
    }

    private class Section(val header: Row.Header, val items: MutableList<Row> = mutableListOf())

    private lateinit var prefs: SharedPreferences
    private lateinit var search: EditText
    private lateinit var countLabel: TextView
    private lateinit var adapter: FeatureAdapter
    private val sections = mutableListOf<Section>()
    private var totalFeatures = 0

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun color(id: Int) = ContextCompat.getColor(this, id)
    private fun lp() = LinearLayout.LayoutParams(-1, -2)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE)
        build()
    }

    private fun build() {
        // Build the (cheap, view-free) data model for every section first.
        smartSection()
        privacySection()
        engineSection()
        tabsSection()
        pageToolsSection()
        workflowSection()
        aiSection()
        downloadsSection()
        privacyLifecycleSection()
        advancedSection()
        wishlistSection()

        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.background_light))
        }
        outer.addView(toolbar())

        val recycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@FeatureCenterActivity)
            setPadding(dp(14), dp(4), dp(14), dp(28))
            clipToPadding = false
            setHasFixedSize(false)
            // The hero/search rows (position 0/1) wrap a single shared View instance
            // (see frameHost below). The default animator can keep an old ViewHolder's
            // view alive mid cross-fade while a new one is created for the same shared
            // child, which rips it out of the still-animating wrapper and crashes on the
            // next scroll/layout pass. No visual loss here since these two rows never
            // move or change size on notifyDataSetChanged.
            itemAnimator = null
        }
        adapter = FeatureAdapter()
        recycler.adapter = adapter

        outer.addView(recycler, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(outer)

        adapter.setTopViews(hero(), searchCard())
        updateFilter("")
    }

    private fun toolbar(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(4), dp(6), dp(4), dp(3))
        addView(TextView(this@FeatureCenterActivity).apply {
            text = "‹"
            textSize = 38f
            gravity = Gravity.CENTER
            setTextColor(color(R.color.text_light))
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(44), dp(50)))
        addView(LinearLayout(this@FeatureCenterActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@FeatureCenterActivity).apply {
                text = "Feature Center"
                textSize = 22f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(color(R.color.text_light))
            })
            countLabel = TextView(this@FeatureCenterActivity).apply {
                textSize = 10.8f
                setTextColor(color(R.color.text_muted_light))
            }
            addView(countLabel)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        addView(TextView(this@FeatureCenterActivity).apply {
            text = "⋮"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(color(R.color.text_light))
            setOnClickListener { showHubMenu() }
        }, LinearLayout.LayoutParams(dp(42), dp(48)))
    }

    private fun hero(): View = MaterialCardView(this).apply {
        radius = dp(19).toFloat()
        cardElevation = 0f
        strokeWidth = dp(1)
        strokeColor = color(R.color.divider)
        setCardBackgroundColor(color(R.color.surface_light))
        layoutParams = lp()
        val box = LinearLayout(this@FeatureCenterActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(14), dp(15), dp(14))
        }
        box.addView(TextView(this@FeatureCenterActivity).apply {
            text = "Powerful browser. Clean home screen."
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(color(R.color.text_light))
        })
        box.addView(TextView(this@FeatureCenterActivity).apply {
            text = "Privacy, tabs, page tools, AI and diagnostics are grouped here so the everyday browser stays uncluttered."
            textSize = 12f
            setTextColor(color(R.color.text_muted_light))
            setPadding(0, dp(4), 0, dp(7))
        })
        box.addView(TextView(this@FeatureCenterActivity).apply {
            text = "Large searchable power-feature library"
            textSize = 11f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(color(R.color.accent_dark))
        })
        addView(box)
    }

    private fun searchCard(): View = MaterialCardView(this).apply {
        radius = dp(16).toFloat(); cardElevation = 0f; strokeWidth = dp(1); strokeColor = color(R.color.divider); setCardBackgroundColor(color(R.color.surface_light))
        layoutParams = lp().apply { topMargin = dp(10) }
        val box = LinearLayout(this@FeatureCenterActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(11), dp(10), dp(11), dp(10)) }
        search = EditText(this@FeatureCenterActivity).apply {
            hint = "Search any feature, setting or shortcut"
            setSingleLine(true)
            setPadding(dp(12), dp(3), dp(12), dp(3))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updateFilter(s?.toString().orEmpty()) }
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }
        box.addView(search, LinearLayout.LayoutParams(-1, dp(52)))
        addView(box)
    }

    // ---- Section data builders (unchanged content/behavior, now data-only) ----

    private fun smartSection() {
        section("Smart presets & quick actions") {
            action("Balanced privacy preset", "Turn on the core privacy protections without breaking most sites.") { openMain(MainActivity.ACTION_APPLY_PRESET, "balanced") }
            action("Strict privacy preset", "Use strict ad blocking plus stronger privacy defaults for new pages.") { openMain(MainActivity.ACTION_APPLY_PRESET, "strict") }
            action("Speed / data saver preset", "Reduce heavy assets and favor cache reuse for faster browsing on slower links.") { openMain(MainActivity.ACTION_APPLY_PRESET, "speed") }
            action("Reading focus preset", "Reader-friendly defaults for long articles, larger text and less visual noise.") { openMain(MainActivity.ACTION_APPLY_PRESET, "reading") }
            action("Maximum privacy preset", "Lock down scripts, images, storage and tracking for sensitive browsing; some sites may break.") { openMain(MainActivity.ACTION_APPLY_PRESET, "maximum") }
            action("Media-friendly preset", "Keep common media features usable while retaining the core tracking protections.") { openMain(MainActivity.ACTION_APPLY_PRESET, "media") }
            action("Developer / debug preset", "Use fresh loads, desktop mode and relaxed page blocking for troubleshooting websites.") { openMain(MainActivity.ACTION_APPLY_PRESET, "developer") }
            action("One-tap cleanup", "Clear cookies, web storage and network diagnostics together.") { openMain(MainActivity.ACTION_PANIC) }
            action("AI privacy review", "Ask the configured AI to inspect the current page for visible privacy and tracking concerns.") { openAi("Review the current page for visible privacy risks, tracking signals, permission requests and data-collection concerns. Base the answer only on the page context and clearly separate observed facts from uncertainty.") }
            action("AI action plan", "Turn the current page into concrete next steps.") { openAi("Turn the current page into a practical action plan with prioritized next steps and any important caveats.") }
            action("AI table extractor", "Convert structured information on the page into a compact table.") { openAi("Extract the most useful structured information from the current page into a compact Markdown table. Do not invent missing values.") }
            action("AI bilingual summary", "Summarize the current page in English and Hindi.") { openAi("Summarize the current page twice: first in clear English, then in clear Hindi. Keep both concise and faithful to the page.") }
            action("AI verify-needed checklist", "List claims or numbers that should be independently verified.") { openAi("Identify claims, statistics, dates or statements on the current page that should be independently verified. Explain what evidence would be needed without pretending you verified them.") }
            action("Search engine", "Switch between DuckDuckGo, Brave Search, Bing, Google, Startpage and Ecosia.") { chooseSearchEngine() }
            action("Theme", "Quickly switch the browser between light and dark appearance.") { toggleTheme() }
            action("Clear site data", "Remove cookies and storage for the current page's origin.") { openMain(MainActivity.ACTION_OPEN_SITE_CONTROLS) }
            action("Open private tab", "Start a fresh private browsing tab immediately.") { openMain(MainActivity.ACTION_OPEN_PRIVATE_TAB) }
        }
    }

    private fun privacySection() {
        section("Privacy & tracking") {
            toggleAction("Ad & tracker blocker", "Built-in blocking engine with Normal/Strict levels.") { cycleAdBlocker() }
            togglePref("Block pop-ups", "Stop automatic JavaScript windows.", MainActivity.KEY_BLOCK_POPUPS, true)
            togglePref("Block social embeds", "Reduce common social iframe/SDK embeds.", MainActivity.KEY_BLOCK_SOCIAL, true)
            togglePref("Block location prompts", "Deny website geolocation by default.", MainActivity.KEY_BLOCK_GEOLOCATION, true)
            togglePref("Block camera & microphone", "Require explicit permission before web media access.", MainActivity.KEY_BLOCK_MEDIA_PERMISSIONS, true)
            togglePref("Block web notifications", "Stop notification prompts from sites.", MainActivity.KEY_BLOCK_WEB_NOTIFICATIONS, true)
            togglePref("Block mixed content", "Do not load insecure subresources on HTTPS pages.", MainActivity.KEY_BLOCK_MIXED_CONTENT, true)
            togglePref("Third-party cookies blocked", "Limit cross-site cookie access.", MainActivity.KEY_THIRD_PARTY_COOKIES, true)
            togglePref("Accept first-party cookies", "Turn the app cookie jar completely on/off.", MainActivity.KEY_ACCEPT_COOKIES, true)
            togglePref("Cookie-banner helper", "Auto-dismiss common consent banners when detectable.", MainActivity.KEY_COOKIE_BANNER, true)
            togglePref("Strip tracking parameters", "Remove common click IDs from URLs before loading.", MainActivity.KEY_STRIP_TRACKING, true)
            togglePref("Do Not Track", "Expose navigator.doNotTrack=1.", MainActivity.KEY_DNT, true)
            togglePref("Global Privacy Control", "Expose the browser privacy opt-out signal.", MainActivity.KEY_GPC, true)
            togglePref("Fresh identity per tab", "Rotate browser identity signals for newly created tabs.", MainActivity.KEY_FRESH_IDENTITY, true)
            togglePref("HTTPS first", "Prefer HTTPS when a plain domain is typed.", MainActivity.KEY_HTTPS_FIRST, true)
            togglePref("Block file:// access", "Prevent web pages from reading local files through WebView APIs.", MainActivity.KEY_BLOCK_FILE_ACCESS, true)
            togglePref("Block content:// access", "Prevent web pages from reading content providers.", MainActivity.KEY_BLOCK_CONTENT_ACCESS, true)
            togglePref("Disable form/password helpers", "Keep legacy form/password persistence disabled.", MainActivity.KEY_DISABLE_FORM_HELPERS, true)
            togglePref("Privacy badge", "Show the per-page privacy grade beside the address bar.", MainActivity.KEY_SHOW_PRIVACY_BADGE, true)
            action("Site controls", "Per-site cookies, storage, permissions and network view.") { openMain(MainActivity.ACTION_OPEN_SITE_CONTROLS) }
            action("Network transparency log", "See allowed and blocked host activity stored locally.") { startActivity(Intent(this, TransparencyLogActivity::class.java)) }
            action("Privacy score", "Open the current page's A–F privacy details.") { openMain(MainActivity.ACTION_OPEN_SITE_CONTROLS) }
            action("Advanced privacy controls", "Domain auto-incognito, cookie timer, vault and biometric tools.") { startActivity(Intent(this, Phase4Activity::class.java)) }
            action("Panic wipe", "Close tabs and clear browsing traces using the app's panic flow.") { openMain(MainActivity.ACTION_PANIC) }
        }
    }

    private fun engineSection() {
        section("Browser engine & performance") {
            togglePref("JavaScript", "Required for most interactive websites.", MainActivity.KEY_JAVASCRIPT, true)
            togglePref("Load images", "Disable automatic image loading for lighter browsing.", MainActivity.KEY_IMAGES, true)
            togglePref("DOM storage", "Allow site localStorage/sessionStorage.", MainActivity.KEY_DOM_STORAGE, true)
            togglePref("Media requires a tap", "Reduce surprise audio/video autoplay.", MainActivity.KEY_MEDIA_GESTURE, true)
            togglePref("Safe Browsing", "Use Android WebView's unsafe-site checks when supported.", MainActivity.KEY_SAFE_BROWSING, true)
            togglePref("Fast cache", "Prefer cached resources for quicker repeat loads.", MainActivity.KEY_FAST_CACHE, true)
            togglePref("Data-saver cache", "Bias browsing toward cache reuse.", MainActivity.KEY_DATA_SAVER, false)
            togglePref("No-cache mode", "Force fresh resources for debugging/problem sites.", MainActivity.KEY_NO_CACHE, false)
            togglePref("Zoom controls", "Enable pinch/text zoom support.", MainActivity.KEY_ENABLE_ZOOM, true)
            togglePref("Wide viewport", "Use wider layout viewport for responsive pages.", MainActivity.KEY_WIDE_VIEWPORT, false)
            togglePref("Overview mode", "Fit wide pages when they first load.", MainActivity.KEY_OVERVIEW_MODE, true)
            togglePref("Auto-stop slow loads", "Stop a page after a 45-second loading timeout.", MainActivity.KEY_AUTO_STOP_LOADING, false)
            togglePref("Keep screen awake", "Keep the display on while PrivBrowse is open.", MainActivity.KEY_KEEP_SCREEN_ON, false)
            togglePref("Show page scrollbars", "Show a vertical scrollbar when the page can scroll.", MainActivity.KEY_SHOW_SCROLLBARS, true)
            togglePref("Desktop by default", "Start newly opened tabs in desktop user-agent mode.", MainActivity.KEY_DESKTOP_DEFAULT, false)
            action("Text size", "Set the default WebView text zoom.") { editIntPref(MainActivity.KEY_TEXT_ZOOM, "Text zoom", listOf(75, 90, 100, 110, 125, 150, 175, 200)) }
            action("Default font size", "Set the base WebView font size.") { editIntPref(MainActivity.KEY_DEFAULT_FONT_SIZE, "Default font size", listOf(10, 12, 14, 16, 18, 20, 22)) }
            action("Minimum font size", "Set the minimum readable font size.") { editIntPref(MainActivity.KEY_MIN_FONT_SIZE, "Minimum font size", listOf(4, 6, 8, 10, 12, 14)) }
            action("Fixed-width font size", "Set the default fixed/monospace font size.") { editIntPref(MainActivity.KEY_MONO_FONT_SIZE, "Fixed-width font size", listOf(9, 11, 13, 15, 17, 19)) }
            action("Search engine", prefs.getString(MainActivity.KEY_SEARCH_ENGINE, "DuckDuckGo") ?: "DuckDuckGo") { chooseSearchEngine() }
            action("Home page", prefs.getString(MainActivity.KEY_HOME_URL, MainActivity.HOME_URL) ?: MainActivity.HOME_URL) { editHomePage() }
            action("Theme", if (prefs.getBoolean(MainActivity.KEY_DARK_MODE, false)) "Dark" else "Light") { toggleTheme() }
        }
    }

    private fun tabsSection() {
        section("Tabs, sessions & navigation") {
            togglePref("Private new tabs", "Make new tabs private by default.", MainActivity.KEY_NEW_TABS_PRIVATE, false)
            togglePref("Private link opens", "Long-press link actions can open in a private tab.", MainActivity.KEY_OPEN_LINKS_PRIVATE, false)
            togglePref("Restore last session", "Reopen recent normal tabs after relaunch.", MainActivity.KEY_RESTORE_SESSION, true)
            togglePref("Recently closed tabs", "Keep a small reopen list for normal tabs.", MainActivity.KEY_RECENT_TABS, true)
            togglePref("Confirm close all", "Ask before bulk closing tabs.", MainActivity.KEY_CONFIRM_CLOSE_ALL, true)
            togglePref("Auto reader mode", "Open likely article pages in reader mode.", MainActivity.KEY_AUTO_READER, false)
            togglePref("Compact tab strip", "Hide the tab strip until more than one tab exists.", MainActivity.KEY_COMPACT_TABS, true)
            togglePref("Restore scroll position", "Remember scroll position while a tab stays alive.", MainActivity.KEY_SCROLL_RESTORE, true)
            togglePref("External link handoff", "Let Android handle non-http(s) schemes.", MainActivity.KEY_EXTERNAL_HANDOFF, true)
            togglePref("Save browsing history", "Store normal-tab visits in the local history database.", MainActivity.KEY_SAVE_HISTORY, true)
            action("Tab manager", "Search tabs, duplicate, close others and close/restore tabs.") { openMain(MainActivity.ACTION_OPEN_TAB_MANAGER) }
            action("New private tab", "Open a private tab immediately.") { openMain(MainActivity.ACTION_OPEN_PRIVATE_TAB) }
            action("Recently closed", "Reopen one of the latest closed normal tabs.") { openMain(MainActivity.ACTION_OPEN_TAB_MANAGER) }
            action("Bookmarks", "Manage local bookmarks.") { startActivity(Intent(this, BookmarksActivity::class.java)) }
            action("History", "Search/browse local history.") { startActivity(Intent(this, HistoryActivity::class.java)) }
            action("Reading list", "Save and reopen pages for later.") { startActivity(Intent(this, ReadingListActivity::class.java)) }
        }
    }

    private fun pageToolsSection() {
        section("Page & accessibility tools") {
            togglePref("Reader mode", "Keep the Reader Mode page tool available.", MainActivity.KEY_READER_MODE, true)
            togglePref("Text extraction", "Allow readable-text, links and metadata extraction.", MainActivity.KEY_TEXT_EXTRACTION, true)
            togglePref("Text to speech", "Keep Listen to page available.", MainActivity.KEY_TTS, true)
            togglePref("Translation shortcut", "Keep the page translation action available.", MainActivity.KEY_TRANSLATE, true)
            togglePref("Reading list", "Keep Save to reading list available from Page tools.", MainActivity.KEY_READING_LIST, true)
            togglePref("Video detection", "Detect direct/embedded media candidates for the download manager.", MainActivity.KEY_VIDEO_DETECTION, true)
            togglePref("Hide screenshot capture", "Enable Android FLAG_SECURE while this option is on.", MainActivity.KEY_DISABLE_SCREEN_CAPTURE, false)
            action("Page tools hub", "Find, copy, reader, translate, listen, PDF, archive and site controls.") { openMain(MainActivity.ACTION_OPEN_PAGE_TOOLS) }
            action("Find in page", "Search text inside the current page.") { openMain(MainActivity.ACTION_OPEN_PAGE_TOOLS) }
            action("Reader mode", "Open the readable-content transformation.") { openMain(MainActivity.ACTION_OPEN_PAGE_TOOLS) }
            action("Translate page", "Open the translation shortcut.") { openMain(MainActivity.ACTION_OPEN_PAGE_TOOLS) }
            action("Listen to page", "Read page text with Android TTS.") { openMain(MainActivity.ACTION_OPEN_PAGE_TOOLS) }
            action("Copy readable text", "Extract visible text from the current page.") { openMain(MainActivity.ACTION_OPEN_PAGE_TOOLS) }
            action("Copy all links", "Extract all page links to the clipboard.") { openMain(MainActivity.ACTION_OPEN_PAGE_TOOLS) }
            action("Copy metadata", "Copy title, URL, description, canonical URL and language.") { openMain(MainActivity.ACTION_OPEN_PAGE_TOOLS) }
            action("Page source", "Open a text tab containing current document source.") { openMain(MainActivity.ACTION_OPEN_PAGE_TOOLS) }
            action("Web archive", "Save the current page as an MHT archive in app storage.") { openMain(MainActivity.ACTION_OPEN_PAGE_TOOLS) }
            action("Capture screenshot", "Capture the visible WebView and share it.") { openMain(MainActivity.ACTION_OPEN_PAGE_TOOLS) }
            action("Print / save PDF", "Use Android print/PDF for the current page.") { openMain(MainActivity.ACTION_OPEN_PAGE_TOOLS) }
            action("Hard reload", "Clear the tab cache and reload.") { openMain(MainActivity.ACTION_OPEN_PAGE_TOOLS) }
            action("Open external browser", "Hand the current URL to another installed browser.") { openMain(MainActivity.ACTION_OPEN_PAGE_TOOLS) }
            action("Desktop site", "Toggle the current tab's desktop user agent.") { openMain(MainActivity.ACTION_OPEN_PAGE_TOOLS) }
            action("Share page", "Share the current page URL/title.") { openMain(MainActivity.ACTION_OPEN_PAGE_TOOLS) }
        }
    }

    private fun workflowSection() {
        section("Pro workflow shortcuts") {
            action("Share current page", "Send the current URL and title to another app.") { openMain(MainActivity.ACTION_SHARE_PAGE) }
            action("Copy page URL", "Copy the current address with one tap.") { openMain(MainActivity.ACTION_COPY_URL) }
            action("Copy page title", "Copy the current document title.") { openMain(MainActivity.ACTION_COPY_TITLE) }
            action("Find in page", "Search within the currently loaded page.") { openMain(MainActivity.ACTION_FIND_IN_PAGE) }
            action("Hard reload", "Clear this tab's cache and reload from the network.") { openMain(MainActivity.ACTION_HARD_RELOAD) }
            action("Capture screenshot", "Share the visible browser page as a PNG.") { openMain(MainActivity.ACTION_CAPTURE_SCREENSHOT) }
            action("Translate page", "Open the translation helper for the current URL.") { openMain(MainActivity.ACTION_TRANSLATE_PAGE) }
            action("Listen to page", "Read the current page with Android text-to-speech.") { openMain(MainActivity.ACTION_LISTEN_PAGE) }
            action("Copy readable text", "Extract visible page text to the clipboard.") { openMain(MainActivity.ACTION_COPY_PAGE_TEXT) }
            action("Copy all links", "Extract the page's link URLs into one clipboard item.") { openMain(MainActivity.ACTION_COPY_PAGE_LINKS) }
            action("Copy metadata", "Copy title, URL, description, canonical URL and language.") { openMain(MainActivity.ACTION_COPY_PAGE_METADATA) }
            action("View page source", "Open a text snapshot of the current document source.") { openMain(MainActivity.ACTION_VIEW_SOURCE) }
            action("Save web archive", "Save the current page as an MHT archive in app storage.") { openMain(MainActivity.ACTION_SAVE_ARCHIVE) }
            action("Print / save PDF", "Use Android's print service for the current page.") { openMain(MainActivity.ACTION_PRINT_PDF) }
            action("Open externally", "Hand the current URL to another installed browser/app.") { openMain(MainActivity.ACTION_OPEN_EXTERNAL) }
            action("Toggle desktop site", "Switch the current tab between mobile and desktop user agents.") { openMain(MainActivity.ACTION_TOGGLE_DESKTOP) }
            action("Add bookmark", "Save the current page to local bookmarks.") { openMain(MainActivity.ACTION_ADD_BOOKMARK) }
            action("Clear site data", "Remove cookies and origin storage for the current site.") { openMain(MainActivity.ACTION_CLEAR_SITE) }
            action("Privacy score", "Inspect the current page's HTTPS, tracker and fingerprint signals.") { openMain(MainActivity.ACTION_SHOW_PRIVACY_SCORE) }
            action("Live diagnostics", "View current page, privacy, WebView and blocking diagnostics.") { openMain(MainActivity.ACTION_SHOW_DIAGNOSTICS) }
            action("Tab manager", "Search, switch, close and restore tabs quickly.") { openMain(MainActivity.ACTION_OPEN_TAB_MANAGER) }
            action("Bookmarks", "Open saved local bookmarks.") { openMain(MainActivity.ACTION_OPEN_BOOKMARKS) }
            action("History", "Open local history and clear it when needed.") { openMain(MainActivity.ACTION_OPEN_HISTORY) }
            action("Downloads", "Open the direct-media download queue.") { openMain(MainActivity.ACTION_OPEN_DOWNLOADS) }
            action("VPN", "Open VPN discovery and diagnostics.") { openMain(MainActivity.ACTION_OPEN_VPN) }
            action("V2Ray / Xray", "Open supported tunnel profiles and packet routing tools.") { openMain(MainActivity.ACTION_OPEN_V2RAY) }
            action("Duplicate current tab", "Create a second tab with the same URL and desktop/private mode.") { openMain(MainActivity.ACTION_DUPLICATE_TAB) }
            action("Open current URL in new tab", "Keep this tab and open its current address separately.") { openMain(MainActivity.ACTION_OPEN_URL_NEW_TAB) }
            action("Save to reading list", "Save the current title and URL for later reading.") { openMain(MainActivity.ACTION_SAVE_READING_LIST) }
            action("Clear history now", "Delete local browsing history without touching bookmarks.") { openMain(MainActivity.ACTION_CLEAR_HISTORY) }
            action("Clear all browser data", "Wipe tabs' WebView cache/history, cookies, storage and local logs.") { openMain(MainActivity.ACTION_CLEAR_ALL_DATA) }
            action("Quick dark mode", "Switch app and WebView appearance immediately.") { openMain(MainActivity.ACTION_TOGGLE_DARK_MODE) }
            action("Quick search engine", "Change the default search provider without opening full settings.") { chooseSearchEngine() }
            action("Quick home page", "Change the browser's startup/home URL.") { editHomePage() }
            action("Quick text zoom", "Set the default WebView text zoom from 75% to 200%.") { editIntPref(MainActivity.KEY_TEXT_ZOOM, "Text zoom", listOf(75, 90, 100, 110, 125, 150, 175, 200)) }
            action("Share settings summary", "Share a small, non-secret configuration summary for troubleshooting.") { shareDiagnostics() }
            action("Capability map", "See which features are native, WebView-based or optional network modules.") { showCoverage() }
        }
    }

    private fun aiSection() {
        section("AI assistant") {
            togglePref("AI page context", "Allow the current page's readable text to be attached to prompts.", MainActivity.KEY_AI_CONTEXT, true)
            togglePref("AI chat history", "Keep recent local AI chat on this device.", MainActivity.KEY_AI_HISTORY, true)
            togglePref("AI concise mode", "Use a shorter default assistant style.", MainActivity.KEY_AI_COMPACT, true)
            togglePref("Show provider/model", "Show active provider and model in the AI header.", MainActivity.KEY_AI_SHOW_MODEL, true)
            action("AI Copilot", "Dedicated clean chat workspace with page context and provider setup.") { startActivity(Intent(this, AiCopilotActivity::class.java)) }
            action("AI summarize", "Summarize the current page.") { openAi("Summarize the current page in concise bullets.") }
            action("AI key points", "Extract important points.") { openAi("Extract the most important points from the current page.") }
            action("AI explain simply", "Explain difficult page text for a beginner.") { openAi("Explain the current page simply and define difficult terms.") }
            action("AI facts", "Extract concrete facts, dates and names.") { openAi("Extract concrete names, dates, numbers and verifiable facts from the current page.") }
            action("AI Hindi", "Explain the page in clear Hindi.") { openAi("Explain the useful content of the current page in clear Hindi.") }
            action("AI notes", "Turn a page into organized notes.") { openAi("Turn the current page into clean structured notes.") }
            action("AI study guide", "Build study material from a page.") { openAi("Create a short study guide from the current page.") }
            action("AI comparison", "Compare options or viewpoints without ranking.") { openAi("Compare the main options or viewpoints on the current page neutrally.") }
            action("AI checklist", "Convert useful instructions into a checklist.") { openAi("Turn the useful instructions from the current page into a checklist.") }
            action("AI questions", "Generate Q&A from page content.") { openAi("Create 10 questions and answers from the current page.") }
            action("AI glossary", "Define important terms.") { openAi("Create a glossary of important terms from the current page.") }
            action("AI timeline", "Extract dated events chronologically.") { openAi("Extract dated events from the current page and arrange them chronologically.") }
            action("AI action items", "Extract concrete next steps.") { openAi("Extract action items or next steps from the current page.") }
            action("AI FAQ", "Create a compact page-based FAQ.") { openAi("Create a concise FAQ based only on the current page.") }
            action("AI entities", "Extract people, organizations, places and products.") { openAi("Extract important named entities from the current page.") }
            action("AI JSON", "Extract structured page data into JSON.") { openAi("Return compact JSON containing title, topic, key points, dates and entities from the current page.") }
            action("AI code explain", "Explain code shown on the current page.") { openAi("Explain any code on the current page in simple terms and call out important behavior.") }
            action("AI prompt profiles", "Choose browser, research, study, writing or developer styles.") { startActivity(Intent(this, AiCopilotActivity::class.java).putExtra(AiCopilotActivity.EXTRA_PROMPT, "Use my current AI profile to help me with this page.")) }
        }
    }

    private fun downloadsSection() {
        section("Downloads, media & connectivity") {
            togglePref("Download confirmations", "Ask before direct media downloads start.", MainActivity.KEY_DOWNLOAD_CONFIRM, true)
            togglePref("Video detection", "Detect direct media candidates in the current page.", MainActivity.KEY_VIDEO_DETECTION, true)
            action("Downloads", "Open the app's direct media/download queue.") { startActivity(Intent(this, VideoDownloadsActivity::class.java)) }
            action("VPN Gate", "Discover and connect to a VPN Gate relay over OpenVPN.") { startActivity(Intent(this, VpnActivity::class.java)) }
            action("V2Ray / Xray", "Configure/import supported tunnel profiles.") { startActivity(Intent(this, V2RayActivity::class.java)) }
            action("Connection diagnostics", "Copy browser settings that help troubleshoot a page.") { copyDiagnostics() }
            action("Network log", "Inspect contacted hosts and blocked categories.") { startActivity(Intent(this, TransparencyLogActivity::class.java)) }
            action("Privacy report", "Open the local transparency log / weekly report tools.") { startActivity(Intent(this, TransparencyLogActivity::class.java)) }
        }
    }

    private fun privacyLifecycleSection() {
        section("Cleanup, lifecycle & data") {
            togglePref("Clear cache on exit", "Delete WebView cache when the activity is destroyed.", MainActivity.KEY_CLEAR_CACHE_EXIT, false)
            togglePref("Clear cookies on exit", "Delete the WebView cookie jar when the activity is destroyed.", MainActivity.KEY_CLEAR_COOKIES_EXIT, false)
            togglePref("Clear storage on exit", "Delete WebView origin storage on exit.", MainActivity.KEY_CLEAR_STORAGE_EXIT, false)
            togglePref("Clear page history on exit", "Remove WebView navigation history on exit.", MainActivity.KEY_CLEAR_PAGE_HISTORY_EXIT, false)
            togglePref("Clear network log on exit", "Erase the local transparency log during teardown.", MainActivity.KEY_CLEAR_NETWORK_LOG_EXIT, false)
            togglePref("Purge consent cookies on exit", "Remove common consent-cookie names for visited origins.", MainActivity.KEY_PURGE_CONSENT_EXIT, false)
            togglePref("Clear when backgrounded", "Use the app's clear-on-exit/background lifecycle path.", MainActivity.KEY_CLEAR_ON_EXIT, false)
            togglePref("Private tabs no-cache", "Keep private tabs out of the normal HTTP cache.", MainActivity.KEY_INCOGNITO_NO_CACHE, true)
            togglePref("Prune transparency logs", "Remove old network-log entries after 30 days.", MainActivity.KEY_PRUNE_LOGS, true)
            togglePref("Auto-clear copied text", "Erase PrivBrowse's copied clipboard content after 30 seconds when enabled.", MainActivity.KEY_AUTO_CLEAR_CLIPBOARD, false)
            togglePref("Darken web pages", "Allow WebView/Android web darkening when dark mode is active.", MainActivity.KEY_DARKEN_PAGES, true)
            togglePref("Focus address bar on launch", "Put the keyboard-ready cursor in the address bar after startup.", MainActivity.KEY_AUTO_FOCUS_ADDRESS, false)
            action("Clear cookies", "Clear all WebView cookies now.") { clearCookies() }
            action("Clear web storage", "Delete all WebView origin storage now.") { clearStorage() }
            action("Clear network log", "Delete all locally stored network transparency events.") { clearNetworkLog() }
            action("Reset browser preferences", "Restore major browser settings to the safe defaults used by this build.") { confirmReset() }
            action("Export settings", "Copy a JSON settings snapshot to the clipboard.") { exportSettings() }
            action("Import settings", "Import a JSON settings snapshot.") { importSettings() }
        }
    }

    private fun wishlistSection() {
        section("Ultimate wishlist") {
            action("Ultimate Feature Lab", "Tabs, gestures, live suggestions, privacy helpers, notes, side-by-side compare, per-site CSS/JS and more.") {
                startActivity(Intent(this, FeatureLabActivity::class.java))
            }
        }
    }

    private fun advancedSection() {
        section("Security, diagnostics & utilities") {
            action("Biometric app lock", "Enable/disable the app lock when supported by the device.") { openSecurity() }
            action("Secure API-key vault", "AI keys are stored through Android Keystore-backed SecureStore.") { startActivity(Intent(this, AiCopilotActivity::class.java)) }
            action("Reader mode notes", "Review the built-in reader-mode behavior.") { startActivity(Intent(this, Phase4Activity::class.java)) }
            action("Breach checker", "Open the optional account-breach check tool.") { startActivity(Intent(this, Phase4Activity::class.java)) }
            action("Bookmarks", "Open local bookmark management.") { startActivity(Intent(this, BookmarksActivity::class.java)) }
            action("History", "Open local browsing history management.") { startActivity(Intent(this, HistoryActivity::class.java)) }
            action("Settings", "Return to the compact settings screen.") { startActivity(Intent(this, BrowserSettingsActivity::class.java)) }
            action("Copy settings summary", "Copy a concise current-configuration summary.") { copySettingsSummary() }
            action("Share diagnostics", "Share a text diagnostic snapshot.") { shareDiagnostics() }
            action("Panic flow", "Open the emergency clear/close action.") { openMain(MainActivity.ACTION_PANIC) }
            action("Private tab", "Open a private tab without navigating through menus.") { openMain(MainActivity.ACTION_OPEN_PRIVATE_TAB) }
            action("About feature coverage", "Show what is native, what uses Android WebView, and what is optional network functionality.") { showCoverage() }
        }
    }

    // ---- Row-building primitives (data only — no View is created here) ----

    private fun section(title: String, body: FeatureCenterActivity.() -> Unit) {
        sections += Section(Row.Header(title))
        body()
    }

    private fun togglePref(title: String, subtitle: String, key: String, default: Boolean) {
        totalFeatures++
        sections.last().items += Row.Toggle(title, subtitle, key, default)
    }

    /** Visually styled like a toggle ("ON" label) but wired to a click action, not a bound pref. */
    private fun toggleAction(title: String, subtitle: String, onClick: () -> Unit) {
        totalFeatures++
        sections.last().items += Row.Action(title, subtitle, onLabel = true, onClick = onClick)
    }

    private fun action(title: String, subtitle: String, onClick: () -> Unit) {
        totalFeatures++
        sections.last().items += Row.Action(title, subtitle, onLabel = false, onClick = onClick)
    }

    private fun updateFilter(query: String) {
        val q = query.trim().lowercase()
        val display = mutableListOf<Row>()
        var visible = 0
        for (sec in sections) {
            val matched = if (q.isBlank()) sec.items else sec.items.filter { rowTitle(it).lowercase().contains(q) }
            if (matched.isEmpty()) continue
            display += sec.header
            display += matched
            visible += matched.size
        }
        adapter.submit(display)
        countLabel.text = if (q.isBlank()) "$totalFeatures features & shortcuts" else "$visible features shown"
    }

    private fun rowTitle(row: Row): String = when (row) {
        is Row.Header -> row.title
        is Row.Action -> row.title
        is Row.Toggle -> row.title
    }

    // ---- RecyclerView adapter: builds a card View once per (recycled) holder, binds data cheaply ----

    private inner class FeatureAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var items: List<Row> = emptyList()
        private var heroView: View? = null
        private var searchView: View? = null

        fun setTopViews(hero: View, searchCard: View) {
            heroView = hero
            searchView = searchCard
            notifyItemRangeInserted(0, 2)
        }

        fun submit(newItems: List<Row>) {
            items = newItems
            notifyDataSetChanged()
        }

        private val topCount get() = if (heroView != null) 2 else 0

        override fun getItemCount(): Int = topCount + items.size

        override fun getItemViewType(position: Int): Int {
            if (position == 0 && heroView != null) return TYPE_HERO
            if (position == 1 && heroView != null) return TYPE_SEARCH
            val row = items[position - topCount]
            return when (row) {
                is Row.Header -> TYPE_HEADER
                is Row.Toggle -> TYPE_TOGGLE
                is Row.Action -> TYPE_ACTION
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = when (viewType) {
            TYPE_HERO -> StaticHolder(frameHost(heroView!!))
            TYPE_SEARCH -> StaticHolder(frameHost(searchView!!))
            TYPE_HEADER -> HeaderHolder(makeHeaderView())
            TYPE_TOGGLE -> ToggleHolder(makeToggleView())
            else -> ActionHolder(makeActionView())
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is HeaderHolder -> holder.bind((items[position - topCount] as Row.Header).title)
                is ToggleHolder -> holder.bind(items[position - topCount] as Row.Toggle)
                is ActionHolder -> holder.bind(items[position - topCount] as Row.Action)
                else -> Unit
            }
        }

        // A plain View can only have one parent; wrap fixed top views (hero/search) so
        // RecyclerView can attach/detach them across layout passes without crashing.
        // If onCreateViewHolder ever fires twice for the same shared child (e.g. an old
        // holder hasn't finished being recycled yet), reuse its existing wrapper instead
        // of tearing it out into a second one — that double-wrap is what was crashing on
        // scroll after a toggle/search refresh.
        private fun frameHost(child: View): View {
            val existing = child.parent as? ViewGroup
            if (existing is FrameHostTag) return existing
            return FrameHostTag(this@FeatureCenterActivity).apply {
                orientation = LinearLayout.VERTICAL
                existing?.removeView(child)
                addView(child)
            }
        }
    }

    /** Marker subclass so frameHost() can recognize a wrapper it already created. */
    private class FrameHostTag(context: android.content.Context) : LinearLayout(context)

    private class StaticHolder(view: View) : RecyclerView.ViewHolder(view)

    private inner class HeaderHolder(view: TextView) : RecyclerView.ViewHolder(view) {
        private val text = view
        fun bind(title: String) { text.text = title.uppercase() }
    }

    private inner class ToggleHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewWithTag<TextView>("title")
        private val subtitle = view.findViewWithTag<TextView>("subtitle")
        private val sw = view.findViewWithTag<MaterialSwitch>("switch")
        fun bind(row: Row.Toggle) {
            title.text = row.title
            subtitle.text = row.subtitle
            sw.setOnCheckedChangeListener(null)
            sw.isChecked = prefs.getBoolean(row.key, row.default)
            sw.setOnCheckedChangeListener { _, value ->
                prefs.edit().putBoolean(row.key, value).apply()
                if (row.key == MainActivity.KEY_DARK_MODE) recreate()
                if (row.key == MainActivity.KEY_DISABLE_SCREEN_CAPTURE) {
                    if (value) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }
    }

    private inner class ActionHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewWithTag<TextView>("title")
        private val subtitle = view.findViewWithTag<TextView>("subtitle")
        private val trailing = view.findViewWithTag<TextView>("trailing")
        fun bind(row: Row.Action) {
            title.text = row.title
            subtitle.text = row.subtitle
            trailing.text = if (row.onLabel) "ON" else "›"
            trailing.textSize = if (row.onLabel) 10f else 24f
            itemView.setOnClickListener { row.onClick() }
        }
    }

    private fun makeHeaderView(): TextView = TextView(this).apply {
        textSize = 11.5f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setTextColor(color(R.color.accent_dark))
        setPadding(dp(2), dp(18), 0, dp(6))
    }

    private fun cardShell(): Pair<MaterialCardView, LinearLayout> {
        val card = MaterialCardView(this).apply {
            radius = dp(14).toFloat(); cardElevation = 0f; strokeWidth = dp(1); strokeColor = color(R.color.divider); setCardBackgroundColor(color(R.color.surface_light))
            layoutParams = lp().apply { bottomMargin = dp(6) }
            isClickable = true
        }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(13), dp(10), dp(9), dp(10)) }
        card.addView(row)
        return card to row
    }

    private fun makeToggleView(): View {
        val (card, row) = cardShell()
        val copy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        copy.addView(TextView(this).apply { tag = "title"; textSize = 14.2f; setTypeface(typeface, android.graphics.Typeface.BOLD); setTextColor(color(R.color.text_light)) })
        copy.addView(TextView(this).apply { tag = "subtitle"; textSize = 11.3f; setTextColor(color(R.color.text_muted_light)); setPadding(0, dp(2), 0, 0); maxLines = 2 })
        val sw = MaterialSwitch(this).apply { tag = "switch" }
        row.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(sw)
        return card
    }

    private fun makeActionView(): View {
        val (card, row) = cardShell()
        row.setPadding(dp(13), dp(11), dp(12), dp(11))
        val copy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        copy.addView(TextView(this).apply { tag = "title"; textSize = 14.2f; setTypeface(typeface, android.graphics.Typeface.BOLD); setTextColor(color(R.color.text_light)) })
        copy.addView(TextView(this).apply { tag = "subtitle"; textSize = 11.3f; setTextColor(color(R.color.text_muted_light)); maxLines = 2; setPadding(0, dp(2), 0, 0) })
        row.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(TextView(this).apply { tag = "trailing"; setTypeface(typeface, android.graphics.Typeface.BOLD); setTextColor(color(R.color.accent_dark)) })
        return card
    }

    private companion object {
        const val TYPE_HERO = 0
        const val TYPE_SEARCH = 1
        const val TYPE_HEADER = 2
        const val TYPE_TOGGLE = 3
        const val TYPE_ACTION = 4
    }

    // ---- Menu / dialog / utility actions (unchanged) ----

    private fun showHubMenu() {
        AlertDialog.Builder(this).setTitle("Feature Center")
            .setItems(arrayOf("Export settings", "Copy diagnostics", "Reset preferences", "Coverage notes")) { _, which ->
                when (which) { 0 -> exportSettings(); 1 -> copyDiagnostics(); 2 -> confirmReset(); 3 -> showCoverage() }
            }.setNegativeButton(R.string.close, null).show()
    }

    private fun openMain(action: String, preset: String? = null) {
        startActivity(Intent(this, MainActivity::class.java).apply {
            this.action = action
            if (preset != null) putExtra(MainActivity.EXTRA_PRESET, preset)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        })
    }

    private fun openAi(text: String) {
        startActivity(Intent(this, AiCopilotActivity::class.java).putExtra(AiCopilotActivity.EXTRA_PROMPT, text))
    }

    private fun cycleAdBlocker() {
        AdBlocker(this).cycleLevel()
        Toast.makeText(this, "Ad blocker level changed", Toast.LENGTH_SHORT).show()
        recreate()
    }

    private fun chooseSearchEngine() {
        val engines = arrayOf("DuckDuckGo", "Brave Search", "Bing", "Google", "Startpage", "Ecosia")
        val current = engines.indexOf(prefs.getString(MainActivity.KEY_SEARCH_ENGINE, engines[0])).coerceAtLeast(0)
        AlertDialog.Builder(this).setTitle("Search engine").setSingleChoiceItems(engines, current) { dialog, which ->
            prefs.edit().putString(MainActivity.KEY_SEARCH_ENGINE, engines[which]).apply(); dialog.dismiss(); recreate()
        }.setNegativeButton(R.string.close, null).show()
    }

    private fun editHomePage() {
        val input = EditText(this).apply { setSingleLine(true); setText(prefs.getString(MainActivity.KEY_HOME_URL, MainActivity.HOME_URL)); selectAll() }
        AlertDialog.Builder(this).setTitle("Home page").setView(input).setPositiveButton("Save") { _, _ ->
            val value = input.text.toString().trim()
            if (value.startsWith("http://") || value.startsWith("https://")) prefs.edit().putString(MainActivity.KEY_HOME_URL, value).apply() else Toast.makeText(this, "Use an http/https URL", Toast.LENGTH_SHORT).show()
            recreate()
        }.setNegativeButton(R.string.close, null).show()
    }

    private fun editIntPref(key: String, title: String, values: List<Int>) {
        val labels = values.map { "$it" }.toTypedArray()
        val current = values.indexOf(prefs.getInt(key, values.first())).coerceAtLeast(0)
        AlertDialog.Builder(this).setTitle(title).setSingleChoiceItems(labels, current) { dialog, which ->
            prefs.edit().putInt(key, values[which]).apply(); dialog.dismiss()
            openMain(MainActivity.ACTION_OPEN_PAGE_TOOLS)
        }.setNegativeButton(R.string.close, null).show()
    }

    private fun toggleTheme() {
        val dark = !prefs.getBoolean(MainActivity.KEY_DARK_MODE, false)
        prefs.edit().putBoolean(MainActivity.KEY_DARK_MODE, dark).apply()
        AppCompatDelegate.setDefaultNightMode(if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        recreate()
    }

    private fun clearCookies() {
        android.webkit.CookieManager.getInstance().removeAllCookies { Toast.makeText(this, "Cookies cleared", Toast.LENGTH_SHORT).show() }
        android.webkit.CookieManager.getInstance().flush()
    }

    private fun clearStorage() {
        android.webkit.WebStorage.getInstance().deleteAllData()
        Toast.makeText(this, "Web storage cleared", Toast.LENGTH_SHORT).show()
    }

    private fun clearNetworkLog() {
        DbHelper(this).clearNetworkLog()
        Toast.makeText(this, "Network log cleared", Toast.LENGTH_SHORT).show()
    }

    private fun confirmReset() {
        AlertDialog.Builder(this).setTitle("Reset browser preferences?")
            .setMessage("Restore browser/privacy preferences. Bookmarks and history remain untouched.")
            .setNegativeButton(R.string.close, null)
            .setPositiveButton("Reset") { _, _ ->
                prefs.edit().clear()
                    .putBoolean(MainActivity.KEY_FRESH_IDENTITY, true)
                    .putBoolean(MainActivity.KEY_DNT, true)
                    .putBoolean(MainActivity.KEY_GPC, true)
                    .putBoolean(MainActivity.KEY_COOKIE_BANNER, true)
                    .putBoolean(MainActivity.KEY_SHOW_PRIVACY_BADGE, true)
                    .putBoolean(MainActivity.KEY_JAVASCRIPT, true)
                    .putBoolean(MainActivity.KEY_IMAGES, true)
                    .putBoolean(MainActivity.KEY_SAFE_BROWSING, true)
                    .putBoolean(MainActivity.KEY_MEDIA_GESTURE, true)
                    .putBoolean(MainActivity.KEY_RESTORE_SESSION, true)
                    .putBoolean(MainActivity.KEY_HTTPS_FIRST, true)
                    .putBoolean(MainActivity.KEY_BLOCK_POPUPS, true)
                    .putBoolean(MainActivity.KEY_BLOCK_SOCIAL, true)
                    .putBoolean(MainActivity.KEY_BLOCK_GEOLOCATION, true)
                    .putBoolean(MainActivity.KEY_BLOCK_MEDIA_PERMISSIONS, true)
                    .putBoolean(MainActivity.KEY_BLOCK_WEB_NOTIFICATIONS, true)
                    .putBoolean(MainActivity.KEY_THIRD_PARTY_COOKIES, true)
                    .putBoolean(MainActivity.KEY_ACCEPT_COOKIES, true)
                    .putBoolean(MainActivity.KEY_STRIP_TRACKING, true)
                    .putBoolean(MainActivity.KEY_BLOCK_MIXED_CONTENT, true)
                    .putBoolean(MainActivity.KEY_BLOCK_FILE_ACCESS, true)
                    .putBoolean(MainActivity.KEY_BLOCK_CONTENT_ACCESS, true)
                    .putBoolean(MainActivity.KEY_DISABLE_FORM_HELPERS, true)
                    .putBoolean(MainActivity.KEY_FAST_CACHE, true)
                    .putBoolean(MainActivity.KEY_INCOGNITO_NO_CACHE, true)
                    .putBoolean(MainActivity.KEY_PRUNE_LOGS, true)
                    .putBoolean(MainActivity.KEY_SAVE_HISTORY, true)
                    .putBoolean(MainActivity.KEY_SHOW_SCROLLBARS, true)
                    .putBoolean(MainActivity.KEY_AUTO_FOCUS_ADDRESS, false)
                    .putBoolean(MainActivity.KEY_AUTO_CLEAR_CLIPBOARD, false)
                    .putBoolean(MainActivity.KEY_DARKEN_PAGES, true)
                    .putInt(MainActivity.KEY_TEXT_ZOOM, 100)
                    .putInt(MainActivity.KEY_DEFAULT_FONT_SIZE, 16)
                    .putInt(MainActivity.KEY_MIN_FONT_SIZE, 8)
                    .putInt(MainActivity.KEY_MONO_FONT_SIZE, 13)
                    .putString(MainActivity.KEY_HOME_URL, MainActivity.HOME_URL)
                    .putString(MainActivity.KEY_SEARCH_ENGINE, "DuckDuckGo")
                    .apply()
                Toast.makeText(this, "Preferences reset", Toast.LENGTH_SHORT).show()
                recreate()
            }.show()
    }

    private fun exportSettings() {
        val keys = listOf(
            MainActivity.KEY_DARK_MODE, MainActivity.KEY_FRESH_IDENTITY, MainActivity.KEY_DNT, MainActivity.KEY_GPC,
            MainActivity.KEY_COOKIE_BANNER, MainActivity.KEY_SHOW_PRIVACY_BADGE, MainActivity.KEY_JAVASCRIPT, MainActivity.KEY_IMAGES,
            MainActivity.KEY_SAFE_BROWSING, MainActivity.KEY_MEDIA_GESTURE, MainActivity.KEY_RESTORE_SESSION, MainActivity.KEY_HTTPS_FIRST,
            MainActivity.KEY_BLOCK_POPUPS, MainActivity.KEY_BLOCK_SOCIAL, MainActivity.KEY_BLOCK_GEOLOCATION, MainActivity.KEY_BLOCK_MEDIA_PERMISSIONS,
            MainActivity.KEY_BLOCK_WEB_NOTIFICATIONS, MainActivity.KEY_THIRD_PARTY_COOKIES, MainActivity.KEY_ACCEPT_COOKIES, MainActivity.KEY_STRIP_TRACKING,
            MainActivity.KEY_DATA_SAVER, MainActivity.KEY_FAST_CACHE, MainActivity.KEY_NO_CACHE, MainActivity.KEY_DOM_STORAGE, MainActivity.KEY_ENABLE_ZOOM,
            MainActivity.KEY_WIDE_VIEWPORT, MainActivity.KEY_OVERVIEW_MODE, MainActivity.KEY_DARKEN_PAGES, MainActivity.KEY_NEW_TABS_PRIVATE,
            MainActivity.KEY_OPEN_LINKS_PRIVATE, MainActivity.KEY_RECENT_TABS, MainActivity.KEY_CONFIRM_CLOSE_ALL, MainActivity.KEY_AUTO_READER,
            MainActivity.KEY_COMPACT_TABS, MainActivity.KEY_SCROLL_RESTORE, MainActivity.KEY_EXTERNAL_HANDOFF, MainActivity.KEY_DOWNLOAD_CONFIRM,
            MainActivity.KEY_BLOCK_MIXED_CONTENT, MainActivity.KEY_BLOCK_FILE_ACCESS, MainActivity.KEY_BLOCK_CONTENT_ACCESS, MainActivity.KEY_DISABLE_FORM_HELPERS,
            MainActivity.KEY_AUTO_STOP_LOADING, MainActivity.KEY_CLEAR_CACHE_EXIT, MainActivity.KEY_CLEAR_COOKIES_EXIT, MainActivity.KEY_CLEAR_STORAGE_EXIT,
            MainActivity.KEY_CLEAR_PAGE_HISTORY_EXIT, MainActivity.KEY_INCOGNITO_NO_CACHE, MainActivity.KEY_PRUNE_LOGS, MainActivity.KEY_AI_CONTEXT,
            MainActivity.KEY_AI_HISTORY, MainActivity.KEY_AI_COMPACT, MainActivity.KEY_AI_SHOW_MODEL, MainActivity.KEY_READER_MODE, MainActivity.KEY_TTS,
            MainActivity.KEY_TRANSLATE, MainActivity.KEY_READING_LIST, MainActivity.KEY_TEXT_EXTRACTION, MainActivity.KEY_VIDEO_DETECTION,
            MainActivity.KEY_DISABLE_SCREEN_CAPTURE, MainActivity.KEY_SAVE_HISTORY, MainActivity.KEY_DESKTOP_DEFAULT, MainActivity.KEY_KEEP_SCREEN_ON,
            MainActivity.KEY_SHOW_SCROLLBARS, MainActivity.KEY_CLEAR_NETWORK_LOG_EXIT, MainActivity.KEY_PURGE_CONSENT_EXIT, MainActivity.KEY_AUTO_FOCUS_ADDRESS,
            MainActivity.KEY_AUTO_CLEAR_CLIPBOARD, MainActivity.KEY_DARKEN_PAGES
        )
        val json = JSONObject()
        keys.forEach { json.put(it, prefs.getBoolean(it, false)) }
        json.put(MainActivity.KEY_HOME_URL, prefs.getString(MainActivity.KEY_HOME_URL, MainActivity.HOME_URL))
        json.put(MainActivity.KEY_SEARCH_ENGINE, prefs.getString(MainActivity.KEY_SEARCH_ENGINE, "DuckDuckGo"))
        json.put(MainActivity.KEY_TEXT_ZOOM, prefs.getInt(MainActivity.KEY_TEXT_ZOOM, 100))
        json.put(MainActivity.KEY_DEFAULT_FONT_SIZE, prefs.getInt(MainActivity.KEY_DEFAULT_FONT_SIZE, 16))
        json.put(MainActivity.KEY_MIN_FONT_SIZE, prefs.getInt(MainActivity.KEY_MIN_FONT_SIZE, 8))
        json.put(MainActivity.KEY_MONO_FONT_SIZE, prefs.getInt(MainActivity.KEY_MONO_FONT_SIZE, 13))
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("PrivBrowse settings", json.toString(2)))
        Toast.makeText(this, "Settings JSON copied", Toast.LENGTH_SHORT).show()
    }

    private fun importSettings() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "application/json"; addCategory(Intent.CATEGORY_OPENABLE) }, 420)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 420 || resultCode != Activity.RESULT_OK || data?.data == null) return
        runCatching {
            val raw = contentResolver.openInputStream(data.data!!)?.bufferedReader()?.use { it.readText() } ?: error("Empty file")
            val json = JSONObject(raw)
            val editor = prefs.edit()
            json.keys().forEach { key ->
                when (val value = json.get(key)) {
                    is Boolean -> editor.putBoolean(key, value)
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Double -> editor.putFloat(key, value.toFloat())
                }
            }
            editor.apply(); Toast.makeText(this, "Settings imported", Toast.LENGTH_SHORT).show(); recreate()
        }.onFailure { Toast.makeText(this, "Import failed: ${it.message}", Toast.LENGTH_SHORT).show() }
    }

    private fun openSecurity() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) {
            Toast.makeText(this, "Biometric lock needs Android 9+", Toast.LENGTH_SHORT).show(); return
        }
        BiometricLock.authenticate(this, {
            getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE).edit().putBoolean("biometric_lock", true).apply()
            Toast.makeText(this, "Biometric app lock enabled", Toast.LENGTH_SHORT).show()
        }, {
            Toast.makeText(this, "Biometric setup cancelled", Toast.LENGTH_SHORT).show()
        })
    }

    private fun copyDiagnostics() {
        val text = "PrivBrowse diagnostics\n" +
            "JS=${prefs.getBoolean(MainActivity.KEY_JAVASCRIPT, true)}\n" +
            "Images=${prefs.getBoolean(MainActivity.KEY_IMAGES, true)}\n" +
            "SafeBrowsing=${prefs.getBoolean(MainActivity.KEY_SAFE_BROWSING, true)}\n" +
            "3P cookies blocked=${prefs.getBoolean(MainActivity.KEY_THIRD_PARTY_COOKIES, true)}\n" +
            "Tracking strip=${prefs.getBoolean(MainActivity.KEY_STRIP_TRACKING, true)}\n" +
            "AI context=${prefs.getBoolean(MainActivity.KEY_AI_CONTEXT, true)}"
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("PrivBrowse diagnostics", text))
        Toast.makeText(this, "Diagnostics copied", Toast.LENGTH_SHORT).show()
    }

    private fun copySettingsSummary() {
        val text = "PrivBrowse • ${prefs.getString(MainActivity.KEY_SEARCH_ENGINE, "DuckDuckGo")} • " +
            "privacy popups=${prefs.getBoolean(MainActivity.KEY_BLOCK_POPUPS, true)}, " +
            "3P-cookies=${prefs.getBoolean(MainActivity.KEY_THIRD_PARTY_COOKIES, true)}, " +
            "GPC=${prefs.getBoolean(MainActivity.KEY_GPC, true)}, " +
            "JS=${prefs.getBoolean(MainActivity.KEY_JAVASCRIPT, true)}, " +
            "AI=${prefs.getBoolean(MainActivity.KEY_AI_CONTEXT, true)}"
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("PrivBrowse summary", text))
        Toast.makeText(this, "Summary copied", Toast.LENGTH_SHORT).show()
    }

    private fun shareDiagnostics() {
        val text = "PrivBrowse diagnostics\n" +
            "Search=${prefs.getString(MainActivity.KEY_SEARCH_ENGINE, "DuckDuckGo")}\n" +
            "Safe Browsing=${prefs.getBoolean(MainActivity.KEY_SAFE_BROWSING, true)}\n" +
            "Tracking protection=${prefs.getBoolean(MainActivity.KEY_STRIP_TRACKING, true)}"
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "Share diagnostics"))
    }

    private fun showCoverage() {
        AlertDialog.Builder(this)
            .setTitle("PrivBrowse capability map")
            .setMessage("Native Android/WebView: tabs, history, bookmarks, reader mode, TTS, PDF/print, downloads, WebView privacy settings, biometric lock and local logs.\n\nOptional network features: AI providers, translation pages and VPN/tunnel modules only run when you use them.\n\nSome desktop-browser features such as arbitrary Chrome extensions cannot be added to Android WebView directly without replacing the browser engine.")
            .setPositiveButton(R.string.close, null)
            .show()
    }
}
