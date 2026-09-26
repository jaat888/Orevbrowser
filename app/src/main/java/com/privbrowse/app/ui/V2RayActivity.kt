package com.privbrowse.app.ui

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.core.content.ContextCompat
import com.privbrowse.app.R
import com.privbrowse.app.v2ray.V2RayStore
import com.privbrowse.app.v2ray.WebViewProxy
import com.privbrowse.app.v2ray.V2RayVpnService
import com.privbrowse.app.v2ray.XrayWebViewProxyService

class V2RayActivity : androidx.appcompat.app.AppCompatActivity() {
    companion object { private const val REQ_VPN = 2603 }
    private lateinit var config: EditText
    private lateinit var split: EditText
    private lateinit var status: TextView
    private lateinit var mode: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 24, 24, 24) }
        root.addView(TextView(this).apply { text = "Phase 6 — V2Ray / Xray tunnel"; textSize = 22f })
        root.addView(TextView(this).apply { text = "Use your own Xray/V2Ray JSON. Packet mode routes the app through Android VpnService; WebView mode uses AndroidX ProxyController."; setPadding(0, 12, 0, 12) })

        config = EditText(this).apply { hint = "Xray/V2Ray JSON config"; minLines = 10; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE; setText(V2RayStore.getConfig(this@V2RayActivity)) }
        root.addView(config, lp())
        root.addView(button("Paste JSON from clipboard") { paste() })

        root.addView(TextView(this).apply { text = "Routing"; setPadding(0, 16, 0, 4) })
        split = EditText(this).apply { hint = "Proxy only these domains (optional), one per line"; minLines = 4; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE; setText(V2RayStore.getSplit(this@V2RayActivity).sorted().joinToString("\n")) }
        root.addView(split, lp())

        root.addView(TextView(this).apply { text = "Mode"; setPadding(0, 16, 0, 4) })
        mode = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        val packet = RadioButton(this).apply { id = 1; text = "Real packet tunnel (whole PrivBrowse app)"; isChecked = true }
        val web = RadioButton(this).apply { id = 2; text = "True WebView-level proxy only" }
        mode.addView(packet); mode.addView(web); root.addView(mode)

        root.addView(button("Save config") { save(); status.text = "Configuration saved." })
        root.addView(button("Connect") { connect() })
        root.addView(button("Disconnect") { disconnectAll() })
        status = TextView(this).apply { setPadding(0, 16, 0, 0) }
        root.addView(status)
        root.addView(button("Done") { finish() })
        ScrollView(this).apply { addView(root); setContentView(this) }
        refreshStatus()
    }

    private fun lp() = LinearLayout.LayoutParams(-1, -2).apply { topMargin = 8 }
    private fun button(text: String, action: () -> Unit) = Button(this).apply { this.text = text; setOnClickListener { action() }; layoutParams = lp() }

    private fun save() {
        V2RayStore.setConfig(this, config.text.toString().trim())
        val domains = split.text.toString().lineSequence().map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        V2RayStore.setSplit(this, domains)
        V2RayStore.setEnabled(this, true)
    }

    private fun connect() {
        save()
        if (V2RayStore.getConfig(this).isBlank()) { status.text = "Paste a valid Xray JSON first."; return }
        val domains = ArrayList(V2RayStore.getSplit(this).sorted())
        val session = V2RayStore.beginSession(this)
        when (mode.checkedRadioButtonId) {
            2 -> {
                startService(Intent(this, V2RayVpnService::class.java).apply {
                    action = V2RayVpnService.ACTION_DISCONNECT
                    putExtra(V2RayVpnService.EXTRA_PRESERVE_SESSION, true)
                })
                stopCoreThen {
                    val i = Intent(this, XrayWebViewProxyService::class.java).apply {
                        action = XrayWebViewProxyService.ACTION_CONNECT
                        putExtra(XrayWebViewProxyService.EXTRA_CONFIG, V2RayStore.getConfig(this@V2RayActivity))
                        putStringArrayListExtra(XrayWebViewProxyService.EXTRA_SPLIT, domains)
                        putExtra(XrayWebViewProxyService.EXTRA_SESSION, session)
                    }
                    ContextCompat.startForegroundService(this, i)
                    status.text = "Starting WebView proxy…"
                }
            }
            else -> {
                startService(Intent(this, XrayWebViewProxyService::class.java).apply {
                    action = XrayWebViewProxyService.ACTION_DISCONNECT
                    putExtra(XrayWebViewProxyService.EXTRA_PRESERVE_SESSION, true)
                })
                WebViewProxy.clear(this)
                val consent = VpnService.prepare(this)
                if (consent != null) startActivityForResult(consent, REQ_VPN) else startPacketService()
            }
        }
    }

    private fun startPacketService() {
        val i = Intent(this, V2RayVpnService::class.java).apply {
            action = V2RayVpnService.ACTION_CONNECT
            putExtra(V2RayVpnService.EXTRA_CONFIG, V2RayStore.getConfig(this@V2RayActivity))
            putStringArrayListExtra(V2RayVpnService.EXTRA_SPLIT, ArrayList(V2RayStore.getSplit(this@V2RayActivity).sorted()))
            putExtra(V2RayVpnService.EXTRA_SESSION, V2RayStore.activeSession(this@V2RayActivity))
        }
        stopCoreThen {
            ContextCompat.startForegroundService(this, i)
            status.text = "Starting packet tunnel…"
        }
    }

    private fun stopCoreThen(next: () -> Unit) {
        Thread {
            runCatching { com.privbrowse.app.v2ray.LibXrayBridge.invokeStop() }
            runCatching { com.privbrowse.app.v2ray.LibXrayBridge.detachVpn() }
            runOnUiThread { if (!isFinishing) next() }
        }.start()
    }

    private fun disconnectAll() {
        startService(Intent(this, V2RayVpnService::class.java).setAction(V2RayVpnService.ACTION_DISCONNECT))
        startService(Intent(this, XrayWebViewProxyService::class.java).setAction(XrayWebViewProxyService.ACTION_DISCONNECT))
        V2RayStore.clearSession(this)
        status.text = "Disconnecting…"
    }

    private fun refreshStatus() { status.text = "Status: ${V2RayStore.runtimeState(this)}" }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_VPN && resultCode == Activity.RESULT_OK) startPacketService()
        else if (requestCode == REQ_VPN) status.text = "VPN permission was not granted."
    }

    private fun paste() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        if (text.isNotBlank()) config.setText(text)
    }

    override fun onResume() { super.onResume(); if (::status.isInitialized) refreshStatus() }
}
