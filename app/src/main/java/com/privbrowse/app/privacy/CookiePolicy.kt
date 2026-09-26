package com.privbrowse.app.privacy

import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView

/**
 * Cookie/privacy policy helpers. Android WebView has one cookie jar per data
 * directory, so per-tab incognito is implemented as ephemeral cleanup rather
 * than pretending that WebViews inside one process have independent jars.
 */
object CookiePolicy {
    private val CONSENT_COOKIE_NAMES = listOf(
        "OptanonConsent", "OptanonAlertBoxClosed", "CookieConsent", "cookieyes-consent",
        "euconsent-v2", "didomi_token", "euconsent", "notice_behavior", "notice_gdpr_prefs",
        "cookie_consent_level", "cookie_consent_user_accepted"
    )

    fun configureWebView(webView: WebView, blockThirdPartyCookies: Boolean = true, acceptCookies: Boolean = true) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(acceptCookies)
        cookieManager.setAcceptThirdPartyCookies(webView, acceptCookies && !blockThirdPartyCookies)
    }

    private fun cookieNamesForOrigin(cookieHeader: String?): Set<String> =
        cookieHeader.orEmpty().split(';')
            .mapNotNull { it.trim().substringBefore('=').trim().takeIf(String::isNotBlank) }
            .toSet()

    private fun normalizeOrigin(raw: String): String? {
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        val host = uri.host ?: return null
        if (scheme != "http" && scheme != "https") return null
        return "$scheme://$host"
    }

    fun wipeOrigins(origins: Collection<String>) {
        val cookieManager = CookieManager.getInstance()
        origins.mapNotNull(::normalizeOrigin).distinct().forEach { origin ->
            val names = cookieNamesForOrigin(cookieManager.getCookie(origin))
            names.forEach { name ->
                cookieManager.setCookie(origin, "$name=; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/")
                cookieManager.setCookie(origin, "$name=; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/; domain=${Uri.parse(origin).host}")
            }
            WebStorage.getInstance().deleteOrigin(origin)
        }
        cookieManager.flush()
    }

    fun purgeConsentCookies(visitedOrigins: Collection<String>) {
        val cookieManager = CookieManager.getInstance()
        visitedOrigins.mapNotNull(::normalizeOrigin).distinct().forEach { origin ->
            CONSENT_COOKIE_NAMES.forEach { name ->
                cookieManager.setCookie(origin, "$name=; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/")
            }
        }
        cookieManager.flush()
    }
}
