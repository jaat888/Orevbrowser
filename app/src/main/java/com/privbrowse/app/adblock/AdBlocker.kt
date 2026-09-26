package com.privbrowse.app.adblock

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

/** Phase 3 - which kind of thing a blocked host was, for logging/scoring purposes. */
enum class BlockCategory { TRACKER, FINGERPRINT }

/** Three simple strictness levels a user actually understands, cycled from one menu row. */
enum class BlockLevel { OFF, NORMAL, STRICT }

/**
 * Phase 1 baseline ad-blocker, extended in Phase 3 to distinguish plain
 * trackers from known fingerprinting/anti-fraud vendors so the Network
 * Transparency Log and Weekly Privacy Report can report each honestly.
 *
 * Loads flat host lists bundled in assets/ and blocks any request whose
 * host matches (or is a subdomain of) an entry in one of them.
 *
 * This is intentionally simple (host matching, not full filter-list syntax
 * like uBlock/EasyList rules) - good enough for Phase 1's baseline goal.
 * Phase 4 adds custom filter-list import for power users.
 *
 * Level is persisted locally so a choice made from the menu survives an
 * app restart:
 *  - OFF: nothing is blocked.
 *  - NORMAL: tracker + fingerprinting hosts (the original Phase 1/3 lists).
 *  - STRICT: NORMAL plus social-widget / share-button / video-ad-network
 *    hosts (assets/strict_hosts.txt) - more aggressive, can occasionally
 *    break an embedded widget on a page.
 */
class AdBlocker(private val context: Context) {

    private val baseBlockedHosts: HashSet<String> = HashSet()
    private val fingerprintHosts: HashSet<String> = HashSet()
    private val strictHosts: HashSet<String> = HashSet()
    @Volatile private var blockedHostsSnapshot: Set<String> = emptySet()
    @Volatile private var customHosts: Set<String> = emptySet()

    @Volatile var level: BlockLevel = BlockLevel.NORMAL
        private set

    /** Kept for any older call site; derived from level. */
    val enabled: Boolean get() = level != BlockLevel.OFF

    init {
        loadHostList(context, "adblock_hosts.txt", baseBlockedHosts)
        loadHostList(context, "fingerprint_hosts.txt", fingerprintHosts)
        loadHostList(context, "strict_hosts.txt", strictHosts)
        level = readLevel()
        reloadCustomHosts()
    }

    private fun prefs() = context.getSharedPreferences("privbrowse_custom_filters", Context.MODE_PRIVATE)

    private fun readLevel(): BlockLevel = when (prefs().getString("adblock_level", "NORMAL")) {
        "OFF" -> BlockLevel.OFF
        "STRICT" -> BlockLevel.STRICT
        else -> BlockLevel.NORMAL
    }

    /** Reloads the persisted mode after another screen changes it. */
    fun refreshFromPreferences() {
        level = readLevel()
        reloadCustomHosts()
    }

    /** Cycles OFF -> NORMAL -> STRICT -> OFF, persists it, and returns the new level. */
    fun cycleLevel(): BlockLevel {
        setLevel(
            when (level) {
                BlockLevel.OFF -> BlockLevel.NORMAL
                BlockLevel.NORMAL -> BlockLevel.STRICT
                BlockLevel.STRICT -> BlockLevel.OFF
            }
        )
        return level
    }

    fun setLevel(newLevel: BlockLevel) {
        level = newLevel
        prefs().edit().putString("adblock_level", newLevel.name).apply()
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
     * effects - callers own their own counters so the same classification
     * can drive blocking, per-page tracker counts, and the persisted
     * Network Transparency Log without triple-counting anything.
     * Checked in fingerprint-first order so a host on both lists is
     * reported as the more specific, more concerning classification.
     */
    fun reloadCustomHosts() {
        customHosts = readCustomHosts()
        blockedHostsSnapshot = buildSet {
            addAll(baseBlockedHosts)
            addAll(customHosts)
            if (level == BlockLevel.STRICT) addAll(strictHosts)
        }
    }

    private fun readCustomHosts(): Set<String> =
        context.getSharedPreferences("privbrowse_custom_filters", Context.MODE_PRIVATE)
            .getStringSet("hosts", emptySet()).orEmpty()
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()

    fun classify(host: String): BlockCategory? {
        if (level == BlockLevel.OFF) return null
        val lower = host.lowercase()
        return when {
            isHostListed(lower, fingerprintHosts) -> BlockCategory.FINGERPRINT
            isHostListed(lower, blockedHostsSnapshot) -> BlockCategory.TRACKER
            level == BlockLevel.STRICT && isHostListed(lower, strictHosts) -> BlockCategory.TRACKER
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
