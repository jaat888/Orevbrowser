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

class BookmarksActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RESULT_URL = "result_url"
    }

    private lateinit var db: DbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmarks)
        title = getString(R.string.bookmarks)

        findViewById<android.view.View>(R.id.btnBackPage).setOnClickListener { finish() }
        db = DbHelper(this)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val emptyText = findViewById<TextView>(R.id.emptyText)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val bookmarks = db.getBookmarks()
        emptyText.visibility = if (bookmarks.isEmpty()) View.VISIBLE else View.GONE

        recyclerView.adapter = LinkAdapter(
            bookmarks,
            onClick = { item ->
                val data = Intent().putExtra(EXTRA_RESULT_URL, item.url)
                setResult(Activity.RESULT_OK, data)
                finish()
            },
            onLongClick = { item ->
                db.removeBookmark(item.id)
                recreate()
            }
        )
    }
}
