package com.privbrowse.app.adblock

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

/** Phase 3 — which kind of thing a blocked host was, for logging/scoring purposes. */
enum class BlockCategory { TRACKER, FINGERPRINT }

/**
 * Phase 1 baseline ad-blocker, extended in Phase 3 to distinguish plain
 * trackers from known fingerprinting/anti-fraud vendors so the Network
 * Transparency Log and Weekly Privacy Report can report each honestly.
 *
 * Loads two flat host lists bundled in assets/ and blocks any request whose
 * host matches (or is a subdomain of) an entry in either list.
 *
 * This is intentionally simple (host matching, not full filter-list syntax
 * like uBlock/EasyList rules) — good enough for Phase 1's "baseline filter
 * list" goal. Phase 4 adds custom filter-list import for power users.
 */
class AdBlocker(private val context: Context) {

    private val baseBlockedHosts: HashSet<String> = HashSet()
    private val fingerprintHosts: HashSet<String> = HashSet()
    @Volatile private var blockedHostsSnapshot: Set<String> = emptySet()
    @Volatile private var customHosts: Set<String> = emptySet()
    @Volatile var enabled: Boolean = true

    init {
        loadHostList(context, "adblock_hosts.txt", baseBlockedHosts)
        loadHostList(context, "fingerprint_hosts.txt", fingerprintHosts)
        reloadCustomHosts()
    }

    private fun loadHostList(context: Context, assetName: String, target: HashSet<String>) {
        try {
            context.assets.open(assetName).bufferedReader().useLines { lines ->
                lines.forEach { raw ->
                    val line = raw.trim()
                    if (line.isNotEmpty() && !line.startsWith("#")) {
                        target.add(line.lowercase())
                    }
                }
            }
        } catch (e: Exception) {
            // If the asset is missing for some reason, fail open rather than crash.
        }
    }

    /**
     * Which category (if any) this host falls under. Pure lookup, no side
     * effects — callers own their own counters so the same classification
     * can drive blocking, per-page tracker counts, and the persisted
     * Network Transparency Log without triple-counting anything.
     * Checked in fingerprint-first order so a host on both lists is
     * reported as the more specific, more concerning classification.
     */
    fun reloadCustomHosts() {
        customHosts = readCustomHosts()
        blockedHostsSnapshot = buildSet { addAll(baseBlockedHosts); addAll(customHosts) }
    }

    private fun readCustomHosts(): Set<String> =
        context.getSharedPreferences("privbrowse_custom_filters", Context.MODE_PRIVATE)
            .getStringSet("hosts", emptySet()).orEmpty()
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()

    fun classify(host: String): BlockCategory? {
        if (!enabled) return null
        val lower = host.lowercase()
        return when {
            isHostListed(lower, fingerprintHosts) -> BlockCategory.FINGERPRINT
            isHostListed(lower, blockedHostsSnapshot) -> BlockCategory.TRACKER
            else -> null
        }
    }

    /**
     * Returns true if this request's host is on either block list.
     */
    fun shouldBlock(request: WebResourceRequest): Boolean {
        val host = request.url?.host ?: return false
        return classify(host) != null
    }

    private fun isHostListed(host: String, list: Set<String>): Boolean {
        if (list.contains(host)) return true
        // Check parent domains too, e.g. sub.doubleclick.net -> doubleclick.net
        var idx = host.indexOf('.')
        while (idx != -1) {
            val parent = host.substring(idx + 1)
            if (list.contains(parent)) return true
            idx = host.indexOf('.', idx + 1)
        }
        return false
    }

    /**
     * An empty response used to swallow a blocked request without breaking
     * the page (empty 200 rather than a broken/failed resource load).
     */
    fun emptyResponse(): WebResourceResponse {
        return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
    }
}
