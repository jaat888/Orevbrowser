package com.privbrowse.app.tor

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import com.privbrowse.app.v2ray.WebViewProxy

/**
 * Real, honest Tor support: PrivBrowse does not bundle or reimplement Tor
 * itself (that would mean shipping our own unaudited crypto/consensus code,
 * which is a bad idea for something people's safety depends on). Instead
 * this routes browser traffic through Orbot — the Guardian Project's
 * well-established, open-source Tor client for Android — the same way it
 * already routes traffic through a V2Ray tunnel: via WebView's per-app
 * SOCKS proxy override, pointed at Orbot's local SOCKS port.
 *
 * This is genuinely real: once Orbot reports it is running, requests from
 * PrivBrowse's WebView actually go through it, through the real Tor
 * network. Nothing here pretends to protect traffic that isn't protected.
 */
object TorBridge {
    const val ORBOT_PACKAGE = "org.torproject.android"

    // Orbot's default local SOCKS port. The user can change this in Orbot's
    // own settings; if they have, this needs to match (surfaced as a
    // setting rather than hard-coded, see VpnActivity).
    const val DEFAULT_SOCKS_PORT = 9050

    fun isOrbotInstalled(context: Context): Boolean =
        try { context.packageManager.getPackageInfo(ORBOT_PACKAGE, 0); true }
        catch (e: Exception) { false }

    /** Opens Orbot so the user can start Tor there (we don't fake-start it for them). */
    fun openOrbot(context: Context) {
        context.packageManager.getLaunchIntentForPackage(ORBOT_PACKAGE)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    }

    fun playStoreUrl() = "https://play.google.com/store/apps/details?id=$ORBOT_PACKAGE"
    fun fdroidUrl() = "https://f-droid.org/packages/$ORBOT_PACKAGE/"

    /**
     * Checks that Orbot's SOCKS port is actually accepting connections right
     * now, on-device, over loopback — not a guess, an actual TCP probe. Must
     * be called off the main thread.
     */
    fun isOrbotReachable(port: Int = DEFAULT_SOCKS_PORT): Boolean = try {
        java.net.Socket().use { it.connect(java.net.InetSocketAddress("127.0.0.1", port), 800) }
        true
    } catch (e: Exception) {
        false
    }

    /** Routes this app's WebView traffic through Orbot's SOCKS port. Call only after isOrbotReachable() is true. */
    fun applyProxy(context: Context, port: Int = DEFAULT_SOCKS_PORT) = WebViewProxy.apply(context, port)

    fun clearProxy(context: Context) = WebViewProxy.clear(context)
}
