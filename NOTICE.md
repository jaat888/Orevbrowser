# NOTICE

PrivBrowse bundles the OpenVPN engine from **ics-openvpn**
(https://github.com/schwabe/ics-openvpn) by Arne Schwabe and contributors,
to connect to VPN Gate (https://www.vpngate.net) relay servers over the
standard OpenVPN protocol.

ics-openvpn is licensed under the **GNU General Public License v2 (GPLv2)**.
Because it is linked into this app (not run as a separate process/IPC-only
service), **PrivBrowse as a whole is distributed under the GPL** as long as
this engine is part of the build. If you strip the `vpn` feature and the
`libs/ics-openvpn` module out entirely, this obligation goes away with it —
but as shipped here, it applies.

Practical obligations when you distribute the APK to anyone besides
yourself (a store listing, a shared link, a friend's phone, etc.):

1. Make the complete corresponding source of this exact build available to
   whoever receives the app (a public GitHub repo is the normal way).
2. Keep this NOTICE and a copy of the GPL license text (see
   `libs/ics-openvpn/README.txt` for where to get the exact canonical text —
   I did not have network access while assembling this, so fetch and commit
   the verbatim COPYING file from the ics-openvpn repo yourself before you
   publish, rather than trusting a paraphrase).
3. Don't add further restrictions on top (no "no redistribution" clauses,
   no closed-source-only forks).

This is a one-way door for this codebase as long as the real OpenVPN engine
is in it — there is no config flag that makes it "GPL for the VPN part
only."
