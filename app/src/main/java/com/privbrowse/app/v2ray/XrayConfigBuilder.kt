package com.privbrowse.app.v2ray

import org.json.JSONArray
import org.json.JSONObject

object XrayConfigBuilder {
    private const val MAX_CONFIG_BYTES = 15 * 1024 * 1024
    const val TUN_PORTLESS_TAG = "privbrowse-tun"
    const val WEBVIEW_SOCKS_PORT = 10808

    private fun normalizeDomain(raw: String): String? {
        val value = raw.trim().lowercase().removeSuffix(".")
        if (value.isBlank()) return null
        if (value.startsWith("full:") || value.startsWith("domain:") || value.startsWith("regexp:")) return value
        if (value.contains("/") || value.contains(":") || value.contains(" ")) return null
        return "domain:$value"
    }

    private fun checkSize(raw: String) {
        require(raw.toByteArray(Charsets.UTF_8).size <= MAX_CONFIG_BYTES) { "V2Ray configuration is too large" }
    }

    private fun proxyTag(root: JSONObject): String {
        val outbounds = root.optJSONArray("outbounds") ?: throw IllegalArgumentException("Config has no outbounds")
        for (i in 0 until outbounds.length()) {
            val o = outbounds.optJSONObject(i) ?: continue
            val tag = o.optString("tag")
            val protocol = o.optString("protocol").lowercase()
            if (tag.isNotBlank() && tag !in setOf("direct", "freedom", "block", "blackhole") && protocol !in setOf("freedom", "blackhole", "dns")) return tag
        }
        throw IllegalArgumentException("Add at least one tagged proxy outbound to the V2Ray JSON config")
    }

    private fun ensureDirectOutbound(outbounds: JSONArray) {
        for (i in 0 until outbounds.length()) {
            val tag = outbounds.optJSONObject(i)?.optString("tag").orEmpty()
            val protocol = outbounds.optJSONObject(i)?.optString("protocol").orEmpty().lowercase()
            if (tag == "direct" || protocol == "freedom") return
        }
        outbounds.put(JSONObject().put("tag", "direct").put("protocol", "freedom").put("settings", JSONObject()))
    }

    private fun buildRouting(root: JSONObject, proxyTag: String, splitDomains: Set<String>): JSONObject {
        val routing = root.optJSONObject("routing") ?: JSONObject()
        val existing = routing.optJSONArray("rules") ?: JSONArray()
        val rules = JSONArray()
        if (splitDomains.isNotEmpty()) {
            val domains = JSONArray()
            splitDomains.mapNotNull(::normalizeDomain).distinct().forEach(domains::put)
            if (domains.length() > 0) {
                rules.put(JSONObject().put("type", "field").put("domain", domains).put("outboundTag", proxyTag))
            }
            for (i in 0 until existing.length()) rules.put(existing.optJSONObject(i))
            routing.put("rules", rules)
            routing.put("domainStrategy", routing.optString("domainStrategy", "IPIfNonMatch"))
            routing.put("final", "direct")
        } else {
            for (i in 0 until existing.length()) rules.put(existing.optJSONObject(i))
            routing.put("rules", rules)
            routing.put("final", proxyTag)
        }
        return routing
    }

    fun buildPacketConfig(raw: String, tunFd: Int, splitDomains: Set<String>): String {
        checkSize(raw)
        require(tunFd >= 0) { "Invalid TUN file descriptor" }
        val root = JSONObject(raw)
        val outbounds = root.optJSONArray("outbounds") ?: throw IllegalArgumentException("No outbounds in JSON")
        val tag = proxyTag(root)
        ensureDirectOutbound(outbounds)
        root.put("outbounds", outbounds)
        root.put("inbounds", JSONArray().put(
            JSONObject()
                .put("tag", TUN_PORTLESS_TAG)
                .put("protocol", "tun")
                .put("settings", JSONObject().put("name", "privbrowse0").put("mtu", 1500))
                .put("sniffing", JSONObject().put("enabled", true).put("destOverride", JSONArray().put("http").put("tls").put("quic")).put("routeOnly", true))
        ))
        val env = root.optJSONObject("env") ?: JSONObject()
        env.put("xray.tun.fd", tunFd.toString())
        root.put("env", env)
        root.put("routing", buildRouting(root, tag, splitDomains))
        return root.toString()
    }

    fun buildWebViewConfig(raw: String, splitDomains: Set<String>): String {
        checkSize(raw)
        val root = JSONObject(raw)
        val outbounds = root.optJSONArray("outbounds") ?: throw IllegalArgumentException("No outbounds in JSON")
        val tag = proxyTag(root)
        ensureDirectOutbound(outbounds)
        root.put("outbounds", outbounds)
        root.put("inbounds", JSONArray().put(
            JSONObject()
                .put("listen", "127.0.0.1")
                .put("port", WEBVIEW_SOCKS_PORT)
                .put("protocol", "socks")
                .put("settings", JSONObject().put("auth", "noauth").put("udp", true))
                .put("sniffing", JSONObject().put("enabled", true).put("destOverride", JSONArray().put("http").put("tls").put("quic")).put("routeOnly", true))
        ))
        root.put("routing", buildRouting(root, tag, splitDomains))
        return root.toString()
    }
}
