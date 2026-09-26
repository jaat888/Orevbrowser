package com.privbrowse.app.vpn

/**
 * Phase 5 — smart server selection.
 *
 * VPN Gate's own "Score" already blends speed/uptime/ping, so we rank by
 * score first, but among close-scoring servers prefer lower ping, and
 * (per the spec) prefer a server whose .ovpn config uses UDP over TCP.
 */
object VpnServerSelector {

    private const val PING_ACCEPTABLE_MS = 400

    fun rank(servers: List<VpnServer>): List<VpnServer> {
        return servers
            .filter { it.pingMs in 1 until PING_ACCEPTABLE_MS && it.score > 0 }
            .sortedWith(
                compareByDescending<VpnServer> { it.isUdp() }
                    .thenByDescending { it.score }
                    .thenBy { it.pingMs }
            )
    }

    fun pickBest(servers: List<VpnServer>): VpnServer? = rank(servers).firstOrNull()

    /** Top N candidates to rotate between, so rotation doesn't always hit the exact same server. */
    fun topCandidates(servers: List<VpnServer>, n: Int = 5): List<VpnServer> = rank(servers).take(n)
}
