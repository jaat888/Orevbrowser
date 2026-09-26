# PrivBrowse 1.5.0 — Feature Matrix

This revision treats the supplied wishlist as a complete implementation audit rather than a partial feature-demo list. The **Ultimate Feature Lab** exposes the new functionality while the existing **Feature Center** retains the broader browser/privacy/AI catalogue.

## Feature families

| Family | Included |
|---|---|
| Tabs | Grid + search, live thumbnails, list/grid switch, groups/colors, vertical rail, pinning, duplicate, preview, recently closed, auto-close, self-destruct, biometric vault, random close, multi-window and split compare |
| Search | Live suggestions, bangs, custom keywords, recent/frequent dashboard, search-engine comparison, calculator/unit/currency answers and QR URL sharing |
| Privacy | Ghost mode, randomized web geolocation, site isolation controls, tracker/ad blocking, per-page score, weekly report, UID network accounting, screenshot protection, clipboard wipe, guest/profile separation, Wi-Fi warning, leak tests and VPN fail-closed mode |
| Content/media | PDF annotation, PiP, gallery, TTS, media speed, audio-only mode, ad-skip attempt, subtitles, long screenshot, reverse-image handoff, background audio |
| Productivity | Sticky overlay notes, web clipper, dashboard, RSS, event/calendar helper, compare/diff, translation, meeting-link detection, bookmark todos and print-friendly extraction |
| AI | Floating page AI bubble, AI summary/actions, provider/model workspace, Q&A history, image alt-text, metadata/tag assistance and safe non-silent form/privacy behavior |
| Downloads | Persistent queue, resume, category folders, speed limiter, heuristic safety scan and optional external magnet/torrent handoff |
| Alerts | Price/page/keyword watchers with local alarms + notifications |
| Accessibility/customization | Dyslexia font, high contrast, one-handed, large touch targets, per-site CSS/JS, icon aliases, media notification controls, gesture editor and bundled/imported themes |
| Sync/backup | Offline QR tabs, Android local-share transfer, daily local backup, typed compressed import/export |
| Power-user | Inspect/source/DOM tools, user-agent switching, per-tab diagnostics, VPN/Xray tools and Ctrl+K command palette |
| Stability | Crash logs, session recovery, tab hibernation, retry pages, pull-refresh and CI build workflow |

## Engineering rules used in this revision

1. No wishlist control is represented as a fake “coming soon” button.
2. When Android WebView cannot expose a requested low-level capability, the UI uses the nearest honest Android/platform flow and the exact boundary is written in `WISHLIST_IMPLEMENTATION.md`.
3. Privacy-sensitive workflows stay local/offline when possible: QR generation is local, notes/bookmarks/todos are local, backups are local, and provider API keys remain user-controlled.
4. Existing browser behavior is preserved rather than replaced with a parallel browser engine.

## Build verification

The repository contains a CI workflow at `.github/workflows/android.yml` that runs `lintDebug` and `assembleDebug` on every push/PR. This packaging container lacks the Android SDK/Gradle toolchain and the native `libXray.aar`, so the final ZIP is statically audited and ZIP-validated here rather than falsely reported as locally APK-built.
