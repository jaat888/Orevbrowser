package com.privbrowse.app.privacy

import android.net.Uri

/**
 * Phase 2 — URL tracking-parameter stripping.
 *
 * Removes known tracking query params (utm_*, fbclid, gclid, ...) before a
 * URL is loaded or a link is followed. Pure string/URI work, nothing is
 * sent anywhere to do this (Zero-Cloud Guarantee).
 */
object TrackingParamStripper {

    private val TRACKED_PARAM_PREFIXES = listOf("utm_")

    private val TRACKED_PARAMS = setOf(
        "fbclid", "gclid", "gclsrc", "dclid", "msclkid", "yclid",
        "mc_eid", "mc_cid", "igshid", "igsh",
        "ref_src", "ref_url", "_ga", "_gl",
        "vero_id", "vero_conv",
        "twclid", "ttclid", "wbraid", "gbraid",
        "mkt_tok", "trk", "trkCampaign", "spm", "scm",
        "si"
    )

    fun clean(url: String): String {
        val uri = try {
            Uri.parse(url)
        } catch (e: Exception) {
            return url
        }
        if (uri.query.isNullOrEmpty()) return url

        val keptParams = uri.queryParameterNames.filterNot { isTrackingParam(it) }
        if (keptParams.size == uri.queryParameterNames.size) return url // nothing to strip

        val builder = uri.buildUpon().clearQuery()
        for (name in keptParams) {
            for (value in uri.getQueryParameters(name)) {
                builder.appendQueryParameter(name, value)
            }
        }
        return builder.build().toString()
    }

    private fun isTrackingParam(name: String): Boolean {
        val lower = name.lowercase()
        if (lower in TRACKED_PARAMS) return true
        return TRACKED_PARAM_PREFIXES.any { lower.startsWith(it) }
    }
}
