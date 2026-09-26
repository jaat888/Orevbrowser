# PrivBrowse 1.3.1 — Build & Mega Feature Fix Report

## Reported CI build failures addressed

The screenshot showed Kotlin compilation failures in three areas:

1. `EditText.singleLine` was resolved incorrectly in the affected screens. The source now uses the explicit `setSingleLine(true)` setter.
2. The tab-manager list had a local `val text` that shadowed `TextView.text`, causing `Val cannot be reassigned` and `String`/`LinearLayout` type errors. The container is now named `textColumn` and the child views use explicit `setText(...)`.
3. `TextInputLayout` corner-radius assignments were using read-only-looking synthetic properties. The source now uses the supported `setBoxCornerRadii(...)` API and adds the edit text before applying the corner radius.
4. `BuildConfig.APPLICATION_ID` was unresolved under the Android Gradle Plugin configuration. `buildFeatures { buildConfig true }` is now enabled.

## Mega feature additions

The Feature Center now exposes 157 controls/shortcuts. New one-tap tools include:

- Balanced privacy preset
- Strict privacy preset
- Speed / Data Saver preset
- Reading Focus preset
- AI privacy review
- AI action plan
- AI table extraction
- AI bilingual English + Hindi summary
- AI verify-needed checklist
- Quick search-engine switching
- Quick theme switching
- Quick private-tab launch

The presets are implemented using the app's existing preference keys and apply the changed WebView settings to open tabs before reloading the current tab.

## Validation completed in the packaging environment

- All 39 XML resources parsed successfully.
- All referenced `MainActivity.KEY_*` values resolved against the current source.
- The four reported risky patterns are absent from the patched source.
- AGP BuildConfig generation is enabled.
- Feature Center count is 157.

A full APK build was not executed in this packaging environment because the Android/Gradle SDK toolchain and the `libXray.aar` binary are not available here. GitHub Actions/Android Studio should be used for the final APK build.
