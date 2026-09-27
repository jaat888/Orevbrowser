package com.privbrowse.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import com.privbrowse.app.R
import com.privbrowse.app.ui.MainActivity
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ConfigParser
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.IOpenVPNServiceInternal
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.VPNLaunchHelper
import de.blinkt.openvpn.core.VpnStatus
import java.io.StringReader

/**
 * Phase 6 - real VPN Gate connectivity.
 *
 * Discovers/ranks VPN Gate relays (VpnGateApi/VpnServerSelector, unchanged),
 * then hands the winning server's .ovpn config to the ics-openvpn engine
 * (GPLv2 - see NOTICE.md) via ConfigParser -> VpnProfile -> VPNLaunchHelper.
 * ics-openvpn's own OpenVPNService owns the actual TUN interface and
 * notification once launched; this class is a thin controller + the
 * VpnActivity-facing entry point (manifest name is unchanged).
 *
 * All ics-openvpn call signatures here were checked against the actual
 * source on GitHub (not recalled from memory) as of writing:
 *   - ConfigParser().parseConfig() / .convertProfile()
 *     -> main/src/main/java/de/blinkt/openvpn/core/ConfigParser.java
 *   - ProfileManager.getInstance(ctx).addProfile(vp),
 *     ProfileManager.setTemporaryProfile(ctx, vp)
 *     -> .../api/ExternalOpenVPNService.java (same calls, in-app usage)
 *   - VPNLaunchHelper.startOpenVpn(VpnProfile, Context, String, Boolean)
 *     -> main/src/main/java/de/blinkt/openvpn/LaunchVPN.java
 *   - bind OpenVPNService with action START_SERVICE, cast the binder via
 *     IOpenVPNServiceInternal.Stub.asInterface(), call stopVPN(false)
 *     -> main/src/main/java/de/blinkt/openvpn/api/RemoteAction.java
 * libs/ics-openvpn is pinned to tag v0.7.64 (see .github/workflows/build.yml
 * and libs/ics-openvpn/README.txt) precisely so these signatures don't
 * silently drift out from under this file — if you bump that pin, re-check
 * these four spots against the new tag's source before assuming it builds.
 *
 * Phase 6b - real "Connected" confirmation.
 * VPNLaunchHelper.startOpenVpn() only proves we handed the profile off;
 * this class also implements VpnStatus.StateListener and registers via
 * VpnStatus.addStateListener(this)/removeStateListener(this)
 * (-> main/src/main/java/de/blinkt/openvpn/core/VpnStatus.java) so
 * updateState()'s ConnectionStatus.LEVEL_CONNECTED is what actually
 * triggers STATE_CONNECTED — not the handoff in connectTo().
 */
class PrivBrowseVpnService : android.net.VpnService(), VpnStatus.StateListener {
    companion object {
        const val ACTION_CONNECT = "com.privbrowse.app.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.privbrowse.app.vpn.DISCONNECT"
        const val EXTRA_ROTATION_MINUTES = "rotation_minutes"

        /** Local status broadcast so VpnActivity's UI reflects what this service is actually doing. */
        const val ACTION_STATE = "com.privbrowse.app.vpn.STATE"
        const val EXTRA_STATE = "state" // one of STATE_* below
        const val EXTRA_LABEL = "label"
        const val STATE_CONNECTING = "connecting"
        const val STATE_LAUNCHED = "launched" // handed off to ics-openvpn; tunnel not yet confirmed up
        const val STATE_CONNECTED = "connected" // ics-openvpn confirmed LEVEL_CONNECTED - tunnel is actually up
        const val STATE_FAILED = "failed"
        const val STATE_DISCONNECTED = "disconnected"

        private const val CHANNEL = "vpn_gate"
        private const val NOTIFICATION_ID = 2501
        private const val TAG = "PrivBrowseVpn"
        private const val MAX_ROTATION_MINUTES = 5
    }

    private fun broadcastState(state: String, label: String? = null) {
        sendBroadcast(Intent(ACTION_STATE).setPackage(packageName).putExtra(EXTRA_STATE, state).putExtra(EXTRA_LABEL, label))
    }

    private val handler = Handler(Looper.getMainLooper())
    private var rotationMinutes = 0
    private var rotationRunnable: Runnable = Runnable { }

    /** Label of the relay we last handed to ics-openvpn, so a later LEVEL_CONNECTED can be broadcast with it. */
    @Volatile private var pendingLabel: String? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        VpnStatus.addStateListener(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                rotationMinutes = intent.getIntExtra(EXTRA_ROTATION_MINUTES, 0)
                    .coerceIn(0, MAX_ROTATION_MINUTES)
                startForeground(NOTIFICATION_ID, notification("Finding a VPN Gate relay..."))
                broadcastState(STATE_CONNECTING)
                Thread { discoverAndConnect() }.start()
            }
            ACTION_DISCONNECT -> disconnect()
        }
        return START_NOT_STICKY
    }

    private fun discoverAndConnect() {
        try {
            val candidates = VpnServerSelector.topCandidates(VpnGateApi.fetchServers())
            val chosen = candidates.firstOrNull()
            if (chosen == null) {
                Log.e(TAG, "VPN Gate discovery returned no usable candidates")
                broadcastState(STATE_FAILED, "No usable VPN Gate servers responded")
                stopSelf()
                return
            }
            connectTo(chosen)
            if (rotationMinutes > 0) scheduleRotation()
        } catch (e: Exception) {
            Log.e(TAG, "VPN Gate discovery/connect failed", e)
            broadcastState(STATE_FAILED, e.message ?: e.javaClass.simpleName)
            stopSelf()
        }
    }

    /** Decodes the server's base64 .ovpn config and launches it through ics-openvpn. */
    private fun connectTo(server: VpnServer) {
        val configText = String(Base64.decode(server.openVpnConfigBase64, Base64.DEFAULT), Charsets.UTF_8)
        val parser = ConfigParser()
        parser.parseConfig(StringReader(configText))
        val profile: VpnProfile = parser.convertProfile()

        profile.mName = "VPN Gate - ${server.countryLong} (${server.hostName})"
        // VPN Gate relays are shared volunteer machines behind CGNAT/dynamic
        // IPs; keep TLS verification as parsed from the config as-is rather
        // than overriding it, and let ics-openvpn's own reconnect logic
        // handle transient drops.

        val profileManager = ProfileManager.getInstance(applicationContext)
        profileManager.addProfile(profile)
        ProfileManager.setTemporaryProfile(applicationContext, profile)

        handler.post {
            // Confirmed signature (LaunchVPN.java, ics-openvpn master):
            // startOpenVpn(VpnProfile, Context, String startReason, boolean replaceRunningVpn)
            VPNLaunchHelper.startOpenVpn(profile, applicationContext, "VPN Gate auto-connect", true)
            val label = "${server.countryLong} (${server.hostName})"
            pendingLabel = label
            notificationUpdate("Handed off to OpenVPN engine: $label")
            // Honest, not optimistic: this confirms we handed the profile to
            // ics-openvpn's own OpenVPNService, not that the tunnel is up
            // yet. STATE_CONNECTED only fires from updateState() below,
            // once ics-openvpn itself reports ConnectionStatus.LEVEL_CONNECTED.
            broadcastState(STATE_LAUNCHED, label)
        }

        Log.i(TAG, "Launched OpenVPN profile for ${server.hostName} (${server.countryShort})")
    }

    private fun scheduleRotation() {
        handler.removeCallbacks(rotationRunnable)
        rotationRunnable = Runnable {
            Log.i(TAG, "Rotation interval elapsed - reconnecting to a fresh VPN Gate relay")
            Thread { discoverAndConnect() }.start()
        }
        handler.postDelayed(rotationRunnable, rotationMinutes * 60_000L)
    }

    // Confirmed pattern (RemoteAction.java, ics-openvpn master): bind to
    // ics-openvpn's own OpenVPNService with action START_SERVICE, get the
    // AIDL IOpenVPNServiceInternal from the binder, call stopVPN(false).
    private val disconnectConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            try {
                IOpenVPNServiceInternal.Stub.asInterface(binder)?.stopVPN(false)
            } catch (e: Exception) {
                Log.w(TAG, "stopVPN() call failed", e)
            } finally {
                runCatching { unbindService(this) }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    private fun disconnect() {
        handler.removeCallbacks(rotationRunnable)
        pendingLabel = null
        try {
            val bindIntent = Intent(applicationContext, OpenVPNService::class.java)
                .setAction(OpenVPNService.START_SERVICE)
            bindService(bindIntent, disconnectConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.w(TAG, "Disconnect failed, engine may already be stopped", e)
        }
        broadcastState(STATE_DISCONNECTED)
        stopSelf()
    }

    // VpnStatus.StateListener (de.blinkt.openvpn.core.VpnStatus, ics-openvpn v0.7.64):
    // updateState() is called on every engine state transition; level is the
    // one field that actually tells us whether the tunnel is up. Everything
    // else here (state/logmessage/resid/intent) is informational only.
    override fun updateState(state: String?, logmessage: String?, localizedResId: Int, level: ConnectionStatus?, intent: Intent?) {
        when (level) {
            ConnectionStatus.LEVEL_CONNECTED -> {
                val label = pendingLabel ?: "VPN Gate"
                notificationUpdate("Connected: $label")
                broadcastState(STATE_CONNECTED, label)
            }
            ConnectionStatus.LEVEL_AUTH_FAILED -> {
                broadcastState(STATE_FAILED, logmessage ?: "Authentication failed")
            }
            ConnectionStatus.LEVEL_NOTCONNECTED -> {
                // Only treat this as a real disconnect signal if we'd actually
                // launched something - avoids a stray LEVEL_NOTCONNECTED at
                // service startup (before any connect) broadcasting DISCONNECTED.
                if (pendingLabel != null) {
                    pendingLabel = null
                    broadcastState(STATE_DISCONNECTED)
                }
            }
            else -> {
                // LEVEL_CONNECTING_SERVER_REPLIED / LEVEL_CONNECTING_NO_SERVER_REPLY_YET /
                // LEVEL_WAITING_FOR_USER_INPUT / LEVEL_VPNPAUSED / LEVEL_UNKNOWN:
                // still en route or paused - STATE_LAUNCHED already covers the
                // "connecting" UI, nothing new to tell VpnActivity here.
            }
        }
    }

    // VpnStatus.StateListener also requires this; ics-openvpn calls it when a
    // profile's UUID becomes the "connected" one. We already key off
    // updateState()'s LEVEL_CONNECTED instead, so there's nothing to do here.
    override fun setConnectedVPN(uuid: String?) {}

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "VPN Gate", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun notification(text: String): Notification = NotificationCompat.Builder(this, CHANNEL)
        .setSmallIcon(R.drawable.ic_launcher)
        .setContentTitle("PrivBrowse VPN Gate")
        .setContentText(text)
        .setOngoing(true)
        .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
        .build()

    private fun notificationUpdate(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(text))
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        VpnStatus.removeStateListener(this)
        super.onDestroy()
    }
}
