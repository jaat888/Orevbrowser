package com.privbrowse.app.v2ray

import android.content.Context
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object WebViewProxy {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    fun apply(context: Context) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            throw UnsupportedOperationException("This Android System WebView does not support process-level proxy override")
        }
        val config = ProxyConfig.Builder()
            .addProxyRule("socks://127.0.0.1:${XrayConfigBuilder.WEBVIEW_SOCKS_PORT}")
            .addBypassRule("localhost")
            .addBypassRule("127.0.0.1")
            .build()
        ProxyController.getInstance().setProxyOverride(config, executor) { /* applied */ }
    }

    fun clear(context: Context) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) return
        ProxyController.getInstance().clearProxyOverride(executor) { /* cleared */ }
    }
}
