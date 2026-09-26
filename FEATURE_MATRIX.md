# PrivBrowse 1.3.0 — Feature Matrix

## Scope

This build keeps the everyday browser UI compact and moves advanced controls into a searchable Feature Center. The center currently exposes **147 feature controls/shortcuts** across privacy, WebView behavior, tabs, page tools, AI, downloads/connectivity, lifecycle, and security/diagnostics.

## Feature groups

| Group | Examples |
|---|---|
| Privacy & tracking | Ad/tracker levels, pop-up blocking, social-embed blocking, GPC, Do Not Track, tracking-parameter stripping, HTTPS-first, cookie controls, per-site controls, privacy grade, transparency log, panic wipe |
| Browser engine & performance | JavaScript, images, DOM storage, media gesture, Safe Browsing, mixed-content blocking, file/content access, form helpers, cache modes, zoom, viewport, overview mode, stalled-load timeout, desktop-by-default, scrollbars, font sizing |
| Tabs & navigation | Private tabs, private link opens, session restore, recently closed tabs, close-all confirmation, auto reader, compact tab strip, scroll restore, external scheme handoff, history switch, tab manager, bookmarks, reading list |
| Page tools | Find in page, reader mode, translation, text extraction, TTS, copy URL/title/text/links/metadata, source viewer, MHT archive, screenshot share, PDF/print, hard reload, external browser, desktop mode |
| AI | Provider/model setup, model discovery, custom model IDs, custom OpenAI-compatible endpoint, secure API-key storage, page context, local chat history, prompt profiles, 20 one-tap page prompts, copy/share/speak, retry/stop, clipboard input, chat export |
| Downloads & connectivity | Direct media detector, download confirmation, download queue, VPN discovery, V2Ray/Xray, connection diagnostics, network activity |
| Lifecycle & cleanup | Cache/cookie/storage/history cleanup on exit, network-log cleanup, consent-cookie purge, clear-on-background behavior, private-tab no-cache, log pruning, import/export/reset settings |
| Security & diagnostics | Biometric lock, secure vault routing, privacy reports, diagnostics, settings summary, panic flow, capability map |

## AI providers

The AI workspace includes Groq, OpenRouter, OpenAI, Gemini, Anthropic and a configurable OpenAI-compatible provider. The user supplies and controls provider API credentials; PrivBrowse does not ship a shared API key.

## Implementation notes

Some features are WebView-backed because PrivBrowse uses Android WebView rather than replacing the Android browser engine. Full Chrome/Brave desktop-extension ecosystems cannot be reproduced by settings alone; the Feature Center labels the boundary instead of pretending those capabilities are native.
