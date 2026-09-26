package com.privbrowse.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
        private const val REQ_BOOKMARKS = 100
        private const val REQ_HISTORY = 101
        private const val REQ_NOTIFICATIONS = 102

        private const val THIRTY_DAYS_MILLIS = 30L * 24 * 60 * 60 * 1000

        private const val DESKTOP_UA =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Safari/537.36"
    }

    private lateinit var urlBar: EditText
    private lateinit var privacyBadge: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var webViewContainer: FrameLayout
    private lateinit var tabStrip: RecyclerView
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnHome: ImageButton
    private lateinit var btnTabs: ImageButton
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
    private var biometricPromptShowing = false

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

        adBlocker = AdBlocker(this)
        db = DbHelper(this)

        bindViews()
        setupTabStrip()
        setupToolbar()

        // Phase 3: Weekly Privacy Report — re-registering an existing alarm
        // is a no-op, so this is safe to call on every launch.
        WeeklyReportScheduler.scheduleIfNeeded(this)
        requestNotificationPermissionIfNeeded()
        db.pruneNetworkLog(THIRTY_DAYS_MILLIS)

        openNewTab(resolveInitialUrl(intent))
    }

    private fun resolveInitialUrl(intent: Intent?): String {
        val incoming = intent?.data?.toString()?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        return incoming?.let(TrackingParamStripper::clean) ?: HOME_URL
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val incoming = intent.data?.toString()?.takeIf { it.startsWith("http://") || it.startsWith("https://") } ?: return
        val cleaned = TrackingParamStripper.clean(incoming)
        val host = Uri.parse(cleaned).host
        if (DomainPrivacyStore.isAutoIncognito(this, host) && currentTab()?.isIncognito != true) openNewTab(cleaned, true)
        else currentTab()?.webView?.loadUrl(cleaned)
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
        tabStrip = findViewById(R.id.tabStrip)
        btnBack = findViewById(R.id.btnBack)
        btnForward = findViewById(R.id.btnForward)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnHome = findViewById(R.id.btnHome)
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
        tabStrip.visibility = if (tabs.size > 1) View.VISIBLE else View.GONE
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
        btnRefresh.setOnClickListener { currentTab()?.webView?.reload() }
        btnHome.setOnClickListener { currentTab()?.webView?.loadUrl(HOME_URL) }
        btnTabs.setOnClickListener { openNewTab(HOME_URL) }
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
        sheetRow(content, getString(R.string.bookmarks)) {
            dialog.dismiss(); startActivityForResult(Intent(this, BookmarksActivity::class.java), REQ_BOOKMARKS)
        }
        sheetRow(content, getString(R.string.history)) {
            dialog.dismiss(); startActivityForResult(Intent(this, HistoryActivity::class.java), REQ_HISTORY)
        }
        sheetRow(content, getString(R.string.add_bookmark)) { dialog.dismiss(); addCurrentPageBookmark() }
        sheetRow(content, getString(R.string.incognito_tab)) { dialog.dismiss(); openNewTab(HOME_URL, incognito = true) }

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
        sheetRow(content, getString(R.string.panic_menu)) { dialog.dismiss(); showPanicDialog() }
        val identityOn = prefs.getBoolean(KEY_FRESH_IDENTITY, true)
        sheetRow(content, getString(R.string.fresh_identity_menu), if (identityOn) "On" else "Off") {
            dialog.dismiss(); toggleFreshIdentity()
        }

        sheetDivider(content)
        sheetHeader(content, "Tools")
        sheetRow(content, getString(R.string.ai_copilot_menu)) {
            dialog.dismiss(); startActivity(Intent(this, AiCopilotActivity::class.java))
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

    private fun addCurrentPageBookmark() {
        val tab = currentTab() ?: return
        db.addBookmark(tab.title, tab.url)
        Toast.makeText(this, "Bookmarked", Toast.LENGTH_SHORT).show()
    }

    // ---------- Phase 3: per-site privacy score ----------

    private fun updatePrivacyBadge(tab: BrowserTab) {
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
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, dark)
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
    private fun openNewTab(url: String, incognito: Boolean = false) {
        val webView = WebView(this)
        if (mobileUserAgent.isEmpty()) mobileUserAgent = webView.settings.userAgentString

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = false
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webView.settings.allowFileAccessFromFileURLs = false
            webView.settings.allowUniversalAccessFromFileURLs = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) webView.settings.safeBrowsingEnabled = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false
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

        // Phase 2: third-party cookies are blocked on every tab, always.
        CookiePolicy.configureWebView(webView)
        if (incognito) {
            // Phase 2: incognito tabs skip the disk cache; their cookies/
            // storage are wiped per-origin the moment the tab closes
            // (see closeTab), giving functional session isolation without
            // needing a second WebView process.
            webView.settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
        }

        val tab = BrowserTab(nextTabId++, webView)
        tab.isIncognito = incognito
        webView.webViewClient = BrowserWebViewClient(tab)
        webView.webChromeClient = BrowserChromeClient(tab)
        webView.setDownloadListener { downloadUrl, _, _, mimeType, _ ->
            handleDownload(downloadUrl, mimeType)
        }

        tabs.add(tab)
        webViewContainer.addView(
            webView,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )
        webView.visibility = View.GONE

        tabAdapter.notifyItemInserted(tabs.size - 1)
        updateTabStripVisibility()
        switchToTab(tabs.size - 1)
        webView.loadUrl(url)
    }

    private fun switchToTab(position: Int) {
        if (position !in tabs.indices) return
        tabs.forEachIndexed { index, tab -> tab.webView.visibility = if (index == position) View.VISIBLE else View.GONE }
        currentTabIndex = position
        tabAdapter.selectedPosition = position
        urlBar.setText(currentTab()?.url ?: "")
        currentTab()?.let { updatePrivacyBadge(it) }
    }

    private fun closeTab(position: Int) {
        if (position !in tabs.indices) return
        val oldCurrent = currentTabIndex
        val closingCurrent = position == oldCurrent
        val tab = tabs.removeAt(position)
        cookieWipeTasks.remove(tab.id)?.let(cookieWipeHandler::removeCallbacks)
        if (tab.isIncognito) {
            CookiePolicy.wipeOrigins(tab.visitedOrigins)
            tab.webView.clearCache(true)
            tab.webView.clearHistory()
        }
        webViewContainer.removeView(tab.webView)
        tab.webView.destroy()
        tabAdapter.notifyItemRemoved(position)
        updateTabStripVisibility()

        if (tabs.isEmpty()) {
            currentTabIndex = 0
            openNewTab(HOME_URL)
            return
        }
        val newPosition = when {
            closingCurrent -> position.coerceAtMost(tabs.lastIndex)
            position < oldCurrent -> (oldCurrent - 1).coerceIn(0, tabs.lastIndex)
            else -> oldCurrent.coerceIn(0, tabs.lastIndex)
        }
        switchToTab(newPosition)
    }

    private fun currentTab(): BrowserTab? = tabs.getOrNull(currentTabIndex)

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
            looksLikeUrl -> "https://$input"
            else -> SEARCH_URL_PREFIX + Uri.encode(input)
        }
        return TrackingParamStripper.clean(resolved)
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

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val original = request.url.toString()
            val cleaned = TrackingParamStripper.clean(original)
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
            updatePrivacyBadge(tab)
            if (prefs.getBoolean(KEY_FRESH_IDENTITY, true) &&
                !WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
            ) {
                // Older WebView: no true document-start hook, so this is a
                // best-effort injection as early in the load as we can get.
                view.evaluateJavascript(FingerprintRandomizer.script(tab.id), null)
            }
        }

        override fun onPageFinished(view: WebView, url: String?) {
            super.onPageFinished(view, url)
            progressBar.visibility = View.GONE
            // Phase 2: heuristically auto-dismiss common cookie-consent banners.
            view.evaluateJavascript(ConsentAutoHandler.SCRIPT, null)
            if (url != null) {
                tab.url = url
                val title = view.title ?: url
                tab.title = title
                if (!tab.isIncognito) db.addHistoryEntry(title, url) // Phase 2: incognito never touches history
                val idx = tabs.indexOf(tab)
                if (idx != -1) tabAdapter.notifyItemChanged(idx)
            }
            tab.detectedVideoUrls.clear()
            view.evaluateJavascript(VideoDetector.SCRIPT) { raw ->
                tab.detectedVideoUrls.addAll(VideoDetector.parse(raw))
            }
            scheduleCookieWipe(tab)
            updatePrivacyBadge(tab) // Phase 3: finalize once HTTPS/tracker state has settled
        }
    }

    private inner class BrowserChromeClient(private val tab: BrowserTab) : android.webkit.WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            if (tab == currentTab()) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
            }
        }

        override fun onPermissionRequest(request: android.webkit.PermissionRequest) {
            // Phase 4 safety default: never grant camera/mic to web content.
            request.deny()
        }

        override fun onGeolocationPermissionsShowPrompt(origin: String, callback: android.webkit.GeolocationPermissions.Callback) {
            // Location is deliberately denied; Android WebView does not provide a reliable per-tab revoke API.
            callback.invoke(origin, false, false)
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
        val id = VideoDownloadsActivity.enqueue(this, url, mimeType)
        Toast.makeText(this, if (id != null) "Video download queued" else "Download could not be started", Toast.LENGTH_SHORT).show()
    }

    private fun openDetectedVideoDownloads() {
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
        globalVisitedOrigins.clear()
        PanicManager.wipe(this, tabs, db)
        tabs.forEach { webViewContainer.removeView(it.webView); it.webView.destroy() }
        tabs.clear()
        currentTabIndex = 0
        tabAdapter.notifyDataSetChanged()
        openNewTab(HOME_URL)
        Toast.makeText(this, "Browsing data cleared", Toast.LENGTH_SHORT).show()
        moveTaskToBack(true)
    }

    // ---------- Result callbacks / back press ----------

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        val url = when (requestCode) {
            REQ_BOOKMARKS -> data.getStringExtra(BookmarksActivity.EXTRA_RESULT_URL)
            REQ_HISTORY -> data.getStringExtra(HistoryActivity.EXTRA_RESULT_URL)
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
        if (::adBlocker.isInitialized) adBlocker.reloadCustomHosts()
        if (prefs.getBoolean("biometric_lock", false) && !biometricPromptShowing) {
            biometricPromptShowing = true
            BiometricLock.authenticate(this, { biometricPromptShowing = false }, {
                biometricPromptShowing = false
                Toast.makeText(this, "Unlock cancelled", Toast.LENGTH_SHORT).show()
                moveTaskToBack(true)
            })
        }
    }

    override fun onDestroy() {
        cookieWipeTasks.values.forEach(cookieWipeHandler::removeCallbacks)
        cookieWipeTasks.clear()
        CookiePolicy.purgeConsentCookies(globalVisitedOrigins)
        tabs.forEach { it.webView.destroy() }
        super.onDestroy()
    }
}
