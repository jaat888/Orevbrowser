package com.privbrowse.app.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.privbrowse.app.R
import com.privbrowse.app.privacy.BreachChecker
import com.privbrowse.app.privacy.BiometricLock
import com.privbrowse.app.privacy.DomainPrivacyStore
import com.privbrowse.app.privacy.SecureStore

/** Clean advanced privacy dashboard. */
class Phase4Activity : AppCompatActivity() {
    private val pickFilter = 410
    private lateinit var status: TextView
    private lateinit var autoIncognito: TextInputEditText
    private lateinit var cookieHost: TextInputEditText
    private lateinit var cookieMinutes: TextInputEditText
    private lateinit var biometricSwitch: MaterialSwitch

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun color(res: Int): Int = ContextCompat.getColor(this, res)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(28)) }
        root.addView(toolbar())
        root.addView(hero(), lp().apply { topMargin = dp(6) })

        root.addView(section("Automation"), lp().apply { topMargin = dp(18) })
        root.addView(domainCard(), lp().apply { topMargin = dp(6) })
        root.addView(timerCard(), lp().apply { topMargin = dp(10) })

        root.addView(section("Privacy tools"), lp().apply { topMargin = dp(18) })
        root.addView(toolCard(), lp().apply { topMargin = dp(6) })

        root.addView(section("App security"), lp().apply { topMargin = dp(18) })
        root.addView(securityCard(prefs.getBoolean("biometric_lock", false)), lp().apply { topMargin = dp(6) })

        root.addView(actionRow("Open Feature Center", "100+ browser controls, privacy, permissions and performance switches") { startActivity(Intent(this, FeatureCenterActivity::class.java)) }, lp().apply { topMargin = dp(12) })

        status = TextView(this).apply { text = "Ready"; textSize = 12.5f; setTextColor(color(R.color.text_muted_light)); setPadding(dp(4), dp(12), dp(4), 0) }
        root.addView(status)
        root.addView(outlinedButton("Done") { finish() }, lp().apply { topMargin = dp(6) })

        setContentView(ScrollView(this).apply { setBackgroundColor(color(R.color.background_light)); clipToPadding = false; addView(root) })
    }

    private fun toolbar() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        addView(TextView(this@Phase4Activity).apply { text = "‹"; textSize = 38f; gravity = Gravity.CENTER; setTextColor(color(R.color.text_light)); setOnClickListener { finish() } }, LinearLayout.LayoutParams(dp(44), dp(48)))
        addView(LinearLayout(this@Phase4Activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@Phase4Activity).apply { text = "Advanced Privacy"; textSize = 22f; setTypeface(typeface, Typeface.BOLD); setTextColor(color(R.color.text_light)) })
            addView(TextView(this@Phase4Activity).apply { text = "Automation, cleanup and app protection"; textSize = 12f; setTextColor(color(R.color.text_muted_light)); setPadding(0,dp(1),0,0) })
        }, LinearLayout.LayoutParams(0,-2,1f))
    }

    private fun hero() = MaterialCardView(this).apply {
        radius=dp(20).toFloat();cardElevation=0f;strokeWidth=dp(1);strokeColor=color(R.color.divider);setCardBackgroundColor(color(R.color.surface_light))
        val row=LinearLayout(this@Phase4Activity).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(14),dp(16),dp(14))}
        row.addView(TextView(this@Phase4Activity).apply{text="✓";textSize=20f;gravity=Gravity.CENTER;setTextColor(color(R.color.on_accent));setBackgroundColor(color(R.color.accent))},LinearLayout.LayoutParams(dp(40),dp(40)))
        row.addView(LinearLayout(this@Phase4Activity).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),0,0,0);addView(TextView(this@Phase4Activity).apply{text="Privacy controls are on";textSize=16f;setTypeface(typeface,Typeface.BOLD);setTextColor(color(R.color.text_light))});addView(TextView(this@Phase4Activity).apply{text="Advanced settings stay here so the main browser remains clean.";textSize=11.5f;setTextColor(color(R.color.text_muted_light));setPadding(0,dp(3),0,0)})},LinearLayout.LayoutParams(0,-2,1f))
        addView(row)
    }

    private fun section(s:String)=TextView(this).apply{text=s.uppercase();textSize=11f;setTypeface(typeface,Typeface.BOLD);setTextColor(color(R.color.accent_dark));setPadding(dp(2),0,0,dp(5))}

    private fun domainCard(): View {
        val card=card(); val box=vertical(14)
        box.addView(TextView(this).apply{text="AUTO-PRIVATE DOMAINS";textSize=11f;setTypeface(typeface,Typeface.BOLD);setTextColor(color(R.color.text_muted_light));text="AUTO-PRIVATE DOMAINS"})
        autoIncognito=TextInputEditText(this).apply{minLines=2;maxLines=4;inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE;setText(DomainPrivacyStore.getAutoIncognito(this@Phase4Activity).sorted().joinToString("\n"))}
        box.addView(input("example.com — one domain per line",autoIncognito))
        box.addView(outlinedButton("Save domains"){val hosts=autoIncognito.text?.toString()?.lines()?.map{it.trim().lowercase()}?.filter{it.isNotBlank()}?.toSet().orEmpty();DomainPrivacyStore.setAutoIncognito(this,hosts);status.text="Saved ${hosts.size} domain rule${if(hosts.size==1)"" else "s"}."})
        card.addView(box);return card
    }

    private fun timerCard(): View {
        val card=card();val box=vertical(14)
        box.addView(TextView(this).apply{text="COOKIE & STORAGE TIMER";textSize=11f;setTypeface(typeface,Typeface.BOLD);setTextColor(color(R.color.text_muted_light))})
        cookieHost=TextInputEditText(this).apply{inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI}
        cookieMinutes=TextInputEditText(this).apply{inputType=InputType.TYPE_CLASS_NUMBER}
        box.addView(input("Domain",cookieHost));box.addView(input("Minutes (0 = off)",cookieMinutes),lp().apply{topMargin=dp(7)})
        box.addView(filledButton("Save timer"){saveCookieTimer()})
        card.addView(box);return card
    }

    private fun toolCard(): View {
        val card=card();val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(4),dp(4),dp(4),dp(4))}
        box.addView(actionRow("Custom ad-block host list","Import a plain-text host list") { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type="text/plain";addCategory(Intent.CATEGORY_OPENABLE) },pickFilter) })
        box.addView(actionRow("Reader mode","Simplify the current article locally") { AlertDialog.Builder(this).setTitle("Reader mode").setMessage("Open the browser Page Tools → Reader Mode on an article page. The transformation stays inside WebView.").setPositiveButton(R.string.close,null).show() })
        box.addView(actionRow("Breach checker","Optional HIBP account check") { showBreachDialog() })
        card.addView(box);return card
    }

    private fun securityCard(enabled:Boolean): View {
        val card=card();val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(14),dp(10),dp(10),dp(10))}
        row.addView(LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;addView(TextView(this@Phase4Activity).apply{text="Biometric app lock";textSize=15f;setTypeface(typeface,Typeface.BOLD);setTextColor(color(R.color.text_light))});addView(TextView(this@Phase4Activity).apply{text="Require fingerprint/face when PrivBrowse resumes.";textSize=11.5f;setTextColor(color(R.color.text_muted_light));setPadding(0,dp(2),0,0)})},LinearLayout.LayoutParams(0,-2,1f))
        biometricSwitch=MaterialSwitch(this).apply{isChecked=enabled;setOnCheckedChangeListener{_,checked->setBiometric(checked)}}
        row.addView(biometricSwitch)
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;addView(row);addView(actionRow("Secure vault","Store a small secret using Android Keystore"){showVaultDialog()})}
        card.addView(box);return card
    }

    private fun card()=MaterialCardView(this).apply{radius=dp(17).toFloat();cardElevation=0f;strokeWidth=dp(1);strokeColor=color(R.color.divider);setCardBackgroundColor(color(R.color.surface_light))}
    private fun vertical(p:Int)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(p),dp(p),dp(p),dp(p))}
    private fun input(h:String,f:TextInputEditText)=TextInputLayout(this).apply{boxBackgroundMode=TextInputLayout.BOX_BACKGROUND_OUTLINE;setBoxCornerRadii(dp(12).toFloat(),dp(12).toFloat(),dp(12).toFloat(),dp(12).toFloat());hint=h;addView(f,LinearLayout.LayoutParams(-1,-2))}
    private fun actionRow(title:String,sub:String,action:()->Unit)=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;isClickable=true;setOnClickListener{action()};setPadding(dp(12),dp(12),dp(12),dp(12));addView(LinearLayout(this@Phase4Activity).apply{orientation=LinearLayout.VERTICAL;addView(TextView(this@Phase4Activity).apply{text=title;textSize=14.5f;setTypeface(typeface,Typeface.BOLD);setTextColor(color(R.color.text_light))});addView(TextView(this@Phase4Activity).apply{text=sub;textSize=11.5f;setTextColor(color(R.color.text_muted_light));setPadding(0,dp(2),0,0)})},LinearLayout.LayoutParams(0,-2,1f));addView(TextView(this@Phase4Activity).apply{text="›";textSize=24f;setTextColor(color(R.color.text_muted_light))})}
    private fun filledButton(t:String,a:()->Unit)=MaterialButton(this).apply{text=t;cornerRadius=dp(12);setOnClickListener{a()}}
    private fun outlinedButton(t:String,a:()->Unit)=MaterialButton(this).apply{text=t;cornerRadius=dp(12);strokeWidth=dp(1);strokeColor=ContextCompat.getColorStateList(this@Phase4Activity,R.color.accent);backgroundTintList=ContextCompat.getColorStateList(this@Phase4Activity,R.color.surface_light);setTextColor(color(R.color.accent_dark));setOnClickListener{a()}}
    private fun lp()=LinearLayout.LayoutParams(-1,-2)

    private fun saveCookieTimer(){val host=cookieHost.text?.toString()?.trim().orEmpty();val mins=cookieMinutes.text?.toString()?.toLongOrNull()?:0L;if(host.isBlank()){status.text="Enter a domain first.";return};DomainPrivacyStore.setCookieTimer(this,host,mins);status.text=if(mins>0)"Timer saved for $host." else "Timer disabled for $host."}

    private fun showBreachDialog(){val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(6),dp(20),0)};val email=EditText(this).apply{hint="Email";inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS};val key=EditText(this).apply{hint="HIBP API key";inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD};box.addView(email,lp());box.addView(key,lp().apply{topMargin=dp(8)});AlertDialog.Builder(this).setTitle("Breach checker").setView(box).setPositiveButton("Check"){_,_->status.text="Checking…";Thread{val result=BreachChecker.check(email.text.toString(),key.text.toString());runOnUiThread{status.text=result.message}}}.setNegativeButton(R.string.close,null).show()}
    private fun showVaultDialog(){val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(6),dp(20),0)};val name=EditText(this).apply{hint="Entry name"};val value=EditText(this).apply{hint="Secret / value";inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD};box.addView(name,lp());box.addView(value,lp().apply{topMargin=dp(8)});AlertDialog.Builder(this).setTitle("Secure vault").setView(box).setPositiveButton("Save"){_,_->val n=name.text.toString().trim();if(n.isNotBlank()){SecureStore.put(this,"vault_$n",value.text.toString());status.text="Saved securely on this device."}}.setNegativeButton(R.string.close,null).show()}
    private fun setBiometric(enabled:Boolean){if(enabled&&!BiometricLock.isAvailable(this)){biometricSwitch.isChecked=false;status.text="Biometric authentication is not available.";return};getSharedPreferences(MainActivity.PREFS,MODE_PRIVATE).edit().putBoolean("biometric_lock",enabled).apply();status.text=if(enabled)"Biometric app lock enabled." else "Biometric app lock disabled."}

    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode!=pickFilter||resultCode!=Activity.RESULT_OK||data?.data==null)return;val uri:Uri=data.data!!;try{val text=contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()}?:"";val hosts=text.lines().mapNotNull{line->val clean=line.substringBefore('#').trim().lowercase();when{clean.startsWith("0.0.0.0 ")->clean.substringAfter(" ").trim().takeIf{it.contains('.')};clean.startsWith("127.0.0.1 ")->clean.substringAfter(" ").trim().takeIf{it.contains('.')};clean.matches(Regex("[a-z0-9.-]+"))->clean.takeIf{it.contains('.')};else->null}}.toSet();getSharedPreferences("privbrowse_custom_filters",MODE_PRIVATE).edit().putStringSet("hosts",hosts).apply();status.text="Imported ${hosts.size} host rule${if(hosts.size==1)"" else "s"}."}catch(e:Exception){status.text="Import failed: ${e.message}";Toast.makeText(this,status.text,Toast.LENGTH_SHORT).show()}}
}
