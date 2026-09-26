package com.privbrowse.app.ui

import android.annotation.SuppressLint
import com.privbrowse.app.BuildConfig
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.print.PrintAttributes
import android.print.PrintManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.privbrowse.app.R
import com.privbrowse.app.adblock.AdBlocker
import com.privbrowse.app.adblock.BlockCategory
import com.privbrowse.app.data.DbHelper
import com.privbrowse.app.privacy.ConsentAutoHandler
import com.privbrowse.app.privacy.CookiePolicy
import com.privbrowse.app.privacy.TrackingParamStripper
import com.privbrowse.app.transparency.PrivacyScore
import com.privbrowse.app.transparency.WeeklyReportScheduler
import com.privbrowse.app.privacy.BiometricLock
import com.privbrowse.app.privacy.DomainPrivacyStore
import com.privbrowse.app.privacy.FingerprintRandomizer
import com.privbrowse.app.privacy.ReaderMode
import com.privbrowse.app.panic.PanicManager
import com.privbrowse.app.video.VideoDetector

class MainActivity : AppCompatActivity() {

    companion object {
        const val HOME_URL = "https://duckduckgo.com"
        const val SEARCH_URL_PREFIX = "https://duckduckgo.com/?q="
        const val PREFS = "privbrowse_prefs"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_FRESH_IDENTITY = "fresh_identity_per_tab"
        const val KEY_DNT = "do_not_track"
        const val KEY_GPC = "global_privacy_control"
        const val KEY_COOKIE_BANNER = "cookie_banner"
        const val KEY_SHOW_PRIVACY_BADGE = "show_privacy_badge"
        const val KEY_JAVASCRIPT = "javascript_enabled"
        const val KEY_IMAGES = "images_enabled"
        const val KEY_SAFE_BROWSING = "safe_browsing"
        const val KEY_MEDIA_GESTURE = "media_user_gesture"
        const val KEY_RESTORE_SESSION = "restore_session"
        const val KEY_HTTPS_FIRST = "https_first"
        const val KEY_CLEAR_ON_EXIT = "clear_on_exit"
        const val KEY_HOME_URL = "home_url"
        const val KEY_SEARCH_ENGINE = "search_engine"
        const val KEY_SESSION_URLS = "session_urls"
        const val KEY_BLOCK_POPUPS = "block_popups"
        const val KEY_BLOCK_SOCIAL = "block_social"
        const val KEY_BLOCK_GEOLOCATION = "block_geolocation"
        const val KEY_BLOCK_MEDIA_PERMISSIONS = "block_media_permissions"
        const val KEY_BLOCK_WEB_NOTIFICATIONS = "block_web_notifications"
        const val KEY_THIRD_PARTY_COOKIES = "third_party_cookies_blocked"
        const val KEY_STRIP_TRACKING = "strip_tracking"
        const val KEY_DATA_SAVER = "data_saver"
        const val KEY_FAST_CACHE = "fast_cache"
        const val KEY_NO_CACHE = "no_cache"
        const val KEY_DOM_STORAGE = "dom_storage"
        const val KEY_ENABLE_ZOOM = "enable_zoom"
        const val KEY_WIDE_VIEWPORT = "wide_viewport"
        const val KEY_OVERVIEW_MODE = "overview_mode"
        const val KEY_DARKEN_PAGES = "darken_pages"
        const val KEY_NEW_TABS_PRIVATE = "new_tabs_private"
        const val KEY_OPEN_LINKS_PRIVATE = "open_links_private"
        const val KEY_RECENT_TABS = "recent_tabs"
        const val KEY_CONFIRM_CLOSE_ALL = "confirm_close_all"
        const val KEY_AUTO_READER = "auto_reader"
        const val KEY_COMPACT_TABS = "compact_tabs"
        const val KEY_SCROLL_RESTORE = "scroll_restore"
        const val KEY_EXTERNAL_HANDOFF = "external_handoff"
        const val KEY_DOWNLOAD_CONFIRM = "download_confirm"
        const val KEY_BLOCK_MIXED_CONTENT = "block_mixed_content"
        const val KEY_BLOCK_FILE_ACCESS = "block_file_access"
        const val KEY_BLOCK_CONTENT_ACCESS = "block_content_access"
        const val KEY_DISABLE_FORM_HELPERS = "disable_form_helpers"
        const val KEY_AUTO_STOP_LOADING = "auto_stop_loading"
        const val KEY_CLEAR_CACHE_EXIT = "clear_cache_exit"
        const val KEY_CLEAR_COOKIES_EXIT = "clear_cookies_exit"
        const val KEY_CLEAR_STORAGE_EXIT = "clear_storage_exit"
        const val KEY_CLEAR_PAGE_HISTORY_EXIT = "clear_page_history_exit"
        const val KEY_INCOGNITO_NO_CACHE = "incognito_no_cache"
        const val KEY_PRUNE_LOGS = "prune_logs"
        const val KEY_AI_CONTEXT = "ai_context"
        const val KEY_AI_HISTORY = "ai_history"
        const val KEY_AI_COMPACT = "ai_compact"
        const val KEY_AI_SHOW_MODEL = "ai_show_model"
        const val KEY_READER_MODE = "reader_mode"
        const val KEY_TTS = "tts"
        const val KEY_TRANSLATE = "translate"
        const val KEY_READING_LIST = "reading_list"
        const val KEY_TEXT_EXTRACTION = "text_extraction"
        const val KEY_VIDEO_DETECTION = "video_detection"
        const val KEY_DISABLE_SCREEN_CAPTURE = "disable_screen_capture"
        const val KEY_SAVE_HISTORY = "save_history"
        const val KEY_DESKTOP_DEFAULT = "desktop_default"
        const val KEY_ACCEPT_COOKIES = "accept_cookies"
        const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        const val KEY_SHOW_SCROLLBARS = "show_scrollbars"
        const val KEY_DEFAULT_FONT_SIZE = "default_font_size"
        const val KEY_MIN_FONT_SIZE = "min_font_size"
        const val KEY_MONO_FONT_SIZE = "mono_font_size"
        const val KEY_TEXT_ZOOM = "text_zoom"
        const val KEY_CLEAR_NETWORK_LOG_EXIT = "clear_network_log_exit"
        const val KEY_PURGE_CONSENT_EXIT = "purge_consent_exit"
        const val KEY_AUTO_FOCUS_ADDRESS = "auto_focus_address"
        const val KEY_AUTO_CLEAR_CLIPBOARD = "auto_clear_clipboard"

        const val ACTION_OPEN_SITE_CONTROLS = "com.privbrowse.app.action.SITE_CONTROLS"
        const val ACTION_OPEN_TAB_MANAGER = "com.privbrowse.app.action.TAB_MANAGER"
        const val ACTION_OPEN_PRIVATE_TAB = "com.privbrowse.app.action.PRIVATE_TAB"
        const val ACTION_OPEN_PAGE_TOOLS = "com.privbrowse.app.action.PAGE_TOOLS"
        const val ACTION_PANIC = "com.privbrowse.app.action.PANIC"
        const val ACTION_APPLY_PRESET = "com.privbrowse.app.action.APPLY_PRESET"
        const val ACTION_SHARE_PAGE = "com.privbrowse.app.action.SHARE_PAGE"
        const val ACTION_COPY_URL = "com.privbrowse.app.action.COPY_URL"
        const val ACTION_COPY_TITLE = "com.privbrowse.app.action.COPY_TITLE"
        const val ACTION_FIND_IN_PAGE = "com.privbrowse.app.action.FIND_IN_PAGE"
        const val ACTION_HARD_RELOAD = "com.privbrowse.app.action.HARD_RELOAD"
        const val ACTION_CAPTURE_SCREENSHOT = "com.privbrowse.app.action.CAPTURE_SCREENSHOT"
        const val ACTION_TRANSLATE_PAGE = "com.privbrowse.app.action.TRANSLATE_PAGE"
        const val ACTION_LISTEN_PAGE = "com.privbrowse.app.action.LISTEN_PAGE"
        const val ACTION_COPY_PAGE_TEXT = "com.privbrowse.app.action.COPY_PAGE_TEXT"
        const val ACTION_COPY_PAGE_LINKS = "com.privbrowse.app.action.COPY_PAGE_LINKS"
        const val ACTION_COPY_PAGE_METADATA = "com.privbrowse.app.action.COPY_PAGE_METADATA"
        const val ACTION_VIEW_SOURCE = "com.privbrowse.app.action.VIEW_SOURCE"
        const val ACTION_SAVE_ARCHIVE = "com.privbrowse.app.action.SAVE_ARCHIVE"
        const val ACTION_PRINT_PDF = "com.privbrowse.app.action.PRINT_PDF"
        const val ACTION_OPEN_EXTERNAL = "com.privbrowse.app.action.OPEN_EXTERNAL"
        const val ACTION_TOGGLE_DESKTOP = "com.privbrowse.app.action.TOGGLE_DESKTOP"
        const val ACTION_ADD_BOOKMARK = "com.privbrowse.app.action.ADD_BOOKMARK"
        const val ACTION_CLEAR_SITE = "com.privbrowse.app.action.CLEAR_SITE"
        const val ACTION_SHOW_PRIVACY_SCORE = "com.privbrowse.app.action.PRIVACY_SCORE"
        const val ACTION_SHOW_DIAGNOSTICS = "com.privbrowse.app.action.DIAGNOSTICS"
        const val ACTION_OPEN_BOOKMARKS = "com.privbrowse.app.action.BOOKMARKS"
        const val ACTION_OPEN_HISTORY = "com.privbrowse.app.action.HISTORY"
        const val ACTION_OPEN_DOWNLOADS = "com.privbrowse.app.action.DOWNLOADS"
        const val ACTION_OPEN_VPN = "com.privbrowse.app.action.VPN"
        const val ACTION_OPEN_V2RAY = "com.privbrowse.app.action.V2RAY"
        const val ACTION_DUPLICATE_TAB = "com.privbrowse.app.action.DUPLICATE_TAB"
        const val ACTION_OPEN_URL_NEW_TAB = "com.privbrowse.app.action.OPEN_URL_NEW_TAB"
        const val ACTION_SAVE_READING_LIST = "com.privbrowse.app.action.SAVE_READING_LIST"
        const val ACTION_CLEAR_HISTORY = "com.privbrowse.app.action.CLEAR_HISTORY"
        const val ACTION_CLEAR_ALL_DATA = "com.privbrowse.app.action.CLEAR_ALL_DATA"
        const val ACTION_TOGGLE_DARK_MODE = "com.privbrowse.app.action.TOGGLE_DARK_MODE"
        const val EXTRA_PRESET = "com.privbrowse.app.extra.PRESET"

        private const val REQ_BOOKMARKS = 100
        private const val REQ_HISTORY = 101
        private const val REQ_READING_LIST = 103
        private const val REQ_NOTIFICATIONS = 102

        private const val THIRTY_DAYS_MILLIS = 30L * 24 * 60 * 60 * 1000
        private const val MAX_REOPENED = 15
        private const val MAX_RESTORED_TABS = 12

        private const val DESKTOP_UA =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Safari/537.36"
    }

    private lateinit var urlBar: EditText
    private lateinit var privacyBadge: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var webViewContainer: FrameLayout
    private lateinit var swipeRefresh: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var tabStrip: RecyclerView
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnHome: ImageButton
    private lateinit var btnTools: ImageButton
    private lateinit var btnTabs: TextView
    private lateinit var btnMenu: ImageButton

    private lateinit var adBlocker: AdBlocker
    private lateinit var db: DbHelper
    private lateinit var prefs: SharedPreferences
    private lateinit var tabAdapter: TabAdapter

    private val tabs = mutableListOf<BrowserTab>()
    private val globalVisitedOrigins = mutableSetOf<String>()
    private var currentTabIndex = 0
    private var mobileUserAgent: String = ""
    private var nextTabId = 1L
    private val cookieWipeHandler = Handler(Looper.getMainLooper())
    private val cookieWipeTasks = mutableMapOf<Long, Runnable>()
    private val loadTimeoutTasks = mutableMapOf<Long, Runnable>()
    private val recentlyClosed = ArrayDeque<ClosedTabSnapshot>()
    private var biometricPromptShowing = false
    private var restoredSession = false
    private var textToSpeech: TextToSpeech? = null
    private var pendingWebPermissionRequest: android.webkit.PermissionRequest? = null
    private val REQ_WEB_MEDIA = 610

    private data class ClosedTabSnapshot(val title: String, val url: String, val incognito: Boolean, val desktop: Boolean)

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        AppCompatDelegate.setDefaultNightMode(
            if (prefs.getBoolean(KEY_DARK_MODE, false))
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (prefs.getBoolean(KEY_DISABLE_SCREEN_CAPTURE, false)) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)

        adBlocker = AdBlocker(this)
        db = DbHelper(this)

        bindViews()
        setupTabStrip()
        setupToolbar()

        // Phase 3: Weekly Privacy Report — re-registering an existing alarm
        // is a no-op, so this is safe to call on every launch.
        WeeklyReportScheduler.scheduleIfNeeded(this)
        requestNotificationPermissionIfNeeded()
        if (prefs.getBoolean(KEY_PRUNE_LOGS, true)) db.pruneNetworkLog(THIRTY_DAYS_MILLIS)
        if (prefs.getBoolean(KEY_KEEP_SCREEN_ON, false)) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (prefs.getBoolean(KEY_AUTO_FOCUS_ADDRESS, false)) {
            urlBar.postDelayed({ urlBar.requestFocus(); (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).showSoftInput(urlBar, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT) }, 180)
        }

        handleIntentAction(intent)
        if (!restoredSession) {
            restoreSessionOrOpenInitial()
        }
    }

    private fun resolveInitialUrl(intent: Intent?): String {
        val incoming = intent?.data?.toString()?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        return incoming?.let { if (prefs.getBoolean(KEY_STRIP_TRACKING, true)) TrackingParamStripper.clean(it) else it } ?: getHomeUrl()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (handleIntentAction(intent)) return
        val incoming = intent.data?.toString()?.takeIf { it.startsWith("http://") || it.startsWith("https://") } ?: return
        val cleaned = if (prefs.getBoolean(KEY_STRIP_TRACKING, true)) TrackingParamStripper.clean(incoming) else incoming
        val host = Uri.parse(cleaned).host
        if (DomainPrivacyStore.isAutoIncognito(this, host) && currentTab()?.isIncognito != true) openNewTab(cleaned, true)
        else currentTab()?.webView?.loadUrl(cleaned)
    }

    private fun handleIntentAction(intent: Intent?): Boolean {
        val handled = when (intent?.action) {
            ACTION_OPEN_SITE_CONTROLS -> if (currentTab() != null) { showSiteControls(); true } else false
            ACTION_OPEN_TAB_MANAGER -> if (currentTab() != null) { showTabManager(); true } else false
            ACTION_OPEN_PRIVATE_TAB -> { restoredSession = true; openNewTab(getHomeUrl(), incognito = true); true }
            ACTION_OPEN_PAGE_TOOLS -> if (currentTab() != null) { showPageTools(); true } else false
            ACTION_PANIC -> { showPanicDialog(); true }
            ACTION_APPLY_PRESET -> { applyPreset(intent?.getStringExtra(EXTRA_PRESET).orEmpty()); true }
            ACTION_SHARE_PAGE -> { shareCurrentPage(); true }
            ACTION_COPY_URL -> { currentTab()?.let { copyText("Page URL", it.url) }; true }
            ACTION_COPY_TITLE -> { currentTab()?.let { copyText("Page title", it.title) }; true }
            ACTION_FIND_IN_PAGE -> { showFindInPageDialog(); true }
            ACTION_HARD_RELOAD -> { hardReload(); true }
            ACTION_CAPTURE_SCREENSHOT -> { capturePageScreenshot(); true }
            ACTION_TRANSLATE_PAGE -> { translatePage(); true }
            ACTION_LISTEN_PAGE -> { speakPage(); true }
            ACTION_COPY_PAGE_TEXT -> { copyPageText(); true }
            ACTION_COPY_PAGE_LINKS -> { copyAllLinks(); true }
            ACTION_COPY_PAGE_METADATA -> { copyPageMetadata(); true }
            ACTION_VIEW_SOURCE -> { viewPageSource(); true }
            ACTION_SAVE_ARCHIVE -> { saveWebArchive(); true }
            ACTION_PRINT_PDF -> { savePageAsPdf(); true }
            ACTION_OPEN_EXTERNAL -> { openExternal(); true }
            ACTION_TOGGLE_DESKTOP -> { toggleDesktopMode(); true }
            ACTION_ADD_BOOKMARK -> { addCurrentPageBookmark(); true }
            ACTION_CLEAR_SITE -> { clearCurrentSiteData(); true }
            ACTION_SHOW_PRIVACY_SCORE -> { showPrivacyScoreDialog(); true }
            ACTION_SHOW_DIAGNOSTICS -> { showDiagnosticsDialog(); true }
            ACTION_OPEN_BOOKMARKS -> { startActivityForResult(Intent(this, BookmarksActivity::class.java), REQ_BOOKMARKS); true }
            ACTION_OPEN_HISTORY -> { startActivityForResult(Intent(this, HistoryActivity::class.java), REQ_HISTORY); true }
            ACTION_OPEN_DOWNLOADS -> { startActivity(Intent(this, VideoDownloadsActivity::class.java)); true }
            ACTION_OPEN_VPN -> { startActivity(Intent(this, VpnActivity::class.java)); true }
            ACTION_OPEN_V2RAY -> { startActivity(Intent(this, V2RayActivity::class.java)); true }
            ACTION_DUPLICATE_TAB -> { currentTab()?.let { openNewTab(it.url, it.isIncognito, it.isDesktopMode) }; true }
            ACTION_OPEN_URL_NEW_TAB -> { currentTab()?.let { openNewTab(it.url, it.isIncognito, it.isDesktopMode) }; true }
            ACTION_SAVE_READING_LIST -> { currentTab()?.let { db.addReadingList(it.title, it.url); Toast.makeText(this, "Saved to reading list", Toast.LENGTH_SHORT).show() }; true }
            ACTION_CLEAR_HISTORY -> { db.clearHistory(); Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show(); true }
            ACTION_CLEAR_ALL_DATA -> { clearAllBrowsingData(); true }
            ACTION_TOGGLE_DARK_MODE -> { toggleDarkMode(); true }
            else -> false
        }
        if (handled) setIntent(Intent(intent).setAction(null))
        return handled
    }

    private fun restoreSessionOrOpenInitial() {
        if (prefs.getBoolean(KEY_RESTORE_SESSION, true)) {
            val raw = prefs.getStringSet(KEY_SESSION_URLS, emptySet()).orEmpty().toList()
            // StringSet does not preserve order; saveSessionSnapshot additionally stores the first
            // tab in a dedicated key so the common case still opens the most recently focused page first.
            val first = prefs.getString("session_first_url", null)
            val urls = buildList {
                first?.takeIf { it.startsWith("http://") || it.startsWith("https://") }?.let(::add)
                raw.filter { it != first }.take(MAX_RESTORED_TABS - 1).forEach(::add)
            }
            if (urls.isNotEmpty()) {
                restoredSession = true
                urls.forEachIndexed { index, url -> openNewTab(url, incognito = false) }
                return
            }
        }
        openNewTab(resolveInitialUrl(intent))
    }

    private fun saveSessionSnapshot() {
        if (!prefs.getBoolean(KEY_RESTORE_SESSION, true) || tabs.isEmpty()) return
        val urls = tabs.filter { !it.isIncognito }.map { it.url }.filter { it.startsWith("http://") || it.startsWith("https://") }
        if (urls.isEmpty()) return
        prefs.edit()
            .putStringSet(KEY_SESSION_URLS, urls.toSet())
            .putString("session_first_url", currentTab()?.url?.takeIf { it.startsWith("http://") || it.startsWith("https://") })
            .apply()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFICATIONS
                )
            }
        }
    }

    private fun bindViews() {
        urlBar = findViewById(R.id.urlBar)
        privacyBadge = findViewById(R.id.privacyBadge)
        progressBar = findViewById(R.id.progressBar)
        webViewContainer = findViewById(R.id.webViewContainer)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setColorSchemeResources(R.color.accent)
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.surface_light)
        swipeRefresh.setOnRefreshListener { currentTab()?.webView?.reload() }
        tabStrip = findViewById(R.id.tabStrip)
        btnBack = findViewById(R.id.btnBack)
        btnForward = findViewById(R.id.btnForward)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnHome = findViewById(R.id.btnHome)
        btnTools = findViewById(R.id.btnTools)
        btnTabs = findViewById(R.id.btnTabs)
        btnMenu = findViewById(R.id.btnMenu)
    }

    private fun setupTabStrip() {
        tabAdapter = TabAdapter(
            tabs,
            onTabSelected = { position -> switchToTab(position) },
            onTabClosed = { position -> closeTab(position) }
        )
        tabStrip.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        tabStrip.adapter = tabAdapter
    }

    /**
     * The tab strip only earns its screen space once there's an actual choice
     * to make between tabs. With a single tab open it stays collapsed so the
     * page gets that row back; it reappears the moment a second tab exists.
     */
    private fun updateTabStripVisibility() {
        val compact = prefs.getBoolean(KEY_COMPACT_TABS, true)
        tabStrip.visibility = if (compact && tabs.size <= 1) View.GONE else View.VISIBLE
    }

    private fun setupToolbar() {
        urlBar.setOnEditorActionListener { _, actionId, event ->
            val isEnter = event != null && event.keyCode == KeyEvent.KEYCODE_ENTER
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO || isEnter) {
                loadInput(urlBar.text.toString())
                true
            } else {
                false
            }
        }

        btnBack.setOnClickListener {
            val webView = currentTab()?.webView
            if (webView?.canGoBack() == true) webView.goBack()
        }
        btnForward.setOnClickListener {
            val webView = currentTab()?.webView
            if (webView?.canGoForward() == true) webView.goForward()
        }
        btnRefresh.setOnClickListener {
            val tab = currentTab() ?: return@setOnClickListener
            if (tab.webView.progress in 1..99) tab.webView.stopLoading() else tab.webView.reload()
        }
        btnHome.setOnClickListener { currentTab()?.webView?.loadUrl(getHomeUrl()) }
        btnTools.setOnClickListener { showPageTools() }
        btnTabs.setOnClickListener { openNewTab(getHomeUrl()) }
        btnTabs.setOnLongClickListener { showTabManager(); true }
        btnMenu.setOnClickListener { showOverflowMenu(it) }
        privacyBadge.setOnClickListener { showPrivacyScoreDialog() }
    }

    /** dp -> px, used only for building this programmatic bottom sheet. */
    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    private fun sheetHeader(container: LinearLayout, label: String) {
        container.addView(TextView(this).apply {
            text = label.uppercase()
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent))
            setPadding(dp(20), dp(18), dp(20), dp(6))
        })
    }

    private fun sheetDivider(container: LinearLayout) {
        container.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                leftMargin = dp(20); rightMargin = dp(20); topMargin = dp(8); bottomMargin = dp(2)
            }
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.divider))
        })
    }

    private fun sheetRow(container: LinearLayout, label: String, trailing: String? = null, onClick: () -> Unit) {
        val ripple = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, ripple, true)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            setBackgroundResource(ripple.resourceId)
            setPadding(dp(20), dp(14), dp(20), dp(14))
            setOnClickListener { onClick() }
        }
        row.addView(TextView(this).apply {
            text = label
            textSize = 15f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_light))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        if (trailing != null) {
            row.addView(TextView(this).apply {
                text = trailing
                textSize = 13f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_muted_light))
            })
        }
        container.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    /**
     * Grouped, theme-matched replacement for the old flat PopupMenu (which rendered as a
     * plain unstyled system list tall enough to cover the whole page). This is a bottom
     * sheet: it only takes up part of the screen, is swipe-to-dismiss, and organizes the
     * 14 destinations into scannable sections instead of one long undifferentiated list.
     */
    private fun showOverflowMenu(anchor: View) {
        val dialog = BottomSheetDialog(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(ContextCompat.getColor(this@MainActivity, R.color.surface_light))
                val r = dp(20).toFloat()
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
        }

        content.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(10)
            }
            background = GradientDrawable().apply {
                setColor(ContextCompat.getColor(this@MainActivity, R.color.text_muted_light))
                cornerRadius = dp(2).toFloat()
            }
        })

        sheetHeader(content, "Browse")
        sheetRow(content, "Tab manager", "Search • restore • close others") { dialog.dismiss(); showTabManager() }
        sheetRow(content, "Recently closed", if (recentlyClosed.isEmpty()) "None" else recentlyClosed.size.toString()) { dialog.dismiss(); showRecentlyClosed() }
        sheetRow(content, getString(R.string.bookmarks)) {
            dialog.dismiss(); startActivityForResult(Intent(this, BookmarksActivity::class.java), REQ_BOOKMARKS)
        }
        sheetRow(content, getString(R.string.history)) {
            dialog.dismiss(); startActivityForResult(Intent(this, HistoryActivity::class.java), REQ_HISTORY)
        }
        sheetRow(content, getString(R.string.add_bookmark)) { dialog.dismiss(); addCurrentPageBookmark() }
        sheetRow(content, getString(R.string.incognito_tab)) { dialog.dismiss(); openNewTab(getHomeUrl(), incognito = true) }
        sheetRow(content, getString(R.string.find_in_page)) { dialog.dismiss(); showFindInPageDialog() }
        sheetRow(content, getString(R.string.share_page)) { dialog.dismiss(); shareCurrentPage() }
        sheetRow(content, "Reading list") { dialog.dismiss(); startActivityForResult(Intent(this, ReadingListActivity::class.java), REQ_READING_LIST) }

        sheetDivider(content)
        sheetHeader(content, "Privacy & security")
        sheetRow(content, getString(R.string.network_log)) {
            dialog.dismiss(); startActivity(Intent(this, TransparencyLogActivity::class.java))
        }
        sheetRow(content, getString(R.string.phase4_menu)) {
            dialog.dismiss(); startActivity(Intent(this, Phase4Activity::class.java))
        }
        sheetRow(content, getString(R.string.vpn_menu)) {
            dialog.dismiss(); startActivity(Intent(this, VpnActivity::class.java))
        }
        sheetRow(content, getString(R.string.v2ray_menu)) {
            dialog.dismiss(); startActivity(Intent(this, V2RayActivity::class.java))
        }
        val adBlockLevelLabel = when (adBlocker.level) {
            com.privbrowse.app.adblock.BlockLevel.OFF -> getString(R.string.adblock_level_off)
            com.privbrowse.app.adblock.BlockLevel.NORMAL -> getString(R.string.adblock_level_normal)
            com.privbrowse.app.adblock.BlockLevel.STRICT -> getString(R.string.adblock_level_strict)
        }
        sheetRow(content, getString(R.string.adblock_menu), adBlockLevelLabel) {
            dialog.dismiss(); cycleAdBlockLevel()
        }
        sheetRow(content, getString(R.string.panic_menu)) { dialog.dismiss(); showPanicDialog() }
        val identityOn = prefs.getBoolean(KEY_FRESH_IDENTITY, true)
        sheetRow(content, getString(R.string.fresh_identity_menu), if (identityOn) "On" else "Off") {
            dialog.dismiss(); toggleFreshIdentity()
        }
        sheetRow(content, "Site controls") { dialog.dismiss(); showSiteControls() }
        sheetRow(content, "Settings & feature center") { dialog.dismiss(); startActivity(Intent(this, BrowserSettingsActivity::class.java)) }
        sheetRow(content, "Feature Center", "100+ controls & shortcuts") { dialog.dismiss(); startActivity(Intent(this, FeatureCenterActivity::class.java)) }

        sheetDivider(content)
        sheetHeader(content, "Tools")
        sheetRow(content, "Page tools", "Find • PDF • listen • translate") { dialog.dismiss(); showPageTools() }
        sheetRow(content, getString(R.string.ai_copilot_menu)) {
            dialog.dismiss(); openAiCopilot()
        }
        sheetRow(content, getString(R.string.video_downloads_menu)) { dialog.dismiss(); openDetectedVideoDownloads() }
        sheetRow(content, getString(R.string.reader_mode_menu)) {
            dialog.dismiss(); currentTab()?.webView?.evaluateJavascript(ReaderMode.SCRIPT, null)
        }

        sheetDivider(content)
        sheetHeader(content, "Display")
        val darkOn = prefs.getBoolean(KEY_DARK_MODE, false)
        sheetRow(content, getString(R.string.dark_mode), if (darkOn) "On" else "Off") {
            dialog.dismiss(); toggleDarkMode()
        }
        val desktopOn = currentTab()?.isDesktopMode ?: false
        sheetRow(content, getString(R.string.desktop_mode), if (desktopOn) "On" else "Off") {
            dialog.dismiss(); toggleDesktopMode()
        }

        content.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(12))
        })

        dialog.setContentView(ScrollView(this).apply { addView(content) })
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.55).toInt()
        dialog.show()
    }

    private fun openAiCopilot() {
        val tab = currentTab() ?: return
        if (!prefs.getBoolean(KEY_AI_CONTEXT, true)) {
            startActivity(Intent(this, AiCopilotActivity::class.java)); return
        }
        tab.webView.evaluateJavascript("document.body ? document.body.innerText : ''") { raw ->
            val text = runCatching { org.json.JSONTokener(raw).nextValue().toString() }.getOrElse { raw.trim('"') }
                .replace("\\n", "\n").take(12000)
            startActivity(Intent(this, AiCopilotActivity::class.java).apply {
                putExtra(AiCopilotActivity.EXTRA_PAGE_TITLE, tab.title)
                putExtra(AiCopilotActivity.EXTRA_PAGE_URL, tab.url)
                putExtra(AiCopilotActivity.EXTRA_PAGE_TEXT, text)
            })
        }
    }

    private fun addCurrentPageBookmark() {
        val tab = currentTab() ?: return
        db.addBookmark(tab.title, tab.url)
        Toast.makeText(this, "Bookmarked", Toast.LENGTH_SHORT).show()
    }

    private fun showFindInPageDialog() {
        val webView = currentTab()?.webView ?: return
        val input = EditText(this).apply {
            hint = "Find text on this page"
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        val wrap = FrameLayout(this).apply {
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(input, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(54)))
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.find_in_page)
            .setView(wrap)
            .setPositiveButton("Find") { _, _ ->
                val query = input.text.toString().trim()
                if (query.isNotEmpty()) webView.findAllAsync(query)
            }
            .setNegativeButton(R.string.close) { _, _ -> webView.clearMatches() }
            .show()
    }

    private fun shareCurrentPage() {
        val tab = currentTab() ?: return
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, tab.title)
            putExtra(Intent.EXTRA_TEXT, tab.url)
        }
        startActivity(Intent.createChooser(share, getString(R.string.share_page)))
    }

    // ---------- Phase 3: per-site privacy score ----------

    private fun updatePrivacyBadge(tab: BrowserTab) {
        privacyBadge.visibility = if (prefs.getBoolean(KEY_SHOW_PRIVACY_BADGE, true)) View.VISIBLE else View.GONE
        if (!prefs.getBoolean(KEY_SHOW_PRIVACY_BADGE, true)) return
        if (tab != currentTab()) return
        val isHttps = tab.url.startsWith("https://")
        val result = PrivacyScore.compute(
            isHttps = isHttps,
            trackersBlocked = tab.trackersBlockedThisPage,
            thirdPartyCookiesBlocked = true // Phase 2: always blocked, see privacy/CookiePolicy.kt
        )
        privacyBadge.text = result.grade
        privacyBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, result.colorRes))
    }

    private fun showPrivacyScoreDialog() {
        val tab = currentTab() ?: return
        val isHttps = tab.url.startsWith("https://")
        val result = PrivacyScore.compute(isHttps, tab.trackersBlockedThisPage, true)
        val host = Uri.parse(tab.url).host ?: tab.url
        val message = getString(
            R.string.privacy_score_details,
            if (isHttps) getString(R.string.yes) else getString(R.string.no),
            tab.trackersBlockedThisPage,
            tab.fingerprintBlockedThisPage
        )
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.privacy_score_title, result.grade, host))
            .setMessage(message)
            .setPositiveButton(R.string.view_network_log) { _, _ ->
                startActivity(
                    Intent(this, TransparencyLogActivity::class.java)
                        .putExtra(TransparencyLogActivity.EXTRA_ORIGIN_FILTER, tab.pageOrigin)
                )
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun cycleAdBlockLevel() {
        val newLevel = adBlocker.cycleLevel()
        val label = when (newLevel) {
            com.privbrowse.app.adblock.BlockLevel.OFF -> getString(R.string.adblock_level_off)
            com.privbrowse.app.adblock.BlockLevel.NORMAL -> getString(R.string.adblock_level_normal)
            com.privbrowse.app.adblock.BlockLevel.STRICT -> getString(R.string.adblock_level_strict)
        }
        Toast.makeText(this, "${getString(R.string.adblock_menu)}: $label", Toast.LENGTH_SHORT).show()
    }

    private fun applyPreset(name: String) {
        val editor = prefs.edit()
        when (name.lowercase()) {
            "balanced" -> {
                adBlocker.setLevel(com.privbrowse.app.adblock.BlockLevel.NORMAL)
                mapOf(
                    KEY_BLOCK_POPUPS to true, KEY_BLOCK_SOCIAL to true, KEY_BLOCK_GEOLOCATION to true,
                    KEY_BLOCK_MEDIA_PERMISSIONS to true, KEY_BLOCK_WEB_NOTIFICATIONS to true, KEY_BLOCK_MIXED_CONTENT to true,
                    KEY_THIRD_PARTY_COOKIES to true, KEY_STRIP_TRACKING to true, KEY_DNT to true, KEY_GPC to true,
                    KEY_HTTPS_FIRST to true, KEY_BLOCK_FILE_ACCESS to true, KEY_BLOCK_CONTENT_ACCESS to true,
                    KEY_DISABLE_FORM_HELPERS to true, KEY_JAVASCRIPT to true, KEY_IMAGES to true, KEY_FAST_CACHE to true,
                    KEY_DATA_SAVER to false, KEY_NO_CACHE to false, KEY_READER_MODE to true, KEY_AUTO_READER to false
                ).forEach { (key, value) -> editor.putBoolean(key, value) }
                editor.putInt(KEY_TEXT_ZOOM, 100)
            }
            "strict" -> {
                adBlocker.setLevel(com.privbrowse.app.adblock.BlockLevel.STRICT)
                mapOf(
                    KEY_BLOCK_POPUPS to true, KEY_BLOCK_SOCIAL to true, KEY_BLOCK_GEOLOCATION to true,
                    KEY_BLOCK_MEDIA_PERMISSIONS to true, KEY_BLOCK_WEB_NOTIFICATIONS to true, KEY_BLOCK_MIXED_CONTENT to true,
                    KEY_THIRD_PARTY_COOKIES to true, KEY_STRIP_TRACKING to true, KEY_DNT to true, KEY_GPC to true,
                    KEY_FRESH_IDENTITY to true, KEY_HTTPS_FIRST to true, KEY_BLOCK_FILE_ACCESS to true,
                    KEY_BLOCK_CONTENT_ACCESS to true, KEY_DISABLE_FORM_HELPERS to true, KEY_JAVASCRIPT to true,
                    KEY_IMAGES to true, KEY_FAST_CACHE to true, KEY_DATA_SAVER to false, KEY_NO_CACHE to false
                ).forEach { (key, value) -> editor.putBoolean(key, value) }
            }
            "speed" -> {
                adBlocker.setLevel(com.privbrowse.app.adblock.BlockLevel.NORMAL)
                mapOf(
                    KEY_BLOCK_POPUPS to true, KEY_BLOCK_SOCIAL to true, KEY_BLOCK_GEOLOCATION to true,
                    KEY_BLOCK_WEB_NOTIFICATIONS to true, KEY_BLOCK_MIXED_CONTENT to true, KEY_THIRD_PARTY_COOKIES to true,
                    KEY_STRIP_TRACKING to true, KEY_DNT to true, KEY_GPC to true, KEY_HTTPS_FIRST to true,
                    KEY_JAVASCRIPT to true, KEY_IMAGES to false, KEY_DOM_STORAGE to true, KEY_FAST_CACHE to true,
                    KEY_DATA_SAVER to true, KEY_NO_CACHE to false, KEY_OVERVIEW_MODE to true, KEY_ENABLE_ZOOM to true
                ).forEach { (key, value) -> editor.putBoolean(key, value) }
            }
            "maximum" -> {
                adBlocker.setLevel(com.privbrowse.app.adblock.BlockLevel.STRICT)
                mapOf(
                    KEY_BLOCK_POPUPS to true, KEY_BLOCK_SOCIAL to true, KEY_BLOCK_GEOLOCATION to true,
                    KEY_BLOCK_MEDIA_PERMISSIONS to true, KEY_BLOCK_WEB_NOTIFICATIONS to true, KEY_BLOCK_MIXED_CONTENT to true,
                    KEY_THIRD_PARTY_COOKIES to true, KEY_STRIP_TRACKING to true, KEY_DNT to true, KEY_GPC to true,
                    KEY_FRESH_IDENTITY to true, KEY_HTTPS_FIRST to true, KEY_BLOCK_FILE_ACCESS to true, KEY_BLOCK_CONTENT_ACCESS to true,
                    KEY_DISABLE_FORM_HELPERS to true, KEY_JAVASCRIPT to false, KEY_IMAGES to false, KEY_DOM_STORAGE to false,
                    KEY_FAST_CACHE to false, KEY_DATA_SAVER to true, KEY_NO_CACHE to true, KEY_SHOW_SCROLLBARS to false
                ).forEach { (key, value) -> editor.putBoolean(key, value) }
                editor.putInt(KEY_TEXT_ZOOM, 110)
            }
            "media" -> {
                adBlocker.setLevel(com.privbrowse.app.adblock.BlockLevel.NORMAL)
                mapOf(
                    KEY_BLOCK_POPUPS to true, KEY_BLOCK_SOCIAL to false, KEY_BLOCK_GEOLOCATION to true,
                    KEY_BLOCK_MEDIA_PERMISSIONS to false, KEY_BLOCK_WEB_NOTIFICATIONS to true, KEY_BLOCK_MIXED_CONTENT to true,
                    KEY_THIRD_PARTY_COOKIES to true, KEY_STRIP_TRACKING to true, KEY_DNT to true, KEY_GPC to true,
                    KEY_JAVASCRIPT to true, KEY_IMAGES to true, KEY_DOM_STORAGE to true, KEY_MEDIA_GESTURE to false,
                    KEY_FAST_CACHE to true, KEY_DATA_SAVER to false, KEY_NO_CACHE to false
                ).forEach { (key, value) -> editor.putBoolean(key, value) }
            }
            "developer" -> {
                adBlocker.setLevel(com.privbrowse.app.adblock.BlockLevel.OFF)
                mapOf(
                    KEY_BLOCK_POPUPS to false, KEY_BLOCK_SOCIAL to false, KEY_BLOCK_GEOLOCATION to false,
                    KEY_BLOCK_MEDIA_PERMISSIONS to false, KEY_BLOCK_WEB_NOTIFICATIONS to false, KEY_BLOCK_MIXED_CONTENT to false,
                    KEY_THIRD_PARTY_COOKIES to true, KEY_STRIP_TRACKING to false, KEY_DNT to false, KEY_GPC to false,
                    KEY_JAVASCRIPT to true, KEY_IMAGES to true, KEY_DOM_STORAGE to true, KEY_FAST_CACHE to false,
                    KEY_DATA_SAVER to false, KEY_NO_CACHE to true, KEY_OVERVIEW_MODE to false, KEY_DESKTOP_DEFAULT to true,
                    KEY_AUTO_STOP_LOADING to false
                ).forEach { (key, value) -> editor.putBoolean(key, value) }
            }
            "reading" -> {
                adBlocker.setLevel(com.privbrowse.app.adblock.BlockLevel.NORMAL)
                mapOf(
                    KEY_BLOCK_POPUPS to true, KEY_BLOCK_SOCIAL to true, KEY_BLOCK_GEOLOCATION to true,
                    KEY_BLOCK_WEB_NOTIFICATIONS to true, KEY_BLOCK_MIXED_CONTENT to true, KEY_THIRD_PARTY_COOKIES to true,
                    KEY_STRIP_TRACKING to true, KEY_DNT to true, KEY_GPC to true, KEY_HTTPS_FIRST to true,
                    KEY_READER_MODE to true, KEY_AUTO_READER to true, KEY_TEXT_EXTRACTION to true, KEY_TTS to true,
                    KEY_TRANSLATE to true, KEY_IMAGES to true, KEY_JAVASCRIPT to true, KEY_FAST_CACHE to true,
                    KEY_DATA_SAVER to false, KEY_NO_CACHE to false, KEY_SHOW_SCROLLBARS to false
                ).forEach { (key, value) -> editor.putBoolean(key, value) }
                editor.putInt(KEY_TEXT_ZOOM, 125)
                editor.putInt(KEY_DEFAULT_FONT_SIZE, 18)
                editor.putInt(KEY_MIN_FONT_SIZE, 10)
                editor.putInt(KEY_MONO_FONT_SIZE, 14)
            }
            else -> {
                Toast.makeText(this, "Unknown preset", Toast.LENGTH_SHORT).show()
                return
            }
        }
        editor.apply()
        adBlocker.refreshFromPreferences()
        applyGlobalSettingsToOpenTabs()
        currentTab()?.webView?.reload()
        Toast.makeText(this, "${name.replaceFirstChar { it.uppercase() }} preset applied", Toast.LENGTH_SHORT).show()
    }

    private fun showDiagnosticsDialog() {
        val tab = currentTab()
        val host = tab?.let { Uri.parse(it.url).host }.orEmpty().ifBlank { "—" }
        val message = buildString {
            appendLine("Page: ${tab?.title.orEmpty().ifBlank { "New tab" }}")
            appendLine("Host: $host")
            appendLine("URL: ${tab?.url.orEmpty().ifBlank { "—" }}")
            appendLine("HTTPS: ${tab?.url?.startsWith("https://") == true}")
            appendLine("Blocked trackers: ${tab?.trackersBlockedThisPage ?: 0}")
            appendLine("Fingerprint events: ${tab?.fingerprintBlockedThisPage ?: 0}")
            appendLine("Ad-block level: ${adBlocker.level.name}")
            appendLine("JavaScript: ${prefs.getBoolean(KEY_JAVASCRIPT, true)}")
            appendLine("Images: ${prefs.getBoolean(KEY_IMAGES, true)}")
            appendLine("3P cookies blocked: ${prefs.getBoolean(KEY_THIRD_PARTY_COOKIES, true)}")
            appendLine("Tracking parameters stripped: ${prefs.getBoolean(KEY_STRIP_TRACKING, true)}")
            appendLine("WebView version: ${android.webkit.WebView.getCurrentWebViewPackage()?.versionName ?: "unknown"}")
        }
        AlertDialog.Builder(this).setTitle("Privacy & performance diagnostics").setMessage(message)
            .setPositiveButton("Copy") { _, _ -> copyText("PrivBrowse diagnostics", message) }
            .setNegativeButton(R.string.close, null).show()
    }

    private fun toggleDarkMode() {
        val newValue = !prefs.getBoolean(KEY_DARK_MODE, false)
        prefs.edit().putBoolean(KEY_DARK_MODE, newValue).apply()
        AppCompatDelegate.setDefaultNightMode(
            if (newValue) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        tabs.forEach { applyWebViewDarkMode(it.webView, newValue) }
        recreate()
    }

    private fun toggleFreshIdentity() {
        val newValue = !prefs.getBoolean(KEY_FRESH_IDENTITY, true)
        prefs.edit().putBoolean(KEY_FRESH_IDENTITY, newValue).apply()
        // The document-start script is fixed to the WebView instance that
        // received it, so this takes effect for tabs opened from now on —
        // same as desktop mode and incognito, which are also set at tab
        // creation rather than retrofitted onto an open tab.
        Toast.makeText(
            this,
            if (newValue) "New tabs will get a fresh fingerprint" else "New tabs will use the real device fingerprint",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun applyWebViewDarkMode(webView: WebView, dark: Boolean) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, dark && prefs.getBoolean(KEY_DARKEN_PAGES, true))
        }
    }

    private fun toggleDesktopMode() {
        val tab = currentTab() ?: return
        tab.isDesktopMode = !tab.isDesktopMode
        applyUserAgent(tab)
        tab.webView.reload()
    }

    private fun applyUserAgent(tab: BrowserTab) {
        tab.webView.settings.userAgentString = if (tab.isDesktopMode) DESKTOP_UA else mobileUserAgent
        tab.webView.settings.useWideViewPort = tab.isDesktopMode
        tab.webView.settings.loadWithOverviewMode = tab.isDesktopMode
    }

    // ---------- Tab management ----------

    @SuppressLint("SetJavaScriptEnabled")
    private fun openNewTab(url: String, incognito: Boolean = false, desktop: Boolean = false) {
        val actualIncognito = incognito || prefs.getBoolean(KEY_NEW_TABS_PRIVATE, false)
        val webView = WebView(this)
        if (mobileUserAgent.isEmpty()) mobileUserAgent = webView.settings.userAgentString

        webView.settings.javaScriptEnabled = prefs.getBoolean(KEY_JAVASCRIPT, true)
        webView.settings.domStorageEnabled = prefs.getBoolean(KEY_DOM_STORAGE, true)
        webView.settings.loadsImagesAutomatically = prefs.getBoolean(KEY_IMAGES, true)
        webView.settings.mediaPlaybackRequiresUserGesture = prefs.getBoolean(KEY_MEDIA_GESTURE, true)
        webView.settings.databaseEnabled = false
        @Suppress("DEPRECATION") webView.settings.saveFormData = !prefs.getBoolean(KEY_DISABLE_FORM_HELPERS, true)
        @Suppress("DEPRECATION") webView.settings.savePassword = !prefs.getBoolean(KEY_DISABLE_FORM_HELPERS, true)
        webView.settings.allowFileAccess = !prefs.getBoolean(KEY_BLOCK_FILE_ACCESS, true)
        webView.settings.allowContentAccess = !prefs.getBoolean(KEY_BLOCK_CONTENT_ACCESS, true)
        webView.settings.setSupportMultipleWindows(!prefs.getBoolean(KEY_BLOCK_POPUPS, true))
        webView.settings.javaScriptCanOpenWindowsAutomatically = !prefs.getBoolean(KEY_BLOCK_POPUPS, true)
        webView.settings.setSupportZoom(prefs.getBoolean(KEY_ENABLE_ZOOM, true))
        webView.settings.builtInZoomControls = prefs.getBoolean(KEY_ENABLE_ZOOM, true)
        webView.settings.displayZoomControls = false
        webView.settings.useWideViewPort = prefs.getBoolean(KEY_WIDE_VIEWPORT, false)
        webView.settings.loadWithOverviewMode = prefs.getBoolean(KEY_OVERVIEW_MODE, true)
        webView.isVerticalScrollBarEnabled = prefs.getBoolean(KEY_SHOW_SCROLLBARS, true)
        webView.isHorizontalScrollBarEnabled = prefs.getBoolean(KEY_SHOW_SCROLLBARS, false)
        webView.isScrollbarFadingEnabled = true
        webView.settings.defaultFontSize = prefs.getInt(KEY_DEFAULT_FONT_SIZE, 16).coerceIn(8, 30)
        webView.settings.minimumFontSize = prefs.getInt(KEY_MIN_FONT_SIZE, 8).coerceIn(1, 24)
        webView.settings.defaultFixedFontSize = prefs.getInt(KEY_MONO_FONT_SIZE, 13).coerceIn(6, 30)
        webView.settings.textZoom = prefs.getInt(KEY_TEXT_ZOOM, 100).coerceIn(50, 200)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webView.settings.allowFileAccessFromFileURLs = false
            webView.settings.allowUniversalAccessFromFileURLs = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) webView.settings.safeBrowsingEnabled = prefs.getBoolean(KEY_SAFE_BROWSING, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.settings.mixedContentMode = if (prefs.getBoolean(KEY_BLOCK_MIXED_CONTENT, true)) android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW else android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }
        applyWebViewDarkMode(webView, prefs.getBoolean(KEY_DARK_MODE, false))

        // Fresh identity per tab: each tab gets its own canvas/audio/WebGL/
        // hardware-profile noise, seeded from its tab id, injected before any
        // page script runs on every navigation in this WebView. See
        // privacy/FingerprintRandomizer.kt for why this — not a literal
        // "Android ID" — is what actually defeats cross-tab fingerprinting.
        if (prefs.getBoolean(KEY_FRESH_IDENTITY, true) &&
            WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
        ) {
            WebViewCompat.addDocumentStartJavaScript(
                webView,
                FingerprintRandomizer.script(nextTabId),
                setOf("*")
            )
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(webView, privacySignalsScript(), setOf("*"))
            WebViewCompat.addDocumentStartJavaScript(webView, contentGuardScript(), setOf("*"))
        }

        // Phase 2: third-party cookies are blocked on every tab, always.
        CookiePolicy.configureWebView(webView, prefs.getBoolean(KEY_THIRD_PARTY_COOKIES, true), prefs.getBoolean(KEY_ACCEPT_COOKIES, true))
        if (actualIncognito && prefs.getBoolean(KEY_INCOGNITO_NO_CACHE, true)) {
            webView.settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
        } else when {
            prefs.getBoolean(KEY_NO_CACHE, false) -> webView.settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
            prefs.getBoolean(KEY_FAST_CACHE, true) || prefs.getBoolean(KEY_DATA_SAVER, false) -> webView.settings.cacheMode = android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK
            else -> webView.settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        }

        val tab = BrowserTab(nextTabId++, webView)
        tab.isIncognito = actualIncognito
        tab.isDesktopMode = desktop || prefs.getBoolean(KEY_DESKTOP_DEFAULT, false)
        applyUserAgent(tab)
        webView.webViewClient = BrowserWebViewClient(tab)
        webView.webChromeClient = BrowserChromeClient(tab)
        webView.setDownloadListener { downloadUrl, _, _, mimeType, _ ->
            handleDownload(downloadUrl, mimeType)
        }
        webView.setOnLongClickListener { handleLongPress(webView) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            webView.setOnScrollChangeListener { _, _, scrollY, _, _ -> tab.lastScrollY = scrollY }
        }

        tabs.add(tab)
        webViewContainer.addView(
            webView,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )
        webView.visibility = View.GONE

        tabAdapter.notifyItemInserted(tabs.size - 1)
        updateTabStripVisibility()
        updateTabCount()
        switchToTab(tabs.size - 1)
        webView.loadUrl(url)
    }

    private fun switchToTab(position: Int) {
        if (position !in tabs.indices) return
        tabs.forEachIndexed { index, tab -> tab.webView.visibility = if (index == position) View.VISIBLE else View.GONE }
        currentTabIndex = position
        tabAdapter.selectedPosition = position
        updateTabCount()
        urlBar.setText(currentTab()?.url ?: "")
        currentTab()?.let { updatePrivacyBadge(it) }
        updateNavState()
    }

    private fun closeTab(position: Int) {
        if (position !in tabs.indices) return
        val oldCurrent = currentTabIndex
        val closingCurrent = position == oldCurrent
        val tab = tabs.removeAt(position)
        if (prefs.getBoolean(KEY_RECENT_TABS, true) && !tab.isIncognito && (tab.url.startsWith("http://") || tab.url.startsWith("https://"))) {
            recentlyClosed.addFirst(ClosedTabSnapshot(tab.title, tab.url, false, tab.isDesktopMode))
            while (recentlyClosed.size > MAX_REOPENED) recentlyClosed.removeLast()
        }
        cookieWipeTasks.remove(tab.id)?.let(cookieWipeHandler::removeCallbacks)
        loadTimeoutTasks.remove(tab.id)?.let(cookieWipeHandler::removeCallbacks)
        if (tab.isIncognito) {
            CookiePolicy.wipeOrigins(tab.visitedOrigins)
            tab.webView.clearCache(true)
            tab.webView.clearHistory()
        }
        webViewContainer.removeView(tab.webView)
        tab.webView.destroy()
        tabAdapter.notifyItemRemoved(position)
        updateTabStripVisibility()
        updateTabCount()

        if (tabs.isEmpty()) {
            currentTabIndex = 0
            openNewTab(getHomeUrl())
            return
        }
        val newPosition = when {
            closingCurrent -> position.coerceAtMost(tabs.lastIndex)
            position < oldCurrent -> (oldCurrent - 1).coerceIn(0, tabs.lastIndex)
            else -> oldCurrent.coerceIn(0, tabs.lastIndex)
        }
        switchToTab(newPosition)
    }

    private fun updateTabCount() {
        btnTabs.text = tabs.size.toString()
        btnTabs.contentDescription = "${tabs.size} tab${if (tabs.size == 1) "" else "s"}"
    }

    private fun currentTab(): BrowserTab? = tabs.getOrNull(currentTabIndex)

    private fun updateNavState() {
        val webView = currentTab()?.webView
        btnBack.isEnabled = webView?.canGoBack() == true
        btnForward.isEnabled = webView?.canGoForward() == true
        btnBack.alpha = if (btnBack.isEnabled) 1f else 0.35f
        btnForward.alpha = if (btnForward.isEnabled) 1f else 0.35f
    }

    private fun getHomeUrl(): String = prefs.getString(KEY_HOME_URL, HOME_URL) ?: HOME_URL

    private fun searchUrl(query: String): String = when (prefs.getString(KEY_SEARCH_ENGINE, "DuckDuckGo")) {
        "Brave Search" -> "https://search.brave.com/search?q=${Uri.encode(query)}"
        "Bing" -> "https://www.bing.com/search?q=${Uri.encode(query)}"
        "Google" -> "https://www.google.com/search?q=${Uri.encode(query)}"
        "Startpage" -> "https://www.startpage.com/sp/search?query=${Uri.encode(query)}"
        "Ecosia" -> "https://www.ecosia.org/search?q=${Uri.encode(query)}"
        else -> SEARCH_URL_PREFIX + Uri.encode(query)
    }

    private fun duplicateCurrentTab() {
        val tab = currentTab() ?: return
        openNewTab(tab.url, tab.isIncognito, tab.isDesktopMode)
    }

    private fun showRecentlyClosed() {
        if (recentlyClosed.isEmpty()) {
            Toast.makeText(this, "No recently closed tabs", Toast.LENGTH_SHORT).show()
            return
        }
        val labels = recentlyClosed.map { it.title.ifBlank { it.url } }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Recently closed")
            .setItems(labels) { _, which ->
                val snapshot = recentlyClosed.elementAt(which)
                recentlyClosed.remove(snapshot)
                openNewTab(snapshot.url, snapshot.incognito, snapshot.desktop)
            }
            .setNeutralButton("Clear") { _, _ -> recentlyClosed.clear() }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun showTabManager() {
        if (tabs.isEmpty()) return
        val wrapper = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(8), dp(18), dp(10)) }
        val search = EditText(this).apply { hint = "Search tabs"; setSingleLine(true) }
        wrapper.addView(search, LinearLayout.LayoutParams(-1, dp(50)))
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        fun smallAction(label: String, click: () -> Unit) = TextView(this).apply {
            text = label; textSize = 12f; setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_dark)); setPadding(dp(5), dp(8), dp(12), dp(8)); setOnClickListener { click() }
        }
        actions.addView(smallAction("Duplicate") { duplicateCurrentTab() })
        actions.addView(smallAction("Close others") { closeOtherTabs() })
        actions.addView(smallAction("Close all") { closeAllTabs() })
        wrapper.addView(actions)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(list) }
        wrapper.addView(scroll, LinearLayout.LayoutParams(-1, dp(360)))

        fun render(filter: String) {
            list.removeAllViews()
            tabs.forEachIndexed { index, tab ->
                val label = (if (tab.isIncognito) "Private · " else "") + (tab.title.ifBlank { tab.url })
                if (filter.isNotBlank() && !label.contains(filter, ignoreCase = true) && !tab.url.contains(filter, ignoreCase = true)) return@forEachIndexed
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(10), dp(9), dp(6), dp(9)); isClickable = true; setOnClickListener { switchToTab(index) }
                }
                val textColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                textColumn.addView(TextView(this@MainActivity).apply { setText(label); textSize = 14f; setTypeface(typeface, Typeface.BOLD); setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_light)); maxLines = 1 })
                textColumn.addView(TextView(this@MainActivity).apply { setText(tab.url); textSize = 11f; setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_muted_light)); maxLines = 1 })
                row.addView(textColumn, LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(TextView(this@MainActivity).apply { text = "×"; textSize = 22f; setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_muted_light)); setPadding(dp(12), 0, 0, 0); setOnClickListener { closeTab(index) } })
                list.addView(row)
            }
        }
        render("")
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { render(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(wrapper)
        dialog.show()
    }

    private fun closeOtherTabs() {
        val keep = currentTabIndex
        for (i in tabs.indices.reversed()) if (i != keep) closeTab(i)
    }

    private fun closeAllTabs() {
        val perform = {
            val indices = tabs.indices.reversed().toList()
            indices.forEach { closeTab(it) }
        }
        if (!prefs.getBoolean(KEY_CONFIRM_CLOSE_ALL, true)) { perform(); return }
        AlertDialog.Builder(this)
            .setTitle("Close all tabs?")
            .setMessage("All open tabs will be closed.")
            .setNegativeButton(R.string.close, null)
            .setPositiveButton("Close all") { _, _ -> perform() }
            .show()
    }

    // ---------- URL / search bar ----------

    private fun loadInput(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return
        val url = resolveInputToUrl(trimmed)
        val host = Uri.parse(url).host
        if (DomainPrivacyStore.isAutoIncognito(this, host) && currentTab()?.isIncognito != true) {
            openNewTab(url, incognito = true)
        } else {
            currentTab()?.webView?.loadUrl(url)
        }
        clearFocusAndHideKeyboard()
    }

    private fun resolveInputToUrl(input: String): String {
        val looksLikeUrl = Patterns.WEB_URL.matcher(input).matches() && !input.contains(" ")
        val resolved = when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            looksLikeUrl -> if (prefs.getBoolean(KEY_HTTPS_FIRST, true)) "https://$input" else "http://$input"
            else -> searchUrl(input)
        }
        return if (prefs.getBoolean(KEY_STRIP_TRACKING, true)) TrackingParamStripper.clean(resolved) else resolved
    }

    private fun clearFocusAndHideKeyboard() {
        urlBar.clearFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(urlBar.windowToken, 0)
    }

    // ---------- WebView clients ----------

    private inner class BrowserWebViewClient(private val tab: BrowserTab) : android.webkit.WebViewClient() {

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest
        ): WebResourceResponse? {
            val host = request.url?.host?.lowercase() ?: return null
            val category = adBlocker.classify(host)

            // Phase 3: Network Transparency Log + per-site Privacy Score.
            if (category != null) {
                tab.incrementTracker(category == BlockCategory.FINGERPRINT)
                db.logNetworkEvent(tab.pageOrigin, host, blocked = true, category = category.name.lowercase())
                runOnUiThread { updatePrivacyBadge(tab) }
                return adBlocker.emptyResponse()
            }

            if (!request.isForMainFrame) {
                val pageHost = try { Uri.parse(tab.pageOrigin).host } catch (e: Exception) { null }
                if (pageHost != null && host != pageHost && tab.loggedHostsThisPage.add(host)) {
                    db.logNetworkEvent(tab.pageOrigin, host, blocked = false, category = null)
                }
            }
            return null
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
            super.onReceivedError(view, request, error)
            if (request.isForMainFrame) {
                showErrorPage(view, request.url.toString(), error.description?.toString() ?: "Couldn't load this page")
            }
        }

        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
            super.onReceivedHttpError(view, request, errorResponse)
            if (request.isForMainFrame && errorResponse.statusCode >= 400) {
                showErrorPage(view, request.url.toString(), "HTTP error ${errorResponse.statusCode}")
            }
        }

        private fun showErrorPage(view: WebView, failedUrl: String, message: String) {
            val safeUrl = android.text.Html.escapeHtml(failedUrl)
            val safeMsg = android.text.Html.escapeHtml(message)
            val html = """
                <html><head><meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                  body{font-family:sans-serif;background:#101120;color:#EEF0FA;display:flex;flex-direction:column;
                       align-items:center;justify-content:center;min-height:100vh;margin:0;padding:32px;text-align:center;box-sizing:border-box}
                  h2{margin:0 0 10px;font-size:20px}
                  p{color:#9497B8;font-size:12.5px;word-break:break-all;margin:0 0 26px;max-width:320px}
                  a{background:#00C2A8;color:#0A0F1F;padding:12px 30px;border-radius:24px;text-decoration:none;font-weight:bold;font-size:14px}
                </style></head>
                <body>
                  <h2>This page couldn't load</h2>
                  <p>$safeMsg<br>$safeUrl</p>
                  <a href="$safeUrl">Retry</a>
                </body></html>
            """.trimIndent()
            view.loadDataWithBaseURL(null, html, "text/html", "UTF-8", failedUrl)
            if (tab == currentTab()) { progressBar.visibility = View.GONE; btnRefresh.setImageResource(R.drawable.ic_refresh) }
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val original = request.url.toString()
            val uri = Uri.parse(original)
            if (uri.scheme != "http" && uri.scheme != "https") {
                if (prefs.getBoolean(KEY_EXTERNAL_HANDOFF, true)) {
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        .onFailure { Toast.makeText(this@MainActivity, "No app can open this link", Toast.LENGTH_SHORT).show() }
                    return true
                }
                return false
            }
            val cleaned = if (prefs.getBoolean(KEY_STRIP_TRACKING, true)) TrackingParamStripper.clean(original) else original
            val host = Uri.parse(cleaned).host
            if (DomainPrivacyStore.isAutoIncognito(this@MainActivity, host) && tab.isIncognito.not()) {
                openNewTab(cleaned, incognito = true)
                return true
            }
            return if (cleaned != original) {
                view.loadUrl(cleaned)
                true
            } else {
                false // let WebView load it, keeps navigation inside the app
            }
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            if (url != null) {
                tab.url = url
                tab.resetPageStats() // Phase 3: tracker/fingerprint counts + logged hosts are per page-load
                loadTimeoutTasks.remove(tab.id)?.let(cookieWipeHandler::removeCallbacks)
                if (prefs.getBoolean(KEY_AUTO_STOP_LOADING, false)) {
                    val task = Runnable {
                        if (tabs.contains(tab) && tab.webView.progress in 0..99) {
                            tab.webView.stopLoading()
                            if (tab == currentTab()) Toast.makeText(this@MainActivity, "Page load timed out; loading stopped", Toast.LENGTH_SHORT).show()
                        }
                    }
                    loadTimeoutTasks[tab.id] = task
                    cookieWipeHandler.postDelayed(task, 45_000L)
                }
                Uri.parse(url).let { uri ->
                    val scheme = uri.scheme?.lowercase()
                    val host = uri.host
                    if ((scheme == "http" || scheme == "https") && !host.isNullOrBlank()) {
                        val origin = "$scheme://$host"
                        tab.pageOrigin = origin
                        tab.visitedOrigins.add(origin)
                        globalVisitedOrigins.add(origin)
                    } else {
                        tab.pageOrigin = ""
                    }
                }
                if (tab == currentTab()) urlBar.setText(url)
            }
            progressBar.visibility = View.VISIBLE
            if (tab == currentTab()) btnRefresh.setImageResource(R.drawable.ic_close_small)
            updatePrivacyBadge(tab)
            if (prefs.getBoolean(KEY_FRESH_IDENTITY, true) &&
                !WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
            ) {
                // Older WebView: no true document-start hook, so this is a
                // best-effort injection as early in the load as we can get.
                view.evaluateJavascript(FingerprintRandomizer.script(tab.id), null)
            }
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                view.evaluateJavascript(privacySignalsScript(), null)
            }
        }

        override fun onPageFinished(view: WebView, url: String?) {
            super.onPageFinished(view, url)
            loadTimeoutTasks.remove(tab.id)?.let(cookieWipeHandler::removeCallbacks)
            if (prefs.getBoolean(KEY_SCROLL_RESTORE, true) && tab.lastScrollY > 0) view.postDelayed({ view.scrollTo(0, tab.lastScrollY) }, 120)
            if (prefs.getBoolean(KEY_AUTO_READER, false) && prefs.getBoolean(KEY_READER_MODE, true)) {
                view.evaluateJavascript("(function(){return !!document.querySelector('article') || ((document.body&&document.body.innerText||'').length>7000)})()") { result ->
                    if (result == "true") view.postDelayed({ view.evaluateJavascript(ReaderMode.SCRIPT, null) }, 200)
                }
            }
            progressBar.visibility = View.GONE
            btnRefresh.setImageResource(R.drawable.ic_refresh)
            if (tab == currentTab()) swipeRefresh.isRefreshing = false
            if (prefs.getBoolean(KEY_COOKIE_BANNER, true)) view.evaluateJavascript(ConsentAutoHandler.SCRIPT, null)
            if (url != null) {
                tab.url = url
                val title = view.title ?: url
                tab.title = title
                if (!tab.isIncognito && prefs.getBoolean(KEY_SAVE_HISTORY, true)) db.addHistoryEntry(title, url) // Incognito and disabled-history tabs never touch local history
                val idx = tabs.indexOf(tab)
                if (idx != -1) tabAdapter.notifyItemChanged(idx)
            }
            tab.detectedVideoUrls.clear()
            view.evaluateJavascript(VideoDetector.SCRIPT) { raw ->
                tab.detectedVideoUrls.addAll(VideoDetector.parse(raw))
            }
            scheduleCookieWipe(tab)
            updatePrivacyBadge(tab) // Phase 3: finalize once HTTPS/tracker state has settled
            updateNavState()
        }
    }

    private inner class BrowserChromeClient(private val tab: BrowserTab) : android.webkit.WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            if (tab == currentTab()) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
                btnRefresh.setImageResource(if (newProgress in 1..99) R.drawable.ic_close_small else R.drawable.ic_refresh)
                updateNavState()
            }
        }

        override fun onPermissionRequest(request: android.webkit.PermissionRequest) {
            if (prefs.getBoolean(KEY_BLOCK_MEDIA_PERMISSIONS, true)) {
                request.deny()
                Toast.makeText(this@MainActivity, "Web camera/microphone blocked", Toast.LENGTH_SHORT).show()
                return
            }
            val wantsCamera = request.resources.contains(android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE)
            val wantsMic = request.resources.contains(android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE)
            val cameraGranted = !wantsCamera || ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val micGranted = !wantsMic || ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (cameraGranted && micGranted) {
                request.grant(request.resources)
            } else {
                pendingWebPermissionRequest = request
                val permissions = buildList {
                    if (wantsCamera && !cameraGranted) add(android.Manifest.permission.CAMERA)
                    if (wantsMic && !micGranted) add(android.Manifest.permission.RECORD_AUDIO)
                }.toTypedArray()
                if (permissions.isNotEmpty()) ActivityCompat.requestPermissions(this@MainActivity, permissions, REQ_WEB_MEDIA) else request.deny()
            }
        }

        override fun onGeolocationPermissionsShowPrompt(origin: String, callback: android.webkit.GeolocationPermissions.Callback) {
            callback.invoke(origin, !prefs.getBoolean(KEY_BLOCK_GEOLOCATION, true), false)
        }

        override fun onReceivedTitle(view: WebView, title: String?) {
            super.onReceivedTitle(view, title)
            if (title != null) {
                tab.title = title
                val idx = tabs.indexOf(tab)
                if (idx != -1) tabAdapter.notifyItemChanged(idx)
            }
        }
    }

    private fun scheduleCookieWipe(tab: BrowserTab) {
        cookieWipeTasks.remove(tab.id)?.let(cookieWipeHandler::removeCallbacks)
        val host = Uri.parse(tab.url).host?.lowercase() ?: return
        val duration = DomainPrivacyStore.timerForHost(this, host) ?: return
        if (duration <= 0L) return
        val scheduledUrl = tab.url
        val task = Runnable {
            cookieWipeTasks.remove(tab.id)
            if (isFinishing || (Build.VERSION.SDK_INT >= 17 && isDestroyed)) return@Runnable
            if (!tabs.contains(tab) || tab.url != scheduledUrl) return@Runnable
            CookiePolicy.wipeOrigins(setOf("https://$host", "http://$host"))
            Toast.makeText(this, "Cookie timer expired for $host", Toast.LENGTH_SHORT).show()
        }
        cookieWipeTasks[tab.id] = task
        cookieWipeHandler.postDelayed(task, duration)
    }

    private fun handleDownload(url: String, mimeType: String?) {
        val host = Uri.parse(url).host?.lowercase().orEmpty()
        if (host == "youtu.be" || host == "youtube.com" || host.endsWith(".youtube.com") || host == "instagram.com" || host.endsWith(".instagram.com") || host == "tiktok.com" || host.endsWith(".tiktok.com")) {
            Toast.makeText(this, "This downloader excludes YouTube, Instagram and TikTok.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!VideoDownloadsActivity.looksLikeVideo(url, mimeType)) {
            Toast.makeText(this, "This download is not a directly retrievable video.", Toast.LENGTH_SHORT).show()
            return
        }
        val enqueue = {
            val id = VideoDownloadsActivity.enqueue(this, url, mimeType)
            Toast.makeText(this, if (id != null) "Video download queued" else "Download could not be started", Toast.LENGTH_SHORT).show()
        }
        if (prefs.getBoolean(KEY_DOWNLOAD_CONFIRM, true)) {
            AlertDialog.Builder(this).setTitle("Download media?").setMessage(host.ifBlank { "Direct media" }).setNegativeButton("Cancel", null).setPositiveButton("Download") { _, _ -> enqueue() }.show()
        } else enqueue()
    }

    private fun openDetectedVideoDownloads() {
        if (!prefs.getBoolean(KEY_VIDEO_DETECTION, true)) { Toast.makeText(this, "Video detection is disabled", Toast.LENGTH_SHORT).show(); return }
        val urls = currentTab()?.detectedVideoUrls.orEmpty().filter { VideoDownloadsActivity.looksLikeVideo(it, null) }
        if (urls.isEmpty()) {
            Toast.makeText(this, "No direct/embedded video URL detected on this page.", Toast.LENGTH_SHORT).show()
            return
        }
        val labels = urls.map { it.take(72) }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Detected videos")
            .setItems(labels) { _, which ->
                val id = VideoDownloadsActivity.enqueue(this, urls[which])
                Toast.makeText(this, if (id != null) "Download queued" else "Download failed to start", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun showPanicDialog() {
        AlertDialog.Builder(this)
            .setTitle("Panic button")
            .setMessage("Close all tabs and clear browsing history, cookies, site storage and network log?")
            .setNegativeButton(R.string.close, null)
            .setPositiveButton("PANIC") { _, _ -> triggerPanic() }
            .show()
    }

    private fun triggerPanic() {
        cookieWipeTasks.values.forEach(cookieWipeHandler::removeCallbacks)
        cookieWipeTasks.clear()
        loadTimeoutTasks.values.forEach(cookieWipeHandler::removeCallbacks)
        loadTimeoutTasks.clear()
        globalVisitedOrigins.clear()
        PanicManager.wipe(this, tabs, db)
        prefs.edit().remove(KEY_SESSION_URLS).remove("session_first_url").apply()
        recentlyClosed.clear()
        tabs.forEach { webViewContainer.removeView(it.webView); it.webView.destroy() }
        tabs.clear()
        currentTabIndex = 0
        tabAdapter.notifyDataSetChanged()
        openNewTab(getHomeUrl())
        Toast.makeText(this, "Browsing data cleared", Toast.LENGTH_SHORT).show()
        moveTaskToBack(true)
    }

    private fun privacySignalsScript(): String = """
        (function(){try{
          Object.defineProperty(navigator,'doNotTrack',{get:function(){return '${if (prefs.getBoolean(KEY_DNT, true)) "1" else "0"}';}, configurable:true});
          Object.defineProperty(navigator,'globalPrivacyControl',{get:function(){return ${prefs.getBoolean(KEY_GPC, true)};}, configurable:true});
        }catch(e){}})();
    """.trimIndent()

    private fun contentGuardScript(): String = """
        (function(){try{
          if(${prefs.getBoolean(KEY_BLOCK_WEB_NOTIFICATIONS, true)}){
            try{Object.defineProperty(window,'Notification',{value:undefined,configurable:true});}catch(e){}
          }
          if(${prefs.getBoolean(KEY_BLOCK_SOCIAL, true)}){
            var hide=function(){try{
              document.querySelectorAll('iframe[src*="facebook.com"],iframe[src*="instagram.com"],iframe[src*="twitter.com"],iframe[src*="x.com"],iframe[src*="linkedin.com"],iframe[src*="pinterest.com"]').forEach(function(x){x.style.display='none';});
            }catch(e){}};
            if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',hide); else hide();
          }
        }catch(e){}})();
    """.trimIndent()

    private fun applyGlobalSettingsToOpenTabs() {
        val dark = prefs.getBoolean(KEY_DARK_MODE, false)
        tabs.forEach { tab ->
            tab.webView.settings.javaScriptEnabled = prefs.getBoolean(KEY_JAVASCRIPT, true)
            tab.webView.settings.loadsImagesAutomatically = prefs.getBoolean(KEY_IMAGES, true)
            tab.webView.settings.domStorageEnabled = prefs.getBoolean(KEY_DOM_STORAGE, true)
            tab.webView.settings.mediaPlaybackRequiresUserGesture = prefs.getBoolean(KEY_MEDIA_GESTURE, true)
            tab.webView.settings.setSupportMultipleWindows(!prefs.getBoolean(KEY_BLOCK_POPUPS, true))
            tab.webView.settings.javaScriptCanOpenWindowsAutomatically = !prefs.getBoolean(KEY_BLOCK_POPUPS, true)
            tab.webView.settings.setSupportZoom(prefs.getBoolean(KEY_ENABLE_ZOOM, true))
            tab.webView.settings.builtInZoomControls = prefs.getBoolean(KEY_ENABLE_ZOOM, true)
            tab.webView.settings.displayZoomControls = false
            tab.webView.settings.useWideViewPort = prefs.getBoolean(KEY_WIDE_VIEWPORT, false)
            tab.webView.settings.loadWithOverviewMode = prefs.getBoolean(KEY_OVERVIEW_MODE, true)
            tab.webView.isVerticalScrollBarEnabled = prefs.getBoolean(KEY_SHOW_SCROLLBARS, true)
            tab.webView.isHorizontalScrollBarEnabled = prefs.getBoolean(KEY_SHOW_SCROLLBARS, false)
            tab.webView.isScrollbarFadingEnabled = true
            tab.webView.settings.defaultFontSize = prefs.getInt(KEY_DEFAULT_FONT_SIZE, 16).coerceIn(8, 30)
            tab.webView.settings.minimumFontSize = prefs.getInt(KEY_MIN_FONT_SIZE, 8).coerceIn(1, 24)
            tab.webView.settings.defaultFixedFontSize = prefs.getInt(KEY_MONO_FONT_SIZE, 13).coerceIn(6, 30)
            tab.webView.settings.textZoom = prefs.getInt(KEY_TEXT_ZOOM, 100).coerceIn(50, 200)
            tab.webView.settings.allowFileAccess = !prefs.getBoolean(KEY_BLOCK_FILE_ACCESS, true)
            tab.webView.settings.allowContentAccess = !prefs.getBoolean(KEY_BLOCK_CONTENT_ACCESS, true)
            @Suppress("DEPRECATION") tab.webView.settings.saveFormData = !prefs.getBoolean(KEY_DISABLE_FORM_HELPERS, true)
            @Suppress("DEPRECATION") tab.webView.settings.savePassword = !prefs.getBoolean(KEY_DISABLE_FORM_HELPERS, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) tab.webView.settings.safeBrowsingEnabled = prefs.getBoolean(KEY_SAFE_BROWSING, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) tab.webView.settings.mixedContentMode = if (prefs.getBoolean(KEY_BLOCK_MIXED_CONTENT, true)) android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW else android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            applyWebViewDarkMode(tab.webView, dark)
            CookiePolicy.configureWebView(tab.webView, prefs.getBoolean(KEY_THIRD_PARTY_COOKIES, true), prefs.getBoolean(KEY_ACCEPT_COOKIES, true))
            if (prefs.getBoolean(KEY_NO_CACHE, false)) tab.webView.settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
            else if (!tab.isIncognito) tab.webView.settings.cacheMode = if (prefs.getBoolean(KEY_FAST_CACHE, true) || prefs.getBoolean(KEY_DATA_SAVER, false)) android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK else android.webkit.WebSettings.LOAD_DEFAULT
        }
        updatePrivacyBadge(currentTab() ?: return)
    }

    private fun handleLongPress(webView: WebView): Boolean {
        val hit = webView.hitTestResult
        val target = hit.extra?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        if (target == null) return false
        val labels = arrayOf("Open in new tab", "Open in private tab", "Copy link", "Share link")
        AlertDialog.Builder(this).setItems(labels) { _, which ->
            when (which) {
                0 -> openNewTab(target, incognito = if (prefs.getBoolean(KEY_OPEN_LINKS_PRIVATE, false)) true else false)
                1 -> openNewTab(target, incognito = true)
                2 -> copyText("Link", target)
                3 -> shareText("Link", target)
            }
        }.setNegativeButton(R.string.close, null).show()
        return true
    }

    private fun showPageTools() {
        val tab = currentTab() ?: return
        val dialog = BottomSheetDialog(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(14))
            background = GradientDrawable().apply { setColor(ContextCompat.getColor(this@MainActivity, R.color.surface_light)); val r = dp(20).toFloat(); cornerRadii = floatArrayOf(r,r,r,r,0f,0f,0f,0f) }
        }
        sheetHeader(content, "Page tools")
        sheetRow(content, "Find in page") { dialog.dismiss(); showFindInPageDialog() }
        sheetRow(content, "Copy page URL") { dialog.dismiss(); copyText("Page URL", tab.url) }
        sheetRow(content, "Copy page title") { dialog.dismiss(); copyText("Page title", tab.title) }
        if (prefs.getBoolean(KEY_TEXT_EXTRACTION, true)) {
            sheetRow(content, "Copy readable text") { dialog.dismiss(); copyPageText() }
            sheetRow(content, "Copy all page links") { dialog.dismiss(); copyAllLinks() }
            sheetRow(content, "Copy page metadata") { dialog.dismiss(); copyPageMetadata() }
        }
        sheetRow(content, "View page source") { dialog.dismiss(); viewPageSource() }
        sheetRow(content, "Save web archive") { dialog.dismiss(); saveWebArchive() }
        sheetRow(content, "Capture page screenshot") { dialog.dismiss(); capturePageScreenshot() }
        if (prefs.getBoolean(KEY_TTS, true)) sheetRow(content, "Listen to page") { dialog.dismiss(); speakPage() }
        if (prefs.getBoolean(KEY_TRANSLATE, true)) sheetRow(content, "Translate page") { dialog.dismiss(); translatePage() }
        if (prefs.getBoolean(KEY_READING_LIST, true)) sheetRow(content, "Save to reading list") { dialog.dismiss(); db.addReadingList(tab.title, tab.url); Toast.makeText(this, "Saved to reading list", Toast.LENGTH_SHORT).show() }
        sheetRow(content, "Print / Save PDF") { dialog.dismiss(); savePageAsPdf() }
        sheetRow(content, "Zoom") { dialog.dismiss(); showZoomDialog() }
        sheetRow(content, "Hard reload") { dialog.dismiss(); hardReload() }
        sheetRow(content, "Open in external browser") { dialog.dismiss(); openExternal() }
        if (prefs.getBoolean(KEY_READER_MODE, true)) sheetRow(content, "Reader mode") { dialog.dismiss(); tab.webView.evaluateJavascript(ReaderMode.SCRIPT, null) }
        sheetRow(content, "Site controls") { dialog.dismiss(); showSiteControls() }
        dialog.setContentView(ScrollView(this).apply { addView(content) })
        dialog.show()
    }

    private fun showSiteControls() {
        val tab = currentTab() ?: return
        val host = Uri.parse(tab.url).host ?: tab.url
        val dialog = BottomSheetDialog(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(8), 0, dp(14)); background = GradientDrawable().apply { setColor(ContextCompat.getColor(this@MainActivity, R.color.surface_light)); val r=dp(20).toFloat(); cornerRadii=floatArrayOf(r,r,r,r,0f,0f,0f,0f) } }
        sheetHeader(content, host)
        sheetRow(content, "JavaScript", if (tab.webView.settings.javaScriptEnabled) "On" else "Off") {
            tab.webView.settings.javaScriptEnabled = !tab.webView.settings.javaScriptEnabled; dialog.dismiss(); tab.webView.reload()
        }
        sheetRow(content, "Images", if (tab.webView.settings.loadsImagesAutomatically) "On" else "Off") {
            tab.webView.settings.loadsImagesAutomatically = !tab.webView.settings.loadsImagesAutomatically; dialog.dismiss(); tab.webView.reload()
        }
        sheetRow(content, "Desktop site", if (tab.isDesktopMode) "On" else "Off") { dialog.dismiss(); toggleDesktopMode() }
        sheetRow(content, "Open this domain in private tabs") { dialog.dismiss(); DomainPrivacyStore.addAutoIncognito(this, host); Toast.makeText(this, "Added $host", Toast.LENGTH_SHORT).show() }
        sheetRow(content, "Clear this site's cookies & storage") { dialog.dismiss(); clearCurrentSiteData() }
        sheetRow(content, "Clear page history") { dialog.dismiss(); tab.webView.clearHistory(); Toast.makeText(this, "Page history cleared", Toast.LENGTH_SHORT).show() }
        sheetRow(content, "Privacy score") { dialog.dismiss(); showPrivacyScoreDialog() }
        sheetRow(content, "View network activity") { dialog.dismiss(); startActivity(Intent(this, TransparencyLogActivity::class.java).putExtra(TransparencyLogActivity.EXTRA_ORIGIN_FILTER, tab.pageOrigin)) }
        dialog.setContentView(ScrollView(this).apply { addView(content) })
        dialog.show()
    }

    private fun clearCurrentSiteData() {
        val origin = currentTab()?.pageOrigin ?: return
        CookiePolicy.wipeOrigins(listOf(origin))
        Toast.makeText(this, "Site cookies and storage cleared", Toast.LENGTH_SHORT).show()
    }

    private fun clearAllBrowsingData() {
        tabs.forEach { tab ->
            tab.webView.clearCache(true)
            tab.webView.clearHistory()
        }
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        android.webkit.CookieManager.getInstance().flush()
        android.webkit.WebStorage.getInstance().deleteAllData()
        CookiePolicy.wipeOrigins(globalVisitedOrigins)
        db.clearHistory()
        db.clearNetworkLog()
        globalVisitedOrigins.clear()
        Toast.makeText(this, "Browser data cleared", Toast.LENGTH_SHORT).show()
    }

    private fun hardReload() {
        currentTab()?.webView?.let { it.clearCache(true); it.reload() }
    }

    private fun showZoomDialog() {
        val webView = currentTab()?.webView ?: return
        val values = intArrayOf(75, 90, 100, 110, 125, 150, 175)
        val labels = values.map { "$it%" }.toTypedArray()
        val current = values.indexOf(webView.settings.textZoom).coerceAtLeast(2)
        AlertDialog.Builder(this).setTitle("Text zoom").setSingleChoiceItems(labels, current) { dialog, which -> webView.settings.textZoom = values[which]; dialog.dismiss() }.setNegativeButton(R.string.close, null).show()
    }

    private fun copyAllLinks() {
        if (!prefs.getBoolean(KEY_TEXT_EXTRACTION, true)) { Toast.makeText(this, "Page extraction is disabled", Toast.LENGTH_SHORT).show(); return }
        currentTab()?.webView?.evaluateJavascript("Array.from(document.links).map(a=>a.href).filter(Boolean).join('\n')") { raw ->
            val text = runCatching { org.json.JSONTokener(raw).nextValue().toString() }.getOrElse { raw.trim('"') }
            copyText("Page links", text.take(30000))
        }
    }

    private fun copyPageMetadata() {
        currentTab()?.webView?.evaluateJavascript("JSON.stringify({title:document.title,url:location.href,description:(document.querySelector('meta[name=\"description\"]')||{}).content||'',canonical:(document.querySelector('link[rel=\"canonical\"]')||{}).href||'',language:document.documentElement.lang||navigator.language})") { raw ->
            val text=runCatching{org.json.JSONTokener(raw).nextValue().toString()}.getOrElse{raw.trim('"')}
            copyText("Page metadata", text)
        }
    }

    private fun viewPageSource() {
        val tab=currentTab() ?: return
        tab.webView.evaluateJavascript("document.documentElement ? document.documentElement.outerHTML : ''") { raw ->
            val source=runCatching{org.json.JSONTokener(raw).nextValue().toString()}.getOrElse{raw.trim('"')}
            openNewTab("data:text/plain;charset=utf-8," + Uri.encode(source.take(120000)))
        }
    }

    private fun saveWebArchive() {
        val tab=currentTab() ?: return
        val host=Uri.parse(tab.url).host?.replace(Regex("[^A-Za-z0-9.-]"), "_") ?: "page"
        val dir=java.io.File(filesDir,"archives").apply{mkdirs()}
        val file=java.io.File(dir,"${System.currentTimeMillis()}-$host.mht")
        tab.webView.saveWebArchive(file.absolutePath, false, null)
        Toast.makeText(this,"Archive saved in app storage",Toast.LENGTH_SHORT).show()
    }

    private fun capturePageScreenshot() {
        val tab=currentTab() ?: return
        val webView=tab.webView
        val bitmap=Bitmap.createBitmap(webView.width.coerceAtLeast(1), webView.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas=android.graphics.Canvas(bitmap)
        webView.draw(canvas)
        val file=java.io.File(cacheDir,"page-${System.currentTimeMillis()}.png")
        file.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        val uri=androidx.core.content.FileProvider.getUriForFile(this,"${BuildConfig.APPLICATION_ID}.files",file)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="image/png";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Share page screenshot"))
    }

    private fun translatePage() {
        val url = currentTab()?.url ?: return
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://translate.google.com/translate?sl=auto&tl=en&u=${Uri.encode(url)}")))
    }

    private fun speakPage() {
        val webView = currentTab()?.webView ?: return
        webView.evaluateJavascript("document.body ? document.body.innerText : ''") { raw ->
            val text = runCatching { org.json.JSONTokener(raw).nextValue().toString() }.getOrElse { raw.trim('\"') }.replace("\\n", "\n").take(12000)
            if (text.isBlank()) { Toast.makeText(this, "No readable text found", Toast.LENGTH_SHORT).show(); return@evaluateJavascript }
            if (textToSpeech == null) textToSpeech = TextToSpeech(this) { status -> if (status == TextToSpeech.SUCCESS) textToSpeech?.language = Locale.getDefault() }
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "privbrowse-page")
        }
    }

    private fun copyPageText() {
        currentTab()?.webView?.evaluateJavascript("document.body ? document.body.innerText : ''") { raw ->
            val text = runCatching { org.json.JSONTokener(raw).nextValue().toString() }.getOrElse { raw.trim('\"') }.replace("\\n", "\n").take(20000)
            copyText("Page text", text)
        }
    }

    private fun copyText(label: String, value: String) {
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, value))
        if (prefs.getBoolean(KEY_AUTO_CLEAR_CLIPBOARD, false)) {
            val expectedLabel = label
            cookieWipeHandler.postDelayed({
                if (cm.hasPrimaryClip() && cm.primaryClipDescription?.label?.toString() == expectedLabel) {
                    cm.setPrimaryClip(ClipData.newPlainText("", ""))
                }
            }, 30_000L)
        }
        Toast.makeText(this, "Copied $label", Toast.LENGTH_SHORT).show()
    }

    private fun shareText(subject: String, value: String) {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, subject); putExtra(Intent.EXTRA_TEXT, value) }, subject))
    }

    private fun openExternal() {
        val url = currentTab()?.url ?: return
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun savePageAsPdf() {
        val webView = currentTab()?.webView ?: return
        val host = Uri.parse(currentTab()?.url ?: "page")?.host ?: "page"
        val manager = getSystemService(PRINT_SERVICE) as PrintManager
        manager.print("PrivBrowse-$host", webView.createPrintDocumentAdapter("PrivBrowse-$host"), PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build())
    }

    // ---------- Result callbacks / back press ----------

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQ_WEB_MEDIA) return
        val request = pendingWebPermissionRequest
        pendingWebPermissionRequest = null
        if (request == null) return
        val allGranted = grantResults.isNotEmpty() && grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (allGranted) request.grant(request.resources) else request.deny()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        val url = when (requestCode) {
            REQ_BOOKMARKS -> data.getStringExtra(BookmarksActivity.EXTRA_RESULT_URL)
            REQ_HISTORY -> data.getStringExtra(HistoryActivity.EXTRA_RESULT_URL)
            REQ_READING_LIST -> data.getStringExtra("reading_url")
            else -> null
        }
        if (url != null) currentTab()?.webView?.loadUrl(url)
    }

    override fun onBackPressed() {
        val webView = currentTab()?.webView
        if (webView?.canGoBack() == true) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::adBlocker.isInitialized) adBlocker.refreshFromPreferences()
        applyGlobalSettingsToOpenTabs()
        if (prefs.getBoolean(KEY_DISABLE_SCREEN_CAPTURE, false)) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        if (prefs.getBoolean("biometric_lock", false) && !biometricPromptShowing) {
            biometricPromptShowing = true
            BiometricLock.authenticate(this, { biometricPromptShowing = false }, {
                biometricPromptShowing = false
                Toast.makeText(this, "Unlock cancelled", Toast.LENGTH_SHORT).show()
                moveTaskToBack(true)
            })
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level == android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN && prefs.getBoolean(KEY_CLEAR_ON_EXIT, false)) {
            CookiePolicy.wipeOrigins(globalVisitedOrigins)
            db.clearHistory()
            globalVisitedOrigins.clear()
        }
    }

    override fun onDestroy() {
        saveSessionSnapshot()
        textToSpeech?.shutdown()
        cookieWipeTasks.values.forEach(cookieWipeHandler::removeCallbacks)
        cookieWipeTasks.clear()
        cookieWipeHandler.removeCallbacksAndMessages(null)
        CookiePolicy.purgeConsentCookies(globalVisitedOrigins)
        if (prefs.getBoolean(KEY_CLEAR_CACHE_EXIT, false)) tabs.forEach { it.webView.clearCache(true) }
        if (prefs.getBoolean(KEY_CLEAR_PAGE_HISTORY_EXIT, false)) tabs.forEach { it.webView.clearHistory() }
        if (prefs.getBoolean(KEY_CLEAR_COOKIES_EXIT, false)) { android.webkit.CookieManager.getInstance().removeAllCookies(null); android.webkit.CookieManager.getInstance().flush() }
        if (prefs.getBoolean(KEY_CLEAR_STORAGE_EXIT, false)) android.webkit.WebStorage.getInstance().deleteAllData()
        if (prefs.getBoolean(KEY_CLEAR_NETWORK_LOG_EXIT, false)) db.clearNetworkLog()
        if (prefs.getBoolean(KEY_PURGE_CONSENT_EXIT, false)) CookiePolicy.purgeConsentCookies(globalVisitedOrigins)
        if (prefs.getBoolean(KEY_KEEP_SCREEN_ON, false)) window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (prefs.getBoolean(KEY_DISABLE_SCREEN_CAPTURE, false)) window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        tabs.forEach { it.webView.destroy() }
        super.onDestroy()
    }
}
