package com.privbrowse.app.video

import android.webkit.WebView
import org.json.JSONArray
import org.json.JSONTokener

object VideoDetector {
    val SCRIPT = """
      (function(){
        var out=[]; var seen={};
        function add(u){ if(!u) return; try{u=new URL(u,location.href).href;}catch(e){return;} if(!/^https?:/i.test(u)) return; if(!seen[u]){seen[u]=1;out.push(u);} }
        document.querySelectorAll('video').forEach(function(v){ add(v.currentSrc || v.src); v.querySelectorAll('source').forEach(function(s){add(s.src);}); });
        document.querySelectorAll('meta[property="og:video"],meta[property="og:video:url"],meta[name="twitter:player:stream"]').forEach(function(m){add(m.content);});
        document.querySelectorAll('a[href]').forEach(function(a){ if(/\.(mp4|webm|m4v|mov|ogv)(\?|#|$)/i.test(a.href)) add(a.href); });
        return JSON.stringify(out.slice(0,20));
      })();
    """.trimIndent()

    fun parse(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val value = JSONTokener(raw).nextValue()
            val json = when (value) {
                is String -> value
                else -> value.toString()
            }
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    arr.optString(i).takeIf { it.startsWith("http://") || it.startsWith("https://") }?.let(::add)
                }
            }.distinct().take(20)
        }.getOrElse { emptyList() }
    }
}
