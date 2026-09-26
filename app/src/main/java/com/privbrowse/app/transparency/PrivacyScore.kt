package com.privbrowse.app.transparency

import com.privbrowse.app.R

/**
 * Phase 3 — per-site Privacy Score.
 *
 * A simple, transparent point score (not a black-box model) computed from
 * three signals already tracked elsewhere in the app: whether the
 * connection is HTTPS, how many trackers/fingerprinting scripts were
 * blocked while loading this page, and whether third-party cookies are
 * blocked (always true today — see privacy/CookiePolicy.kt — but kept as
 * an explicit factor so the score stays honest if that ever becomes
 * configurable in a later phase).
 */
object PrivacyScore {

    data class Result(val grade: String, val colorRes: Int)

    fun compute(isHttps: Boolean, trackersBlocked: Int, thirdPartyCookiesBlocked: Boolean): Result {
        var points = 0
        if (isHttps) points += 40
        if (thirdPartyCookiesBlocked) points += 20
        points += when {
            trackersBlocked <= 0 -> 40
            trackersBlocked <= 2 -> 30
            trackersBlocked <= 5 -> 20
            trackersBlocked <= 10 -> 10
            else -> 0
        }
        return when {
            points >= 90 -> Result("A", R.color.grade_a)
            points >= 75 -> Result("B", R.color.grade_b)
            points >= 55 -> Result("C", R.color.grade_c)
            points >= 35 -> Result("D", R.color.grade_d)
            else -> Result("F", R.color.grade_f)
        }
    }
}
