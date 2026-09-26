package com.privbrowse.app.ultimate

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.privbrowse.app.R

class QrTransferActivity : AppCompatActivity() {
    private fun dp(v: Int)= (v*resources.displayMetrics.density).toInt()
    private lateinit var qr: ImageView
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); build(); showPayload(intent.getStringExtra(EXTRA_PAYLOAD).orEmpty()) }
    private fun build(){
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(dp(18),dp(18),dp(18),dp(24));setBackgroundColor(getColor(R.color.background_light))}
        root.addView(TextView(this).apply{text="QR Tab Transfer";textSize=23f;setTextColor(getColor(R.color.text_light));setTypeface(typeface,1)})
        qr=ImageView(this).apply{adjustViewBounds=true;setPadding(dp(12),dp(12),dp(12),dp(12))}
        root.addView(qr,LinearLayout.LayoutParams(-1,dp(330)))
        root.addView(TextView(this).apply{text="Scan on the other PrivBrowse device or share the session file locally. No server is used.";textSize=13f;setTextColor(getColor(R.color.text_muted_light));gravity=Gravity.CENTER})
        root.addView(TextView(this).apply{text="Scan QR";textSize=14f;setPadding(dp(20),dp(14),dp(20),dp(14));setTextColor(getColor(R.color.accent_dark));setOnClickListener{scanner.launch(ScanOptions().setPrompt("Scan PrivBrowse tab QR").setBeepEnabled(true))}},LinearLayout.LayoutParams(-2,dp(52)))
        setContentView(root)
    }
    private val scanner = registerForActivityResult(ScanContract()){ result ->
        val raw=result.contents ?: return@registerForActivityResult
        if(!raw.startsWith("privbrowse-tabs:")){ AlertDialog.Builder(this).setTitle("Not a PrivBrowse transfer").setMessage("The scanned code is not a PrivBrowse tab package.").setPositiveButton("OK",null).show(); return@registerForActivityResult }
        val payload=runCatching{android.util.Base64.decode(raw.removePrefix("privbrowse-tabs:"),android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP).toString(Charsets.UTF_8)}.getOrNull()?:return@registerForActivityResult
        startActivity(Intent(this, com.privbrowse.app.ui.MainActivity::class.java).setAction(com.privbrowse.app.ui.MainActivity.ACTION_IMPORT_TABS).putExtra("payload",payload).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)); finish()
    }
    private fun showPayload(payload:String){ if(payload.isBlank())return; runCatching{qr.setImageBitmap(UltimateTools.qrBitmap("privbrowse-tabs:"+android.util.Base64.encodeToString(payload.toByteArray(),android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)))} }
    companion object { const val EXTRA_PAYLOAD="payload" }
}
