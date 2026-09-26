package com.privbrowse.app.ui

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.VpnService
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.privbrowse.app.R
import com.privbrowse.app.v2ray.V2RayLinkParser
import com.privbrowse.app.v2ray.V2RayStore
import com.privbrowse.app.v2ray.WebViewProxy
import com.privbrowse.app.v2ray.V2RayVpnService
import com.privbrowse.app.v2ray.XrayWebViewProxyService

/**
 * Simple-first V2Ray/Xray screen, styled after apps like HTTP Custom /
 * DarkTunnel: one field to paste a server link, one button that toggles
 * connect/disconnect, and a big status readout. Routing rules and tunnel
 * mode — things most people never touch — live behind a collapsed
 * "Advanced" section instead of sitting in the main flow.
 */
class V2RayActivity : androidx.appcompat.app.AppCompatActivity() {
    companion object { private const val REQ_VPN = 2603 }

    private lateinit var link: EditText
    private lateinit var statusDot: View
    private lateinit var statusText: TextView
    private lateinit var connectButton: MaterialButton
    private lateinit var advancedBody: LinearLayout
    private lateinit var advancedToggle: TextView
    private lateinit var split: EditText
    private lateinit var mode: RadioGroup

    private var connected = false

    private fun dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()
    private fun color(res: Int) = ContextCompat.getColor(this, res)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }

        root.addView(TextView(this).apply {
            text = "V2Ray / Xray"
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(color(R.color.text_light))
        })

        // Big, unmissable status readout — the thing DarkTunnel-style apps
        // lead with, so you know at a glance if you're protected.
        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(16), 0, dp(16))
        }
        statusDot = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(14), dp(14)).apply { marginEnd = dp(10) }
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color(R.color.text_muted_light)) }
        }
        statusText = TextView(this).apply {
            text = "Disconnected"
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(color(R.color.text_light))
        }
        statusRow.addView(statusDot)
        statusRow.addView(statusText)
        root.addView(statusRow)

        // ---- The one field most people need ----
        val linkCard = card()
        val linkInner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)) }
        val linkLayout = TextInputLayout(this).apply {
            hint = "Paste your server link (vmess:// vless:// trojan:// ss://)"
        }
        link = TextInputEditText(linkLayout.context).apply {
            minLines = 3
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setText(V2RayStore.getConfig(this@V2RayActivity))
        }
        linkLayout.addView(link)
        linkInner.addView(linkLayout)
        linkInner.addView(outlinedButton("Paste from clipboard") { paste() }, lp().apply { topMargin = dp(12) })
        linkCard.addView(linkInner)
        root.addView(linkCard, lp())

        // ---- One button that does the obvious thing ----
        connectButton = filledButton("Connect") { onConnectButton() }
        root.addView(connectButton, lp().apply { topMargin = dp(16) })

        // ---- Everything else, collapsed ----
        advancedToggle = TextView(this).apply {
            text = "Advanced ▾"
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(color(R.color.accent))
            setPadding(dp(4), dp(24), 0, dp(8))
            setOnClickListener { toggleAdvanced() }
        }
        root.addView(advancedToggle)

        advancedBody = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        val routingCard = card()
        val routingInner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)) }
        routingInner.addView(TextView(this).apply {
            text = "Routing"
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(color(R.color.text_light))
            setPadding(0, 0, 0, dp(8))
        })
        val splitLayout = TextInputLayout(this).apply { hint = "Proxy only these domains (optional), one per line" }
        split = TextInputEditText(splitLayout.context).apply {
            minLines = 4
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setText(V2RayStore.getSplit(this@V2RayActivity).sorted().joinToString("\n"))
        }
        splitLayout.addView(split)
        routingInner.addView(splitLayout)
        routingCard.addView(routingInner)
        advancedBody.addView(routingCard, lp())

        val modeCard = card()
        val modeInner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(12)) }
        modeInner.addView(TextView(this).apply {
            text = "Mode"
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(color(R.color.text_light))
            setPadding(0, 0, 0, dp(8))
        })
        mode = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        val accentTint = ColorStateList.valueOf(color(R.color.accent))
        val packet = radioButton("Real packet tunnel (whole PrivBrowse app)", 1, accentTint, checked = true)
        val web = radioButton("True WebView-level proxy only", 2, accentTint)
        mode.addView(packet); mode.addView(web)
        modeInner.addView(mode)
        modeCard.addView(modeInner)
        advancedBody.addView(modeCard, lp().apply { topMargin = dp(12) })

        root.addView(advancedBody)

        root.addView(outlinedButton("Done") { finish() }, lp().apply { topMargin = dp(24) })

        ScrollView(this).apply {
            setBackgroundColor(color(R.color.background_light))
            addView(root)
            setContentView(this)
        }
        refreshStatus()
    }

    private fun toggleAdvanced() {
        val show = advancedBody.visibility != View.VISIBLE
        advancedBody.visibility = if (show) View.VISIBLE else View.GONE
        advancedToggle.text = if (show) "Advanced ▴" else "Advanced ▾"
    }

    private fun lp() = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) }

    private fun card() = MaterialCardView(this).apply {
        radius = dp(16).toFloat()
        cardElevation = dp(1).toFloat()
        strokeWidth = 0
        setCardBackgroundColor(color(R.color.surface_light))
    }

    private fun filledButton(text: String, action: () -> Unit) = MaterialButton(this).apply {
        this.text = text
        cornerRadius = dp(14)
        backgroundTintList = ColorStateList.valueOf(color(R.color.accent))
        setTextColor(color(R.color.on_accent))
        setOnClickListener { action() }
    }

    private fun outlinedButton(text: String, action: () -> Unit) = MaterialButton(
        this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
    ).apply {
        this.text = text
        cornerRadius = dp(14)
        strokeColor = ColorStateList.valueOf(color(R.color.accent))
        strokeWidth = dp(1)
        setTextColor(color(R.color.accent))
        setOnClickListener { action() }
    }

    private fun radioButton(text: String, buttonId: Int, tint: ColorStateList, checked: Boolean = false) =
        RadioButton(this).apply {
            id = buttonId
            this.text = text
            isChecked = checked
            buttonTintList = tint
            setTextColor(color(R.color.text_light))
            setPadding(dp(8), dp(8), 0, dp(8))
        }

    /** Accepts either a share-link (vmess/vless/trojan/ss) or raw Xray JSON, pasted straight into the one field. */
    private fun resolveConfig(): Result<String> {
        val raw = link.text.toString().trim()
        if (raw.isBlank()) return Result.failure(V2RayLinkParser.ParseException("Paste a server link first."))
        return if (V2RayLinkParser.looksLikeLink(raw)) {
            runCatching { V2RayLinkParser.parse(raw) }
        } else {
            Result.success(raw) // advanced users can still paste a full JSON config directly
        }
    }

    private fun save(): Boolean {
        val resolved = resolveConfig()
        val json = resolved.getOrElse {
            setStatus(Connection.STOPPED, it.message ?: "Couldn't read that link.")
            return false
        }
        V2RayStore.setConfig(this, json)
        val domains = split.text.toString().lineSequence().map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        V2RayStore.setSplit(this, domains)
        V2RayStore.setEnabled(this, true)
        return true
    }

    private fun onConnectButton() {
        if (connected) disconnectAll() else connect()
    }

    private fun connect() {
        if (!save()) return
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
                    setStatus(Connection.CONNECTING, "Starting WebView proxy…")
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
            setStatus(Connection.CONNECTING, "Starting packet tunnel…")
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
        setStatus(Connection.STOPPED, "Disconnecting…")
    }

    private fun refreshStatus() {
        val state = V2RayStore.runtimeState(this)
        when {
            state.contains("connected", ignoreCase = true) -> setStatus(Connection.CONNECTED, null)
            state.startsWith("error", ignoreCase = true) -> setStatus(Connection.STOPPED, state.removePrefix("error:").trim())
            else -> setStatus(Connection.STOPPED, null)
        }
    }

    private enum class Connection { CONNECTED, CONNECTING, STOPPED }

    private fun setStatus(state: Connection, message: String?) {
        connected = state == Connection.CONNECTED
        val dotColor: Int
        when (state) {
            Connection.CONNECTED -> {
                dotColor = color(R.color.accent)
                statusText.text = "Connected"
                connectButton.text = "Disconnect"
            }
            Connection.CONNECTING -> {
                dotColor = color(R.color.accent)
                statusText.text = message ?: "Connecting…"
                connectButton.text = "Cancel"
            }
            Connection.STOPPED -> {
                dotColor = if (message != null) color(R.color.danger) else color(R.color.text_muted_light)
                statusText.text = message ?: "Disconnected"
                connectButton.text = "Connect"
            }
        }
        (statusDot.background as? GradientDrawable)?.setColor(dotColor)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_VPN && resultCode == Activity.RESULT_OK) startPacketService()
        else if (requestCode == REQ_VPN) setStatus(Connection.STOPPED, "VPN permission was not granted.")
    }

    private fun paste() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        if (text.isNotBlank()) link.setText(text)
    }

    override fun onResume() { super.onResume(); if (::statusText.isInitialized) refreshStatus() }
}
