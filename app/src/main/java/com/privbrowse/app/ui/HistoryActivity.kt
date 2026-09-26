package com.privbrowse.app.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.privbrowse.app.R
import com.privbrowse.app.data.DbHelper

class HistoryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RESULT_URL = "result_url"
    }

    private lateinit var db: DbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        title = getString(R.string.history)
        db = DbHelper(this)
        findViewById<android.view.View>(R.id.btnBackPage).setOnClickListener { finish() }
        findViewById<android.view.View>(R.id.btnClear).setOnClickListener { db.clearHistory(); bindList() }
        bindList()
    }

    private fun bindList() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val emptyText = findViewById<TextView>(R.id.emptyText)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val history = db.getHistory()
        emptyText.text = getString(R.string.no_history)
        emptyText.visibility = if (history.isEmpty()) View.VISIBLE else View.GONE

        recyclerView.adapter = LinkAdapter(history, onClick = { item ->
            val data = Intent().putExtra(EXTRA_RESULT_URL, item.url)
            setResult(Activity.RESULT_OK, data)
            finish()
        })
    }

}
