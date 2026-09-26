package com.privbrowse.app.ultimate

import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.privbrowse.app.R

class WatchActivity:AppCompatActivity(){
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);build()}
 private fun build(){val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(getColor(R.color.background_light));setPadding(dp(14),dp(8),dp(14),dp(20))};root.addView(TextView(this).apply{text="Page / price / keyword alerts";textSize=22f;setTextColor(getColor(R.color.text_light));setTypeface(typeface,1)});root.addView(TextView(this).apply{text="Checks every ~6 hours in the background and only stores local hashes/prices.";textSize=12f;setTextColor(getColor(R.color.text_muted_light));setPadding(0,dp(5),0,dp(12))});root.addView(button("Add watch") {add()});WatchStore.list(this).forEach{item->root.addView(row(item))};setContentView(root)}
 private fun button(t:String,click:()->Unit)=TextView(this).apply{text=t;textSize=14f;setTextColor(getColor(R.color.accent_dark));setPadding(dp(12),dp(12),dp(12),dp(12));setOnClickListener{click()}}
 private fun add(){val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};val url=EditText(this).apply{hint="https://example.com/product";setSingleLine(true)};val key=EditText(this).apply{hint="Optional keyword";setSingleLine(true)};box.addView(url);box.addView(key);AlertDialog.Builder(this).setTitle("Add page watch").setView(box).setMultiChoiceItems(arrayOf("Price-drop tracker"),booleanArrayOf(false),null).setNegativeButton("Cancel",null).setPositiveButton("Save"){d,_->val priceBox=(d as? AlertDialog);val selected=priceBox?.listView?.isItemChecked(0)==true;WatchStore.add(this,url.text.toString().trim(),key.text.toString().trim(),selected);build()}.show()}
 private fun row(i:WatchStore.Item)=TextView(this).apply{text="${if(i.price)"💰 " else "🔔 "}${i.url}\n${i.keyword.ifBlank{"Any page change"}}";textSize=13f;setTextColor(getColor(R.color.text_light));setPadding(dp(10),dp(12),dp(10),dp(12));setOnLongClickListener{WatchStore.remove(this@WatchActivity,i.id);build();true}}
}
