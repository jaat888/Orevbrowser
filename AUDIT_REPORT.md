# PrivBrowse — source audit / bug-fix pass

## Scope

This audit covers the uploaded Phase 1–5 project plus the integrated Phase 4, 6, 7, 8 and 9 code. A separate DeepSeek list containing 55 findings was mentioned in the conversation, but that list was not included in the files available to this run; therefore this report does **not** claim a 55/55 match.

## Fixed in this follow-up pass (post-v2ray upload)

- **`V2RayVpnService.onDestroy()`** called `stopTunnelInternal()` with no arguments, but the function's first parameter (`expectedGeneration: Long?`) has no default — this was a compile error. Fixed to `stopTunnelInternal(null)`.
- **`SecureStore.key()`** generated its AndroidKeyStore AES key with `KeyGenerator.init(256)` (the plain-int overload). The AndroidKeyStore provider rejects that and requires a `KeyGenParameterSpec`; this would throw at runtime on first use, breaking every encrypted-storage call (V2Ray config, vault entries, saved API keys). Fixed to build a proper `KeyGenParameterSpec` (AES/GCM/NoPadding, 256-bit).
- **`VideoDetector.SCRIPT`** is a Kotlin *raw* triple-quoted string, where `\\` is not collapsed into `\`. The embedded JS regex `/\\.(mp4|webm|...)(\\?|#|$)/i` therefore matched "a literal backslash + any character" instead of "a literal dot", so extension-based video-link detection almost never matched real URLs. Fixed to `/\.(mp4|webm|m4v|mov|ogv)(\?|#|$)/i`.
- **`VideoDownloadStore.add()`** appended the new item to the end of an already newest-first-sorted list and then did `.take(100)` — once the 100-item cap was hit, the **newest** download was the one dropped, not the oldest. Fixed by sorting by `createdAt` descending before capping.

## Validated in this follow-up pass (no changes needed)

- Every remaining Kotlin file (MainActivity, CookiePolicy, AdBlocker, TrackingParamStripper, DomainPrivacyStore, ConsentAutoHandler, ReaderMode, DbHelper, the VPN Gate stack, the V2Ray/Xray stack besides the two bugs above, BreachChecker, BiometricLock, the AI client/provider/profile store, all UI activities, adapters, BrowserTab, the weekly-report scheduler/receiver, BootReceiver, PanicManager).
- Every `R.string`, `R.color`, `R.drawable`, `R.layout` and `R.id` reference in code cross-checked against `res/` — all resolve (the two framework resources, `android.R.drawable.ic_lock_lock` and `android.R.layout.simple_spinner_dropdown_item`, are intentional).
- `AndroidManifest.xml` cross-checked against every Activity/Service/Receiver class — all present, permissions look correct for what's used (VPN, foreground service, notifications, boot receiver, biometric).
- All `res/**/*.xml` and the manifest parse as valid XML.
- No remaining Kotlin `!!`, no TODO/FIXME markers, brace/paren counts balanced in every file.
- `build.gradle` (root + app), `settings.gradle`, `gradle.properties`, the GitHub Actions workflow, and `scripts/build_libxray.sh` (`bash -n` clean) reviewed — consistent and no syntax issues found.

## Fixed in earlier pass

### V2Ray / Xray
- Replaced the previous fake-TUN concept with a real Android `VpnService` packet path backed by the official XTLS/libXray Android build.
- Added libXray runtime calls for `testXray`, `runXray` and `stopXray`.
- Passes the Android TUN file descriptor through Xray's `env.xray.tun.fd` field.
- Added socket-protect and DNS-protect controller registration to keep Xray's own control traffic outside its TUN loop.
- Uses `addAllowedApplication(packageName)` so packet mode is scoped to PrivBrowse instead of routing the whole phone.
- Added IPv4 + IPv6 TUN routes and DNS endpoints.
- Added split-domain routing with a direct fallback; selected domains are inserted before existing rules.
- Added strict config-size validation and requires a tagged non-direct outbound.
- Added a real WebView-only mode: Xray exposes a local SOCKS inbound and AndroidX `ProxyController` applies the proxy to WebView network requests in the app process.
- Added lifecycle generation/session guards so old connect/disconnect races cannot silently replace a newer tunnel.
- Fixed the mode-switch race where stopping the old service could erase the newly created session token.
- Encrypted saved Xray JSON with Android Keystore-backed AES-GCM storage.

### Browser / WebView
- Fixed a UI-thread bug: privacy-badge UI updates from `shouldInterceptRequest` are now dispatched to the main thread.
- Fixed tab-close index handling when a tab before the current tab is removed.
- Added `onNewIntent` support for incoming http/https links with `singleTask`.
- Hardened WebView file/content access, mixed-content handling and Safe Browsing defaults.
- Third-party cookies are disabled on WebViews.
- Camera/microphone permission requests and WebView geolocation prompts are denied by default instead of pretending that Android exposes a universal per-tab revoke API.
- Invalid/non-http page URLs no longer become fake `scheme://host` privacy origins.
- Cookie wipe tasks are cancelled on tab close/destroy and rescheduled on navigation; duplicate timer callbacks are prevented.
- Parent-domain cookie timers now choose the most-specific matching rule.
- Cookie deletion no longer writes a malformed `$origin=...` cookie; it enumerates cookie names and expires them.

### Privacy / transparency
- Custom ad-block hosts are cached into an immutable snapshot for concurrent WebView request threads.
- Custom host-list import refreshes the filter on the next app resume without requiring a full reinstall.
- Weekly report scheduling no longer resets the 7-day alarm on every app launch; it checks for an existing `PendingIntent` first.
- Alarm lookup flags include immutable semantics for Android 12+.
- Corrupt AI profile JSON is treated as empty data rather than crashing the screen.
- Saved vault/API/V2Ray secrets use Android Keystore-backed storage.

### Video downloader
- Generic WebView downloads are classified before entering the video queue.
- YouTube/Instagram/TikTok exclusions are enforced in both the browser listener and downloader layer.
- Wildcard `video/*` is no longer forced into `DownloadManager`; MIME is inferred only when it is concrete.
- Video URL parsing is resilient to JSON escaping/format differences.
- Download history migrated from delimiter strings to JSON and is capped at 100 records.
- Directly retrievable media only is supported; DRM/blob-only/adaptive stream ripping is not falsely advertised.

### VPN Gate
- Removed the unsafe fake-TUN connection behavior.
- VPN Gate remains discovery/ranking only until a real OpenVPN/OpenVPN3 engine is included.
- CSV parsing now honors quoted fields, so operator/message commas do not shift later columns.

### Build / CI
- GitHub Actions now provisions Android SDK/NDK, builds the pinned official libXray Android AAR, injects it into `app/libs`, then runs Gradle 8.5 and uploads the debug APK.
- The workflow uses `python3 build/main.py android`, matching the official libXray build entry point instead of hand-editing generated native artifacts.

## Validation performed in this environment

- All project XML files parsed successfully.
- GitHub Actions workflow text checks passed for native build + Gradle build + artifact upload.
- Native build shell script passed `bash -n`.
- App resource reference scan found no missing **app-owned** resources; the only two matches that are not app resources are `android.R.drawable.ic_lock_lock` and `android.R.layout.simple_spinner_dropdown_item`.
- Source scan found no remaining Kotlin `!!`, TODO/FIXME markers or fake active VPN-TUN implementation.

## Not claimed as locally verified

This execution environment has no Android SDK/NDK and cannot resolve external GitHub/Gradle dependencies, so an actual APK compile/run and live tunnel/WebView traffic test were not possible here. The repository's GitHub workflow is designed to perform that native build in CI.

## Remaining platform scope

- VPN Gate connection is still discovery-only; a real OpenVPN/OpenVPN3 engine is a separate native integration.
- True per-tab isolated WebView cookie jars require separate WebView data-directory/process architecture.
- WebView camera/mic/location are denied by default rather than granted then revoked.
- Direct generic media downloads do not decode DRM or rip adaptive HLS/DASH streams.
