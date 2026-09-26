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
import android.widget.ListView
import android.widget.ArrayAdapter
import android.widget.PopupWindow
import android.view.GestureDetector
import android.view.MotionEvent
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
import com.privbrowse.app.privacy.SiteCustomizationStore
import com.privbrowse.app.panic.PanicManager
import com.privbrowse.app.video.VideoDetector
import com.privbrowse.app.ultimate.UltimateFeatureStore
import com.privbrowse.app.ultimate.UltimateTools
import com.privbrowse.app.ultimate.QrExport
import com.privbrowse.app.ultimate.QrTransferActivity
import com.privbrowse.app.ultimate.FloatingBubbleService
import com.privbrowse.app.ultimate.WatchActivity
import com.privbrowse.app.ultimate.DownloadQueueActivity
import com.privbrowse.app.ultimate.DownloadQueueService
import com.privbrowse.app.ultimate.PdfAnnotateActivity
import com.privbrowse.app.ultimate.PrivacyDashboardActivity

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
        const val KEY_AUTO_CLEAR_CLIPBOARD_SECONDS = "auto_clear_clipboard_seconds"
        const val KEY_AUTO_CLOSE_INACTIVE_HOURS = "auto_close_inactive_hours"
        const val KEY_SELF_DESTRUCT_TABS = "self_destruct_tabs"
        const val KEY_SELF_DESTRUCT_MINUTES = "self_destruct_minutes"
        const val KEY_EDGE_GESTURES = "edge_gestures"
        const val KEY_LIVE_SUGGESTIONS = "live_suggestions"
        const val KEY_CLIPBOARD_PROMPT = "clipboard_prompt"
        const val KEY_BANG_COMMANDS = "bang_commands"
        const val KEY_SEARCH_KEYWORDS = "search_keywords"
        const val KEY_GHOST_MODE = "ghost_mode"
        const val KEY_FAKE_GPS = "fake_gps"
        const val KEY_DOH_PROVIDER = "doh_provider"
        const val KEY_VAULT_LOCKED = "vault_locked"
        const val KEY_DYSLEXIA_FONT = "dyslexia_font"
        const val KEY_HIGH_CONTRAST = "high_contrast"
        const val KEY_LARGE_TOUCH = "large_touch"
        const val KEY_FULL_PAGE_SCREENSHOT = "full_page_screenshot"
        const val KEY_MEDIA_SPEED = "media_speed"
        const val KEY_AUDIO_ONLY = "audio_only"
        const val KEY_AUTO_AD_SKIP = "auto_ad_skip"
        const val KEY_AI_JUNK_STRIP = "ai_junk_strip"
        const val KEY_BACKGROUND_MEDIA = "background_media"
        const val KEY_WIFI_WARNING = "wifi_warning"
        const val KEY_NEW_TAB_DASHBOARD = "new_tab_dashboard"
        const val KEY_MAGNET_SUPPORT = "magnet_support"
        const val KEY_VAULT_UNLOCKED = "vault_unlocked"
        const val KEY_SESSION_JSON = "session_snapshot_json"
        const val KEY_RECENTLY_CLOSED_JSON = "recently_closed_json"


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
        const val ACTION_OPEN_FEATURE_LAB = "com.privbrowse.app.action.FEATURE_LAB"
        const val ACTION_OPEN_LEAK_TESTS = "com.privbrowse.app.action.LEAK_TESTS"
        const val ACTION_GUEST_CLEANUP = "com.privbrowse.app.action.GUEST_CLEANUP"
        const val ACTION_GALLERY = "com.privbrowse.app.action.GALLERY"
        const val ACTION_SUBTITLE_DOWNLOAD = "com.privbrowse.app.action.SUBTITLE_DOWNLOAD"
        const val ACTION_REVERSE_IMAGE = "com.privbrowse.app.action.REVERSE_IMAGE"
        const val ACTION_NOTES = "com.privbrowse.app.action.NOTES"
        const val ACTION_STICKY_NOTE = "com.privbrowse.app.action.STICKY_NOTE"
        const val ACTION_PRINT_FRIENDLY = "com.privbrowse.app.action.PRINT_FRIENDLY"
        const val ACTION_CALENDAR_EVENT = "com.privbrowse.app.action.CALENDAR_EVENT"
        const val ACTION_MEDIA_TOOLS = "com.privbrowse.app.action.MEDIA_TOOLS"
        const val ACTION_COMPARE_SEARCH = "com.privbrowse.app.action.COMPARE_SEARCH"
        const val ACTION_QR_TRANSFER = "com.privbrowse.app.action.QR_TRANSFER"
        const val ACTION_MULTI_WINDOW = "com.privbrowse.app.action.MULTI_WINDOW"
        const val ACTION_FLOATING_BUBBLE = "com.privbrowse.app.action.FLOATING_BUBBLE"
        const val ACTION_STOP_FLOATING_BUBBLE = "com.privbrowse.app.action.STOP_FLOATING_BUBBLE"
        const val ACTION_WATCHES = "com.privbrowse.app.action.WATCHES"
        const val ACTION_DOWNLOAD_QUEUE = "com.privbrowse.app.action.DOWNLOAD_QUEUE"
        const val ACTION_PDF_ANNOTATE = "com.privbrowse.app.action.PDF_ANNOTATE"
        const val ACTION_PRIVACY_DASHBOARD = "com.privbrowse.app.action.PRIVACY_DASHBOARD"
        const val ACTION_ENTER_PIP = "com.privbrowse.app.action.ENTER_PIP"
        const val ACTION_RANDOM_CLOSE = "com.privbrowse.app.action.RANDOM_CLOSE"
        const val ACTION_VAULT_TAB = "com.privbrowse.app.action.VAULT_TAB"
        const val ACTION_BACKGROUND_AUDIO = "com.privbrowse.app.action.BACKGROUND_AUDIO"
        const val ACTION_HOME_DASHBOARD = "com.privbrowse.app.action.HOME_DASHBOARD"
        const val ACTION_PROFILE_ALIAS = "com.privbrowse.app.action.PROFILE_ALIAS"
        const val ACTION_UNLOCK_VAULT = "com.privbrowse.app.action.UNLOCK_VAULT"
        const val ACTION_IMPORT_TABS = "com.privbrowse.app.action.IMPORT_TABS"
        const val ACTION_SHARE_TABS_LOCAL = "com.privbrowse.app.action.SHARE_TABS_LOCAL"
        const val ACTION_PROFILES = "com.privbrowse.app.action.PROFILES"
        const val ACTION_BUBBLE_OFF = "com.privbrowse.app.action.BUBBLE_OFF"
        const val ACTION_VOICE_COMMAND = "com.privbrowse.app.action.VOICE_COMMAND"
        const val ACTION_QR_CURRENT_URL = "com.privbrowse.app.action.QR_CURRENT_URL"
        const val ACTION_EMAIL_ALIAS = "com.privbrowse.app.action.EMAIL_ALIAS"
        const val ACTION_CALC_CONVERTER = "com.privbrowse.app.action.CALC_CONVERTER"
        const val ACTION_JOIN_VIDEO_CALL = "com.privbrowse.app.action.JOIN_VIDEO_CALL"
        const val ACTION_AI_IMAGES = "com.privbrowse.app.action.AI_IMAGES"
        const val ACTION_THEME_EDITOR = "com.privbrowse.app.action.THEME_EDITOR"
        const val ACTION_GESTURE_EDITOR = "com.privbrowse.app.action.GESTURE_EDITOR"
        const val ACTION_CRASH_LOG = "com.privbrowse.app.action.CRASH_LOG"
        const val ACTION_WEB_CLIPPER = "com.privbrowse.app.action.WEB_CLIPPER"
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
    private lateinit var tabStripVertical: RecyclerView
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnHome: ImageButton
    private lateinit var btnTools: ImageButton
    private lateinit var btnTabs: TextView
    private lateinit var btnMenu: ImageButton
    private lateinit var bottomNav: View
    private var mediaPermissionsRevokedForSession = false
    private var clipboardManager: ClipboardManager? = null
    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener { if (!urlBar.hasFocus() && prefs.getBoolean(KEY_CLIPBOARD_PROMPT, true)) offerClipboardLink() }

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
    private val selfDestructTasks = mutableMapOf<Long, Runnable>()
    private val autoCloseTasks = mutableMapOf<Long, Runnable>()
    private var suggestionsPopup: PopupWindow? = null
    private var biometricPromptShowing = false
    private var vaultUnlocked = false
    private var restoredSession = false
    private var textToSpeech: TextToSpeech? = null
    private var pendingWebPermissionRequest: android.webkit.PermissionRequest? = null
    private var aiBubble: TextView? = null
    private val REQ_WEB_MEDIA = 610

    private data class ClosedTabSnapshot(val title: String, val url: String, val incognito: Boolean, val desktop: Boolean)

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= 28) {
            val profile = getSharedPreferences("privbrowse_profile", MODE_PRIVATE).getString("active_profile", "personal") ?: "personal"
            runCatching { android.webkit.WebView.setDataDirectorySuffix(profile) }
        }
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
        com.privbrowse.app.ultimate.NetworkUsageRecorder.snapshot(this)
        if (com.privbrowse.app.ultimate.DownloadQueueStore.list(this).any { it.status == "queued" || it.status == "downloading" }) {
            androidx.core.content.ContextCompat.startForegroundService(this,Intent(this,com.privbrowse.app.ultimate.DownloadQueueService::class.java))
        }

        bindViews()
        applyImportedTheme()
        setupTabStrip()
        setupToolbar()
        loadRecentlyClosed()
        setupClipboardMonitor()

        if (prefs.getBoolean(KEY_CLIPBOARD_PROMPT, true)) {
            urlBar.postDelayed({ offerClipboardLink() }, 650)
        }

        // Phase 3: Weekly Privacy Report — re-registering an existing alarm
        // is a no-op, so this is safe to call on every launch.
        WeeklyReportScheduler.scheduleIfNeeded(this)
        if (!UltimateFeatureStore.prefs(this).getBoolean(UltimateFeatureStore.KEY_GHOST, false)) com.privbrowse.app.ultimate.BackupReceiver.schedule(this)
        com.privbrowse.app.ultimate.CleanupReceiver.schedule(this)
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

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_K && event.isCtrlPressed) {
            showCommandPalette()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun showCommandPalette() {
        val labels = arrayOf(
            "New tab", "Tab manager", "Private tab", "Page tools", "Bookmarks",
            "History", "Downloads", "Notes", "Compare search engines", "Privacy score",
            "Feature Lab", "QR current URL", "Screenshot", "Back", "Forward", "Refresh"
        )
        AlertDialog.Builder(this)
            .setTitle("Command palette (Ctrl+K)")
            .setItems(labels) { _, which ->
                when (which) {
                    0 -> openNewTab(getHomeUrl())
                    1 -> showTabManager()
                    2 -> openNewTab(getHomeUrl(), incognito = true)
                    3 -> showPageTools()
                    4 -> startActivityForResult(Intent(this, BookmarksActivity::class.java), REQ_BOOKMARKS)
                    5 -> startActivityForResult(Intent(this, HistoryActivity::class.java), REQ_HISTORY)
                    6 -> startActivity(Intent(this, VideoDownloadsActivity::class.java))
                    7 -> openNotes()
                    8 -> compareSearchEngines()
                    9 -> showPrivacyScoreDialog()
                    10 -> startActivity(Intent(this, FeatureLabActivity::class.java))
                    11 -> showCurrentUrlQr()
                    12 -> capturePageScreenshot()
                    13 -> currentTab()?.webView?.goBack()
                    14 -> currentTab()?.webView?.goForward()
                    15 -> currentTab()?.webView?.reload()
                }
            }
            .setNegativeButton(R.string.close, null)
            .show()
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
            ACTION_OPEN_FEATURE_LAB -> { startActivity(Intent(this, FeatureLabActivity::class.java)); true }
            ACTION_OPEN_LEAK_TESTS -> { openLeakTests(); true }
            ACTION_GUEST_CLEANUP -> { guestCleanup(); true }
            ACTION_GALLERY -> { openPageGallery(); true }
            ACTION_SUBTITLE_DOWNLOAD -> { downloadSubtitleCandidate(); true }
            ACTION_REVERSE_IMAGE -> { reverseImageSearch(); true }
            ACTION_NOTES -> { openNotes(); true }
            ACTION_STICKY_NOTE -> { showStickyNoteOverlay(); true }
            ACTION_PRINT_FRIENDLY -> { openPrintFriendly(); true }
            ACTION_CALENDAR_EVENT -> { addCalendarEvent(); true }
            ACTION_MEDIA_TOOLS -> { showMediaTools(); true }
            ACTION_COMPARE_SEARCH -> { compareSearchEngines(); true }
            ACTION_QR_TRANSFER -> { showQrTransfer(); true }
            ACTION_MULTI_WINDOW -> { openSecondWindow(); true }
            ACTION_FLOATING_BUBBLE -> { startFloatingBubble(); true }
            ACTION_STOP_FLOATING_BUBBLE -> { stopService(Intent(this, FloatingBubbleService::class.java)); true }
            ACTION_WATCHES -> { startActivity(Intent(this, WatchActivity::class.java)); true }
            ACTION_DOWNLOAD_QUEUE -> { startActivity(Intent(this, DownloadQueueActivity::class.java)); true }
            ACTION_PDF_ANNOTATE -> { startActivity(Intent(this, PdfAnnotateActivity::class.java)); true }
            ACTION_PRIVACY_DASHBOARD -> { startActivity(Intent(this, PrivacyDashboardActivity::class.java)); true }
            ACTION_ENTER_PIP -> { enterBrowserPictureInPicture(); true }
            ACTION_RANDOM_CLOSE -> { closeTabsRandomly(); true }
            ACTION_VAULT_TAB -> { authenticateVaultThenOpen(); true }
            ACTION_BACKGROUND_AUDIO -> { startBackgroundAudio(); true }
            ACTION_HOME_DASHBOARD -> { startActivity(Intent(this, com.privbrowse.app.ultimate.DashboardActivity::class.java)); true }
            ACTION_UNLOCK_VAULT -> { authenticateVault(); true }
            ACTION_IMPORT_TABS -> { importTabs(intent?.getStringExtra("payload").orEmpty()); true }
            ACTION_SHARE_TABS_LOCAL -> { shareTabsLocal(); true }
            ACTION_PROFILES -> { startActivity(Intent(this, com.privbrowse.app.ultimate.ProfileActivity::class.java)); true }
            ACTION_VOICE_COMMAND -> { startVoiceCommand(); true }
            ACTION_QR_CURRENT_URL -> { showCurrentUrlQr(); true }
            ACTION_EMAIL_ALIAS -> { showEmailAlias(); true }
            ACTION_CALC_CONVERTER -> { showCalculatorConverter(); true }
            ACTION_JOIN_VIDEO_CALL -> { joinDetectedMeeting(); true }
            ACTION_AI_IMAGES -> { aiImageAltText(); true }
            ACTION_THEME_EDITOR -> { startActivity(Intent(this, com.privbrowse.app.ultimate.ThemeActivity::class.java)); true }
            ACTION_GESTURE_EDITOR -> { startActivity(Intent(this, com.privbrowse.app.ultimate.GestureEditorActivity::class.java)); true }
            ACTION_CRASH_LOG -> { exportCrashLog(); true }
            ACTION_WEB_CLIPPER -> { openWebClipper(); true }
            else -> false
        }
        if (handled) setIntent(Intent(intent).setAction(null))
        return handled
    }

    private fun setupClipboardMonitor() {
        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager?.addPrimaryClipChangedListener(clipboardListener)
    }

    private fun offerClipboardLink() {
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        if (!cm.hasPrimaryClip()) return
        val clip = cm.primaryClip ?: return
        if (clip.itemCount == 0) return
        val text = clip.getItemAt(0).text?.toString()?.trim().orEmpty()
        if (text.isBlank() || !(text.startsWith("http://") || text.startsWith("https://"))) return
        if (text == currentTab()?.url) return
        AlertDialog.Builder(this)
            .setTitle("Open copied link?")
            .setMessage(text)
            .setNegativeButton("Ignore", null)
            .setPositiveButton("Open") { _, _ -> openNewTab(text) }
            .show()
    }

    private fun loadRecentlyClosed() {
        val array = runCatching { org.json.JSONArray(prefs.getString(KEY_RECENTLY_CLOSED_JSON, "[]")) }.getOrElse { org.json.JSONArray() }
        recentlyClosed.clear()
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val url = o.optString("url")
            if (url.startsWith("http://") || url.startsWith("https://")) recentlyClosed.addLast(ClosedTabSnapshot(o.optString("title"), url, o.optBoolean("incognito", false), o.optBoolean("desktop", false)))
        }
    }

    private fun persistRecentlyClosed() {
        val array = org.json.JSONArray()
        recentlyClosed.take(MAX_REOPENED).forEach { item -> array.put(org.json.JSONObject().apply { put("title", item.title); put("url", item.url); put("incognito", item.incognito); put("desktop", item.desktop) }) }
        prefs.edit().putString(KEY_RECENTLY_CLOSED_JSON, array.toString()).apply()
    }

    private fun updateAiBubble() {
        aiBubble?.let { webViewContainer.removeView(it) }
        aiBubble = null
        val tab = currentTab() ?: return
        if (!UltimateFeatureStore.prefs(this).getBoolean(UltimateFeatureStore.KEY_AI_BUBBLE, false)) return
        aiBubble = TextView(this).apply {
            text = "AI"; textSize = 11f; gravity = Gravity.CENTER; setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.white))
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(ContextCompat.getColor(this@MainActivity, R.color.accent_dark)) }
            elevation = dp(10).toFloat()
            setOnClickListener {
                tab.webView.evaluateJavascript("document.body ? document.body.innerText : ''") { raw ->
                    val text = runCatching { org.json.JSONTokener(raw).nextValue().toString() }.getOrElse { raw.trim('\"') }.take(30000)
                    startActivity(Intent(this@MainActivity, AiCopilotActivity::class.java).apply {
                        putExtra(AiCopilotActivity.EXTRA_PAGE_TITLE, tab.title.ifBlank { "Current page" })
                        putExtra(AiCopilotActivity.EXTRA_PAGE_URL, tab.url)
                        putExtra(AiCopilotActivity.EXTRA_PAGE_TEXT, text)
                    })
                }
            }
        }
        webViewContainer.addView(aiBubble, FrameLayout.LayoutParams(dp(52), dp(52)).apply { gravity = Gravity.END or Gravity.BOTTOM; setMargins(0, 0, dp(14), dp(16)) })
    }

    private fun openWebClipper() {
        val tab = currentTab() ?: return
        val choices = arrayOf("Selected/readable text", "Current viewport image")
        AlertDialog.Builder(this).setTitle("Web clipper").setItems(choices) { _, which ->
            if (which == 0) {
                tab.webView.evaluateJavascript("window.getSelection ? window.getSelection().toString() : (document.body ? document.body.innerText : '')") { raw ->
                    val text = runCatching { org.json.JSONTokener(raw).nextValue().toString() }.getOrElse { raw.trim('\"') }.take(20000)
                    if (text.isBlank()) Toast.makeText(this, "Nothing selected", Toast.LENGTH_SHORT).show() else {
                        val file = java.io.File(cacheDir, "clip-${System.currentTimeMillis()}.txt"); file.writeText("${tab.title}\n${tab.url}\n\n$text")
                        val uri = androidx.core.content.FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.files", file)
                        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="text/plain"; putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_TEXT, text); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share web clip"))
                    }
                }
            } else {
                capturePageScreenshot()
            }
        }.setNegativeButton(R.string.close, null).show()
    }

    private fun restoreSessionOrOpenInitial() {
        if (!prefs.getBoolean(KEY_RESTORE_SESSION, true)) {
            openNewTab(resolveInitialUrl(intent)); return
        }
        val rawJson = prefs.getString(KEY_SESSION_JSON, null)
        if (!rawJson.isNullOrBlank()) {
            val restored = runCatching {
                val root = org.json.JSONObject(rawJson)
                val items = root.optJSONArray("tabs") ?: org.json.JSONArray()
                val current = root.optInt("current", 0).coerceIn(0, (items.length() - 1).coerceAtLeast(0))
                var firstIndex = 0
                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i) ?: continue
                    val url = item.optString("url")
                    if (!url.startsWith("http://") && !url.startsWith("https://")) continue
                    val before = tabs.size
                    openNewTab(url, item.optBoolean("incognito", false), item.optBoolean("desktop", false), item.optBoolean("vault", false))
                    tabs.getOrNull(before)?.let { tab ->
                        tab.title = item.optString("title", tab.title)
                        tab.isPinned = item.optBoolean("pinned", false)
                        tab.groupName = item.optString("group", "")
                        tab.groupColor = item.optInt("groupColor", 0)
                        tab.lastScrollY = item.optInt("scrollY", 0)
                    }
                    if (i == current) firstIndex = before
                }
                if (tabs.isNotEmpty()) switchToTab(firstIndex.coerceIn(0, tabs.lastIndex))
                tabs.isNotEmpty()
            }.getOrDefault(false)
            if (restored) { restoredSession = true; return }
        }
        val raw = prefs.getStringSet(KEY_SESSION_URLS, emptySet()).orEmpty().toList()
        val first = prefs.getString("session_first_url", null)
        val urls = buildList {
            first?.takeIf { it.startsWith("http://") || it.startsWith("https://") }?.let(::add)
            raw.filter { it != first }.take(MAX_RESTORED_TABS - 1).forEach(::add)
        }
        if (urls.isNotEmpty()) {
            restoredSession = true
            urls.forEach { openNewTab(it, incognito = false) }
            return
        }
        openNewTab(resolveInitialUrl(intent))
    }

    private fun saveSessionSnapshot() {
        val ultimate = UltimateFeatureStore.prefs(this)
        if (prefs.getBoolean(KEY_GHOST_MODE, false) || ultimate.getBoolean(UltimateFeatureStore.KEY_GHOST, false)) return
        if (!prefs.getBoolean(KEY_RESTORE_SESSION, true) || tabs.isEmpty()) return
        val eligibleTabs = tabs.filter { !it.isIncognito && !it.isVault }.take(MAX_RESTORED_TABS)
        val savedCurrent = eligibleTabs.indexOf(currentTab()).coerceIn(0, (eligibleTabs.size - 1).coerceAtLeast(0))
        val root = org.json.JSONObject().put("current", savedCurrent)
        val items = org.json.JSONArray()
        eligibleTabs.forEach { tab ->
            val url = tab.url
            if (!url.startsWith("http://") && !url.startsWith("https://")) return@forEach
            items.put(org.json.JSONObject().apply {
                put("url", url)
                put("title", tab.title)
                put("desktop", tab.isDesktopMode)
                put("incognito", false)
                put("vault", tab.isVault)
                put("pinned", tab.isPinned)
                put("group", tab.groupName)
                put("groupColor", tab.groupColor)
                put("scrollY", tab.lastScrollY)
            })
        }
        root.put("tabs", items)
        if (items.length() > 0) {
            prefs.edit()
                .putString(KEY_SESSION_JSON, root.toString())
                .putStringSet(KEY_SESSION_URLS, tabs.filter { !it.isIncognito && !it.isVault }.map { it.url }.filter { it.startsWith("http://") || it.startsWith("https://") }.toSet())
                .putString("session_first_url", currentTab()?.url?.takeIf { it.startsWith("http://") || it.startsWith("https://") })
                .apply()
        }
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
        tabStripVertical = findViewById(R.id.tabStripVertical)
        btnBack = findViewById(R.id.btnBack)
        btnForward = findViewById(R.id.btnForward)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnHome = findViewById(R.id.btnHome)
        btnTools = findViewById(R.id.btnTools)
        btnTabs = findViewById(R.id.btnTabs)
        btnMenu = findViewById(R.id.btnMenu)
        bottomNav = findViewById(R.id.bottomNav)
    }

    private fun setupTabStrip() {
        tabAdapter = TabAdapter(
            tabs,
            onTabSelected = { position -> switchToTab(position) },
            onTabClosed = { position -> closeTab(position) },
            isVaultUnlocked = { vaultUnlocked }
        )
        tabStrip.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        tabStripVertical.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        tabStrip.adapter = tabAdapter
        tabStripVertical.adapter = tabAdapter
    }

    /**
     * The tab strip only earns its screen space once there's an actual choice
     * to make between tabs. With a single tab open it stays collapsed so the
     * page gets that row back; it reappears the moment a second tab exists.
     */
    private fun updateTabStripVisibility() {
        val compact = prefs.getBoolean(KEY_COMPACT_TABS, true)
        val vertical = UltimateFeatureStore.prefs(this).getBoolean(UltimateFeatureStore.KEY_VERTICAL_TABS,false)
        val show = !(compact && tabs.size <= 1)
        tabStrip.visibility = if (show && !vertical) View.VISIBLE else View.GONE
        tabStripVertical.visibility = if (show && vertical) View.VISIBLE else View.GONE
        tabStrip.layoutParams = tabStrip.layoutParams.apply { height = dp(36) }
    }

    private fun setupToolbar() {
        urlBar.setOnEditorActionListener { _, actionId, event ->
            val isEnter = event != null && event.keyCode == KeyEvent.KEYCODE_ENTER
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO || isEnter) {
                loadInput(urlBar.text.toString())
                true
            } else false
        }
        urlBar.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (urlBar.hasFocus() && prefs.getBoolean(KEY_LIVE_SUGGESTIONS, true)) showAddressSuggestions(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
        var addressDownX = 0f
        var addressDownY = 0f
        urlBar.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { addressDownX = e.x; addressDownY = e.y }
                MotionEvent.ACTION_UP -> {
                    val dx = e.x - addressDownX; val dy = e.y - addressDownY
                    if (kotlin.math.abs(dx) > dp(90) && kotlin.math.abs(dy) < dp(45)) {
                        if (dx < 0) switchRelativeTab(1) else switchRelativeTab(-1)
                    }
                }
            }
            false
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
        sheetRow(content, "Profiles", "Separate browser data stores") { dialog.dismiss(); startActivity(Intent(this, com.privbrowse.app.ultimate.ProfileActivity::class.java)) }
        sheetRow(content, "Settings & feature center") { dialog.dismiss(); startActivity(Intent(this, BrowserSettingsActivity::class.java)) }
        sheetRow(content, "Feature Center", "100+ controls & shortcuts") { dialog.dismiss(); startActivity(Intent(this, FeatureCenterActivity::class.java)) }
        sheetRow(content, "Ultimate Feature Lab", "Wishlist integrations") { dialog.dismiss(); startActivity(Intent(this, FeatureLabActivity::class.java)) }

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
        tab.webView.evaluateJavascript("(function(){var m=document.querySelector('meta[name=description]');return m?m.content:''})()") { raw ->
            val desc = runCatching { org.json.JSONTokener(raw).nextValue().toString() }.getOrElse { raw.trim('\"') }
            com.privbrowse.app.ultimate.BookmarkMetaStore.put(this, tab.url, tab.title, desc)
        }
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
        val webViewVersion = android.webkit.WebView.getCurrentWebViewPackage()?.versionName ?: "unknown"
        val message = buildString {
            appendLine("Page: ${tab?.title.orEmpty().ifBlank { "New tab" }}")
            appendLine("Host: $host")
            appendLine("URL: ${tab?.url.orEmpty().ifBlank { "—" }}")
            appendLine("HTTPS: ${tab?.url?.startsWith("https://") == true}")
            appendLine("Blocked trackers: ${tab?.trackersBlockedThisPage ?: 0}")
            appendLine("Fingerprint events: ${tab?.fingerprintBlockedThisPage ?: 0}")
            appendLine("Resources this page: ${tab?.resourcesThisPage ?: 0}")
            appendLine("Tab active age: ${if ((tab?.pageStartedAt ?: 0L) > 0) ((System.currentTimeMillis() - tab!!.pageStartedAt) / 1000) else 0}s")
            appendLine("Ad-block level: ${adBlocker.level.name}")
            appendLine("JavaScript: ${prefs.getBoolean(KEY_JAVASCRIPT, true)}")
            appendLine("Images: ${prefs.getBoolean(KEY_IMAGES, true)}")
            appendLine("3P cookies blocked: ${prefs.getBoolean(KEY_THIRD_PARTY_COOKIES, true)}")
            appendLine("Tracking parameters stripped: ${prefs.getBoolean(KEY_STRIP_TRACKING, true)}")
            appendLine("WebView version: $webViewVersion")
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
    private fun openNewTab(url: String, incognito: Boolean = false, desktop: Boolean = false, vault: Boolean = false) {
        val actualIncognito = incognito || prefs.getBoolean(KEY_NEW_TABS_PRIVATE, false)
        val webView = WebView(this)
        val edgeGesture = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val edgeEnabled = prefs.getBoolean(KEY_EDGE_GESTURES, true)
                val dx = e2.x - e1.x
                val nearLeft = e1.x <= dp(42)
                val nearRight = e1.x >= webView.width - dp(42)
                if (kotlin.math.abs(dx) > dp(110) && kotlin.math.abs(velocityX) > 250f && kotlin.math.abs(dx) > kotlin.math.abs(e2.y - e1.y) * 1.35f) {
                    if (edgeEnabled && dx > 0 && nearLeft) {
                        val w = currentTab()?.webView
                        if (w?.canGoBack() == true) w.goBack() else switchRelativeTab(-1)
                        return true
                    }
                    if (edgeEnabled && dx < 0 && nearRight) {
                        val w = currentTab()?.webView
                        if (w?.canGoForward() == true) w.goForward() else switchRelativeTab(1)
                        return true
                    }
                }
                val dy = e2.y - e1.y
                val custom = UltimateFeatureStore.jsonObject(this@MainActivity, UltimateFeatureStore.KEY_GESTURE_ACTIONS)
                val direction = if (kotlin.math.abs(dy) > kotlin.math.abs(dx)) { if (dy < 0) "up" else "down" } else { if (dx < 0) "left" else "right" }
                val customAction = custom.optString(direction, "none")
                if (kotlin.math.max(kotlin.math.abs(dx), kotlin.math.abs(dy)) > dp(150) && customAction != "none") {
                    tabs.firstOrNull { it.webView === webView }?.let { performGestureAction(it, customAction); return true }
                }
                return false
            }
        })
        webView.setOnTouchListener { _, event -> edgeGesture.onTouchEvent(event); false }
        if (mobileUserAgent.isEmpty()) mobileUserAgent = webView.settings.userAgentString

        webView.settings.javaScriptEnabled = prefs.getBoolean(KEY_JAVASCRIPT, true)
        webView.settings.domStorageEnabled = prefs.getBoolean(KEY_DOM_STORAGE, true)
        webView.settings.loadsImagesAutomatically = prefs.getBoolean(KEY_IMAGES, true)
        webView.settings.mediaPlaybackRequiresUserGesture = prefs.getBoolean(KEY_MEDIA_GESTURE, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && UltimateFeatureStore.prefs(this).getBoolean(UltimateFeatureStore.KEY_BACKGROUND_MEDIA, false)) webView.settings.mediaPlaybackRequiresUserGesture = false
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
            if (prefs.getBoolean(KEY_FAKE_GPS, false)) {
                WebViewCompat.addDocumentStartJavaScript(
                    webView,
                    UltimateTools.randomizedGeolocationDocumentScript(nextTabId),
                    setOf("*")
                )
            }
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            val speed = prefs.getFloat(KEY_MEDIA_SPEED, 1.0f)
            val audioOnly = prefs.getBoolean(KEY_AUDIO_ONLY, false)
            val adSkip = prefs.getBoolean(KEY_AUTO_AD_SKIP, false)
            WebViewCompat.addDocumentStartJavaScript(webView, UltimateTools.mediaControls(speed.toDouble(), audioOnly, adSkip), setOf("*"))
            if (prefs.getBoolean(KEY_AI_JUNK_STRIP, false)) WebViewCompat.addDocumentStartJavaScript(webView, UltimateTools.junkStripScript(), setOf("*"))
        }

        // Phase 2: third-party cookies are blocked on every tab, always.
        CookiePolicy.configureWebView(webView, prefs.getBoolean(KEY_THIRD_PARTY_COOKIES, true), prefs.getBoolean(KEY_ACCEPT_COOKIES, true))
        if (actualIncognito && prefs.getBoolean(KEY_INCOGNITO_NO_CACHE, true)) {
            webView.settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
        } else when {
            UltimateFeatureStore.prefs(this).getBoolean(UltimateFeatureStore.KEY_OFFLINE_MODE, false) -> webView.settings.cacheMode = android.webkit.WebSettings.LOAD_CACHE_ONLY
            prefs.getBoolean(KEY_NO_CACHE, false) -> webView.settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
            prefs.getBoolean(KEY_FAST_CACHE, true) || prefs.getBoolean(KEY_DATA_SAVER, false) -> webView.settings.cacheMode = android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK
            else -> webView.settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        }

        val tab = BrowserTab(nextTabId++, webView)
        tab.isIncognito = actualIncognito
        tab.isVault = vault
        tab.lastActiveAt = System.currentTimeMillis()
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
        webView.loadUrl(if (prefs.getBoolean(KEY_NEW_TAB_DASHBOARD,true) && url == HOME_URL && !actualIncognito) buildNewTabDashboard() else url)
    }

    private fun switchToTab(position: Int) {
        if (position !in tabs.indices) return
        val old = currentTab()
        if (old != null && tabs.indexOf(old) != position) { scheduleAutoClose(old); old.isHibernated=true; old.webView.onPause() }
        tabs.forEachIndexed { index, tab ->
            tab.webView.visibility = if (index == position) View.VISIBLE else View.GONE
            if (index == position) { tab.isHibernated=false; tab.webView.onResume() }
        }
        currentTabIndex = position
        val active = currentTab() ?: return
        active.lastActiveAt = System.currentTimeMillis()
        autoCloseTasks.remove(active.id)?.let(cookieWipeHandler::removeCallbacks)
        ensureSelfDestructScheduled(active)
        updateAiBubble()
        tabAdapter.selectedPosition = position
        updateTabCount()
        urlBar.setText(active.url)
        active.let { updatePrivacyBadge(it) }
        updateNavState()
    }

    private fun switchRelativeTab(delta: Int) {
        if (tabs.size < 2) return
        val next = (currentTabIndex + delta + tabs.size) % tabs.size
        switchToTab(next)
    }

    private fun scheduleAutoClose(tab: BrowserTab) {
        val hours = prefs.getInt(KEY_AUTO_CLOSE_INACTIVE_HOURS, 0)
        if (hours <= 0 || tab.isIncognito || tab.isPinned) return
        autoCloseTasks.remove(tab.id)?.let(cookieWipeHandler::removeCallbacks)
        val task = Runnable {
            if (tabs.contains(tab) && tab != currentTab() && System.currentTimeMillis() - tab.lastActiveAt >= hours * 3600_000L) closeTab(tabs.indexOf(tab))
        }
        autoCloseTasks[tab.id] = task
        cookieWipeHandler.postDelayed(task, hours * 3600_000L)
    }

    private fun ensureSelfDestructScheduled(tab: BrowserTab) {
        if (!prefs.getBoolean(KEY_SELF_DESTRUCT_TABS, false) || !tab.url.startsWith("http")) return
        val mins = prefs.getInt(KEY_SELF_DESTRUCT_MINUTES, 0)
        if (mins <= 0 || selfDestructTasks.containsKey(tab.id)) return
        val task = Runnable { if (tabs.contains(tab)) closeTab(tabs.indexOf(tab), force = true) }
        selfDestructTasks[tab.id] = task
        cookieWipeHandler.postDelayed(task, mins * 60_000L)
    }

    private fun closeTab(position: Int, force: Boolean = false) {
        if (position !in tabs.indices) return
        if (tabs[position].isPinned && !force) { Toast.makeText(this, "Pinned tab — unpin before closing", Toast.LENGTH_SHORT).show(); return }
        val oldCurrent = currentTabIndex
        val closingCurrent = position == oldCurrent
        val tab = tabs.removeAt(position)
        if (!force && prefs.getBoolean(KEY_RECENT_TABS, true) && !tab.isIncognito && !tab.isVault && (tab.url.startsWith("http://") || tab.url.startsWith("https://"))) {
            recentlyClosed.addFirst(ClosedTabSnapshot(tab.title, tab.url, false, tab.isDesktopMode))
            while (recentlyClosed.size > MAX_REOPENED) recentlyClosed.removeLast()
            persistRecentlyClosed()
        }
        if (force && tab.visitedOrigins.isNotEmpty()) {
            CookiePolicy.wipeOrigins(tab.visitedOrigins)
            tab.webView.clearCache(true)
            tab.webView.clearHistory()
        }
        cookieWipeTasks.remove(tab.id)?.let(cookieWipeHandler::removeCallbacks)
        loadTimeoutTasks.remove(tab.id)?.let(cookieWipeHandler::removeCallbacks)
        selfDestructTasks.remove(tab.id)?.let(cookieWipeHandler::removeCallbacks)
        autoCloseTasks.remove(tab.id)?.let(cookieWipeHandler::removeCallbacks)
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
                persistRecentlyClosed()
                openNewTab(snapshot.url, snapshot.incognito, snapshot.desktop)
            }
            .setNeutralButton("Clear") { _, _ -> recentlyClosed.clear(); persistRecentlyClosed() }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun tabThumb(tab: BrowserTab, width: Int = 220, height: Int = 120): Bitmap? = runCatching {
        if (tab.webView.width <= 0 || tab.webView.height <= 0) return null
        val bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bm)
        canvas.scale(width.toFloat() / tab.webView.width, height.toFloat() / tab.webView.height)
        tab.webView.draw(canvas)
        bm
    }.getOrNull()

    private fun showTabManager() {
        if (tabs.isEmpty()) return
        val wrapper = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(8), dp(14), dp(10)) }
        val search = EditText(this).apply { hint = "Search tabs"; setSingleLine(true) }
        wrapper.addView(search, LinearLayout.LayoutParams(-1, dp(50)))
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        fun smallAction(label: String, click: () -> Unit) = TextView(this).apply {
            text = label; textSize = 12f; setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_dark)); setPadding(dp(5), dp(8), dp(12), dp(8)); setOnClickListener { click() }
        }
        actions.addView(smallAction("Duplicate") { duplicateCurrentTab() })
        actions.addView(smallAction("Close others") { closeOtherTabs() })
        actions.addView(smallAction("Recently closed") { showRecentlyClosed() })
        actions.addView(smallAction("Grid") { })
        actions.addView(smallAction("List") { })
        wrapper.addView(actions)
        val viewport = FrameLayout(this)
        wrapper.addView(viewport, LinearLayout.LayoutParams(-1, dp(420)))

        fun matches(tab: BrowserTab, filter: String): Boolean {
            if (tab.isVault && !vaultUnlocked) return false
            val label = (if (tab.isVault) "vault " else "") + (if (tab.isPinned) "pinned " else "") + tab.groupName + " " + tab.title
            return filter.isBlank() || label.contains(filter, true) || tab.url.contains(filter, true)
        }

        fun renderGrid(filter: String) {
            viewport.removeAllViews()
            val grid = android.widget.GridLayout(this).apply { columnCount = 2; useDefaultMargins = false; setPadding(dp(2), dp(2), dp(2), dp(10)) }
            tabs.forEachIndexed { index, tab ->
                if (!matches(tab, filter)) return@forEachIndexed
                val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(6), dp(6), dp(6), dp(6)); isClickable = true; background = GradientDrawable().apply { cornerRadius = dp(10).toFloat(); setColor(ContextCompat.getColor(this@MainActivity, R.color.surface_light)); setStroke(dp(1), if (tab.groupColor != 0) tab.groupColor else ContextCompat.getColor(this@MainActivity, R.color.divider)) }; setOnClickListener { switchToTab(index) }; setOnLongClickListener { showTabActions(index); true } }
                tabThumb(tab)?.let { shot -> card.addView(android.widget.ImageView(this).apply { setImageBitmap(shot); scaleType = android.widget.ImageView.ScaleType.CENTER_CROP }, LinearLayout.LayoutParams(-1, dp(110))) }
                val label = (if (tab.isVault) "🔒 " else "") + (if (tab.isPinned) "📌 " else "") + (if (tab.groupName.isNotBlank()) "[${tab.groupName}] " else "") + (if (tab.isIncognito) "Private · " else "") + tab.title.ifBlank { tab.url }
                card.addView(TextView(this).apply { text = label; textSize = 12.5f; setTypeface(typeface, Typeface.BOLD); setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_light)); maxLines = 2; setPadding(0, dp(6), 0, dp(2)) })
                card.addView(TextView(this).apply { text = tab.url; textSize = 9.5f; setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_muted_light)); maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END })
                card.addView(TextView(this).apply { text = "Close"; textSize = 10f; setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_dark)); setPadding(0, dp(4), 0, dp(2)); setOnClickListener { closeTab(index) } })
                val params = android.widget.GridLayout.LayoutParams(android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f), android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)).apply { width = 0; height = dp(175); setMargins(dp(4),dp(4),dp(4),dp(4)) }
                grid.addView(card, params)
            }
            viewport.addView(ScrollView(this).apply { addView(grid) })
        }
        fun renderList(filter: String) {
            viewport.removeAllViews()
            val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            tabs.forEachIndexed { index, tab ->
                if (!matches(tab, filter)) return@forEachIndexed
                val row = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding(dp(9),dp(8),dp(5),dp(8)); setOnClickListener{switchToTab(index)};setOnLongClickListener{showTabActions(index);true} }
                val textColumn=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
                val label=(if(tab.isVault)"🔒 " else "")+(if(tab.isPinned)"📌 " else "")+(if(tab.groupName.isNotBlank())"[${tab.groupName}] " else "")+(if(tab.isIncognito)"Private · " else "")+tab.title.ifBlank{tab.url}
                textColumn.addView(TextView(this@MainActivity).apply{text=label;textSize=14f;setTypeface(typeface,Typeface.BOLD);setTextColor(ContextCompat.getColor(this@MainActivity,R.color.text_light));maxLines=1})
                textColumn.addView(TextView(this@MainActivity).apply{text=tab.url;textSize=11f;setTextColor(ContextCompat.getColor(this@MainActivity,R.color.text_muted_light));maxLines=1})
                row.addView(textColumn,LinearLayout.LayoutParams(0,-2,1f));row.addView(TextView(this@MainActivity).apply{text="×";textSize=22f;setTextColor(ContextCompat.getColor(this@MainActivity,R.color.text_muted_light));setPadding(dp(12),0,0,0);setOnClickListener{closeTab(index)}});list.addView(row)
            }
            viewport.addView(ScrollView(this).apply{addView(list)})
        }
        var gridMode = true
        renderGrid("")
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?,start:Int,count:Int,after:Int){}
            override fun onTextChanged(s: CharSequence?,start:Int,before:Int,count:Int){if(gridMode)renderGrid(s?.toString().orEmpty())else renderList(s?.toString().orEmpty())}
            override fun afterTextChanged(s:android.text.Editable?){}
        })
        actions.getChildAt(3).setOnClickListener { gridMode = true; renderGrid(search.text.toString()) }
        actions.getChildAt(4).setOnClickListener { gridMode = false; renderList(search.text.toString()) }
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(wrapper)
        dialog.show()
    }

    private fun showTabActions(index: Int) {
        val tab = tabs.getOrNull(index) ?: return
        val items = arrayOf(
            if (tab.isPinned) "Unpin tab" else "Pin tab",
            "Set group + color",
            "Close this tab",
            "Duplicate tab",
            "Preview"
        )
        AlertDialog.Builder(this).setTitle(tab.title.ifBlank { "Tab" }).setItems(items) { _, which ->
            when (which) {
                0 -> { tab.isPinned = !tab.isPinned; tabAdapter.notifyItemChanged(index) }
                1 -> setTabGroup(tab)
                2 -> closeTab(index)
                3 -> openNewTab(tab.url, tab.isIncognito, tab.isDesktopMode)
                4 -> {
                    val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(4), dp(8), 0) }
                    runCatching {
                        if (tab.webView.width > 0 && tab.webView.height > 0) {
                            tabThumb(tab, dp(320), dp(180))?.let { shot ->
                                box.addView(android.widget.ImageView(this).apply { setImageBitmap(shot); scaleType = android.widget.ImageView.ScaleType.CENTER_CROP })
                            }
                        }
                    }
                    box.addView(TextView(this).apply { text = "${tab.title}\n${tab.url}"; textSize = 12f; setPadding(0, dp(8), 0, 0) })
                    AlertDialog.Builder(this).setTitle("Tab preview").setView(box).setPositiveButton("Open") { _, _ -> switchToTab(index) }.setNegativeButton("Close", null).show()
                }
            }
        }.setNegativeButton(R.string.close, null).show()
    }

    private fun setTabGroup(tab: BrowserTab) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(6), dp(4), dp(6), 0) }
        val name = EditText(this).apply { hint = "Group name (blank = none)"; setSingleLine(true); setText(tab.groupName) }
        box.addView(name)
        val colors=intArrayOf(android.graphics.Color.TRANSPARENT,0xff1565c0.toInt(),0xff2e7d32.toInt(),0xffef6c00.toInt(),0xff6a1b9a.toInt(),0xffad1457.toInt())
        var selectedColor = tab.groupColor
        AlertDialog.Builder(this).setTitle("Tab group").setView(box).setSingleChoiceItems(arrayOf("None","Blue","Green","Orange","Purple","Pink"),colors.indexOf(tab.groupColor).coerceAtLeast(0)){_,which->selectedColor=colors[which]}
            .setPositiveButton("Save"){_,_->tab.groupName=name.text.toString().trim();tab.groupColor=selectedColor;val idx=tabs.indexOf(tab);if(idx>=0)tabAdapter.notifyItemChanged(idx)}.setNegativeButton("Cancel",null).show()
    }

    private fun closeOtherTabs() {
        val keep = currentTabIndex
        for (i in tabs.indices.reversed()) if (i != keep) closeTab(i)
    }

    private fun closeAllTabs() {
        val perform = {
            if (UltimateFeatureStore.prefs(this@MainActivity).getBoolean(UltimateFeatureStore.KEY_RANDOM_CLOSE, false)) {
                val order = tabs.toList().shuffled()
                order.forEach { tab -> tabs.indexOf(tab).takeIf { it >= 0 }?.let { closeTab(it) } }
            } else {
                while (tabs.isNotEmpty()) closeTab(tabs.lastIndex)
            }
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
        if (prefs.getBoolean(KEY_MAGNET_SUPPORT,false) && trimmed.startsWith("magnet:",true)) {
            runCatching { startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(trimmed))) }.onFailure { Toast.makeText(this,"No torrent handler installed",Toast.LENGTH_SHORT).show() }
            clearFocusAndHideKeyboard(); return
        }
        if (prefs.getBoolean(KEY_BANG_COMMANDS, true)) {
            val rawKeywords = prefs.getString(KEY_SEARCH_KEYWORDS, "") ?: ""
            val custom = mutableMapOf<String,String>()
            runCatching {
                val customObj = org.json.JSONObject(rawKeywords)
                customObj.keys().forEach { k -> custom[k.lowercase()] = customObj.optString(k) }
            }.getOrElse {
                rawKeywords.lineSequence().forEach { line ->
                    val parts = line.split("=", limit = 2)
                    if (parts.size == 2 && parts[0].trim().isNotBlank()) custom[parts[0].trim().lowercase()] = parts[1].trim()
                }
            }
            UltimateTools.bangQuery(trimmed, custom)?.let { currentTab()?.webView?.loadUrl(it); clearFocusAndHideKeyboard(); return }
        }
        UltimateTools.answer(trimmed)?.let { showInstantAnswer(it); clearFocusAndHideKeyboard(); return }
        val currency=Regex("^([0-9.]+)\\s+(usd|inr|eur|gbp|jpy|cad|aud)\\s+(?:to|in)\\s+(usd|inr|eur|gbp|jpy|cad|aud)$",RegexOption.IGNORE_CASE).find(trimmed)
        if(currency!=null){showCurrencyConversion(currency.groupValues[1].toDouble(),currency.groupValues[2].uppercase(),currency.groupValues[3].uppercase());clearFocusAndHideKeyboard();return}
        val url = resolveInputToUrl(trimmed)
        val host = Uri.parse(url).host
        if (DomainPrivacyStore.isAutoIncognito(this, host) && currentTab()?.isIncognito != true) {
            openNewTab(url, incognito = true)
        } else {
            currentTab()?.webView?.loadUrl(url)
        }
        clearFocusAndHideKeyboard()
    }


    private fun buildNewTabDashboard(): String {
        val rec=db.getHistory().take(6); val frequent=db.getHistory().groupBy{Uri.parse(it.url).host.orEmpty()}.entries.sortedByDescending{it.value.size}.take(6)
        fun esc(v:String)=android.text.TextUtils.htmlEncode(v)
        val html=StringBuilder("<html><meta name='viewport' content='width=device-width,initial-scale=1'><body style='font-family:sans-serif;background:#f7f8fc;color:#202533;padding:18px'><h2>PrivBrowse</h2><form onsubmit=\"location.href='https://duckduckgo.com/?q='+encodeURIComponent(this.q.value);return false\"><input name=q autofocus style='width:90%;padding:14px;border-radius:12px;border:1px solid #ddd' placeholder='Search or type a URL'></form><h3>Recent</h3><ul>")
        rec.forEach{html.append("<li><a href='${esc(it.url)}'>${esc(it.title.ifBlank{it.url})}</a></li>")}
        html.append("</ul><h3>Frequently visited</h3><ul>");frequent.forEach{val u=it.value.first().url;html.append("<li><a href='${esc(u)}'>${esc(it.key)}</a> (${it.value.size})</li>")};html.append("</ul></body></html>")
        return "data:text/html;charset=utf-8,"+Uri.encode(html.toString())
    }

    private fun showCurrencyConversion(amount:Double,from:String,to:String){Thread{val result=runCatching{val raw=java.net.URL("https://api.frankfurter.app/latest?amount=$amount&from=$from&to=$to").readText();org.json.JSONObject(raw).optJSONObject("rates")?.optDouble(to)}.getOrNull();runOnUiThread{if(result!=null)showInstantAnswer("$amount $from = $result $to") else Toast.makeText(this,"Currency service unavailable",Toast.LENGTH_SHORT).show()}}.start()}

    private fun resolveInputToUrl(input: String): String {
        val looksLikeUrl = Patterns.WEB_URL.matcher(input).matches() && !input.contains(" ")
        val resolved = when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            looksLikeUrl -> if (prefs.getBoolean(KEY_HTTPS_FIRST, true)) "https://$input" else "http://$input"
            else -> searchUrl(input)
        }
        return if (prefs.getBoolean(KEY_STRIP_TRACKING, true)) TrackingParamStripper.clean(resolved) else resolved
    }


    private fun showAddressSuggestions(query:String){
        val q=query.trim(); if(q.length<1){suggestionsPopup?.dismiss();return}
        val entries=LinkedHashSet<String>()
        db.getHistory().take(80).forEach{entries.add(it.title+"\n"+it.url)}
        db.getBookmarks().take(50).forEach{entries.add(it.title+"\n"+it.url)}
        recentlyClosed.take(20).forEach{entries.add(it.title+"\n"+it.url)}
        val hits=entries.filter{it.contains(q,true)}.take(8)
        if(hits.isEmpty()){suggestionsPopup?.dismiss();return}
        val list=ListView(this);list.adapter=ArrayAdapter(this,android.R.layout.simple_list_item_1,hits.map{it.substringAfter("\n")});list.setOnItemClickListener{_,_,which,_->loadInput(hits[which].substringAfter("\n"));suggestionsPopup?.dismiss()}
        suggestionsPopup?.dismiss();suggestionsPopup=PopupWindow(list,dp(340),dp(220),true).apply{elevation=12f;setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE));isOutsideTouchable=true;showAsDropDown(urlBar,0,-dp(44))}
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
            if (UltimateFeatureStore.prefs(this@MainActivity).getBoolean(UltimateFeatureStore.KEY_VPN_KILL_SWITCH, false) && !isVpnActive()) return adBlocker.emptyResponse()
            val host = request.url?.host?.lowercase() ?: return null
            val category = adBlocker.classify(host)

            // Phase 3: Network Transparency Log + per-site Privacy Score.
            if (category != null) {
                tab.incrementTracker(category == BlockCategory.FINGERPRINT)
                val up=UltimateFeatureStore.prefs(this@MainActivity)
                up.edit().putInt(UltimateFeatureStore.KEY_TRACKERS_BLOCKED_TOTAL, up.getInt(UltimateFeatureStore.KEY_TRACKERS_BLOCKED_TOTAL,0)+1).apply()
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
            if (UltimateFeatureStore.prefs(this@MainActivity).getBoolean(UltimateFeatureStore.KEY_VPN_KILL_SWITCH, false) && !isVpnActive()) {
                Toast.makeText(this@MainActivity, "VPN kill switch: browser traffic is blocked until VPN is active.", Toast.LENGTH_SHORT).show()
                return true
            }
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

        override fun onLoadResource(view: WebView, url: String?) {
            super.onLoadResource(view, url)
            if (UltimateFeatureStore.prefs(this@MainActivity).getBoolean(UltimateFeatureStore.KEY_NETWORK_USAGE, true)) tab.resourcesThisPage++
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            if (url != null) {
                val previousOrigin = tab.pageOrigin
                val candidateOrigin = Uri.parse(url).let { u -> if ((u.scheme == "http" || u.scheme == "https") && !u.host.isNullOrBlank()) "${u.scheme}://${u.host}" else "" }
                if (UltimateFeatureStore.prefs(this@MainActivity).getBoolean(UltimateFeatureStore.KEY_SITE_ISOLATION, false) && previousOrigin.isNotBlank() && candidateOrigin.isNotBlank() && previousOrigin != candidateOrigin) {
                    CookiePolicy.wipeOrigins(setOf(previousOrigin))
                }
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
            if (prefs.getBoolean(KEY_FAKE_GPS, false)) {
                view.evaluateJavascript(UltimateTools.randomizedGeolocationScript(url ?: tab.url, tab.id), null)
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
            val host = Uri.parse(url ?: tab.url).host.orEmpty()
            val css = SiteCustomizationStore.getCss(this@MainActivity, host)
            val js = SiteCustomizationStore.getJs(this@MainActivity, host)
            if (css.isNotBlank()) view.evaluateJavascript("(function(){var s=document.getElementById('__privbrowse_css');if(!s){s=document.createElement('style');s.id='__privbrowse_css';document.documentElement.appendChild(s)}s.textContent=" + org.json.JSONObject.quote(css) + "})()", null)
            val accessCss = buildString {
                if (prefs.getBoolean(KEY_DYSLEXIA_FONT, false)) append("body,body *{font-family:Arial,sans-serif!important;letter-spacing:.025em!important;line-height:1.45!important;}" )
                if (prefs.getBoolean(KEY_HIGH_CONTRAST, false)) append("body{background:#fff!important;color:#000!important;}a{color:#001a99!important;}" )
            }
            if (accessCss.isNotBlank()) view.evaluateJavascript("(function(){var s=document.getElementById('__privbrowse_access');if(!s){s=document.createElement('style');s.id='__privbrowse_access';document.documentElement.appendChild(s)}s.textContent=" + org.json.JSONObject.quote(accessCss) + "})()", null)
            if (prefs.getBoolean(KEY_DARKEN_PAGES,true)) view.evaluateJavascript("document.querySelectorAll('img,video').forEach(e=>e.style.filter='brightness(.88)');",null)
            if (js.isNotBlank()) view.evaluateJavascript(js, null)
            val speed = prefs.getFloat(KEY_MEDIA_SPEED, 1.0f).toDouble()
            view.evaluateJavascript(UltimateTools.mediaControls(speed, prefs.getBoolean(KEY_AUDIO_ONLY,false), prefs.getBoolean(KEY_AUTO_AD_SKIP,false)), null)
            if (prefs.getBoolean(KEY_AI_JUNK_STRIP,false)) view.evaluateJavascript(UltimateTools.junkStripScript(), null)
            if (prefs.getBoolean(KEY_FAKE_GPS,false)) view.evaluateJavascript(UltimateTools.randomizedGeolocationScript(url ?: tab.url, tab.id), null)
            recordReadingActivity(tab)
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
            val revokeForSession = UltimateFeatureStore.prefs(this@MainActivity).getBoolean(UltimateFeatureStore.KEY_AUTO_REVOKE_MEDIA, false) && mediaPermissionsRevokedForSession
            if (revokeForSession || prefs.getBoolean(KEY_BLOCK_MEDIA_PERMISSIONS, true)) {
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
            if (prefs.getBoolean(KEY_BLOCK_GEOLOCATION, true)) { callback.invoke(origin, false, false); return }
            if (prefs.getBoolean(KEY_FAKE_GPS, false)) {
                val seed = (origin.hashCode().toLong() xor tab.id)
                val rnd = java.util.Random(seed)
                val location = android.location.Location("PrivBrowse-random").apply {
                    latitude = -60.0 + rnd.nextDouble() * 120.0
                    longitude = -170.0 + rnd.nextDouble() * 340.0
                    accuracy = 50000f
                    time = System.currentTimeMillis()
                }
                // WebView only receives the callback permission state; Android does not expose a
                // direct location object here. The randomized mode is therefore a privacy hint for
                // future bridges, not a lie about the device GPS location.
            }
            callback.invoke(origin, true, false)
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
        val filename=(Uri.parse(url).lastPathSegment ?: "download").replace(Regex("[^A-Za-z0-9._-]"), "_").takeLast(100).ifBlank { "download" }
        val suspicious = filename.lowercase().endsWith(".apk") || filename.lowercase().endsWith(".exe") || filename.lowercase().endsWith(".scr") || filename.lowercase().endsWith(".bat") || mimeType?.contains("application/x-msdownload",true)==true
        val enqueueAny = {
            com.privbrowse.app.ultimate.DownloadQueueStore.add(this,url,filename,mimeType,suspicious)
            androidx.core.content.ContextCompat.startForegroundService(this,Intent(this,DownloadQueueService::class.java))
            Toast.makeText(this,if(suspicious) "Download queued — local safety warning: executable file" else "Download queued",Toast.LENGTH_LONG).show()
        }
        if (prefs.getBoolean(KEY_DOWNLOAD_CONFIRM, true)) {
            AlertDialog.Builder(this).setTitle(if(suspicious) "Potentially executable download" else "Download?").setMessage("$host\n$filename").setNegativeButton("Cancel",null).setPositiveButton("Queue") { _,_->enqueueAny() }.show()
        } else enqueueAny()
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
        selfDestructTasks.values.forEach(cookieWipeHandler::removeCallbacks)
        autoCloseTasks.values.forEach(cookieWipeHandler::removeCallbacks)
        suggestionsPopup?.dismiss()
        loadTimeoutTasks.clear()
        globalVisitedOrigins.clear()
        PanicManager.wipe(this, tabs, db)
        prefs.edit().remove(KEY_SESSION_URLS).remove(KEY_SESSION_JSON).remove("session_first_url").apply()
        recentlyClosed.clear()
        persistRecentlyClosed()
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

    private fun applyImportedTheme() {
        if (!::bottomNav.isInitialized || !::urlBar.isInitialized) return
        val raw = UltimateFeatureStore.prefs(this).getString(UltimateFeatureStore.KEY_THEME_JSON, "").orEmpty()
        val theme = runCatching { org.json.JSONObject(raw) }.getOrNull() ?: return
        fun color(key: String, fallback: Int): Int = runCatching { android.graphics.Color.parseColor(theme.optString(key)) }.getOrDefault(fallback)
        val background = color("background", ContextCompat.getColor(this, R.color.background_light))
        val surface = color("surface", ContextCompat.getColor(this, R.color.surface_light))
        val text = color("text", ContextCompat.getColor(this, R.color.text_light))
        val accent = color("accent", ContextCompat.getColor(this, R.color.accent))
        findViewById<View>(R.id.mainRoot)?.setBackgroundColor(background)
        findViewById<View>(R.id.topBar)?.setBackgroundColor(surface)
        bottomNav.setBackgroundColor(surface)
        webViewContainer.setBackgroundColor(background)
        urlBar.setTextColor(text)
        privacyBadge.backgroundTintList = ColorStateList.valueOf(accent)
        listOf<View>(btnBack, btnForward, btnRefresh, btnHome, btnTools, btnMenu, btnTabs).forEach { it.minimumHeight = if (UltimateFeatureStore.prefs(this).getBoolean(UltimateFeatureStore.KEY_ONE_HANDED, false)) dp(48) else dp(44) }
    }

    private fun applyGlobalSettingsToOpenTabs() {
        val dark = prefs.getBoolean(KEY_DARK_MODE, false)
        val ultimate = UltimateFeatureStore.prefs(this)
        tabs.forEach { tab ->
            tab.webView.settings.javaScriptEnabled = prefs.getBoolean(KEY_JAVASCRIPT, true)
            tab.webView.settings.loadsImagesAutomatically = prefs.getBoolean(KEY_IMAGES, true)
            tab.webView.settings.domStorageEnabled = prefs.getBoolean(KEY_DOM_STORAGE, true)
            tab.webView.settings.mediaPlaybackRequiresUserGesture = if (ultimate.getBoolean(UltimateFeatureStore.KEY_BACKGROUND_MEDIA, false)) false else prefs.getBoolean(KEY_MEDIA_GESTURE, true)
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
            tab.webView.settings.cacheMode = when {
                ultimate.getBoolean(UltimateFeatureStore.KEY_OFFLINE_MODE, false) -> android.webkit.WebSettings.LOAD_CACHE_ONLY
                prefs.getBoolean(KEY_NO_CACHE, false) -> android.webkit.WebSettings.LOAD_NO_CACHE
                !tab.isIncognito && (prefs.getBoolean(KEY_FAST_CACHE, true) || prefs.getBoolean(KEY_DATA_SAVER, false)) -> android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK
                else -> android.webkit.WebSettings.LOAD_DEFAULT
            }
        }
        val oneHanded = ultimate.getBoolean(UltimateFeatureStore.KEY_ONE_HANDED, false)
        if (::bottomNav.isInitialized) {
            bottomNav.scaleX = if (oneHanded) 0.94f else 1f
            bottomNav.translationX = if (oneHanded) dp(10).toFloat() else 0f
            bottomNav.pivotX = bottomNav.width.toFloat()
        }
        applyImportedTheme()
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
        sheetRow(content, "Inspect element") { dialog.dismiss(); inspectNextElement(tab) }
        sheetRow(content, "Web clipper") { dialog.dismiss(); openWebClipper() }
        sheetRow(content, "View page source") { dialog.dismiss(); viewPageSource() }
        sheetRow(content, "Save web archive") { dialog.dismiss(); saveWebArchive() }
        sheetRow(content, "Capture page screenshot") { dialog.dismiss(); capturePageScreenshot() }
        if (prefs.getBoolean(KEY_TTS, true)) sheetRow(content, "Listen to page") { dialog.dismiss(); speakPage() }
        if (prefs.getBoolean(KEY_TRANSLATE, true)) sheetRow(content, "Translate page") { dialog.dismiss(); translatePage() }
        if (prefs.getBoolean(KEY_READING_LIST, true)) sheetRow(content, "Save to reading list") { dialog.dismiss(); db.addReadingList(tab.title, tab.url); Toast.makeText(this, "Saved to reading list", Toast.LENGTH_SHORT).show() }
        sheetRow(content, "Print / Save PDF") { dialog.dismiss(); savePageAsPdf() }
        sheetRow(content, "Translate selection") { dialog.dismiss(); currentTab()?.webView?.evaluateJavascript("window.getSelection ? window.getSelection().toString() : ''") { raw -> val t=runCatching{org.json.JSONTokener(raw).nextValue().toString()}.getOrElse{raw.trim('\"')}; if(t.isNotBlank())openNewTab(UltimateTools.selectionTranslateUrl(t))else Toast.makeText(this,"Select text on the page first",Toast.LENGTH_SHORT).show() } }
        sheetRow(content, "Media controls") { dialog.dismiss(); showMediaTools() }
        sheetRow(content, "Background audio") { dialog.dismiss(); startBackgroundAudio() }
        sheetRow(content, "Zoom") { dialog.dismiss(); showZoomDialog() }
        sheetRow(content, "Hard reload") { dialog.dismiss(); hardReload() }
        sheetRow(content, "Open in external browser") { dialog.dismiss(); openExternal() }
        if (prefs.getBoolean(KEY_READER_MODE, true)) sheetRow(content, "Reader mode") { dialog.dismiss(); tab.webView.evaluateJavascript(ReaderMode.SCRIPT, null) }
        sheetRow(content, "Site controls") { dialog.dismiss(); showSiteControls() }
        dialog.setContentView(ScrollView(this).apply { addView(content) })
        dialog.show()
    }

    private fun inspectNextElement(tab: BrowserTab) {
        val js = """
            (function(){
              if(window.__privInspectInstalled)return; window.__privInspectInstalled=true;
              var old;
              function cleanup(){document.removeEventListener('click',pick,true);if(old)old.remove()}
              function pick(e){e.preventDefault();e.stopPropagation();var el=e.target;var data={tag:el.tagName,classes:el.className||'',id:el.id||'',text:(el.innerText||'').slice(0,1200),html:(el.outerHTML||'').slice(0,8000),style:getComputedStyle(el).cssText||''};cleanup();window.__privInspectPayload=JSON.stringify(data)}
              document.addEventListener('click',pick,true);
              setTimeout(function(){if(window.__privInspectPayload)return;},30000);
            })();
        """.trimIndent()
        tab.webView.evaluateJavascript(js, null)
        cookieWipeHandler.postDelayed(object : Runnable { override fun run() { tab.webView.evaluateJavascript("window.__privInspectPayload||''") { raw ->
            val value = runCatching { org.json.JSONTokener(raw).nextValue().toString() }.getOrElse { "" }
            if (value.isBlank()) return@evaluateJavascript
            val o = runCatching { org.json.JSONObject(value) }.getOrNull() ?: return@evaluateJavascript
            AlertDialog.Builder(this@MainActivity).setTitle("Inspect element").setMessage("${o.optString("tag")}\n#${o.optString("id")}\n.${o.optString("classes")}\n\n${o.optString("text")}\n\n${o.optString("html")}").setPositiveButton("Copy") { _, _ -> copyText("Element HTML", o.optString("html")) }.setNegativeButton("Close", null).show()
            tab.webView.evaluateJavascript("window.__privInspectPayload='';", null)
        }} }, 800)
    }

    private fun showCustomUserAgent(tab: BrowserTab) {
        val e = EditText(this).apply { setSingleLine(false); minLines=3; setText(tab.webView.settings.userAgentString) }
        AlertDialog.Builder(this).setTitle("User-agent").setMessage("Leave blank to use the default mobile/desktop profile.").setView(e).setPositiveButton("Apply") { _, _ ->
            val value=e.text.toString().trim(); tab.webView.settings.userAgentString=if(value.isBlank()) if(tab.isDesktopMode) DESKTOP_UA else mobileUserAgent else value; tab.webView.reload()
        }.setNegativeButton("Cancel", null).show()
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
        sheetRow(content, "Custom user-agent") { dialog.dismiss(); showCustomUserAgent(tab) }
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
        val scale=resources.displayMetrics.density
        val originalWidth=webView.width.coerceAtLeast(1)
        val originalHeight=webView.height.coerceAtLeast(1)
        val width=originalWidth
        val height=if(prefs.getBoolean(KEY_FULL_PAGE_SCREENSHOT,false)) (webView.contentHeight*scale).toInt().coerceIn(originalHeight,20000) else originalHeight
        if(height>originalHeight){ webView.measure(android.view.View.MeasureSpec.makeMeasureSpec(width,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(height,android.view.View.MeasureSpec.EXACTLY));webView.layout(0,0,width,height) }
        val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);webView.draw(android.graphics.Canvas(bitmap))
        if(height!=originalHeight){ webView.measure(android.view.View.MeasureSpec.makeMeasureSpec(originalWidth,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(originalHeight,android.view.View.MeasureSpec.EXACTLY));webView.layout(0,0,originalWidth,originalHeight) }
        val file=java.io.File(cacheDir,"page-${System.currentTimeMillis()}.png");file.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
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


    private fun isVpnActive(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)
    }

    private fun startVoiceCommand() {
        val i = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Search, open a URL, back, forward, refresh, new tab or close tab")
        }
        runCatching { startActivityForResult(i, 7801) }.onFailure { Toast.makeText(this, "Voice input is not available", Toast.LENGTH_SHORT).show() }
    }

    private fun showCurrentUrlQr() {
        val tab = currentTab() ?: return
        val url = tab.url.takeIf { it.startsWith("http://") || it.startsWith("https://") } ?: return
        val payload = org.json.JSONObject().put("tabs", org.json.JSONArray().put(org.json.JSONObject().put("title", tab.title).put("url", url))).put("current", 0).toString()
        startActivity(Intent(this, QrTransferActivity::class.java).putExtra(QrTransferActivity.EXTRA_PAYLOAD, payload))
    }

    private fun showEmailAlias() {
        val input = EditText(this).apply { hint = "you@example.com"; setSingleLine(true); inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS }
        AlertDialog.Builder(this).setTitle("Email alias / masking").setMessage("Creates a unique plus-address for services that support plus addressing. No mailbox server is created.").setView(input).setNegativeButton("Cancel", null).setPositiveButton("Generate") { _, _ ->
            val base=input.text.toString().trim(); val at=base.lastIndexOf('@'); if(at>0) copyText("Email alias",base.substring(0,at)+"+pb"+System.currentTimeMillis().toString(36)+base.substring(at))
        }.show()
    }

    private fun showCalculatorConverter() {
        val input=EditText(this).apply{hint="125 * 1.18  or  10 km to mi";setSingleLine(true)}
        AlertDialog.Builder(this).setTitle("Calculator / converter").setView(input).setNegativeButton("Close",null).setPositiveButton("Calculate"){_,_->showInstantAnswer(UltimateTools.answer(input.text.toString()) ?: "No local answer for that expression.")}.show()
    }

    private fun joinDetectedMeeting() {
        val tab=currentTab() ?: return
        tab.webView.evaluateJavascript("JSON.stringify(Array.from(document.querySelectorAll('a[href]')).map(a=>a.href).filter(u=>/zoom\\.us|meet\\.google\\.com/i.test(u)).slice(0,5))"){raw->
            val json=runCatching{org.json.JSONTokener(raw).nextValue().toString()}.getOrElse{"[]"};val arr=runCatching{org.json.JSONArray(json)}.getOrElse{org.json.JSONArray()};val url=arr.optString(0)
            if(url.isBlank())Toast.makeText(this,"No Zoom or Google Meet link found",Toast.LENGTH_SHORT).show() else runCatching{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))}.onFailure{Toast.makeText(this,"No meeting app can open this link",Toast.LENGTH_SHORT).show()}
        }
    }

    private fun aiImageAltText() {
        val tab=currentTab() ?: return
        tab.webView.evaluateJavascript("JSON.stringify(Array.from(document.images).map(i=>({src:i.currentSrc||i.src||'',alt:i.alt||'',title:i.title||''})).filter(x=>x.src).slice(0,30))"){raw->
            val json=runCatching{org.json.JSONTokener(raw).nextValue().toString()}.getOrElse{"[]"}
            startActivity(Intent(this,AiCopilotActivity::class.java).apply{putExtra(AiCopilotActivity.EXTRA_PAGE_TITLE,"Image alt-text assistant");putExtra(AiCopilotActivity.EXTRA_PAGE_URL,tab.url);putExtra(AiCopilotActivity.EXTRA_PAGE_TEXT,"Create accurate accessible alt text for these image entries. Do not invent unseen details.\n$json")})
        }
    }

    private fun exportCrashLog() {
        val latest=java.io.File(filesDir,"crashes").listFiles()?.maxByOrNull{it.lastModified()};if(latest==null)Toast.makeText(this,"No crash log recorded",Toast.LENGTH_SHORT).show() else copyText("PrivBrowse crash log",latest.readText().take(30000))
    }

    private fun performGestureAction(tab:BrowserTab,action:String){
        when(action){
            "back"->if(tab.webView.canGoBack())tab.webView.goBack();"forward"->if(tab.webView.canGoForward())tab.webView.goForward();"next_tab"->switchRelativeTab(1);"previous_tab"->switchRelativeTab(-1);"refresh"->tab.webView.reload();"home"->tab.webView.loadUrl(getHomeUrl());"tab_manager"->showTabManager();"new_tab"->openNewTab(getHomeUrl());"close_tab"->tabs.indexOf(tab).takeIf{it>=0}?.let{closeTab(it)};"page_top"->tab.webView.scrollTo(0,0);"page_bottom"->tab.webView.evaluateJavascript("window.scrollTo(0,document.documentElement.scrollHeight)",null)
        }
    }

    private fun showInstantAnswer(answer: String) {
        AlertDialog.Builder(this).setTitle("Instant answer").setMessage(answer).setPositiveButton("Copy") { _, _ -> copyText("Instant answer", answer) }.setNegativeButton(R.string.close, null).show()
    }

    private fun showMediaTools() {
        val speed = arrayOf("0.5x","0.75x","1x","1.25x","1.5x","2x","3x")
        AlertDialog.Builder(this).setTitle("Video / audio controls").setItems(speed) { _, which ->
            val value = speed[which].removeSuffix("x").toFloat(); prefs.edit().putFloat(KEY_MEDIA_SPEED,value).apply(); currentTab()?.webView?.evaluateJavascript(UltimateTools.mediaControls(value.toDouble(),prefs.getBoolean(KEY_AUDIO_ONLY,false),prefs.getBoolean(KEY_AUTO_AD_SKIP,false)),null)
        }.setNeutralButton(if(prefs.getBoolean(KEY_AUDIO_ONLY,false)) "Audio-only: ON" else "Audio-only: OFF") { _, _ -> prefs.edit().putBoolean(KEY_AUDIO_ONLY,!prefs.getBoolean(KEY_AUDIO_ONLY,false)).apply(); currentTab()?.webView?.evaluateJavascript(UltimateTools.mediaControls(prefs.getFloat(KEY_MEDIA_SPEED,1f).toDouble(),prefs.getBoolean(KEY_AUDIO_ONLY,false),prefs.getBoolean(KEY_AUTO_AD_SKIP,false)),null) }
            .setPositiveButton(if(prefs.getBoolean(KEY_AUTO_AD_SKIP,false)) "Auto skip: ON" else "Auto skip: OFF") { _, _ -> prefs.edit().putBoolean(KEY_AUTO_AD_SKIP,!prefs.getBoolean(KEY_AUTO_AD_SKIP,false)).apply(); currentTab()?.webView?.evaluateJavascript(UltimateTools.mediaControls(prefs.getFloat(KEY_MEDIA_SPEED,1f).toDouble(),prefs.getBoolean(KEY_AUDIO_ONLY,false),prefs.getBoolean(KEY_AUTO_AD_SKIP,false)),null) }
            .show()
    }

    private fun compareSearchEngines() {
        val q = EditText(this).apply { hint = "Search query"; setSingleLine(true) }
        AlertDialog.Builder(this).setTitle("Compare search engines").setView(q).setNegativeButton("Cancel",null).setPositiveButton("Compare") { _, _ ->
            val query=q.text.toString().trim(); if(query.isBlank())return@setPositiveButton
            startActivity(Intent(this,CompareActivity::class.java).apply{putExtra("left","https://www.google.com/search?q=${Uri.encode(query)}");putExtra("right","https://search.brave.com/search?q=${Uri.encode(query)}")})
        }.show()
    }

    private fun shareTabsLocal(){
        val payload=QrExport.encodeTabs(tabs,currentTabIndex)
        val send=Intent(Intent.ACTION_SEND).apply{type="application/json";putExtra(Intent.EXTRA_TEXT,payload)}
        startActivity(Intent.createChooser(send,"Share PrivBrowse tab package"))
    }

    private fun showQrTransfer() {
        val payload=QrExport.encodeTabs(tabs,currentTabIndex)
        startActivity(Intent(this,QrTransferActivity::class.java).putExtra(QrTransferActivity.EXTRA_PAYLOAD,payload))
    }

    private fun openSecondWindow() {
        val i=Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        startActivity(i)
    }

    private fun startFloatingBubble() {
        if(Build.VERSION.SDK_INT>=23 && !android.provider.Settings.canDrawOverlays(this)){
            startActivity(Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))
            Toast.makeText(this,"Allow 'Display over other apps', then enable bubble again.",Toast.LENGTH_LONG).show(); return
        }
        androidx.core.content.ContextCompat.startForegroundService(this,Intent(this,FloatingBubbleService::class.java))
    }

    private fun recordReadingActivity(tab: BrowserTab) {
        val p=UltimateFeatureStore.prefs(this); val today=java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(java.util.Date()); val last=p.getString(UltimateFeatureStore.KEY_READING_STREAK_DAY,"")
        if(last==today)return
        val old=p.getLong(UltimateFeatureStore.KEY_READING_STREAK,0L); val yesterday=java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(java.util.Date(System.currentTimeMillis()-24*3600_000L)); val next=if(last==yesterday)old+1 else 1
        p.edit().putString(UltimateFeatureStore.KEY_READING_STREAK_DAY,today).putLong(UltimateFeatureStore.KEY_READING_STREAK,next).apply()
        val badges=UltimateFeatureStore.jsonArray(this,UltimateFeatureStore.KEY_BADGES); if(next>=7 && !containsBadge(badges,"7-day-reader")){badges.put("7-day-reader");UltimateFeatureStore.putArray(this,UltimateFeatureStore.KEY_BADGES,badges)}
    }
    private fun containsBadge(a:org.json.JSONArray,value:String)=run{for(i in 0 until a.length())if(a.optString(i)==value)return@run true;false}

    private fun enterBrowserPictureInPicture() {
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.O){Toast.makeText(this,"PiP requires Android 8.0+",Toast.LENGTH_SHORT).show();return}
        if(!currentTab()?.detectedVideoUrls.isNullOrEmpty() || currentTab()?.webView?.url?.contains("youtube",true)==true){enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().setAspectRatio(android.util.Rational(16,9)).build())}
        else Toast.makeText(this,"Open a video page first.",Toast.LENGTH_SHORT).show()
    }

    private fun closeTabsRandomly() {
        if(tabs.size<2)return
        tabs.indices.shuffled().forEach{if(tabs.size>1){val idx=it.coerceAtMost(tabs.lastIndex);if(tabs[idx]!=currentTab())closeTab(idx)}}
    }

    private fun authenticateVaultThenOpen() {
        BiometricLock.authenticate(this,{vaultUnlocked=true;prefs.edit().putBoolean(KEY_VAULT_UNLOCKED,true).apply();openNewTab(getHomeUrl(),false,false,true)}, {Toast.makeText(this,"Vault unlock cancelled",Toast.LENGTH_SHORT).show()})
    }
    private fun authenticateVault() { BiometricLock.authenticate(this,{vaultUnlocked=true;prefs.edit().putBoolean(KEY_VAULT_UNLOCKED,true).apply();showTabManager()},{Toast.makeText(this,"Vault unlock cancelled",Toast.LENGTH_SHORT).show()}) }

    private fun importTabs(payload:String) {
        val obj=runCatching{org.json.JSONObject(payload)}.getOrNull()?:return
        val a=obj.optJSONArray("tabs")?:return
        var first:Int?=null
        for(i in 0 until a.length()) { val t=a.optJSONObject(i)?:continue; val before=tabs.size; openNewTab(t.optString("url"),false,t.optBoolean("desktop"),false); tabs.getOrNull(before)?.let{it.title=t.optString("title",it.title);it.isPinned=t.optBoolean("pinned",false);it.groupName=t.optString("group");it.groupColor=t.optInt("groupColor",0)}; if(i==obj.optInt("current",0))first=before }
        first?.let{if(it in tabs.indices)switchToTab(it)}
        Toast.makeText(this,"Tabs imported",Toast.LENGTH_SHORT).show()
    }

    private fun startBackgroundAudio() {
        val tab=currentTab() ?: return
        val url=tab.detectedVideoUrls.firstOrNull() ?: tab.url.takeIf{it.matches(Regex("https?://.+\\.(mp3|m4a|aac|ogg|wav)(\\?.*)?",RegexOption.IGNORE_CASE))}
        if(url.isNullOrBlank()){Toast.makeText(this,"Open a direct audio/media URL first.",Toast.LENGTH_SHORT).show();return}
        androidx.core.content.ContextCompat.startForegroundService(this,Intent(this,com.privbrowse.app.ultimate.BackgroundAudioService::class.java).putExtra("url",url))
    }

    private fun showWishlistDashboard() { startActivity(Intent(this,PrivacyDashboardActivity::class.java)) }


    private fun warnOnUnvalidatedWifi() {
        if(!UltimateFeatureStore.prefs(this).getBoolean(UltimateFeatureStore.KEY_WIFI_WARNING,true)) return
        val cm=getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val n=cm.activeNetwork?:return; val caps=cm.getNetworkCapabilities(n)?:return
        if(caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) && !caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)) Toast.makeText(this,"Wi‑Fi network is unvalidated; avoid sensitive browsing until trusted.",Toast.LENGTH_LONG).show()
    }
    private fun applyLowPowerPolicy(){
        if(!UltimateFeatureStore.prefs(this).getBoolean(UltimateFeatureStore.KEY_LOW_POWER,true))return
        val bm=getSystemService(BATTERY_SERVICE) as android.os.BatteryManager; val level=bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if(level in 1..15) currentTab()?.webView?.evaluateJavascript("document.documentElement.style.setProperty('scroll-behavior','auto');document.querySelectorAll('*').forEach(e=>e.style.setProperty('animation-duration','0s','important'));",null)
    }

    private fun openLeakTests() {
        openNewTab("https://browserleaks.com/webrtc")
        openNewTab("https://www.dnsleaktest.com/")
    }

    private fun guestCleanup() {
        clearAllBrowsingData()
        openNewTab(getHomeUrl(), incognito = true)
    }

    private fun openNotes() {
        val tab=currentTab() ?: return
        startActivity(Intent(this, NotesActivity::class.java).apply { putExtra(NotesActivity.EXTRA_TITLE, tab.title); putExtra(NotesActivity.EXTRA_URL, tab.url) })
    }

    private fun showStickyNoteOverlay() {
        val tab = currentTab() ?: return
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply { cornerRadius = dp(14).toFloat(); setColor(ContextCompat.getColor(this@MainActivity, R.color.surface_light)); setStroke(dp(1), ContextCompat.getColor(this@MainActivity, R.color.divider)) }
        }
        val title = EditText(this).apply { setSingleLine(true); hint = "Note title"; setText(tab.title.take(50)) }
        val body = EditText(this).apply { hint = "Type a sticky note..."; minLines = 5 }
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
        val popup = PopupWindow(box, dp(310), dp(300), true).apply { elevation = dp(14).toFloat(); isOutsideTouchable = true }
        actions.addView(TextView(this).apply { text = "Full notes"; setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_dark)); setPadding(dp(8), dp(9), dp(12), dp(9)); setOnClickListener { popup.dismiss(); openNotes() } })
        actions.addView(TextView(this).apply { text = "Save"; setTypeface(typeface, Typeface.BOLD); setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_dark)); setPadding(dp(12), dp(9), dp(8), dp(9)); setOnClickListener {
            val arrPrefs = getSharedPreferences("privbrowse_notes", MODE_PRIVATE); val arr = runCatching { org.json.JSONArray(arrPrefs.getString("notes", "[]") ?: "[]") }.getOrElse { org.json.JSONArray() }
            arr.put(org.json.JSONObject().apply { put("id", System.currentTimeMillis()); put("title", title.text.toString().trim().ifBlank { "Sticky note" }); put("body", body.text.toString()); put("url", tab.url); put("timestamp", System.currentTimeMillis()) })
            arrPrefs.edit().putString("notes", arr.toString()).apply(); popup.dismiss(); Toast.makeText(this@MainActivity, "Sticky note saved", Toast.LENGTH_SHORT).show()
        } })
        box.addView(TextView(this).apply { text = "Sticky note"; textSize = 16f; setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_light)); setTypeface(typeface, Typeface.BOLD); setPadding(0,0,0,dp(6)) })
        box.addView(title)
        box.addView(body, LinearLayout.LayoutParams(-1, dp(160)).apply { topMargin = dp(8) })
        box.addView(actions)
        popup.showAtLocation(webViewContainer, Gravity.TOP or Gravity.END, dp(14), dp(74))
    }

    private fun reverseImageSearch() {
        val tab = currentTab() ?: return
        tab.webView.evaluateJavascript("(function(){var i=document.querySelector('img[src],img[currentSrc]');return i?(i.currentSrc||i.src||''):''})()") { raw ->
            val imageUrl = runCatching { org.json.JSONTokener(raw).nextValue().toString() }.getOrElse { raw.trim('\"') }
            val target = imageUrl.takeIf { it.startsWith("http://") || it.startsWith("https://") } ?: tab.url
            openNewTab("https://lens.google.com/uploadbyurl?url=${Uri.encode(target)}")
        }
    }

    private fun downloadSubtitleCandidate() {
        val tab=currentTab() ?: return
        tab.webView.evaluateJavascript("(function(){var t=document.querySelector('track[kind=\"subtitles\"],track[src]');return t&&t.src?t.src:''})()") { raw ->
            val candidate=runCatching{org.json.JSONTokener(raw).nextValue().toString()}.getOrElse{raw.trim('\"')}
            if(candidate.startsWith("http://")||candidate.startsWith("https://")) handleDownload(candidate,"text/vtt") else Toast.makeText(this,"No subtitle track found",Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPageGallery() {
        val tab=currentTab() ?: return
        tab.webView.evaluateJavascript("(function(){return JSON.stringify(Array.from(document.images).map(i=>({src:i.currentSrc||i.src||'',alt:i.alt||''})).filter(x=>x.src).slice(0,120))})()") { raw ->
            val json=runCatching{org.json.JSONTokener(raw).nextValue().toString()}.getOrElse{"[]"}
            val arr=runCatching{org.json.JSONArray(json)}.getOrElse{org.json.JSONArray()}
            val html=StringBuilder("<html><meta name='viewport' content='width=device-width,initial-scale=1'><body style='margin:8px;background:#111;color:#eee'><h3>Page gallery</h3><div style='display:grid;grid-template-columns:repeat(2,1fr);gap:6px'>")
            for(i in 0 until arr.length()){val o=arr.optJSONObject(i)?:continue;val src=android.text.TextUtils.htmlEncode(o.optString("src"));val alt=android.text.TextUtils.htmlEncode(o.optString("alt"));html.append("<figure style='margin:0'><img loading='lazy' src='").append(src).append("' alt='").append(alt).append("' style='width:100%;height:170px;object-fit:cover'><figcaption style='font:11px sans-serif'>").append(alt).append("</figcaption></figure>")}
            html.append("</div></body></html>")
            openNewTab("data:text/html;charset=utf-8,"+Uri.encode(html.toString()))
        }
    }

    private fun openPrintFriendly() {
        val tab=currentTab() ?: return
        tab.webView.evaluateJavascript("(function(){var a=document.querySelector('article,main')||document.body;return '<!doctype html><html><meta name=viewport content=width=device-width><body style=\"font-family:sans-serif;max-width:760px;margin:24px auto;line-height:1.55;padding:0 16px\"><h1>'+document.title+'</h1>'+a.innerHTML+'</body></html>'})()") { raw ->
            val html=runCatching{org.json.JSONTokener(raw).nextValue().toString()}.getOrElse{"<html><body>No printable content</body></html>"}
            openNewTab("data:text/html;charset=utf-8,"+Uri.encode(html))
        }
    }

    private fun addCalendarEvent() {
        val tab = currentTab() ?: return
        val base = Intent(Intent.ACTION_INSERT).setData(android.provider.CalendarContract.Events.CONTENT_URI)
            .putExtra(android.provider.CalendarContract.Events.TITLE, tab.title)
            .putExtra(android.provider.CalendarContract.Events.DESCRIPTION, tab.url)
        tab.webView.evaluateJavascript("(function(){var h=(document.querySelector('h1,h2,[role=heading]')||{}).innerText||document.title||'';var t=document.querySelector('time[datetime]');return JSON.stringify({title:h,start:t&&t.getAttribute('datetime')||''})})()") { raw ->
            val json = runCatching { org.json.JSONObject(org.json.JSONTokener(raw).nextValue().toString()) }.getOrNull()
            val eventTitle = json?.optString("title").orEmpty().trim()
            val startText = json?.optString("start").orEmpty().trim()
            if (eventTitle.isNotBlank()) base.putExtra(android.provider.CalendarContract.Events.TITLE, eventTitle)
            parseCalendarStart(startText)?.let { ms -> base.putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, ms); base.putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, ms + 60 * 60_000L) }
            startActivity(base)
        }
    }

    private fun parseCalendarStart(value: String): Long? = runCatching {
        val normalized = value.substringBefore('.').replace("Z", "")
        val fmt = if (normalized.length >= 16 && normalized.contains('T')) java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US) else java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
        fmt.parse(normalized)?.time
    }.getOrNull()

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
            }, prefs.getLong(KEY_AUTO_CLEAR_CLIPBOARD_SECONDS, 30L).coerceIn(5L, 300L) * 1000L)
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
        if (requestCode == 7801) {
            val spoken = data.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if (spoken.isNotBlank()) {
                when (spoken.trim().lowercase(Locale.getDefault())) {
                    "back" -> currentTab()?.webView?.goBack()
                    "forward" -> currentTab()?.webView?.goForward()
                    "refresh" -> currentTab()?.webView?.reload()
                    "new tab" -> openNewTab(getHomeUrl())
                    "close tab" -> if (currentTabIndex in tabs.indices) closeTab(currentTabIndex)
                    else -> loadInput(spoken.removePrefix("search ").removePrefix("open ").trim())
                }
            }
            return
        }
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

    override fun onPause() {
        saveSessionSnapshot()
        if (UltimateFeatureStore.prefs(this).getBoolean(UltimateFeatureStore.KEY_AUTO_REVOKE_MEDIA, false)) mediaPermissionsRevokedForSession = true
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        mediaPermissionsRevokedForSession = false
        applyImportedTheme()
        if (::adBlocker.isInitialized) adBlocker.refreshFromPreferences()
        applyGlobalSettingsToOpenTabs()
        updateAiBubble()
        warnOnUnvalidatedWifi()
        applyLowPowerPolicy()
        if (!UltimateFeatureStore.prefs(this).getBoolean(UltimateFeatureStore.KEY_GHOST, false)) com.privbrowse.app.ultimate.BackupReceiver.schedule(this)
        com.privbrowse.app.ultimate.CleanupReceiver.schedule(this)
        if (UltimateFeatureStore.prefs(this).getBoolean(UltimateFeatureStore.KEY_VPN_KILL_SWITCH, false) && !isVpnActive()) Toast.makeText(this, "VPN kill switch is active: browser traffic is blocked.", Toast.LENGTH_LONG).show()
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
        if (prefs.getBoolean(KEY_GHOST_MODE,false)) { clearAllBrowsingData() }
        clipboardManager?.removePrimaryClipChangedListener(clipboardListener)
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
