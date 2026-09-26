# PrivBrowse 1.3.0 — Static Audit

## Changes audited

- Reworked the AI client to use direct provider APIs with provider-specific request/response handling.
- OpenAI path now attempts the Responses API and falls back to Chat Completions for compatible models/endpoints.
- Gemini and Anthropic have dedicated request parsers; OpenAI-compatible providers share a common path.
- Added model discovery and configurable custom OpenAI-compatible endpoints.
- Fixed secure-key lookup to use the provider-specific Keystore-backed key consistently.
- Reworked AI UI into a dedicated clean chat workspace with page context, tool chips, setup/test controls, retry/stop and chat export.
- Expanded Feature Center to 157 controls/shortcuts, including one-tap privacy/performance presets and AI quick actions.
- Added search engines: Startpage and Ecosia in addition to DuckDuckGo, Brave Search, Bing and Google.
- Added cookie acceptance control, history-saving control, desktop-by-default, screen-awake option, scrollbar control, font/text sizing and lifecycle cleanup options.
- Fixed reported CI Kotlin errors: explicit EditText single-line setters, TextView receiver shadowing in the tab manager, explicit TextInputLayout corner-radius setter, and AGP BuildConfig generation.

## Static validation

- AndroidManifest.xml parses successfully.
- All resource XML files parse successfully.
- No unresolved `MainActivity.KEY_*` references were found by the project scan.
- No duplicate `KEY_*` declarations were found in MainActivity.
- The project contains the Android Gradle configuration, but the runtime environment used for this audit does not have a complete Android SDK/Gradle build toolchain and the libXray AAR is not present in the working checkout. Therefore a signed APK build/runtime test could not be truthfully claimed here.

## Runtime caveats to test on a phone

1. Provider API keys, model availability and account limits are external to the app and can change.
2. AI requests are sent directly to the selected provider when the user uses the AI feature.
3. Some WebView privacy controls take effect on new WebViews or after a reload.
4. VPN/tunnel behavior depends on Android VPN permissions, device networking and the supplied configuration.
