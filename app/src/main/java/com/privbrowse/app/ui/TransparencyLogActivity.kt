package com.privbrowse.app.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.privbrowse.app.R
import com.privbrowse.app.data.DbHelper

/**
 * Phase 3 — Network Transparency Log.
 *
 * Read-only view over the local network_log table: every domain the
 * browser contacted — trackers/fingerprinting scripts blocked, plus
 * allowed third-party contacts — newest first. Purely on-device; nothing
 * shown here is ever uploaded (Zero-Cloud Guarantee).
 *
 * Launched either from the overflow menu (full log) or from the per-site
 * privacy score dialog with EXTRA_ORIGIN_FILTER set, to show just the
 * current page's activity.
 */
class TransparencyLogActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ORIGIN_FILTER = "origin_filter"
    }

    private lateinit var db: DbHelper
    private var originFilter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transparency_log)
        title = getString(R.string.network_log)
        db = DbHelper(this)
        originFilter = intent.getStringExtra(EXTRA_ORIGIN_FILTER)
        bindList()
    }

    private fun bindList() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val emptyText = findViewById<TextView>(R.id.emptyText)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val entries = db.getNetworkLog(originFilter)
        emptyText.text = getString(R.string.no_network_log)
        emptyText.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.adapter = NetworkLogAdapter(entries)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, R.string.clear_network_log)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
            db.clearNetworkLog()
            bindList()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
