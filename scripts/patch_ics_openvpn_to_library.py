#!/usr/bin/env python3
"""
libs/ics-openvpn's main/build.gradle.kts applies com.android.application
(it's a real, complete standalone VPN app on its own — see its plugins{}
block). Android Gradle Plugin does NOT support one application module
depending on another application module's resources/classes the normal
`implementation project(...)` way — that's why ":app:processDebugResources"
failed with "Cannot find PROCESSED_RES output for SerializedForm(...)":
AGP's application-specific resource/artifact bookkeeping doesn't produce
what a *consuming* project needs from a dependency.

Fix: patch ics-openvpn's own script (in place, after cloning, same
approach as the AGP-version TOML patch) so :openvpn behaves as a plain
com.android.library instead. That's the officially-supported shape for
"app depends on this other module" and matches the variant names
(skeletonOvpn2DebugApiElements etc.) Gradle was already resolving fine.

Switching the plugin alone isn't enough — several DSL blocks/properties
in the real file are application-only and don't exist on LibraryExtension
in Kotlin's statically-typed DSL (unlike Groovy, where unknown properties
are just silently ignored): signingConfigs{}, buildTypes{} (references
signingConfig), splits{}, bundle{}, and versionCode/versionName/targetSdk
inside defaultConfig. Confirmed against AGP's own LibraryDefaultConfig
docs and multiple real-world "Unresolved reference: versionCode" reports
for exactly this application-to-library conversion. This script removes
all of them along with the plugin swap.

Run this after cloning ics-openvpn, before Gradle configures anything
(right after the existing AGP-version TOML patch step).
"""
import re
import sys

BUILD_FILE = "libs/ics-openvpn/main/build.gradle.kts"


def remove_brace_block(content, start_marker, label):
    """Delete `start_marker { ... }` (brace-matched) if present. Returns
    (new_content, found_and_removed)."""
    idx = content.find(start_marker)
    if idx == -1:
        return content, False
    brace_idx = content.find('{', idx)
    if brace_idx == -1:
        return content, False
    depth = 0
    i = brace_idx
    while i < len(content):
        if content[i] == '{':
            depth += 1
        elif content[i] == '}':
            depth -= 1
            if depth == 0:
                break
        i += 1
    if depth != 0:
        sys.exit(f"Could not find matching closing brace for {label} — "
                  f"ics-openvpn's file layout may have changed; patch this "
                  f"script by hand after checking the actual file.")
    end_idx = i + 1
    # also eat a trailing newline so we don't leave a blank line behind
    if end_idx < len(content) and content[end_idx] == '\n':
        end_idx += 1
    # and eat the leading indentation/newline before start_marker
    line_start = content.rfind('\n', 0, idx) + 1
    return content[:line_start] + content[end_idx:], True


def remove_line_containing(content, needle, label):
    lines = content.split('\n')
    new_lines = [ln for ln in lines if needle not in ln]
    if len(new_lines) == len(lines):
        print(f"NOTE: no line containing {needle!r} found ({label}) — "
              f"nothing to remove, continuing.")
    else:
        print(f"Removed line(s) containing {needle!r} ({label})")
    return '\n'.join(new_lines)


def main():
    with open(BUILD_FILE, encoding="utf-8") as f:
        content = f.read()

    # 1. Plugin swap: application -> library.
    #
    # NOT `alias(libs.plugins.android.library)`: ics-openvpn's own
    # gradle/libs.versions.toml only ever defines an "android-application"
    # plugin alias (it never had a library module before), so that
    # type-safe accessor doesn't exist and fails with "Unresolved
    # reference: library". Apply the plugin by its literal id instead —
    # settings.gradle's pluginManagement.resolutionStrategy.eachPlugin
    # already forces BOTH 'com.android.application' and
    # 'com.android.library' ids to the same pinned AGP module, so no
    # version needs to be given here either.
    new_content, n = re.subn(
        r'alias\(\s*libs\.plugins\.android\.application\s*\)',
        'id("com.android.library")',
        content,
        count=1,
    )
    if n == 0:
        sys.exit(
            "Could not find 'alias(libs.plugins.android.application)' in "
            f"{BUILD_FILE} — ics-openvpn's plugin declaration style may "
            "have changed; patch this script (or the file) by hand."
        )
    print("Patched plugins{} -> com.android.library (by literal id, not catalog alias)")
    content = new_content

    # 2. Application-only blocks that don't exist / don't compile under
    #    com.android.library's Kotlin DSL surface.
    for marker, label in [
        ("signingConfigs {", "signingConfigs{} block (app-only signing)"),
        ("buildTypes {", "buildTypes{} block (references signingConfig, app-only)"),
        ("splits {", "splits{} block (APK splits, app-only)"),
        ("bundle {", "bundle{} block (App Bundle transparency signing, app-only)"),
    ]:
        content, found = remove_brace_block(content, marker, label)
        if found:
            print(f"Removed {label}")
        else:
            print(f"NOTE: {label} not found — nothing to remove, continuing.")

    # 3. defaultConfig properties that are application-only in the
    #    statically-typed Kotlin DSL (versionCode/versionName/targetSdk
    #    don't exist on LibraryDefaultConfig -> "Unresolved reference").
    for needle, label in [
        ("versionCode =", "versionCode (app-only in defaultConfig)"),
        ("versionName =", "versionName (app-only in defaultConfig)"),
        ("targetSdk =", "targetSdk (app-only in defaultConfig)"),
    ]:
        content = remove_line_containing(content, needle, label)

    with open(BUILD_FILE, "w", encoding="utf-8") as f:
        f.write(content)

    open_braces = content.count('{')
    close_braces = content.count('}')
    if open_braces != close_braces:
        sys.exit(
            f"Post-patch brace count mismatch ({open_braces} open vs "
            f"{close_braces} close) in {BUILD_FILE} — the patch likely "
            "removed something incorrectly. Aborting so this doesn't "
            "silently ship a broken build script."
        )
    print(f"{BUILD_FILE} patched successfully, braces balanced.")


if __name__ == "__main__":
    main()
