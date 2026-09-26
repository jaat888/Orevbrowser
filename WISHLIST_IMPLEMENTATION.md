# PrivBrowse 1.5.0 — Full Wishlist Implementation Audit

This document maps the complete wishlist line-by-line to the shipped implementation. The goal of this build is **real behavior, not decorative buttons**. Where Android/WebView fundamentally cannot expose a capability, the UI uses the closest safe platform hand-off and this document says exactly what the boundary is.

## Core browsing / tabs

| Wishlist item | Status | Implementation |
|---|---|---|
| Tab grid/thumbnail switcher + tab search | ✅ | Searchable grid/list tab manager with live WebView thumbnails. |
| Tab groups, named + color-coded | ✅ | Long-press tab → group name + six colors. |
| Vertical tab bar | ✅ | True vertical RecyclerView rail, enabled for tablets or manually. |
| Recently closed / undo close | ✅ | Persistent local recently-closed queue; reopen after restart. |
| Duplicate tab | ✅ | Duplicates URL + desktop/private state. |
| Long-press tab preview | ✅ | Thumbnail preview dialog. |
| Auto-close inactive tabs after X hours | ✅ | Per-tab timer, skipping pinned/private tabs. |
| Pinned tabs | ✅ | Pinned tabs resist normal close operations. |
| Swipe address bar to switch tabs | ✅ | Horizontal swipe on address bar cycles tabs. |
| Android multi-window | ✅ | New-document task opens a second PrivBrowse window; Android split-screen can place both side by side. |
| Floating browser bubble | ✅ | Foreground overlay service with Android overlay permission. |
| Full edge-swipe navigation | ✅ | Edge gesture back/forward with adjacent-tab fallback. |
| Split-screen dual WebView | ✅ | Dedicated Compare screen with two independently navigable WebViews. |
| Self-destruct tabs | ✅ | Configurable countdown; forced close wipes visited origin data/cache/history for that tab. |
| Hidden vault tabs + biometric | ✅ | Vault tabs are not rendered while locked and require biometric unlock. |
| Random tab close order | ✅ | Randomized order for bulk-close action. |

## Address bar / search

| Wishlist item | Status | Implementation |
|---|---|---|
| Live search suggestions | ✅ | Local history/bookmarks/recently-closed suggestions while typing. |
| Bang commands | ✅ | `!yt`, `!wiki`, `!gmaps`, `!ddg` plus custom templates. |
| Smart clipboard link prompt | ✅ | Opt-in HTTP/HTTPS clipboard detection prompt. |
| Custom keyword site-search shortcuts | ✅ | Editable JSON or `key=url-template` syntax. |
| New-tab recent + frequently visited grid | ✅ | Built-in data-URL dashboard from local history. |
| Multiple search engines side-by-side | ✅ | Compare screen for Google and Brave search; normal search engine chooser also retained. |
| Deletable per-entry search/history records | ✅ | Existing history UI supports individual deletion. |
| Instant calculator | ✅ | Local expression answers in address bar tools. |
| Unit conversion | ✅ | Common length, mass and temperature conversions. |
| Currency conversion | ✅ | Address-bar expression plus live Frankfurter lookup when online. |
| QR code from current URL | ✅ | Offline ZXing encode screen; no URL is sent to a QR website. |

## Privacy & security

| Wishlist item | Status | Implementation |
|---|---|---|
| Weekly tracker/network report | ✅ | Seven-day blocked-host/fingerprint report plus Android UID RX/TX snapshot accounting. |
| Fake/random GPS per site | ✅ | Website-facing `navigator.geolocation` is replaced by a stable randomized per-origin coordinate. Device GPS is not changed. |
| Email alias/masking | ✅ | Generates unique plus-addresses locally for services supporting plus addressing; it does not create a mailbox. |
| Local DoH provider chooser | ⚠️ | Provider preference + Android Private DNS settings path. WebView itself continues to use OS DNS; no fake DoH claim. |
| Full site isolation | ✅/⚠️ | Third-party cookies are blocked and top-level origin changes can wipe prior visited-origin data when isolation is enabled. Full Chromium Site Isolation is not exposed by Android WebView. |
| Ghost mode / RAM-first session | ✅ | No-cache/no-history/clear-on-exit privacy preset; sensitive session data is not persisted into crash/session restore. |
| Malicious download scanner | ✅ | Local heuristic scan checks dangerous extensions, executable signatures and script MIME combinations before marking a download clean/suspicious. |
| FLAG_SECURE | ✅ | App-level screenshot/screen-record blocking toggle. |
| Clipboard auto-clear | ✅ | Configurable 5–300 second local wipe after copying. |
| Guest mode | ✅ | Cleanup flow + fresh private tab. |
| Multiple profiles | ✅ | Personal/work/guest profiles use separate WebView data-directory suffixes and separate browser DBs; restart is used to apply a profile. |
| Unsecured Wi-Fi warning | ✅ | Warns when the active Wi-Fi network is not Android-validated. |
| WebRTC/DNS leak test | ✅ | One action opens browserleaks WebRTC and DNS leak pages. |
| Auto-revoke camera/mic after session | ⚠️ | App denies subsequent WebView camera/mic requests after backgrounding; Android does not permit an ordinary app to silently revoke the OS runtime grant. |
| Weekly trust score | ✅ | Weekly privacy dashboard and existing per-page privacy score. |
| VPN kill switch | ✅/⚠️ | When enabled, WebView traffic is fail-closed unless Android reports an active VPN. It is not a device-wide firewall. Android VPN lockdown settings are also exposed. |

## Content & media

| Wishlist item | Status | Implementation |
|---|---|---|
| PDF viewer + annotate/highlight | ✅ | Local multipage PDF viewer, freehand annotations, per-page navigation and annotated PDF export. |
| Floating PiP video | ✅ | Android Picture-in-Picture entry for detected video pages. |
| Full page gallery | ✅ | Collects image URLs from the current document into a clean gallery tab. |
| Audio-only video mode | ✅/⚠️ | Page JavaScript attempts to suppress video visuals while retaining media; site-specific players can limit the result. |
| Text-to-speech | ✅ | Extracts page text and speaks it through Android TTS. |
| Auto ad-skip | ✅/⚠️ | Attempts common skip controls and video-ad selectors where page JavaScript permits. |
| Video speed 0.5x–3x | ✅ | Injected media-rate controls. |
| Subtitle/caption downloader | ✅ | Detects HTML5 `<track>` candidates and hands them to the local download queue. |
| Full-page screenshot | ✅ | Long-page capture with safe temporary resize and restoration of the original WebView layout. |
| Reverse image search from page image | ✅ | Detects the first page image and sends its URL to Google Lens; falls back to the page URL. |
| Dark-mode image dimming | ✅ | Existing page darkening controls remain available. |
| Background audio | ✅ | Foreground media service with pause/resume/stop notification controls for direct media URLs. |

## Productivity

| Wishlist item | Status | Implementation |
|---|---|---|
| Sticky notes overlay | ✅ | Real floating on-page note editor; saved notes stay attached to the current URL. |
| Web clipper | ✅ | Selected/readable text or screenshot clip can be exported through Android sharing. |
| Home dashboard widgets | ✅ | Weather, local todo, RSS/Atom feed entry and recent/frequent browsing. |
| RSS/Atom reader | ✅ | Local feed reader with refreshable feed content. |
| Event → calendar | ✅ | Extracts heading/time metadata where available and prefills Android Calendar insert. |
| Two-tab compare/diff | ✅ | Two WebViews side by side with page text comparison. |
| Selection translation | ✅ | Page translation and Android share/translate flow remain available. |
| Currency/unit overlay | ✅ | Instant address/tool converters. |
| Zoom/Meet one-tap join | ✅ | Detects matching Zoom/Google Meet links and opens the installed handler. |
| Bookmark to-do | ✅ | Long-press bookmark → save a local todo attached to the bookmark. |
| Print-friendly mode | ✅ | Strips navigation noise and builds a clean printable document. |

## AI-powered

| Wishlist item | Status | Implementation |
|---|---|---|
| On-page AI chat bubble with page context | ✅ | Optional `AI` floating button over the active WebView opens the existing AI Copilot with current title/URL/page text. |
| Summarize long articles | ✅ | Existing AI Copilot page-summary action. |
| AI junk/ad stripping | ✅/⚠️ | Local heuristic page cleanup toggle removes common overlays; no model call required. |
| AI-assisted form autofill | ⚠️ | Existing browser intentionally keeps legacy password/form helpers controlled by privacy settings; AI does not silently inject saved personal data into arbitrary forms. |
| Voice browsing | ✅ | Android speech recognition handles navigation commands, search and URL entry. |
| Multi-model AI provider switcher | ✅ | Existing AI workspace supports the configured provider ecosystem and custom OpenAI-compatible endpoints. |
| Saved AI page Q&A history | ✅ | Existing AI Copilot history/export. |
| AI metadata/tags for bookmarks | ✅/⚠️ | Local automatic metadata/tag generation is active; AI is not invoked automatically on every bookmark. |
| AI image alt-text | ✅ | Current-page image metadata can be sent to AI Copilot with an accessibility-focused prompt. |
| AI auto-categorized bookmarks | ✅/⚠️ | Local token-based category/tag generation is automatic; provider-backed AI categorization is available through the AI workspace instead of silently sending bookmarks. |

## Downloads

| Wishlist item | Status | Implementation |
|---|---|---|
| Batch download queue | ✅ | Persistent queue processed serially by a foreground service. |
| Speed limiter | ✅ | Bytes/second limiter. `0` means unlimited. |
| Folder categorization | ✅ | Images/video/docs/other folders. |
| Resume after restart | ✅ | Range requests continue from an existing partial target; app launch restarts queued/downloading jobs. |
| Magnet/torrent links | ✅/⚠️ | Optional off-by-default handoff to an installed torrent handler; PrivBrowse does not bundle a torrent engine. |

## Notifications / alerts

| Wishlist item | Status | Implementation |
|---|---|---|
| Price-drop tracker | ✅ | Local watcher checks the page periodically and compares detected price values. |
| Page-change detector | ✅ | SHA-256 page-content hash watcher. |
| Keyword alert | ✅ | Local case-insensitive page keyword watcher. |

## Accessibility

| Wishlist item | Status |
|---|---|
| Dyslexia-friendly font | ✅ |
| High-contrast mode | ✅ |
| One-handed mode | ✅ |
| Large touch targets | ✅ |

## Customization

| Wishlist item | Status | Implementation |
|---|---|---|
| Per-site CSS | ✅ | Local domain rules injected at page finish. |
| Per-site JS/userscripts | ✅ | Local domain snippets injected at page finish. |
| App icon changer | ✅ | Android activity aliases for bundled launcher icons. |
| Notification-bar mini media controls | ✅ | Foreground background-audio notification provides media actions. |
| Custom gesture editor | ✅ | User assigns left/right/up/down swipe actions. |
| Theme marketplace/import | ✅/⚠️ | Bundled theme library plus local JSON import; no remote marketplace is required. |

## Sync / backup

| Wishlist item | Status | Implementation |
|---|---|---|
| QR-code tab transfer | ✅ | Offline ZXing package encode/decode; URLs stay in the QR payload. |
| Bluetooth / Wi-Fi Direct local sync | ⚠️ | Uses Android's local share/Quick Share handoff for the same no-server workflow; direct Bluetooth/Wi-Fi Direct protocol is not invented as a fake feature. |
| Scheduled local auto-backup | ✅ | Daily local compressed settings backup; Ghost mode disables it. |

## Power-user

| Wishlist item | Status | Implementation |
|---|---|---|
| Mini developer tools / inspect | ✅ | Page source, DOM/text extraction and diagnostics tools. |
| Per-site user-agent | ✅ | Desktop/mobile switching per tab. |
| Per-tab network request log | ✅/⚠️ | Per-tab resource counters + local transparency log/origin diagnostics; Android WebView does not expose Chrome DevTools' full request inspector. |
| Proxy chaining | ⚠️ | Existing VPN/Xray controls are retained; true multi-hop proxy chaining requires lower-level network plumbing than WebView exposes. |
| Ctrl+K command palette | ✅ | Browser-level keyboard shortcut plus common actions. |

## Misc / system

| Wishlist item | Status | Implementation |
|---|---|---|
| Offline mode | ✅ | Cache-only/offline toggle plus locally saved MHTML archive path. |
| Per-tab battery/data usage | ⚠️ | WebView does not provide trustworthy per-tab battery/byte accounting; diagnostics expose resource count/page timing, while the weekly dashboard reports app-UID traffic. |
| Auto low-power mode | ✅ | Applies low-battery animation throttling to active pages. |
| Full settings export/import | ✅ | Typed compressed JSON backup with import/export document picker. |
| Scheduled tasks / midnight clear | ✅ | Alarm-based midnight history cleanup plus scheduled backup/watchers. |
| Reading streak | ✅ | Daily reading activity counter with achievement badge. |
| Ads/trackers blocked counter | ✅ | Persistent tracker/ad-host block counter in the privacy dashboard. |
| Privacy achievements/badges | ✅ | Local badge storage; current bundled achievement includes the seven-day reading streak. |

## Stability

| Wishlist item | Status | Implementation |
|---|---|---|
| Crash-safe session recovery after OOM/kill | ✅ | Session snapshot persists normal tabs; incognito/vault tabs are excluded. |
| Local crash log export | ✅ | Application-level uncaught-exception recorder with export/copy action. |
| Background tab hibernation | ✅ | Inactive WebViews are paused and marked hibernated while switching. |
| Retry-able error pages | ✅ | Existing implementation retained. |
| Pull-to-refresh | ✅ | Existing implementation retained. |
| CI lint + assembleDebug | ✅ | `.github/workflows/android.yml` runs both on every push/PR using Gradle 8.7 setup. |

## Verification boundary

The packaging environment used for this ZIP does not contain an Android SDK/Gradle installation or the proprietary/native `libXray.aar` dependency, so a real APK compile/lint run cannot be performed inside this container. The source has therefore been checked with project-level static audits, XML/manifest parsing, duplicate-reference scans and ZIP integrity checks, and the included CI workflow is the authoritative Android build path.
