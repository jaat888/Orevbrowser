package com.privbrowse.app.vpn

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Phase 5 — VPN Gate server list.
 *
 * Fetches https://www.vpngate.net/api/iphone/, a plain CSV (with a leading
 * comment line and a trailing "*" terminator) — one line per volunteer
 * relay server. Network call only, no data about the user is sent.
 *
 * Must be called off the main thread.
 */
object VpnGateApi {

    private const val API_URL = "https://www.vpngate.net/api/iphone/"

    /** Column order per VPN Gate's documented CSV schema. */
    private const val COL_HOSTNAME = 0
    private const val COL_IP = 1
    private const val COL_SCORE = 2
    private const val COL_PING = 3
    private const val COL_SPEED = 4
    private const val COL_COUNTRY_LONG = 5
    private const val COL_COUNTRY_SHORT = 6
    private const val COL_SESSIONS = 7
    private const val COL_UPTIME = 8
    private const val COL_TOTAL_USERS = 9
    private const val COL_TOTAL_TRAFFIC = 10
    private const val COL_LOG_TYPE = 11
    private const val COL_OPERATOR = 12
    private const val COL_MESSAGE = 13
    private const val COL_OVPN_CONFIG = 14


    private fun parseCsvLine(line: String): List<String> {
        val fields = ArrayList<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.setLength(0)
                }
                else -> current.append(ch)
            }
            i++
        }
        fields.add(current.toString())
        return fields
    }

    @Throws(Exception::class)
    fun fetchServers(): List<VpnServer> {
        val connection = URL(API_URL).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.requestMethod = "GET"

        val servers = mutableListOf<VpnServer>()
        try {
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                reader.forEachLine { line ->
                    if (line.startsWith("#") || line.startsWith("*") || line.isBlank()) return@forEachLine
                    val cols = parseCsvLine(line)
                    if (cols.size <= COL_OVPN_CONFIG) return@forEachLine
                    try {
                        servers.add(
                            VpnServer(
                                hostName = cols[COL_HOSTNAME],
                                ip = cols[COL_IP],
                                score = cols[COL_SCORE].toLongOrNull() ?: 0L,
                                pingMs = cols[COL_PING].toIntOrNull() ?: Int.MAX_VALUE,
                                speedBps = cols[COL_SPEED].toLongOrNull() ?: 0L,
                                countryLong = cols[COL_COUNTRY_LONG],
                                countryShort = cols[COL_COUNTRY_SHORT],
                                numSessions = cols[COL_SESSIONS].toIntOrNull() ?: 0,
                                uptimeMs = cols[COL_UPTIME].toLongOrNull() ?: 0L,
                                totalUsers = cols[COL_TOTAL_USERS].toLongOrNull() ?: 0L,
                                totalTraffic = cols[COL_TOTAL_TRAFFIC].toLongOrNull() ?: 0L,
                                logType = cols[COL_LOG_TYPE],
                                operator = cols[COL_OPERATOR],
                                message = cols[COL_MESSAGE],
                                openVpnConfigBase64 = cols[COL_OVPN_CONFIG]
                            )
                        )
                    } catch (e: Exception) {
                        // Skip malformed rows rather than failing the whole fetch.
                    }
                }
            }
        } finally {
            // Bug fix: connection.disconnect() previously sat after the read block
            // unguarded, so any exception while reading (timeout, malformed stream)
            // skipped it and leaked the underlying socket/connection.
            connection.disconnect()
        }
        return servers
    }
}
