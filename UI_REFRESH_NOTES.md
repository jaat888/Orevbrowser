# PrivBrowse 1.3.0 — UI refresh

## Browser shell

- Everyday view remains intentionally sparse: address/search bar, tab count, page, and five bottom actions.
- Tab strip collapses to one row only when multiple tabs need it.
- Settings and advanced controls use small rounded cards rather than long grey Android buttons.

## Feature Center

- Search-first layout with 150+ controls/shortcuts, including one-tap privacy presets and AI quick actions.
- Eight compact groups keep the screen scannable even with a large feature set.
- Boolean controls use switches; actions use a chevron affordance.
- Reset, export and diagnostics live in the overflow menu as secondary actions.

## AI workspace

- Dedicated header shows provider/model state without taking over the page.
- Current-page context is a compact card with an explicit on/off switch.
- 20 horizontal one-tap AI actions reduce typing.
- Composer stays pinned to the bottom and uses multiline input.
- Setup includes provider, model, API key, custom endpoint, model refresh, model test and custom model.
- Responses have Copy, Share and Speak controls.
- Chat can be stopped, retried, cleared or exported.

## Mega feature pack

- Added Balanced, Strict, Speed/Data Saver and Reading Focus presets that update the existing browser privacy/performance settings in one tap.
- Added AI quick actions for privacy review, action planning, table extraction, bilingual summary and verify-needed checks.
- Fixed Kotlin receiver/property collisions in the tab manager and replaced fragile WebView/TextInputLayout synthetic property assignments with explicit setters.

## Design target

Clean, neutral, touch-friendly, low visual noise. Advanced functionality is discoverable without making the main browser screen look like a settings dashboard.
