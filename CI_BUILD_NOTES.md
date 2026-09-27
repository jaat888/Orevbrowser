# CI build notes

The project targets Gradle 8.5, Android Gradle Plugin 8.2.2, Kotlin 1.9.22, compile/target SDK 34 and JDK 17.

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
