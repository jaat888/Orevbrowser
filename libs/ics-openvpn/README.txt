This directory must contain a checkout of schwabe/ics-openvpn (the real
OpenVPN engine — native OpenSSL/openvpn binary, JNI bridge, ConfigParser,
VpnProfile, ProfileManager, VPNLaunchHelper, OpenVPNService, etc.) so that
its ":main" module can be built as a Gradle project dependency:

    git clone --recursive --branch v0.7.64 https://github.com/schwabe/ics-openvpn.git libs/ics-openvpn

Pinned to v0.7.64 on purpose: PrivBrowseVpnService.kt's calls into
ConfigParser, VpnProfile, ProfileManager, VPNLaunchHelper and OpenVPNService
were checked against that tag's actual source (not guessed), so upgrading
this pin means re-checking those four call sites too, not just re-cloning.

The included .github/workflows/build.yml does this step automatically in CI.
For a local build, run the clone command above before opening the project
in Android Studio / running ./gradlew.

WHY THIS EXISTS (read before removing or "stubbing" it):
VPN Gate serves standard .ovpn (OpenVPN) configs. There is no WireGuard or
other "simpler" client that can connect to those servers — the protocol on
the wire is OpenVPN, full stop. ics-openvpn is the OpenVPN engine every
serious Android OpenVPN client (including this one) is built on, because
writing/maintaining an independent OpenVPN-protocol + OpenSSL native stack
from scratch is not realistic for an app like this.

LICENSE / GPL OBLIGATION — READ THIS:
ics-openvpn is licensed GPLv2 (verify the exact text at
https://github.com/schwabe/ics-openvpn/blob/master/COPYING and copy it into
LICENSE-ics-openvpn.txt in this repo before you publish — I could not fetch
it verbatim myself, see NOTICE.md). Linking it into PrivBrowse means:

  - The complete source of PrivBrowse (this whole repository, not just the
    VPN files) must be made available under GPL terms to anyone you give
    the app to, if you ever distribute/share the APK outside just your own
    device.
  - This is why README.md and NOTICE.md now say PrivBrowse is GPL-licensed
    as a whole. That's not reversible for any build that ships this engine
    without re-doing the VPN feature on a non-GPL engine instead.
