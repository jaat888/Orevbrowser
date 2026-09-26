package com.privbrowse.app.ui

import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.*
import com.privbrowse.app.ai.*
import com.privbrowse.app.privacy.SecureStore

class AiCopilotActivity : androidx.appcompat.app.AppCompatActivity() {
    private lateinit var provider: Spinner
    private lateinit var model: EditText
    private lateinit var key: EditText
    private lateinit var system: EditText
    private lateinit var user: EditText
    private lateinit var output: TextView
    private lateinit var profileName: EditText
    private lateinit var defaultBox: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(20,20,20,20)}
        root.addView(TextView(this).apply{ text="Phase 7 — AI Copilot";textSize=22f })
        root.addView(TextView(this).apply{ text="Direct client → provider calls. No PrivBrowse server is used.";setPadding(0,8,0,8) })
        provider=Spinner(this); provider.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,AiProvider.values().map{it.label}); root.addView(provider,lp())
        model=EditText(this).apply{hint="Model"}; root.addView(model,lp())
        key=EditText(this).apply{hint="API key";inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD}; root.addView(key,lp())
        root.addView(Button(this).apply{text="Load saved key";setOnClickListener{ key.setText(SecureStore.get(this@AiCopilotActivity,"ai_${AiProvider.values()[provider.selectedItemPosition].name.lowercase()}_key") ?: "") }})
        profileName=EditText(this).apply{hint="Prompt profile name"};root.addView(profileName,lp())
        system=EditText(this).apply{hint="System/prompt profile";minLines=3;inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE};root.addView(system,lp())
        defaultBox=CheckBox(this).apply{text="Make this the default profile"};root.addView(defaultBox)
        root.addView(Button(this).apply{text="Save profile + key";setOnClickListener{save()}})
        user=EditText(this).apply{hint="Ask the copilot…";minLines=4;inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE};root.addView(user,lp())
        root.addView(Button(this).apply{text="Send";setOnClickListener{send()}})
        output=TextView(this).apply{text="";setPadding(0,16,0,16)};root.addView(output,ViewGroup.LayoutParams(-1,-2))
        root.addView(Button(this).apply{text="Done";setOnClickListener{finish()}})
        val saved=AiProfileStore.default(this); if(saved!=null){system.setText(saved.prompt);profileName.setText(saved.name);defaultBox.isChecked=true}
        setContentView(ScrollView(this).apply{addView(root)})
        applyProviderDefaults()
        provider.onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener{override fun onNothingSelected(p:android.widget.AdapterView<*>?){};override fun onItemSelected(p:android.widget.AdapterView<*>?,v:android.view.View?,pos:Int,id:Long){applyProviderDefaults()}}
    }
    private fun lp()=LinearLayout.LayoutParams(-1,-2).apply{topMargin=8}
    private fun applyProviderDefaults(){val p=AiProvider.values()[provider.selectedItemPosition];model.setText(p.defaultModel)}
    private fun save(){val p=AiProvider.values()[provider.selectedItemPosition];SecureStore.put(this,"ai_${p.name.lowercase()}_key",key.text.toString());if(profileName.text.isNotBlank())AiProfileStore.save(this,AiProfile(profileName.text.toString(),system.text.toString(),defaultBox.isChecked));output.text="Saved on-device."}
    private fun send(){val p=AiProvider.values()[provider.selectedItemPosition];val k=key.text.toString().trim();val modelId=model.text.toString().trim().ifBlank{p.defaultModel};if(k.isBlank()){output.text="API key required.";return};if(modelId.isBlank()){output.text="Enter a model ID for this provider.";return};if(user.text.toString().isBlank()){output.text="Enter a prompt first.";return};output.text="Thinking…";Thread{try{val r=AiClient.chat(p,k,modelId,system.text.toString().ifBlank{"You are a helpful browsing copilot."},user.text.toString());runOnUiThread{output.text=r.text}}catch(e:Exception){runOnUiThread{output.text=e.message ?: "Request failed"}}}.start()}
}
