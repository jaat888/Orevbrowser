# PrivBrowse — Phases 1–9 (with documented platform scope)

**Phase 1 (Core Browsing):** multi-tab browsing, baseline ad-block (host
list), dark mode, bookmarks & history (local SQLite, no cloud), desktop-mode
toggle per tab, DuckDuckGo as default search/home.

**Phase 2 (Baseline Privacy Layer):**
- Third-party cookies blocked on every tab, always (`privacy/CookiePolicy.kt`).
- URL tracking-parameter stripping — utm_*, fbclid, gclid, etc. — on typed
  URLs, searches, and tapped links (`privacy/TrackingParamStripper.kt`).
- Private tabs (menu → "New private tab"): no history, no disk cache, and
  their cookies/local storage are wiped for every origin they visited the
  moment the tab closes.
- Heuristic cookie-banner auto-dismiss for OneTrust/Cookiebot/Quantcast/
  Didomi/TrustArc (`privacy/ConsentAutoHandler.kt`), plus known consent
  cookies are re-expired at app shutdown so "already answered" doesn't
  persist beyond the session (`CookiePolicy.purgeConsentCookies`).
- Zero-Cloud Guarantee: no analytics/telemetry SDKs anywhere in
  `app/build.gradle`; the ad-block list already covers the major analytics
  domains (Google Analytics, Mixpanel, Segment, Hotjar, ...).

**Phase 3 (Visibility & Trust Features):**
- Network Transparency Log — every tracker/fingerprinting host blocked,
  plus distinct third-party domains contacted, is recorded on-device
  (`data/DbHelper.kt`'s `network_log` table) and viewable newest-first from
  the overflow menu → "Network log" (`ui/TransparencyLogActivity.kt`), or
  filtered to just the current page from the privacy-score dialog below.
  Entries older than 30 days are pruned automatically on launch.
- Per-site Privacy Score — a badge in the address bar (tap for a
  breakdown) grades the current page A–F from HTTPS, tracker/fingerprint
  blocks this page-load, and third-party-cookie blocking
  (`transparency/PrivacyScore.kt`). `AdBlocker` now distinguishes plain
  trackers from known fingerprinting/anti-fraud vendors
  (`assets/fingerprint_hosts.txt`) so both the score and the log can be
  specific about what was actually stopped.
- Weekly Privacy Report — an inexact repeating `AlarmManager` alarm
  (`transparency/WeeklyReportScheduler.kt`) fires roughly every 7 days;
  `WeeklyReportReceiver` tallies blocked trackers/fingerprinting attempts
  from the local log since the last report and posts a notification
  ("N trackers blocked this week, M fingerprinting attempts stopped").
  `BootReceiver` re-arms the alarm after a reboot, since repeating alarms
  don't survive one. Needs notification permission on Android 13+, which
  `MainActivity` requests on first launch; if declined, the report simply
  isn't shown — the in-app log is unaffected either way.

**Phase 5 (VPN Gate discovery) — safe discovery-only mode.**
- Fetches VPN Gate's public server list.
- Ranks candidates by score/ping with a UDP preference.
- Shows the volunteer-relay disclosure in the UI.
- Does **not** establish a TUN or claim a connection without an embedded OpenVPN/OpenVPN3 engine; this avoids black-holing device traffic.

## Build locally
The Gradle wrapper binary (`gradlew` + `gradle-wrapper.jar`) isn't bundled —
open this folder in Android Studio once and it auto-generates both on sync.
Then:
```
./gradlew assembleDebug
```
APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Build via GitHub (no local Android Studio needed)
1. Push this folder as a git repo (not a zip) to a new GitHub repository.
2. GitHub Actions (`.github/workflows/build.yml`) runs automatically on push
   to `main` — it installs Gradle itself, so the missing wrapper doesn't
   matter — and attaches `privbrowse-debug-apk` to the workflow run.

## Extending
- Ad-block list: `app/src/main/assets/adblock_hosts.txt` (one host per line).
- Phases 4, 6, 7, 8 and 9 are now integrated; see the sections below for exact scope and platform limitations.

## Phase 4 — Advanced Privacy Controls (implemented)
- Domain-locked auto-incognito: configured hosts open in a private tab.
- Per-domain cookie auto-wipe timer: local timer wipes cookies/storage for the selected host.
- Local encrypted vault: small AES-GCM store backed by Android Keystore for user-entered secrets/API values.
- Custom ad-block host-list import: plain host lists can be imported into the on-device filter set.
- Reader mode: locally simplifies the current page in WebView; page text is not uploaded to a service.
- Breach checker UI: optional Have I Been Pwned API-key based check.
- Biometric app lock: enabled from the Phase 4 screen when the device exposes a compatible biometric authenticator.

**Phase 4 platform note:** Android WebView does not expose a universal API for revoking every site camera/microphone/location permission on tab close, and this build does not pretend otherwise. It also stores DoH provider preference only; true per-WebView DNS routing requires a proxy/VPN/network-layer implementation rather than a WebView setting.

## Phase 6 — Custom Tunnel Support (real native integration)
- V2Ray/Xray JSON import/paste.
- Real Android `VpnService` TUN packet mode for **PrivBrowse only** (not the whole device).
- Xray socket protection + DNS protection callbacks to prevent the tunnel from looping on its own control sockets.
- Split routing: configured proxy domains use the selected Xray proxy outbound; final traffic is direct.
- True WebView-level proxy mode using AndroidX `ProxyController` and a local SOCKS inbound.
- Runtime validation via libXray `testXray` before the packet interface is created.

## Phase 7 — AI Copilot (implemented)
- Direct client-to-provider calls for Groq, OpenRouter, OpenAI, Gemini and Anthropic.
- API keys are stored through the local Android Keystore-backed `SecureStore`.
- Prompt profiles can be saved and one profile can be marked default.
- No request is sent through a PrivBrowse backend.

## Phase 8 — Generic Video Downloader (implemented)
- Detects direct `<video>`/`<source>` and common social/metadata stream URLs exposed by the current page.
- Downloads via Android `DownloadManager` into the normal Downloads folder.
- Download history is kept locally; progress/notification handling is delegated to Android's DownloadManager.
- YouTube, Instagram and TikTok are explicitly excluded, matching the project scope.

**Phase 8 scope note:** blob-only streams, DRM, encrypted media and adaptive HLS/DASH ripping are not turned into a fake "download everything" promise; only directly retrievable URLs are queued.

## Phase 9 — Panic Button (implemented)
- Menu → PANIC clears open tabs, history, cookies, WebView cache/site storage and the local network transparency log, then minimizes the app.
- Bookmarks and saved settings are deliberately preserved.

## Build status
The source tree has been updated and packaged. This execution environment does not contain a configured Android SDK, so the final APK could not be compiled here. The GitHub Actions workflow is self-contained: it builds the pinned native libXray AAR first and then runs Gradle.

## Native tunnel implementation

The V2Ray feature uses the official XTLS/libXray Android artifact, pinned to v26.9.9. The GitHub Actions workflow builds this native AAR before Gradle compilation, so the repository itself does not need to commit a generated binary. libXray's Android API exposes `runXray`, the runtime `xray.tun.fd` environment field, and Android socket/DNS protection callbacks.

Two runtime modes are implemented:

1. **Packet tunnel:** Android `VpnService` establishes a TUN interface and passes its file descriptor to Xray. PrivBrowse's own Xray sockets are protected from re-entering the VPN.
2. **WebView-only:** Xray runs a local SOCKS listener and AndroidX `ProxyController` applies a process-level WebView proxy override.

Phase 5 VPN Gate discovery no longer creates a TUN without a real OpenVPN engine; this prevents a false-connected state from black-holing traffic.

### GitHub build

GitHub Actions installs Android SDK/NDK, Go and Python, builds official libXray v26.9.9 with its `build/main.py android` entry point, copies the resulting AAR to `app/libs/libXray.aar`, then runs Gradle 8.5. The workflow produces `app-debug.apk` as an artifact.

A local APK was not compiled in this packaging environment because Android SDK/NDK and external Gradle dependencies are unavailable here. XML/source/CI static checks were performed before packaging.

For a local machine with Android SDK/NDK, run `scripts/build_libxray.sh` once before Gradle.
