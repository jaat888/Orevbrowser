package com.privbrowse.app.privacy

/**
 * Phase 2 — "Session-only Cookie Consent" auto-handling.
 *
 * This is a heuristic, not a full Consent-O-Matic-style rules engine: it
 * looks for the reject/necessary-only buttons of the handful of CMPs that
 * cover most of the web (OneTrust, Cookiebot, Quantcast/IAB TCF, Didomi,
 * TrustArc) and clicks them so the user isn't nagged on every site. It runs
 * once per page load and fails silently if nothing matches.
 */
object ConsentAutoHandler {

    val SCRIPT = """
        (function() {
            try {
                var selectors = [
                    '#onetrust-reject-all-handler',
                    '.ot-pc-refuse-all-handler',
                    '#CybotCookiebotDialogBodyButtonDecline',
                    '.qc-cmp2-summary-buttons button[mode="secondary"]',
                    '#didomi-notice-disagree-button',
                    '.didomi-continue-without-agreeing',
                    '#truste-consent-required',
                    'button[data-testid="uc-deny-all-button"]',
                    'button[aria-label="Reject all"]',
                    'button[aria-label="Reject All"]'
                ];
                for (var i = 0; i < selectors.length; i++) {
                    var el = document.querySelector(selectors[i]);
                    if (el) { el.click(); return; }
                }
            } catch (e) { /* fail silent, never break the page */ }
        })();
    """.trimIndent()
}
