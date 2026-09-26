package com.privbrowse.app.ui

import android.webkit.WebView
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** One open tab and its WebView-scoped state. */
class BrowserTab(val id: Long, val webView: WebView) {
    var title: String = "New Tab"
    var url: String = "https://duckduckgo.com"
    var isDesktopMode: Boolean = false
    var isIncognito: Boolean = false
    var isPinned: Boolean = false
    var isVault: Boolean = false
    var isHibernated: Boolean = false
    var groupName: String = ""
    var groupColor: Int = 0
    var lastActiveAt: Long = System.currentTimeMillis()
    var lastScrollY: Int = 0
    var resourcesThisPage: Int = 0
    var pageStartedAt: Long = 0L
    val visitedOrigins: MutableSet<String> = mutableSetOf()

    var pageOrigin: String = ""
    private val trackersBlocked = AtomicInteger(0)
    private val fingerprintBlocked = AtomicInteger(0)
    val loggedHostsThisPage: MutableSet<String> = ConcurrentHashMap.newKeySet()
    val detectedVideoUrls: MutableList<String> = mutableListOf()

    val trackersBlockedThisPage: Int get() = trackersBlocked.get()
    val fingerprintBlockedThisPage: Int get() = fingerprintBlocked.get()

    fun incrementTracker(fingerprint: Boolean) {
        trackersBlocked.incrementAndGet()
        if (fingerprint) fingerprintBlocked.incrementAndGet()
    }

    fun resetPageStats() {
        trackersBlocked.set(0)
        fingerprintBlocked.set(0)
        loggedHostsThisPage.clear()
        resourcesThisPage = 0
        pageStartedAt = System.currentTimeMillis()
    }
}
