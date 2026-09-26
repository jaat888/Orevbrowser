package com.privbrowse.app.ultimate

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.privbrowse.app.R

class DownloadQueueActivity:AppCompatActivity(){
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 override fun onCreate(b:Bundle?){super.onCreate(b);build()}
 private fun build(){val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(getColor(R.color.background_light));setPadding(dp(14),dp(10),dp(14),dp(20))};root.addView(TextView(this).apply{text="Download queue";textSize=22f;setTextColor(getColor(R.color.text_light));setTypeface(typeface,1)});val url=EditText(this).apply{hint="HTTPS URL to download";setSingleLine(true)};root.addView(url,LinearLayout.LayoutParams(-1,dp(52)));root.addView(TextView(this).apply{text="Queue download";textSize=14f;setTextColor(getColor(R.color.accent_dark));setPadding(dp(10),dp(12),dp(10),dp(12));setOnClickListener{val u=url.text.toString().trim();if(u.isNotBlank()){val file=(u.substringAfterLast('/').substringBefore('?')).ifBlank{"download"};DownloadQueueStore.add(this,u,file,null);androidx.core.content.ContextCompat.startForegroundService(this,Intent(this,DownloadQueueService::class.java));build()}}});DownloadQueueStore.list(this).forEach{i->root.addView(TextView(this).apply{text="${i.status.uppercase()} · ${i.file} · ${i.bytes}/${if(i.total>0)i.total else "?"}";textSize=12f;setTextColor(getColor(R.color.text_light));setPadding(dp(8),dp(10),dp(8),dp(10));setOnLongClickListener{DownloadQueueStore.remove(this@DownloadQueueActivity,i.id);build();true}})};setContentView(root)}
}
