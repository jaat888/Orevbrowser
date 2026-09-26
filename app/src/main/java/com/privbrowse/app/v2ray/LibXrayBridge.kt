package com.privbrowse.app.v2ray

import android.net.VpnService
import libXray.DialerController
import libXray.LibXray
import org.json.JSONObject

/** Thin adapter around the official XTLS/libXray Android API. */
object LibXrayBridge {
    private val controller = object : DialerController {
        @Volatile var vpn: VpnService? = null
        override fun protectFd(fd: Int): Boolean = vpn?.protect(fd) ?: false
    }
    @Volatile private var registered = false

    private fun ensureControllerRegistered() {
        if (registered) return
        synchronized(this) {
            if (registered) return
            LibXray.registerDialerController(controller)
            LibXray.registerListenerController(controller)
            registered = true
        }
    }

    @Synchronized fun attachVpn(service: VpnService) {
        ensureControllerRegistered()
        controller.vpn = service
        LibXray.setDNS(controller, "1.1.1.1:53")
    }

    @Synchronized fun detachVpn() {
        controller.vpn = null
        runCatching { LibXray.resetDNS() }
    }

    @Synchronized fun invokeTest(configJson: String): String {
        val request = JSONObject()
            .put("apiVersion", 3)
            .put("method", "testXray")
            .put("payload", JSONObject().put("xrayJson", configJson))
        return LibXray.invoke(request.toString())
    }

    @Synchronized fun invokeRun(configJson: String): String {
        val request = JSONObject()
            .put("apiVersion", 3)
            .put("method", "runXray")
            .put("payload", JSONObject().put("xrayJson", configJson))
        return LibXray.invoke(request.toString())
    }

    @Synchronized fun invokeStop(): String = LibXray.invoke(
        JSONObject().put("apiVersion", 3).put("method", "stopXray").put("payload", JSONObject()).toString()
    )

    fun assertSuccess(response: String) {
        val json = JSONObject(response)
        if (!json.optBoolean("success", false)) {
            throw IllegalStateException(json.optString("error").ifBlank { "Xray rejected the configuration" })
        }
    }
}
