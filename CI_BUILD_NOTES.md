# CI build notes

The project targets Gradle 8.5, Android Gradle Plugin 8.2.2, Kotlin 1.9.22, compile/target SDK 34 and JDK 17.

## AGP plugin-version conflict with libs/ics-openvpn (resolved via settings.gradle)

`libs/ics-openvpn`'s own `main/build.gradle.kts` (real schwabe/ics-openvpn
v0.7.64 source, cloned fresh by CI) requests `com.android.application`
version `9.0.0` through its own version catalog (wired in as our `libs`
catalog by `settings.gradle`). Two different attempts were made here:

1. First, the root project's old `buildscript{ classpath ... }`-style AGP
   8.2.2 application conflicted with that plugins-DSL request for a
   different version, failing with "already on the classpath with an
   unknown version".
2. So the whole project was bumped to match ics-openvpn's requested AGP
   9.0.0 (plus Gradle 9.2.0, Kotlin 2.2.20, AGP 9's built-in-Kotlin). That
   config phase succeeded, but `:openvpn`'s actual build script failed to
   even *configure*: its real code (`Configure project :openvpn`) still
   calls the old, pre-AGP9 variant API — `android.applicationVariants.all`,
   `srcDirs`, `registerJavaGeneratingTask`, etc. — which AGP 9 removed
   outright ("Unresolved reference 'applicationVariants'"). So
   ics-openvpn's catalog version pin (9.0.0) doesn't match what its own
   script code actually needs.

Final fix: back to AGP 8.2.2 / Gradle 8.5 / Kotlin 1.9.22 (this file's
top line), and `settings.gradle`'s `pluginManagement.resolutionStrategy.eachPlugin`
now force-resolves *every* request for `com.android.application`/
`com.android.library` — ours or ics-openvpn's, whatever version either one
asks for — to the explicit module `com.android.tools.build:gradle:8.2.2`.
That keeps `:app` and `:openvpn` on one consistent, actually-compatible AGP
version regardless of what ics-openvpn's own catalog says, and sidesteps
the "already on the classpath" conflict entirely since there's only ever
one real resolution path for that plugin id now.

If `libs/ics-openvpn` is ever re-pinned to a tag whose *code* has actually
migrated to AGP 9's new variant API, this override (and the AGP/Gradle/
Kotlin versions above) can be revisited — but don't just match its
catalog's version number without checking its actual script code first,
per the above.

## VPN Gate / ics-openvpn (GPLv2)

`settings.gradle` includes a `:openvpn` module pointing at
`libs/ics-openvpn/main`, which does **not** exist in a fresh checkout. Before
`:app:assembleDebug`/`:app:assembleRelease` will even configure, run:

    git clone --recursive --branch v0.7.64 https://github.com/schwabe/ics-openvpn.git libs/ics-openvpn

`.github/workflows/build.yml` does this automatically in CI (pinned to
v0.7.64 — see libs/ics-openvpn/README.txt for why the pin matters), plus
installs the NDK/autotools it needs to build its native OpenSSL/openvpn
binaries. The clone/native-build step is still the most likely thing to
need debugging on first run — read the Actions log, not just the final
failure line, if it breaks; the Kotlin-side ConfigParser/VpnProfile/
ProfileManager/VPNLaunchHelper/OpenVPNService calls in
PrivBrowseVpnService.kt have already been checked against v0.7.64's actual
source, so a failure there specifically is more likely a gradle/NDK
environment issue than an API mismatch.

See `NOTICE.md` and `libs/ics-openvpn/README.txt` for the licensing
consequence of this dependency: it makes the whole app GPL, not just the
VPN feature.

Before running `:app:assembleDebug` or `:app:assembleRelease`, provide the real `app/libs/libXray.aar` binary used by the V2Ray/Xray bridge. The source code intentionally keeps this dependency external because a placeholder AAR would produce an APK whose tunnel feature does not actually work.

A no-SDK static validation script is included at `scripts/audit_privbrowse.py`; it checks Feature Center action/key references, MainActivity action branches and activity links.
