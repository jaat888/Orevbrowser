# PrivBrowse

Privacy-first Android browser built around Android WebView, with a deliberately clean everyday UI and a large searchable power-feature layer (150+ controls/shortcuts).

## What this build adds

**Browser:** tab manager, private tabs, session restore, recently closed tabs, bookmarks, history, reading list, custom home/search engines, page tools, reader mode, translation shortcut, PDF/print, web archive, page screenshot, desktop mode, zoom and font controls.

**Privacy:** tracker/ad blocking, privacy grade, network transparency log, fingerprint hardening, GPC, Do Not Track, tracking-parameter stripping, HTTPS-first, third-party-cookie control, cookie-banner helper, per-site cleanup, camera/mic/geolocation controls, mixed-content blocking, file/content access controls, panic wipe and lifecycle cleanup.

**AI:** dedicated AI workspace with Groq, OpenRouter, OpenAI, Gemini, Anthropic and custom OpenAI-compatible endpoints; secure API-key storage; model discovery; page context; prompt profiles; 20 one-tap page actions; copy/share/speak; retry/stop; clipboard input; chat history and export.

**Connectivity/media:** direct media candidate detection, download queue, VPN discovery, V2Ray/Xray modules and network diagnostics.

## AI setup

1. Open `AI Copilot`.
2. Choose your provider.
3. Paste your own provider API key.
4. Select a model, or refresh the provider's model list.
5. Press `Test` before sending a real prompt.
6. Press `Save`.

For a custom OpenAI-compatible server, choose `Custom OpenAI-compatible`, enter its HTTPS chat-completions endpoint, key and exact model ID.

PrivBrowse does not ship a shared API key. The key is stored through the app's secure local store and requests are made directly to the selected provider when the AI feature is used.

## Main UI principle

The browser itself stays simple. The Feature Center is where the large set of controls lives, with search instead of a giant wall of settings.

## Build

Use a normal Android Studio/Gradle Android environment with the Android SDK and the project's `libXray.aar` dependency available under `app/libs/`.

This source package includes build fixes for the reported Kotlin errors and enables AGP BuildConfig generation. A full APK build was not executed in the packaging environment because the Android/Gradle toolchain and libXray AAR are not available there.
