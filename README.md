# PrivBrowse

Privacy-first Android browser built around Android WebView, with a deliberately clean everyday UI and a large searchable power-feature layer (200+ controls/shortcuts).

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

Use a normal Android Studio/Gradle Android environment with the Android SDK. The CI workflow automatically builds `libXray.aar` from the pinned XTLS/libXray source when the AAR is not already present under `app/libs/`.

This source package includes build fixes for the reported Kotlin errors and enables AGP BuildConfig generation. A full APK build was not executed in the packaging environment because the Android/Gradle toolchain and native build dependencies are not available there.

## 1.4 Ultimate feature pack

This revision adds a dedicated Pro Workflow area with direct one-tap actions for sharing/copying page data, find-in-page, hard reload, screenshot, translation, TTS, source/archiving, PDF/print, external handoff, desktop mode, bookmarks, site cleanup, privacy diagnostics, tabs, downloads and connectivity. It also adds maximum-privacy, media-friendly and developer/debug presets plus optional 30-second clipboard auto-clear.

The feature catalog is designed around real app actions and existing WebView capabilities rather than placeholder buttons.

## 1.5 Ultimate wishlist integration

The new **Ultimate Feature Lab** wires the supplied full wishlist into the real browser engine, Android services and existing browser modules where those capabilities are available: tab pin/group/preview controls, automatic inactive-tab cleanup, self-destruct timers, address-bar and edge gestures, live local suggestions, bang commands, clipboard link prompts, Ghost mode, leak-test shortcuts, side-by-side compare, page notes, image gallery, subtitle-track download handoff, reverse-image search handoff, print-friendly extraction, calendar intents, per-domain CSS/JS, accessibility page styles and a command palette. See `WISHLIST_IMPLEMENTATION.md` for the line-by-line status of every wishlist item. Platform-limited items are documented with their exact Android/WebView boundary rather than exposed as fake functionality.

## License

PrivBrowse links the `ics-openvpn` OpenVPN engine (GPLv2) to talk to VPN
Gate. That makes this repository **GPL-licensed as a whole** for any build
that includes the `vpn` feature — see `NOTICE.md` for what that obligates
you to do before sharing the APK with anyone.
