package com.privbrowse.app.ultimate

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object WatchStore {
    private const val KEY="watch_items"
    data class Item(val id:Long,val url:String,val keyword:String,val price:Boolean,val lastHash:String,val lastPrice:Double)
    fun list(ctx:Context):List<Item>{ val a=UltimateFeatureStore.jsonArray(ctx,KEY);return (0 until a.length()).mapNotNull{i->a.optJSONObject(i)?.let{Item(it.optLong("id"),it.optString("url"),it.optString("keyword"),it.optBoolean("price"),it.optString("hash"),it.optDouble("lastPrice",Double.NaN))}} }
    fun save(ctx:Context,item:Item){val a=UltimateFeatureStore.jsonArray(ctx,KEY);var replaced=false;for(i in 0 until a.length()){if(a.optJSONObject(i)?.optLong("id")==item.id){a.put(i,android.content.ContentValues().let{org.json.JSONObject().apply{put("id",item.id);put("url",item.url);put("keyword",item.keyword);put("price",item.price);put("hash",item.lastHash);put("lastPrice",item.lastPrice)}});replaced=true}};if(!replaced)a.put(org.json.JSONObject().apply{put("id",item.id);put("url",item.url);put("keyword",item.keyword);put("price",item.price);put("hash",item.lastHash);put("lastPrice",item.lastPrice)});UltimateFeatureStore.putArray(ctx,KEY,a)}
    fun add(ctx:Context,url:String,keyword:String,price:Boolean){save(ctx,Item(System.currentTimeMillis(),url,keyword,price,"",Double.NaN));schedule(ctx)}
    fun remove(ctx:Context,id:Long){val a=UltimateFeatureStore.jsonArray(ctx,KEY);val out=org.json.JSONArray();for(i in 0 until a.length())if(a.optJSONObject(i)?.optLong("id")!=id)out.put(a.getJSONObject(i));UltimateFeatureStore.putArray(ctx,KEY,out);schedule(ctx)}
    fun schedule(ctx:Context){val am=ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager; val pi=PendingIntent.getBroadcast(ctx,4401,Intent(ctx,PageWatchReceiver::class.java),PendingIntent.FLAG_UPDATE_CURRENT or if(Build.VERSION.SDK_INT>=23)PendingIntent.FLAG_IMMUTABLE else 0);am.cancel(pi);if(list(ctx).isNotEmpty())am.setInexactRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+60_000,6*60*60_000L,pi)}
}

class PageWatchReceiver:BroadcastReceiver(){
 override fun onReceive(ctx:Context,intent:Intent?){if(intent?.action==Intent.ACTION_BOOT_COMPLETED){WatchStore.schedule(ctx);return};val pending=goAsync(); Thread{try{WatchStore.list(ctx).forEach{check(ctx,it)}}finally{pending.finish()}}.start()}
 private fun check(ctx:Context,item:WatchStore.Item){try{val c=URL(item.url).openConnection() as HttpURLConnection;c.connectTimeout=15000;c.readTimeout=15000;c.instanceFollowRedirects=true;c.setRequestProperty("User-Agent","PrivBrowse Watcher");val text=c.inputStream.bufferedReader().use{it.readText().take(300_000)};c.disconnect();val hash=MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString(""){"%02x".format(it)};var newPrice=Double.NaN;if(item.price){val m=Regex("(?:₹|Rs\\.?|INR|\\$|USD|EUR|€)\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)",RegexOption.IGNORE_CASE).find(text);newPrice=m?.groupValues?.getOrNull(1)?.replace(",","")?.toDoubleOrNull()?:Double.NaN};val changed=item.lastHash.isNotBlank()&&hash!=item.lastHash;val keyword=item.keyword.isNotBlank()&&text.contains(item.keyword,true);val drop=item.price&&!newPrice.isNaN()&&!item.lastPrice.isNaN()&&newPrice<item.lastPrice;if(changed||keyword||drop){notify(ctx,item,when{drop->"Price dropped to $newPrice";keyword->"Keyword found: ${item.keyword}";else->"Page changed"})};WatchStore.save(ctx,item.copy(lastHash=hash,lastPrice=if(newPrice.isNaN())item.lastPrice else newPrice))}catch(_:Throwable){}}
 private fun notify(ctx:Context,item:WatchStore.Item,msg:String){val nm=ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(NotificationChannel("watch","Page alerts",NotificationManager.IMPORTANCE_DEFAULT));nm.notify(item.id.toInt(),NotificationCompat.Builder(ctx,"watch").setSmallIcon(com.privbrowse.app.R.drawable.ic_tools).setContentTitle("PrivBrowse alert").setContentText(msg).setStyle(NotificationCompat.BigTextStyle().bigText("$msg\n${item.url}")).setAutoCancel(true).build())}
}
