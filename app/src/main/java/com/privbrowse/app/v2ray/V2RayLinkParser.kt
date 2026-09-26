package com.privbrowse.app.v2ray

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

/**
 * Turns a plain share-link — the kind any V2Ray/Xray provider hands out —
 * into the JSON config XrayConfigBuilder expects. This is the difference
 * between "paste a link" (what every simple VPN-style app does) and asking
 * the user to hand-write an Xray JSON config themselves.
 *
 * Supported: vmess://, vless://, trojan://, ss://
 * Anything else (starts with "{") is treated as an already-complete raw
 * JSON config and passed through untouched — advanced users can still use
 * that path directly.
 */
object V2RayLinkParser {

    class ParseException(message: String) : Exception(message)

    fun looksLikeLink(raw: String): Boolean {
        val t = raw.trim()
        return t.startsWith("vmess://") || t.startsWith("vless://") ||
            t.startsWith("trojan://") || t.startsWith("ss://")
    }

    /** Returns a full Xray JSON config string with one "proxy"-tagged outbound. */
    fun parse(raw: String): String {
        val link = raw.trim()
        val outbound = when {
            link.startsWith("vmess://") -> parseVmess(link)
            link.startsWith("vless://") -> parseVlessOrTrojan(link, isTrojan = false)
            link.startsWith("trojan://") -> parseVlessOrTrojan(link, isTrojan = true)
            link.startsWith("ss://") -> parseShadowsocks(link)
            else -> throw ParseException("Unrecognized link format")
        }
        return JSONObject().put("outbounds", JSONArray().put(outbound)).toString()
    }

    private fun b64Decode(s: String): String {
        var padded = s.trim().replace('-', '+').replace('_', '/')
        val pad = (4 - padded.length % 4) % 4
        padded += "=".repeat(pad)
        return String(Base64.decode(padded, Base64.DEFAULT))
    }

    private fun parseVmess(link: String): JSONObject {
        val payload = link.removePrefix("vmess://")
        val json = runCatching { JSONObject(b64Decode(payload)) }
            .getOrElse { throw ParseException("Couldn't decode this vmess:// link") }

        val address = json.optString("add").ifBlank { throw ParseException("vmess link is missing a server address") }
        val port = json.optString("port").toIntOrNull() ?: throw ParseException("vmess link has an invalid port")
        val id = json.optString("id").ifBlank { throw ParseException("vmess link is missing a user id") }
        val alterId = json.optString("aid", "0").toIntOrNull() ?: 0
        val network = json.optString("net", "tcp").ifBlank { "tcp" }
        val host = json.optString("host")
        val path = json.optString("path")
        val tls = json.optString("tls")
        val sni = json.optString("sni").ifBlank { host }
        val serviceName = json.optString("path") // grpc reuses the "path" field as serviceName by convention

        val streamSettings = JSONObject().put("network", network)
        applyTransport(streamSettings, network, host, path, serviceName)
        applyTls(streamSettings, tls, sni)

        return JSONObject()
            .put("tag", "proxy")
            .put("protocol", "vmess")
            .put(
                "settings", JSONObject().put(
                    "vnext", JSONArray().put(
                        JSONObject().put("address", address).put("port", port).put(
                            "users", JSONArray().put(
                                JSONObject().put("id", id).put("alterId", alterId).put("security", "auto")
                            )
                        )
                    )
                )
            )
            .put("streamSettings", streamSettings)
    }

    /** vless and trojan share the same user@host:port?query#remark shape. */
    private fun parseVlessOrTrojan(link: String, isTrojan: Boolean): JSONObject {
        val uri = runCatching { java.net.URI(link) }
            .getOrElse { throw ParseException("Couldn't parse this ${if (isTrojan) "trojan" else "vless"}:// link") }
        val userInfo = uri.userInfo ?: throw ParseException("Link is missing a ${if (isTrojan) "password" else "user id"}")
        val address = uri.host ?: throw ParseException("Link is missing a server address")
        val port = if (uri.port > 0) uri.port else throw ParseException("Link is missing a port")

        val query = parseQuery(uri.rawQuery)
        val network = query["type"] ?: "tcp"
        val security = query["security"] ?: "none"
        val sni = query["sni"] ?: query["host"] ?: address
        val path = query["path"] ?: ""
        val host = query["host"] ?: ""
        val serviceName = query["serviceName"] ?: path
        val flow = query["flow"]

        val streamSettings = JSONObject().put("network", network)
        applyTransport(streamSettings, network, host, path, serviceName)
        applyTls(streamSettings, security, sni)

        val outbound = JSONObject().put("tag", "proxy")
        return if (isTrojan) {
            outbound.put("protocol", "trojan").put(
                "settings", JSONObject().put(
                    "servers", JSONArray().put(
                        JSONObject().put("address", address).put("port", port).put("password", userInfo)
                    )
                )
            ).put("streamSettings", streamSettings)
        } else {
            val user = JSONObject().put("id", userInfo).put("encryption", "none")
            if (!flow.isNullOrBlank()) user.put("flow", flow)
            outbound.put("protocol", "vless").put(
                "settings", JSONObject().put(
                    "vnext", JSONArray().put(
                        JSONObject().put("address", address).put("port", port).put("users", JSONArray().put(user))
                    )
                )
            ).put("streamSettings", streamSettings)
        }
    }

    private fun parseShadowsocks(link: String): JSONObject {
        val body = link.removePrefix("ss://").substringBefore('#')
        val (methodPass, hostPort) = if (body.contains('@')) {
            val at = body.lastIndexOf('@')
            val left = body.substring(0, at)
            val decodedLeft = runCatching { b64Decode(left) }.getOrDefault(left)
            decodedLeft to body.substring(at + 1)
        } else {
            val decoded = runCatching { b64Decode(body) }.getOrElse { throw ParseException("Couldn't decode this ss:// link") }
            val at = decoded.lastIndexOf('@')
            if (at < 0) throw ParseException("ss:// link is missing a server address")
            decoded.substring(0, at) to decoded.substring(at + 1)
        }
        val methodParts = methodPass.split(":", limit = 2)
        if (methodParts.size != 2) throw ParseException("ss:// link is missing a method/password")
        val (method, password) = methodParts
        val hostPortClean = hostPort.substringBefore('/').substringBefore('?')
        val hpParts = hostPortClean.split(":")
        if (hpParts.size != 2) throw ParseException("ss:// link is missing host:port")
        val address = hpParts[0]
        val port = hpParts[1].toIntOrNull() ?: throw ParseException("ss:// link has an invalid port")

        return JSONObject()
            .put("tag", "proxy")
            .put("protocol", "shadowsocks")
            .put(
                "settings", JSONObject().put(
                    "servers", JSONArray().put(
                        JSONObject().put("address", address).put("port", port)
                            .put("method", method).put("password", password)
                    )
                )
            )
    }

    private fun applyTransport(streamSettings: JSONObject, network: String, host: String, path: String, serviceName: String) {
        when (network.lowercase()) {
            "ws" -> streamSettings.put(
                "wsSettings",
                JSONObject().put("path", path).apply {
                    if (host.isNotBlank()) put("headers", JSONObject().put("Host", host))
                }
            )
            "grpc" -> streamSettings.put("grpcSettings", JSONObject().put("serviceName", serviceName))
            "h2", "http" -> streamSettings.put(
                "httpSettings",
                JSONObject().put("path", path.ifBlank { "/" }).apply {
                    if (host.isNotBlank()) put("host", JSONArray().put(host))
                }
            )
        }
    }

    private fun applyTls(streamSettings: JSONObject, security: String, sni: String) {
        when (security.lowercase()) {
            "tls" -> {
                streamSettings.put("security", "tls")
                streamSettings.put("tlsSettings", JSONObject().put("serverName", sni).put("allowInsecure", false))
            }
            "reality" -> {
                // Reality needs a public key / short id from the provider; the
                // basic serverName is set so a real config still parses, but
                // full reality support means editing the raw JSON (advanced).
                streamSettings.put("security", "reality")
                streamSettings.put("realitySettings", JSONObject().put("serverName", sni))
            }
            else -> streamSettings.put("security", "none")
        }
    }

    private fun parseQuery(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split("&").mapNotNull { pair ->
            val idx = pair.indexOf('=')
            if (idx < 0) return@mapNotNull null
            val k = pair.substring(0, idx)
            val v = java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
            k to v
        }.toMap()
    }
}
