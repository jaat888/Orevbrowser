#!/usr/bin/env python3
"""
libs/ics-openvpn's own gradle/libs.versions.toml (cloned fresh by CI —
see .github/workflows/build.yml) requests com.android.application (and
possibly com.android.library) at whatever AGP version its own maintainers
currently pin — which has been observed to be a version whose actual
build.gradle.kts code is NOT compatible with (e.g. it requested AGP 9.0.0
while its own script still used pre-AGP9 variant APIs that AGP 9 removed
outright). Our own root build.gradle / settings.gradle pin AGP to a known-
working version (see AGP_VERSION below and CI_BUILD_NOTES.md).

A single Gradle build can only ever load one AGP version. Redirecting BOTH
requests to the same module via settings.gradle's
pluginManagement.resolutionStrategy.eachPlugin { useModule(...) } stops the
wrong *module* from loading, but Gradle still separately compares the two
REQUESTED version strings (ours vs. ics-openvpn's) and fails with
"already on the classpath with a different version" even when both get
redirected to the identical artifact.

So instead of only redirecting, this script rewrites the REQUESTED version
in ics-openvpn's own catalog file, in place, before Gradle ever reads it —
so both sides request the same version and there's nothing left to
compare. It's written generically (regex over the actual file content,
not a hardcoded key name) because ics-openvpn's exact catalog key naming
can change between tags.

Run this after cloning ics-openvpn, before Gradle configures anything.
"""
import re
import sys

# Keep this in sync with the AGP version pinned in root build.gradle /
# settings.gradle.
AGP_VERSION = "8.2.2"

TOML_PATH = "libs/ics-openvpn/gradle/libs.versions.toml"


def main():
    with open(TOML_PATH, encoding="utf-8") as f:
        content = f.read()

    # Matches a [plugins] table entry (TOML inline-table syntax) whose id
    # is any AGP plugin, e.g.:
    #   android-application = { id = "com.android.application", version.ref = "agp" }
    pattern = re.compile(
        r'^[ \t]*[\w.-]+\s*=\s*\{[^}]*id\s*=\s*'
        r'"(com\.android\.(?:application|library|dynamic-feature|test))"[^}]*\}',
        re.MULTILINE,
    )
    matches = list(pattern.finditer(content))
    if not matches:
        sys.exit(
            f"Could not find any com.android.* plugin entry in {TOML_PATH} — "
            "ics-openvpn's catalog format may have changed; patch this script "
            "(or settings.gradle's eachPlugin override / build.gradle's AGP "
            "pin) by hand after checking the actual file."
        )

    refs = set()
    literal_entries = []
    for m in matches:
        entry = m.group(0)
        ref_match = re.search(r'version\.ref\s*=\s*"([\w.-]+)"', entry)
        if ref_match:
            refs.add(ref_match.group(1))
        else:
            literal_entries.append(entry)

    changed = False

    for ref in refs:
        content, n = re.subn(
            r'(^[ \t]*' + re.escape(ref) + r'\s*=\s*")[^"]+(")',
            r'\g<1>' + AGP_VERSION + r'\g<2>',
            content,
            count=1,
            flags=re.MULTILINE,
        )
        if n:
            print(f"Patched versions.{ref} -> {AGP_VERSION}")
            changed = True
        else:
            print(f"WARNING: plugin entry referenced versions.{ref} but that "
                  f"key wasn't found under [versions] in {TOML_PATH}; left as-is.")

    for entry in literal_entries:
        new_entry = re.sub(r'version\s*=\s*"[\w.-]+"', f'version = "{AGP_VERSION}"', entry)
        content = content.replace(entry, new_entry, 1)
        print(f"Patched inline AGP plugin version -> {AGP_VERSION}")
        changed = True

    if not changed:
        sys.exit(f"Found AGP plugin entries in {TOML_PATH} but failed to patch any of them.")

    with open(TOML_PATH, "w", encoding="utf-8") as f:
        f.write(content)


if __name__ == "__main__":
    main()
