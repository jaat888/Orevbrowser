package com.privbrowse.app.ultimate

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.privbrowse.app.R
import com.privbrowse.app.ui.MainActivity

class ProfileActivity:AppCompatActivity(){
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 override fun onCreate(b:Bundle?){super.onCreate(b);build()}
 private fun build(){val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(getColor(R.color.background_light));setPadding(dp(16),dp(12),dp(16),dp(20));gravity=Gravity.TOP};root.addView(TextView(this).apply{text="Browser profiles";textSize=23f;setTextColor(getColor(R.color.text_light));setTypeface(typeface,1)});root.addView(TextView(this).apply{text="Each profile uses its own WebView data directory and local SQLite database. Switching restarts the app so Android can safely change the WebView suffix.";textSize=12f;setTextColor(getColor(R.color.text_muted_light));setPadding(0,dp(6),0,dp(14))});val profiles=arrayOf("personal","work","guest");profiles.forEach{p->root.addView(TextView(this).apply{text=if(active()==p)"✓ $p" else p;textSize=15f;setTextColor(getColor(R.color.text_light));setPadding(dp(14),dp(14),dp(14),dp(14));setOnClickListener{switchTo(p)}})};setContentView(root)}
 private fun active()=getSharedPreferences("privbrowse_profile",MODE_PRIVATE).getString("active_profile","personal")?:"personal"
 private fun switchTo(p:String){if(p==active())return;getSharedPreferences("privbrowse_profile",MODE_PRIVATE).edit().putString("active_profile",p).apply();val i=packageManager.getLaunchIntentForPackage(packageName)?.apply{addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)}?:Intent(this,MainActivity::class.java).apply{addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)};val pending=PendingIntent.getActivity(this,7711,i,PendingIntent.FLAG_UPDATE_CURRENT or if(android.os.Build.VERSION.SDK_INT>=23)PendingIntent.FLAG_IMMUTABLE else 0);(getSystemService(ALARM_SERVICE) as AlarmManager).setExact(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+350,pending);finishAffinity();Process.killProcess(Process.myPid())}
}
