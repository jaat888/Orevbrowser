# CI build notes

The project targets Gradle 9.2.0, Android Gradle Plugin 9.0.0, Kotlin Gradle Plugin 2.2.20, compile/target SDK 34 and JDK 17.

## AGP/Gradle version bump (was Gradle 8.5 / AGP 8.2.2 / Kotlin 1.9.22)

`libs/ics-openvpn`'s own `main/build.gradle.kts` (real schwabe/ics-openvpn
v0.7.64 source) requests `com.android.application` version `9.0.0` through
its own version catalog (which `settings.gradle` wires in as our `libs`
catalog). A single Gradle build can only have one AGP version on the
classpath, so the root project previously applying AGP 8.2.2 via an old-style
`buildscript{ classpath ... }` block conflicted with that request:

    Error resolving plugin [id: 'com.android.application', version: '9.0.0']
    > The request for this plugin could not be satisfied because the plugin
      is already on the classpath with an unknown version, so compatibility
      cannot be checked.

Fix: root `build.gradle` now declares AGP and Kotlin via the modern
`plugins { ... apply false }` DSL, pinned to the exact same AGP version
ics-openvpn requests (9.0.0), so both `:app` and `:openvpn` resolve to one
consistent plugin/version. That pulled in matching bumps: Gradle wrapper and
the workflow's `Setup Gradle` step to 9.2.0 (AGP 9.0.0 requires Gradle
>= 9.1.0), Kotlin Gradle Plugin to 2.2.20 (AGP 9.0.0 requires KGP >= 2.2.10),
and installing NDK 28.2.13676358 in CI (AGP 9.0.0's new default, since
neither module sets `android.ndkVersion` explicitly).

If `libs/ics-openvpn` is ever re-pinned to a different tag that requests a
different AGP version, update the version in root `build.gradle` to match
it exactly, plus the Gradle version in both `gradle-wrapper.properties` and
the workflow's `Setup Gradle` step if that new AGP version's minimum Gradle
requirement is higher.

Before running `:app:assembleDebug` or `:app:assembleRelease`, provide the real `app/libs/libXray.aar` binary used by the V2Ray/Xray bridge. The source code intentionally keeps this dependency external because a placeholder AAR would produce an APK whose tunnel feature does not actually work.

A no-SDK static validation script is included at `scripts/audit_privbrowse.py`; it checks Feature Center action/key references, MainActivity action branches and activity links.

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
