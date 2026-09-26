package com.privbrowse.app.vpn

/**
 * One row from the VPN Gate public server CSV.
 * https://www.vpngate.net/en/api.aspx
 */
data class VpnServer(
    val hostName: String,
    val ip: String,
    val score: Long,
    val pingMs: Int,
    val speedBps: Long,
    val countryLong: String,
    val countryShort: String,
    val numSessions: Int,
    val uptimeMs: Long,
    val totalUsers: Long,
    val totalTraffic: Long,
    val logType: String,
    val operator: String,
    val message: String,
    val openVpnConfigBase64: String
) {
    /** True if the embedded .ovpn config requests UDP (Phase 5: prefer UDP). */
    fun isUdp(): Boolean {
        val decoded = try {
            String(android.util.Base64.decode(openVpnConfigBase64, android.util.Base64.DEFAULT))
        } catch (e: Exception) {
            ""
        }
        return decoded.contains("proto udp", ignoreCase = true)
    }
}
