package com.privbrowse.app.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.privbrowse.app.R
import com.privbrowse.app.data.DbHelper

class BookmarksActivity : AppCompatActivity() {
    companion object { const val EXTRA_RESULT_URL = "result_url" }
    private lateinit var db: DbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_bookmarks); title = getString(R.string.bookmarks)
        findViewById<View>(R.id.btnBackPage).setOnClickListener { finish() }
        db = DbHelper(this)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView); val emptyText = findViewById<TextView>(R.id.emptyText); recyclerView.layoutManager = LinearLayoutManager(this)
        val bookmarks = db.getBookmarks(); emptyText.visibility = if (bookmarks.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.adapter = LinkAdapter(bookmarks,
            onClick = { item -> setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_RESULT_URL, item.url)); finish() },
            onLongClick = { item ->
                val todo = com.privbrowse.app.ultimate.BookmarkTodoStore.get(this, item.url)
                val meta = com.privbrowse.app.ultimate.BookmarkMetaStore.get(this, item.url)
                val input = EditText(this).apply { hint = "Todo attached to this bookmark"; minLines = 3; setText(todo) }
                val detail = "Auto tags: ${meta?.optString("tags").orEmpty().ifBlank { "none" }}\\n\\n${meta?.optString("description").orEmpty()}"
                AlertDialog.Builder(this).setTitle(item.title.ifBlank { "Bookmark" }).setMessage(detail).setView(input)
                    .setNeutralButton("Delete") { _, _ -> db.removeBookmark(item.id); com.privbrowse.app.ultimate.BookmarkTodoStore.remove(this, item.url); recreate() }
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Save todo") { _, _ -> com.privbrowse.app.ultimate.BookmarkTodoStore.put(this, item.url, input.text.toString().trim()); recreate() }.show()
            }
        )
    }
}
