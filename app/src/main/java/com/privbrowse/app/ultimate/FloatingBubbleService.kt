package com.privbrowse.app.ultimate

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import com.privbrowse.app.ui.MainActivity

class FloatingBubbleService: Service(){
    private var wm:WindowManager?=null; private var view:TextView?=null
    override fun onCreate(){super.onCreate(); if(android.os.Build.VERSION.SDK_INT>=23 && !android.provider.Settings.canDrawOverlays(this)){stopSelf();return};if(android.os.Build.VERSION.SDK_INT>=26){val nm=getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager;nm.createNotificationChannel(android.app.NotificationChannel("bubble","PrivBrowse bubble",android.app.NotificationManager.IMPORTANCE_LOW));startForeground(7201,androidx.core.app.NotificationCompat.Builder(this,"bubble").setSmallIcon(com.privbrowse.app.R.drawable.ic_tools).setContentTitle("PrivBrowse bubble").setContentText("Browser bubble is active").build())};wm=getSystemService(WINDOW_SERVICE) as WindowManager; val type=if(android.os.Build.VERSION.SDK_INT>=26)WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE; view=TextView(this).apply{text="PB";textSize=12f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);setBackgroundColor(Color.rgb(27,31,59));setOnClickListener{startActivity(Intent(this@FloatingBubbleService,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))}};wm?.addView(view,WindowManager.LayoutParams(56,56,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.CENTER_VERTICAL or Gravity.RIGHT; x=12;y=0})}
    override fun onDestroy(){view?.let{runCatching{wm?.removeView(it)}};view=null;super.onDestroy()}
    override fun onBind(intent:Intent?):IBinder?=null
}
