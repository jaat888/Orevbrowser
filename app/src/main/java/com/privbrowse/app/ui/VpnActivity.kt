package com.privbrowse.app.ui

import android.app.Activity
import android.app.AlertDialog
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
import com.privbrowse.app.v2ray.LibXrayBridge
import com.privbrowse.app.v2ray.V2RayLinkParser
import com.privbrowse.app.v2ray.V2RayStore
import com.privbrowse.app.v2ray.V2RayVpnService
import com.privbrowse.app.v2ray.WebViewProxy
import com.privbrowse.app.v2ray.XrayWebViewProxyService
import org.json.JSONObject

/**
 * One VPN screen instead of two confusing ones. A small segmented control
 * at the top switches between:
 *
 *  - Quick: the everyday screen. One saved server, one big on/off control,
 *    nothing else to think about. This is what "Normal" mode means here.
 *  - Custom: paste a server link (vmess/vless/trojan/ss, or a raw Xray
 *    JSON config) and tune routing/tunnel mode. This is "Hard"/advanced
 *    mode — everything that used to live on the separate V2Ray screen.
 *
 * A "Public relay list" entry under Quick keeps the VPN Gate discovery
 * feature reachable; tapping a relay now actually connects through the
 * ics-openvpn engine (GPLv2 — see NOTICE.md and PrivBrowseVpnService).
 */
class VpnActivity : androidx.appcompat.app.AppCompatActivity() {

    companion object { private const val REQ_VPN = 2603; private const val REQ_VPN_RELAY = 2604 }

    private enum class Tab { QUICK, CUSTOM }
    private var tab = Tab.QUICK

    // Shared status
    private lateinit var statusDot: View
    private lateinit var statusText: TextView
    private lateinit var statusSub: TextView
    private var connected = false

    // Tabs
    private lateinit var tabQuickBtn: TextView
    private lateinit var tabCustomBtn: TextView
    private lateinit var quickSection: LinearLayout
    private lateinit var customSection: LinearLayout

    // Quick
    private lateinit var quickConnectButton: MaterialButton
    private lateinit var quickServerLabel: TextView

    // Custom
    private lateinit var link: EditText
    private lateinit var label: EditText
    private lateinit var split: EditText
    private lateinit var mode: RadioGroup
    private lateinit var advancedBody: LinearLayout
    private lateinit var advancedToggle: TextView
    private lateinit var customSaveConnectButton: MaterialButton

    private fun dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()
    private fun color(res: Int) = ContextCompat.getColor(this, res)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.vpn_menu)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }

        root.addView(TextView(this).apply {
            text = getString(R.string.vpn_menu)
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(color(R.color.text_light))
        })

        root.addView(buildTabRow(), lp().apply { topMargin = dp(16) })

        // Big, unmissable status readout shared by both tabs.
        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(16), 0, dp(4))
        }
        statusDot = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(14), dp(14)).apply { marginEnd = dp(10) }
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color(R.color.text_muted_light)) }
        }
        val statusTextCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        statusText = TextView(this).apply {
            text = "Disconnected"
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(color(R.color.text_light))
        }
        statusSub = TextView(this).apply {
            textSize = 13f
            setTextColor(color(R.color.text_muted_light))
            visibility = View.GONE
        }
        statusTextCol.addView(statusText)
        statusTextCol.addView(statusSub)
        statusRow.addView(statusDot)
        statusRow.addView(statusTextCol)
        root.addView(statusRow)

        quickSection = buildQuickSection()
        customSection = buildCustomSection()
        root.addView(quickSection)
        root.addView(customSection)

        root.addView(outlinedButton("Done") { finish() }, lp().apply { topMargin = dp(24) })

        ScrollView(this).apply {
            setBackgroundColor(color(R.color.background_light))
            addView(root)
            setContentView(this)
        }

        showTab(Tab.QUICK)
        refreshStatus()
    }

    // ---------------------------------------------------------------- Tabs

    private fun buildTabRow(): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(color(R.color.surface_light_alt))
            }
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        tabQuickBtn = tabButton(getString(R.string.vpn_tab_quick)) { showTab(Tab.QUICK) }
        tabCustomBtn = tabButton(getString(R.string.vpn_tab_custom)) { showTab(Tab.CUSTOM) }
        row.addView(tabQuickBtn, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(tabCustomBtn, LinearLayout.LayoutParams(0, -2, 1f))
        return row
    }

    private fun tabButton(text: String, action: () -> Unit) = TextView(this).apply {
        this.text = text
        gravity = Gravity.CENTER
        textSize = 14f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(10), 0, dp(10))
        isClickable = true
        isFocusable = true
        setOnClickListener { action() }
    }

    private fun showTab(t: Tab) {
        tab = t
        quickSection.visibility = if (t == Tab.QUICK) View.VISIBLE else View.GONE
        customSection.visibility = if (t == Tab.CUSTOM) View.VISIBLE else View.GONE

        val activeBg = GradientDrawable().apply { cornerRadius = dp(11).toFloat(); setColor(color(R.color.accent)) }
        tabQuickBtn.background = if (t == Tab.QUICK) activeBg else null
        tabQuickBtn.setTextColor(if (t == Tab.QUICK) color(R.color.on_accent) else color(R.color.text_muted_light))
        val activeBg2 = GradientDrawable().apply { cornerRadius = dp(11).toFloat(); setColor(color(R.color.accent)) }
        tabCustomBtn.background = if (t == Tab.CUSTOM) activeBg2 else null
        tabCustomBtn.setTextColor(if (t == Tab.CUSTOM) color(R.color.on_accent) else color(R.color.text_muted_light))
    }

    // --------------------------------------------------------- Quick tab

    private fun buildQuickSection(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL

        val card = card()
        val inner = LinearLayout(this@VpnActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        quickServerLabel = TextView(this@VpnActivity).apply {
            textSize = 15f
            setTextColor(color(R.color.text_light))
        }
        inner.addView(quickServerLabel)
        card.addView(inner)
        addView(card, lp().apply { topMargin = dp(12) })

        quickConnectButton = filledButton("Connect") { onQuickConnectButton() }
        addView(quickConnectButton, lp().apply { topMargin = dp(16) })

        addView(TextView(this@VpnActivity).apply {
            text = getString(R.string.vpn_manage_servers)
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(color(R.color.accent))
            setPadding(dp(4), dp(16), 0, dp(4))
            setOnClickListener { showTab(Tab.CUSTOM) }
        })

        addView(TextView(this@VpnActivity).apply {
            text = getString(R.string.vpn_public_relay_button)
            textSize = 13f
            setTextColor(color(R.color.text_muted_light))
            setPadding(dp(4), dp(8), 0, dp(4))
            setOnClickListener { showPublicRelayDialog() }
        })

        addView(TextView(this@VpnActivity).apply {
            text = "Tor (via Orbot)"
            textSize = 13f
            setTextColor(color(R.color.text_muted_light))
            setPadding(dp(4), dp(8), 0, dp(4))
            setOnClickListener { showTorDialog() }
        })
    }

    // -------------------------------------------------- Tor via Orbot
    // We don't ship or reimplement Tor ourselves — that's real cryptographic,
    // security-critical code that belongs to people who specialize in it.
    // Orbot (org.torproject.android) already is that, open-source and
    // widely audited. If it's installed and its local SOCKS port actually
    // answers, we point WebView's proxy at it — same mechanism as the
    // V2Ray tunnel above. If it isn't reachable, this says so plainly
    // instead of pretending to be connected.

    private fun showTorDialog() {
        val installed = com.privbrowse.app.tor.TorBridge.isOrbotInstalled(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(4))
        }
        val statusLine = TextView(this).apply {
            textSize = 13f
            setTextColor(color(R.color.text_light))
            setPadding(0, 0, 0, dp(14))
        }
        container.addView(TextView(this).apply {
            text = "Routes browser traffic through Orbot's local Tor proxy. Orbot is a separate, independent open-source app (Guardian Project) — PrivBrowse only points itself at Orbot once Orbot itself reports it's actually running."
            textSize = 12f
            setTextColor(color(R.color.text_muted_light))
            setPadding(0, 0, 0, dp(12))
        })
        container.addView(statusLine)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Tor (via Orbot)")
            .setView(container)
            .setPositiveButton(if (installed) "Check & connect" else "Get Orbot", null)
            .setNeutralButton("Open Orbot") { _, _ -> com.privbrowse.app.tor.TorBridge.openOrbot(this) }
            .setNegativeButton(R.string.close, null)
            .create()
        dialog.show()

        fun refreshStatus() {
            if (!installed) {
                statusLine.text = "Orbot isn't installed. Install it, start Tor inside Orbot, then come back here."
                return
            }
            statusLine.text = "Checking whether Orbot's Tor proxy is running..."
            Thread {
                val reachable = com.privbrowse.app.tor.TorBridge.isOrbotReachable()
                runOnUiThread {
                    if (isFinishing) return@runOnUiThread
                    statusLine.text = if (reachable)
                        "Orbot's Tor proxy is running on 127.0.0.1:9050."
                    else
                        "Orbot's Tor proxy isn't answering yet. Open Orbot and tap its own Start/power button, then check again."
                }
            }.start()
        }
        refreshStatus()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (!installed) {
                startActivity(Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(com.privbrowse.app.tor.TorBridge.playStoreUrl())))
                return@setOnClickListener
            }
            statusLine.text = "Checking..."
            Thread {
                val reachable = com.privbrowse.app.tor.TorBridge.isOrbotReachable()
                runOnUiThread {
                    if (isFinishing) return@runOnUiThread
                    if (reachable) {
                        com.privbrowse.app.tor.TorBridge.applyProxy(this)
                        setStatus(Connection.CONNECTED, "Browsing through Tor (Orbot)")
                        connected = true
                        refreshQuickLabel()
                        dialog.dismiss()
                    } else {
                        statusLine.text = "Still not reachable — start Tor inside Orbot first, then tap Check & connect again."
                    }
                }
            }.start()
        }
    }

    private fun refreshQuickLabel() {
        val savedLabel = V2RayStore.getLabel(this)
        val hasServer = V2RayStore.getConfig(this).isNotBlank()
        quickServerLabel.text = when {
            !hasServer -> getString(R.string.vpn_quick_no_server) + "\n" + getString(R.string.vpn_quick_no_server_sub)
            savedLabel.isNotBlank() -> getString(R.string.vpn_quick_saved_server) + ": " + savedLabel
            else -> getString(R.string.vpn_quick_saved_server)
        }
        quickConnectButton.text = when {
            connected -> "Disconnect"
            !hasServer -> getString(R.string.vpn_quick_add_server)
            else -> "Connect"
        }
    }

    private fun onQuickConnectButton() {
        if (connected) { disconnectAll(); return }
        if (V2RayStore.getConfig(this).isBlank()) { showTab(Tab.CUSTOM); return }
        connectSavedConfig()
    }

    private fun connectSavedConfig() {
        val domains = ArrayList(V2RayStore.getSplit(this).sorted())
        val session = V2RayStore.beginSession(this)
        when (V2RayStore.getTunnelMode(this)) {
            2 -> {
                startService(Intent(this, V2RayVpnService::class.java).apply {
                    action = V2RayVpnService.ACTION_DISCONNECT
                    putExtra(V2RayVpnService.EXTRA_PRESERVE_SESSION, true)
                })
                stopCoreThen {
                    val i = Intent(this, XrayWebViewProxyService::class.java).apply {
                        action = XrayWebViewProxyService.ACTION_CONNECT
                        putExtra(XrayWebViewProxyService.EXTRA_CONFIG, V2RayStore.getConfig(this@VpnActivity))
                        putStringArrayListExtra(XrayWebViewProxyService.EXTRA_SPLIT, domains)
                        putExtra(XrayWebViewProxyService.EXTRA_SESSION, session)
                    }
                    ContextCompat.startForegroundService(this, i)
                    setStatus(Connection.CONNECTING, "Starting WebView proxy...")
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

    // -------------------------------------------------- Public relay list

    private var relayFetchThread: Thread? = null

    private fun showPublicRelayDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(4))
        }
        container.addView(TextView(this).apply {
            text = getString(R.string.vpn_disclaimer)
            textSize = 12f
            setTextColor(color(R.color.text_muted_light))
            setPadding(0, 0, 0, dp(12))
        })
        container.addView(TextView(this).apply {
            text = getString(R.string.vpn_discovery_note)
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(color(R.color.text_light))
            setPadding(0, 0, 0, dp(12))
        })

        val resultsBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        container.addView(resultsBox)

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.vpn_discover)
            .setView(ScrollView(this).apply { addView(container) })
            .setPositiveButton("Refresh", null) // wired below so it doesn't auto-dismiss
            .setNegativeButton(R.string.close, null)
            .create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            fetchRelayList(resultsBox)
        }
        fetchRelayList(resultsBox)
    }

    private fun fetchRelayList(resultsBox: LinearLayout) {
        resultsBox.removeAllViews()
        resultsBox.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
            addView(ProgressBar(this@VpnActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(20), dp(20)).apply { marginEnd = dp(10) }
            })
            addView(TextView(this@VpnActivity).apply {
                text = getString(R.string.vpn_status_discovering)
                setTextColor(color(R.color.text_muted_light))
            })
        })

        relayFetchThread?.interrupt()
        relayFetchThread = Thread {
            val result = runCatching {
                com.privbrowse.app.vpn.VpnServerSelector.rank(
                    com.privbrowse.app.vpn.VpnGateApi.fetchServers()
                ).take(15)
            }
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                resultsBox.removeAllViews()
                result.onSuccess { servers ->
                    if (servers.isEmpty()) {
                        resultsBox.addView(relayMessage("No usable servers responded just now. Try Refresh in a bit."))
                    } else {
                        servers.forEach { s -> resultsBox.addView(relayRow(s)) }
                    }
                }.onFailure { e ->
                    resultsBox.addView(
                        relayMessage(
                            "Couldn't reach VPN Gate (${e.javaClass.simpleName}: ${e.message ?: "no details"}). " +
                                "Check your internet connection and try Refresh."
                        )
                    )
                }
            }
        }.also { it.start() }
    }

    private fun startVpnGateService() {
        ContextCompat.startForegroundService(
            this,
            Intent(this, com.privbrowse.app.vpn.PrivBrowseVpnService::class.java)
                .setAction(com.privbrowse.app.vpn.PrivBrowseVpnService.ACTION_CONNECT)
        )
    }

    private fun relayMessage(text: String) = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(color(R.color.text_muted_light))
        setPadding(dp(4), dp(8), dp(4), dp(8))
    }

    private fun relayRow(s: com.privbrowse.app.vpn.VpnServer): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(color(R.color.surface_light_alt))
            }
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        top.addView(TextView(this).apply {
            text = "${s.countryLong.ifBlank { s.countryShort }} (${s.countryShort})"
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(color(R.color.text_light))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(TextView(this).apply {
            text = if (s.isUdp()) "UDP" else "TCP"
            textSize = 12f
            setTextColor(color(R.color.accent))
        })
        row.addView(top)
        val mbps = s.speedBps / 1_000_000.0
        row.addView(TextView(this).apply {
            text = "Ping %dms · %.1f Mbps · %d active sessions".format(s.pingMs, mbps, s.numSessions)
            textSize = 12f
            setTextColor(color(R.color.text_muted_light))
        })
        row.setOnClickListener {
            // Real OpenVPN engine now backs this (ics-openvpn, GPLv2 - see
            // NOTICE.md). The service re-ranks and picks the strongest
            // reachable candidate itself, so every row starts the same
            // "connect to best available" flow.
            Toast.makeText(this, "Connecting via VPN Gate...", Toast.LENGTH_SHORT).show()
            val consent = VpnService.prepare(this)
            if (consent != null) startActivityForResult(consent, REQ_VPN_RELAY) else startVpnGateService()
        }
        val lp = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }
        row.layoutParams = lp
        return row
    }

    // -------------------------------------------------------- Custom tab

    private fun buildCustomSection(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        visibility = View.GONE

        addView(TextView(this@VpnActivity).apply {
            text = getString(R.string.vpn_custom_hint)
            textSize = 12f
            setTextColor(color(R.color.text_muted_light))
            setPadding(0, dp(8), 0, dp(8))
        })

        val linkCard = card()
        val linkInner = LinearLayout(this@VpnActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)) }
        val linkLayout = TextInputLayout(this@VpnActivity).apply {
            hint = "Paste your server link (vmess:// vless:// trojan:// ss://)"
        }
        link = TextInputEditText(linkLayout.context).apply {
            minLines = 3
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setText(V2RayStore.getConfig(this@VpnActivity))
        }
        linkLayout.addView(link)
        linkInner.addView(linkLayout)

        val labelLayout = TextInputLayout(this@VpnActivity).apply { hint = getString(R.string.vpn_custom_label_hint) }
        label = TextInputEditText(labelLayout.context).apply { setText(V2RayStore.getLabel(this@VpnActivity)) }
        labelLayout.addView(label)
        linkInner.addView(labelLayout, lp().apply { topMargin = dp(8) })

        linkInner.addView(outlinedButton("Paste from clipboard") { paste() }, lp().apply { topMargin = dp(12) })
        linkCard.addView(linkInner)
        addView(linkCard, lp())

        val buttonRow = LinearLayout(this@VpnActivity).apply { orientation = LinearLayout.HORIZONTAL }
        customSaveConnectButton = filledButton(getString(R.string.vpn_save_connect)) { onCustomSaveAndConnect() }
        buttonRow.addView(
            customSaveConnectButton,
            LinearLayout.LayoutParams(0, -2, 1f).apply { topMargin = dp(16); marginEnd = dp(8) }
        )
        buttonRow.addView(
            outlinedButton(getString(R.string.vpn_save_only)) { onCustomSaveOnly() },
            LinearLayout.LayoutParams(0, -2, 1f).apply { topMargin = dp(16); marginStart = dp(8) }
        )
        addView(buttonRow)

        advancedToggle = TextView(this@VpnActivity).apply {
            text = "Advanced \u25be"
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(color(R.color.accent))
            setPadding(dp(4), dp(24), 0, dp(8))
            setOnClickListener { toggleAdvanced() }
        }
        addView(advancedToggle)

        advancedBody = LinearLayout(this@VpnActivity).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        val routingCard = card()
        val routingInner = LinearLayout(this@VpnActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)) }
        routingInner.addView(TextView(this@VpnActivity).apply {
            text = "Routing"
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(color(R.color.text_light))
            setPadding(0, 0, 0, dp(8))
        })
        val splitLayout = TextInputLayout(this@VpnActivity).apply { hint = "Proxy only these domains (optional), one per line" }
        split = TextInputEditText(splitLayout.context).apply {
            minLines = 4
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setText(V2RayStore.getSplit(this@VpnActivity).sorted().joinToString("\n"))
        }
        splitLayout.addView(split)
        routingInner.addView(splitLayout)
        routingCard.addView(routingInner)
        advancedBody.addView(routingCard, lp())

        val modeCard = card()
        val modeInner = LinearLayout(this@VpnActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(12)) }
        modeInner.addView(TextView(this@VpnActivity).apply {
            text = "Tunnel mode"
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(color(R.color.text_light))
            setPadding(0, 0, 0, dp(8))
        })
        mode = RadioGroup(this@VpnActivity).apply { orientation = RadioGroup.VERTICAL }
        val accentTint = ColorStateList.valueOf(color(R.color.accent))
        val savedMode = V2RayStore.getTunnelMode(this@VpnActivity)
        val packet = radioButton("Real packet tunnel (whole PrivBrowse app)", 1, accentTint, checked = savedMode != 2)
        val web = radioButton("True WebView-level proxy only", 2, accentTint, checked = savedMode == 2)
        mode.addView(packet); mode.addView(web)
        modeInner.addView(mode)
        modeCard.addView(modeInner)
        advancedBody.addView(modeCard, lp().apply { topMargin = dp(12) })

        addView(advancedBody)
    }

    private fun toggleAdvanced() {
        val show = advancedBody.visibility != View.VISIBLE
        advancedBody.visibility = if (show) View.VISIBLE else View.GONE
        advancedToggle.text = if (show) "Advanced \u25b4" else "Advanced \u25be"
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

    /** vmess links carry a "ps" remark; use it as the auto label when the user didn't type one. */
    private fun autoLabelFrom(raw: String): String {
        val typed = label.text.toString().trim()
        if (typed.isNotBlank()) return typed
        return runCatching {
            when {
                raw.startsWith("vmess://") -> {
                    val json = JSONObject(String(android.util.Base64.decode(
                        raw.removePrefix("vmess://").let {
                            var p = it.trim().replace('-', '+').replace('_', '/')
                            p += "=".repeat((4 - p.length % 4) % 4); p
                        }, android.util.Base64.DEFAULT
                    )))
                    json.optString("ps").ifBlank { "" }
                }
                raw.contains('#') -> java.net.URLDecoder.decode(raw.substringAfterLast('#'), "UTF-8")
                else -> ""
            }
        }.getOrDefault("")
    }

    private fun saveCustom(): Boolean {
        val resolved = resolveConfig()
        val json = resolved.getOrElse {
            setStatus(Connection.STOPPED, it.message ?: "Couldn't read that link.")
            return false
        }
        val raw = link.text.toString().trim()
        V2RayStore.setConfig(this, json)
        V2RayStore.setLabel(this, autoLabelFrom(raw))
        val domains = split.text.toString().lineSequence().map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        V2RayStore.setSplit(this, domains)
        V2RayStore.setTunnelMode(this, if (mode.checkedRadioButtonId == 2) 2 else 1)
        V2RayStore.setEnabled(this, true)
        return true
    }

    private fun onCustomSaveOnly() {
        if (saveCustom()) {
            refreshQuickLabel()
            Toast.makeText(this, R.string.vpn_saved_toast, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onCustomSaveAndConnect() {
        if (connected) { disconnectAll(); return }
        if (!saveCustom()) return
        refreshQuickLabel()
        connectSavedConfig()
    }

    // ------------------------------------------------------------ Shared

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

    private fun startPacketService() {
        val i = Intent(this, V2RayVpnService::class.java).apply {
            action = V2RayVpnService.ACTION_CONNECT
            putExtra(V2RayVpnService.EXTRA_CONFIG, V2RayStore.getConfig(this@VpnActivity))
            putStringArrayListExtra(V2RayVpnService.EXTRA_SPLIT, ArrayList(V2RayStore.getSplit(this@VpnActivity).sorted()))
            putExtra(V2RayVpnService.EXTRA_SESSION, V2RayStore.activeSession(this@VpnActivity))
        }
        stopCoreThen {
            ContextCompat.startForegroundService(this, i)
            setStatus(Connection.CONNECTING, "Starting packet tunnel...")
        }
    }

    private fun stopCoreThen(next: () -> Unit) {
        Thread {
            runCatching { LibXrayBridge.invokeStop() }
            runCatching { LibXrayBridge.detachVpn() }
            runOnUiThread { if (!isFinishing) next() }
        }.start()
    }

    private fun disconnectAll() {
        startService(Intent(this, V2RayVpnService::class.java).setAction(V2RayVpnService.ACTION_DISCONNECT))
        startService(Intent(this, XrayWebViewProxyService::class.java).setAction(XrayWebViewProxyService.ACTION_DISCONNECT))
        com.privbrowse.app.tor.TorBridge.clearProxy(this)
        V2RayStore.clearSession(this)
        setStatus(Connection.STOPPED, "Disconnecting...")
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
                // Bug fix: this used to ignore `message` entirely and always show the
                // V2Ray-store label, so a real VPN Gate/Tor connection (whose caller
                // passes a specific "Connected via ..." message) silently displayed
                // stale or unrelated V2Ray label text instead.
                val sub = message ?: V2RayStore.getLabel(this)
                statusSub.visibility = if (sub.isNotBlank()) View.VISIBLE else View.GONE
                statusSub.text = sub
            }
            Connection.CONNECTING -> {
                dotColor = color(R.color.accent)
                statusText.text = message ?: "Connecting..."
                statusSub.visibility = View.GONE
            }
            Connection.STOPPED -> {
                dotColor = if (message != null) color(R.color.danger) else color(R.color.text_muted_light)
                statusText.text = message ?: "Disconnected"
                statusSub.visibility = View.GONE
            }
        }
        (statusDot.background as? GradientDrawable)?.setColor(dotColor)
        if (::quickConnectButton.isInitialized) refreshQuickLabel()
        if (::customSaveConnectButton.isInitialized) {
            customSaveConnectButton.text = if (connected) "Disconnect" else getString(R.string.vpn_save_connect)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_VPN && resultCode == Activity.RESULT_OK) startPacketService()
        else if (requestCode == REQ_VPN) setStatus(Connection.STOPPED, "VPN permission was not granted.")
        else if (requestCode == REQ_VPN_RELAY && resultCode == Activity.RESULT_OK) startVpnGateService()
        else if (requestCode == REQ_VPN_RELAY) Toast.makeText(this, "VPN permission was not granted.", Toast.LENGTH_SHORT).show()
    }

    private fun paste() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        if (text.isNotBlank()) link.setText(text)
    }

    // -------------------------------------------- VPN Gate status receiver

    private val vpnGateStateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val label = intent.getStringExtra(com.privbrowse.app.vpn.PrivBrowseVpnService.EXTRA_LABEL)
            when (intent.getStringExtra(com.privbrowse.app.vpn.PrivBrowseVpnService.EXTRA_STATE)) {
                com.privbrowse.app.vpn.PrivBrowseVpnService.STATE_CONNECTING ->
                    setStatus(Connection.CONNECTING, "Finding a VPN Gate relay...")
                com.privbrowse.app.vpn.PrivBrowseVpnService.STATE_LAUNCHED ->
                    setStatus(Connection.CONNECTING, "Connecting via ${label ?: "VPN Gate"}...")
                com.privbrowse.app.vpn.PrivBrowseVpnService.STATE_CONNECTED ->
                    // Real tunnel-up confirmation from ics-openvpn's VpnStatus.StateListener
                    // (LEVEL_CONNECTED), not just the earlier handoff - this is what should
                    // actually flip the status dot green.
                    setStatus(Connection.CONNECTED, "Connected via ${label ?: "VPN Gate"}")
                com.privbrowse.app.vpn.PrivBrowseVpnService.STATE_FAILED ->
                    setStatus(Connection.STOPPED, label ?: "VPN Gate connection failed")
                com.privbrowse.app.vpn.PrivBrowseVpnService.STATE_DISCONNECTED ->
                    setStatus(Connection.STOPPED, null)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        androidx.core.content.ContextCompat.registerReceiver(
            this,
            vpnGateStateReceiver,
            android.content.IntentFilter(com.privbrowse.app.vpn.PrivBrowseVpnService.ACTION_STATE),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        runCatching { unregisterReceiver(vpnGateStateReceiver) }
        super.onStop()
    }

    override fun onResume() { super.onResume(); if (::statusText.isInitialized) refreshStatus() }
}
