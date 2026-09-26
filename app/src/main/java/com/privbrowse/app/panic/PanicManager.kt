package com.privbrowse.app.panic

import android.content.Context
import android.content.Intent
import android.webkit.CookieManager
import android.webkit.WebStorage
import com.privbrowse.app.data.DbHelper
import com.privbrowse.app.ui.BrowserTab
import com.privbrowse.app.v2ray.V2RayVpnService
import com.privbrowse.app.v2ray.XrayWebViewProxyService
import com.privbrowse.app.v2ray.WebViewProxy
import com.privbrowse.app.v2ray.V2RayStore
import com.privbrowse.app.vpn.PrivBrowseVpnService

object PanicManager {
    fun wipe(context: Context, tabs: Collection<BrowserTab>, db: DbHelper) {
        tabs.forEach { tab ->
            runCatching { tab.webView.stopLoading(); tab.webView.clearHistory(); tab.webView.clearCache(true); tab.webView.clearFormData() }
        }
        runCatching { CookieManager.getInstance().removeAllCookies(null); CookieManager.getInstance().flush() }
        runCatching { WebStorage.getInstance().deleteAllData() }
        db.clearHistory(); db.clearNetworkLog()
        V2RayStore.clearSession(context)
        context.startService(Intent(context, V2RayVpnService::class.java).setAction(V2RayVpnService.ACTION_DISCONNECT))
        context.startService(Intent(context, XrayWebViewProxyService::class.java).setAction(XrayWebViewProxyService.ACTION_DISCONNECT))
        context.startService(Intent(context, PrivBrowseVpnService::class.java).setAction(PrivBrowseVpnService.ACTION_DISCONNECT))
        runCatching { WebViewProxy.clear(context) }
    }
}
