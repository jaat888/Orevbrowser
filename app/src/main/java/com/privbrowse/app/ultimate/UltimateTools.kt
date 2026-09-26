package com.privbrowse.app.ultimate

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlin.math.abs
import kotlin.math.floor

object UltimateTools {
    fun bangQuery(input: String, custom: Map<String, String>): String? {
        val m = Regex("^!([a-zA-Z0-9_-]+)\\s+(.+)$").matchEntire(input.trim()) ?: return null
        val key = m.groupValues[1].lowercase(); val q = m.groupValues[2]
        val template = custom[key] ?: when (key) {
            "yt" -> "https://www.youtube.com/results?search_query=${Uri.encode(q)}"
            "wiki" -> "https://en.wikipedia.org/wiki/Special:Search?search=${Uri.encode(q)}"
            "gmaps", "maps" -> "https://www.google.com/maps/search/?api=1&query=${Uri.encode(q)}"
            "ddg" -> "https://duckduckgo.com/?q=${Uri.encode(q)}"
            "bing" -> "https://www.bing.com/search?q=${Uri.encode(q)}"
            "google" -> "https://www.google.com/search?q=${Uri.encode(q)}"
            "brave" -> "https://search.brave.com/search?q=${Uri.encode(q)}"
            else -> null
        } ?: return null
        return template.replace("%s", Uri.encode(q)).replace("{q}", Uri.encode(q))
    }

    fun answer(input: String): String? {
        val s = input.trim().lowercase().replace(",", "")
        if (s.matches(Regex("[0-9+\\-*/().% \\^]+"))) return runCatching { eval(s) }.getOrNull()?.let { format(it) }
        val m = Regex("^(-?[0-9.]+)\\s*(km|mi|m|ft|kg|lb|c|f)\\s+(?:to|in)\\s+([a-z]+)$").find(s) ?: return null
        return unit(m.groupValues[1].toDouble(), m.groupValues[2], m.groupValues[3])
    }

    private fun format(x: Double): String = if (abs(x - floor(x)) < 1e-9) x.toLong().toString() else "%.8f".format(java.util.Locale.US, x).trimEnd('0').trimEnd('.')
    private fun eval(input: String): Double {
        val tokens = Regex("(?<=[+\\-*/%^()])|(?=[+\\-*/%^()])").split(input.replace(" ", "")).filter { it.isNotBlank() }
        val vals = java.util.ArrayDeque<Double>(); val ops = java.util.ArrayDeque<String>()
        fun prec(o: String)=when(o){"+","-"->1;"*","/","%"->2;"^"->3;else->0}
        fun apply(){ val o=ops.removeLast(); val b=vals.removeLast(); val a=vals.removeLast(); vals.addLast(when(o){"+"->a+b;"-"->a-b;"*"->a*b;"/"->a/b;"%"->a%b;"^"->Math.pow(a,b);else->0.0}) }
        for (t in tokens) when {
            t.toDoubleOrNull()!=null -> vals.addLast(t.toDouble())
            t=="(" -> ops.addLast(t)
            t==")" -> { while(ops.isNotEmpty()&&ops.last()!="(") apply(); if(ops.isNotEmpty())ops.removeLast() }
            else -> { while(ops.isNotEmpty()&&ops.last()!="("&&prec(ops.last())>=prec(t)) apply(); ops.addLast(t) }
        }
        while(ops.isNotEmpty()) apply(); return vals.last()
    }

    private fun unit(v: Double, from: String, to: String): String? {
        val meters = when(from){"km"->v*1000;"mi"->v*1609.344;"m"->v;"ft"->v*0.3048;else->Double.NaN}
        if(!meters.isNaN() && to in setOf("km","mi","m","ft")) return format(when(to){"km"->meters/1000;"mi"->meters/1609.344;"m"->meters;else->meters/0.3048})+" $to"
        val kg = when(from){"kg"->v;"lb"->v*0.45359237;else->Double.NaN}
        if(!kg.isNaN() && to in setOf("kg","lb")) return format(if(to=="kg")kg else kg/0.45359237)+" $to"
        val c = when(from){"c"->v;"f"->(v-32)*5/9;else->Double.NaN}
        if(!c.isNaN() && to in setOf("c","f")) return format(if(to=="c")c else c*9/5+32)+" °$to"
        return null
    }

    fun mediaControls(speed: Double, audioOnly: Boolean, autoAdSkip: Boolean): String = """
      (function(){
        const speed=${speed.coerceIn(0.5,3.0)};
        function apply(){document.querySelectorAll('video,audio').forEach(v=>{try{v.playbackRate=speed;if(${audioOnly}){v.style.height='1px';v.style.width='1px';v.style.opacity='0.01';}}catch(e){}})}
        apply(); if(window.MutationObserver)new MutationObserver(apply).observe(document.documentElement,{childList:true,subtree:true});
        if(${autoAdSkip}){const words=['skip ad','skip ads','skip advertisement','skip intro']; const scan=()=>document.querySelectorAll('button,[role=button],.ytp-ad-skip-button,.ytp-ad-skip-button-modern').forEach(b=>{const t=(b.innerText||b.getAttribute('aria-label')||'').toLowerCase();if(words.some(w=>t.includes(w)))b.click()});scan();setInterval(scan,1000)}
      })();
    """.trimIndent()

    fun junkStripScript(): String = """
      (function(){
        const sel=['[class*="cookie"]','[id*="cookie"]','[class*="newsletter"]','[class*="subscribe"]','[class*="modal"]','[aria-label*="advert"]','[id*="ad-"],[class*="ad-"],[class*="advert"]'];
        sel.forEach(s=>document.querySelectorAll(s).forEach(e=>{if((e.innerText||'').length<1800)e.remove()}));
      })();
    """.trimIndent()

    fun randomizedGeolocationScript(origin: String, tabId: Long): String {
        val seed = (origin.hashCode().toLong() xor tabId) * 1103515245L + 12345L
        val lat = -60.0 + ((seed ushr 8) % 120000L) / 1000.0
        val lon = -170.0 + ((seed ushr 20) % 340000L) / 1000.0
        return """(function(){var lat=$lat,lon=$lon,acc=50000;var n=navigator.geolocation;if(!n)return;n.getCurrentPosition=function(s,f,o){setTimeout(function(){s({coords:{latitude:lat,longitude:lon,accuracy:acc,altitude:null,altitudeAccuracy:null,heading:null,speed:null},timestamp:Date.now()})},0)};n.watchPosition=function(s){s({coords:{latitude:lat,longitude:lon,accuracy:acc},timestamp:Date.now()});return 1};n.clearWatch=function(){}})();"""
    }

    /** Document-start variant: the page sees stable-but-random coordinates derived from its origin. */
    fun randomizedGeolocationDocumentScript(tabId: Long): String = """
      (function(){
        var n=navigator.geolocation;if(!n)return;
        function hash(s){var h=2166136261;for(var i=0;i<s.length;i++){h^=s.charCodeAt(i);h=Math.imul(h,16777619)}return h>>>0}
        var seed=hash(location.origin+'|$tabId');
        var lat=-60+(seed%120000)/1000;
        var lon=-170+((Math.floor(seed/120000))%340000)/1000;
        var acc=50000;
        function pos(){return {coords:{latitude:lat,longitude:lon,accuracy:acc,altitude:null,altitudeAccuracy:null,heading:null,speed:null},timestamp:Date.now()}}
        n.getCurrentPosition=function(success){setTimeout(function(){success(pos())},0)};
        n.watchPosition=function(success){setTimeout(function(){success(pos())},0);return 1};
        n.clearWatch=function(){};
      })();
    """.trimIndent()

    fun selectionTranslateUrl(text: String): String = "https://translate.google.com/?sl=auto&tl=en&text=${Uri.encode(text)}&op=translate"

    fun qrBitmap(payload: String, size: Int = 768): Bitmap {
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size)
        val bm = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (y in 0 until size) for (x in 0 until size) {
            bm.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
        }
        return bm
    }
}
