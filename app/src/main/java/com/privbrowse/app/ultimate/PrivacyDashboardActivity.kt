package com.privbrowse.app.ultimate

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.privbrowse.app.R
import com.privbrowse.app.data.DbHelper

class PrivacyDashboardActivity:AppCompatActivity(){
 private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
 override fun onCreate(b:Bundle?){super.onCreate(b);NetworkUsageRecorder.snapshot(this);val db=DbHelper(this);val week=System.currentTimeMillis()-7*24*3600_000L;val s=db.getBlockedStatsSince(week);val streak=UltimateFeatureStore.prefs(this).getLong(UltimateFeatureStore.KEY_READING_STREAK,0L);val total=UltimateFeatureStore.prefs(this).getInt(UltimateFeatureStore.KEY_TRACKERS_BLOCKED_TOTAL,0);val (rx,tx)=NetworkUsageRecorder.weeklyRxTx(this);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(getColor(R.color.background_light));setPadding(dp(16),dp(16),dp(16),dp(22))};root.addView(TextView(this).apply{text="Weekly privacy report";textSize=23f;setTextColor(getColor(R.color.text_light));setTypeface(typeface,1)});root.addView(TextView(this).apply{text="Last 7 days\n\nTrackers/ad hosts blocked: ${s.trackersBlocked}\nFingerprint attempts blocked: ${s.fingerprintBlocked}\nAll-time tracker blocks: $total\nApprox. app data RX: ${formatBytes(rx)}\nApprox. app data TX: ${formatBytes(tx)}\nReading streak: $streak day(s)\n\nBadges: ${UltimateFeatureStore.jsonArray(this@PrivacyDashboardActivity,UltimateFeatureStore.KEY_BADGES).let{a->(0 until a.length()).joinToString(", "){a.optString(it)}}}";textSize=14f;setTextColor(getColor(R.color.text_light));setPadding(0,dp(16),0,0)});root.addView(TextView(this).apply{text="Network accounting is app-level (Android UID), not per-tab.";textSize=11f;setTextColor(getColor(R.color.text_muted_light));setPadding(0,dp(10),0,0)});setContentView(root)}
 private fun formatBytes(v:Long):String{val units=arrayOf("B","KB","MB","GB");var n=v.toDouble();var i=0;while(n>=1024&&i<units.lastIndex){n/=1024;i++};return String.format(java.util.Locale.US,"%.1f %s",n,units[i])}
}
