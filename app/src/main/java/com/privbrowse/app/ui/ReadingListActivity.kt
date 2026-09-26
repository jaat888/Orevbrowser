package com.privbrowse.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.privbrowse.app.R
import com.privbrowse.app.data.DbHelper
import com.privbrowse.app.data.LinkItem

class ReadingListActivity : AppCompatActivity() {
    private lateinit var db: DbHelper
    private lateinit var list: LinearLayout
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun color(res: Int): Int = ContextCompat.getColor(this, res)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = DbHelper(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.background_light))
        }
        root.addView(toolbar())
        val scroll = android.widget.ScrollView(this)
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(24))
        }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        refresh()
    }

    private fun toolbar(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(6), dp(8), dp(12), dp(8))
        addView(TextView(this@ReadingListActivity).apply {
            text = "‹"
            textSize = 38f
            gravity = Gravity.CENTER
            setTextColor(color(R.color.text_light))
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(46), dp(48)))
        addView(TextView(this@ReadingListActivity).apply {
            text = "Reading list"
            textSize = 22f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(color(R.color.text_light))
        }, LinearLayout.LayoutParams(0, -2, 1f))
    }

    private fun refresh() {
        list.removeAllViews()
        val items = db.getReadingList()
        if (items.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "No saved pages yet.\nUse Page tools → Save to reading list."
                textSize = 15f
                setTextColor(color(R.color.text_muted_light))
                gravity = Gravity.CENTER
                setPadding(dp(20), dp(80), dp(20), dp(20))
            })
            return
        }
        items.forEach { item -> addItem(item) }
    }

    private fun addItem(item: LinkItem) {
        val card = MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = color(R.color.divider)
            setCardBackgroundColor(color(R.color.surface_light))
            isClickable = true
            setOnClickListener {
                startActivity(Intent(this@ReadingListActivity, MainActivity::class.java).apply {
                    data = android.net.Uri.parse(item.url)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                })
                finish()
            }
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(13), dp(14), dp(10))
        }
        box.addView(TextView(this).apply {
            text = item.title.ifBlank { item.url }
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(color(R.color.text_light))
            maxLines = 2
        })
        box.addView(TextView(this).apply {
            text = item.url
            textSize = 12f
            setTextColor(color(R.color.text_muted_light))
            maxLines = 2
            setPadding(0, dp(3), 0, dp(8))
        })
        val remove = TextView(this).apply {
            text = "Remove"
            textSize = 12f
            setTextColor(color(R.color.accent_dark))
            setOnClickListener { db.removeReadingList(item.id); refresh() }
        }
        box.addView(remove)
        card.addView(box)
        list.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) })
    }
}
