package com.privbrowse.app.ui

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.privbrowse.app.R
import com.privbrowse.app.vpn.PrivBrowseVpnService

class VpnActivity : AppCompatActivity() {

    companion object {
        private const val REQ_VPN_PERMISSION = 200
    }

    private var pendingRotationMinutes = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vpn)
        title = getString(R.string.vpn_menu)

        val rotationGroup = findViewById<RadioGroup>(R.id.rotationGroup)
        rotationGroup.isEnabled = false
        val btnConnect = findViewById<Button>(R.id.btnConnect)
        val btnDisconnect = findViewById<Button>(R.id.btnDisconnect)
        val statusText = findViewById<TextView>(R.id.statusText)

        btnConnect.setOnClickListener {
            pendingRotationMinutes = when (rotationGroup.checkedRadioButtonId) {
                R.id.rotation1 -> 1
                R.id.rotation3 -> 3
                R.id.rotation5 -> 5
                else -> 0
            }
            statusText.setText(R.string.vpn_status_discovering)
            requestVpnPermissionThenConnect()
        }

        btnDisconnect.setOnClickListener {
            val intent = Intent(this, PrivBrowseVpnService::class.java).apply {
                action = PrivBrowseVpnService.ACTION_DISCONNECT
            }
            startService(intent)
            statusText.setText(R.string.vpn_status_idle)
        }
    }

    private fun requestVpnPermissionThenConnect() {
        val consentIntent = VpnService.prepare(this)
        if (consentIntent != null) {
            startActivityForResult(consentIntent, REQ_VPN_PERMISSION)
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, PrivBrowseVpnService::class.java).apply {
            action = PrivBrowseVpnService.ACTION_CONNECT
            putExtra(PrivBrowseVpnService.EXTRA_ROTATION_MINUTES, pendingRotationMinutes)
        }
        startService(intent)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_VPN_PERMISSION && resultCode == Activity.RESULT_OK) {
            startVpnService()
        }
    }
}
