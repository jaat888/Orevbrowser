package com.privbrowse.app.ultimate

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.privbrowse.app.R
import org.json.JSONObject

class GestureEditorActivity : AppCompatActivity() {
    private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
    private val actions=arrayOf("none","back","forward","next_tab","previous_tab","refresh","home","tab_manager","new_tab","close_tab","page_top","page_bottom")
    private val dirs=arrayOf("left","right","up","down")
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);build()}
    private fun build(){
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(12),dp(16),dp(20));setBackgroundColor(getColor(R.color.background_light))}
        root.addView(TextView(this).apply{text="Custom gestures";textSize=22f;setTextColor(getColor(R.color.text_light));setTypeface(typeface,1)})
        root.addView(TextView(this).apply{text="Assign a browser action to a four-direction swipe.";textSize=12f;setTextColor(getColor(R.color.text_muted_light));setPadding(0,dp(4),0,dp(14))})
        val stored=UltimateFeatureStore.jsonObject(this,UltimateFeatureStore.KEY_GESTURE_ACTIONS)
        val spinners=linkedMapOf<String,Spinner>()
        dirs.forEach{d->
            val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
            row.addView(TextView(this).apply{text=d.replaceFirstChar{it.uppercase()};textSize=14f;setTextColor(getColor(R.color.text_light));gravity=16},LinearLayout.LayoutParams(dp(75),dp(52)))
            val sp=Spinner(this);sp.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,actions);val current=actions.indexOf(stored.optString(d,"none"));sp.setSelection(current.coerceAtLeast(0));row.addView(sp,LinearLayout.LayoutParams(0,dp(52),1f));spinners[d]=sp;root.addView(row)
        }
        root.addView(TextView(this).apply{text="SAVE";textSize=14f;setTextColor(getColor(R.color.accent_dark));setPadding(dp(10),dp(14),dp(10),dp(14));setOnClickListener{
            val o=JSONObject();spinners.forEach{(d,sp)->o.put(d,sp.selectedItem.toString())};UltimateFeatureStore.putObject(this@GestureEditorActivity,UltimateFeatureStore.KEY_GESTURE_ACTIONS,o);finish()
        }})
        setContentView(root)
    }
}
