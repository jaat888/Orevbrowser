package com.privbrowse.app.ui

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.*
import com.privbrowse.app.ai.*
import com.privbrowse.app.privacy.SecureStore

class AiCopilotActivity : androidx.appcompat.app.AppCompatActivity() {
    private lateinit var provider: Spinner
    private lateinit var model: Spinner
    private lateinit var key: EditText
    private lateinit var system: EditText
    private lateinit var user: EditText
    private lateinit var output: TextView
    private lateinit var profileName: EditText
    private lateinit var defaultBox: CheckBox

    // Sentinel row at the end of every model list — picking it opens the
    // "type a model ID" dialog instead of selecting an actual model.
    private val ADD_NEW = "+ Add new model…"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(20,20,20,20)}
        root.addView(TextView(this).apply{ text="AI Copilot";textSize=22f })
        root.addView(TextView(this).apply{ text="Direct client → provider calls. No PrivBrowse server is used.";setPadding(0,8,0,8) })

        root.addView(TextView(this).apply{ text="Provider";setPadding(0,12,0,2) })
        provider=Spinner(this); provider.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,AiProvider.values().map{it.label}); root.addView(provider,lp())

        root.addView(TextView(this).apply{ text="Model";setPadding(0,12,0,2) })
        model=Spinner(this); root.addView(model,lp())

        key=EditText(this).apply{hint="API key";inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD}; root.addView(key,lp())
        root.addView(Button(this).apply{text="Load saved key";setOnClickListener{ key.setText(SecureStore.get(this@AiCopilotActivity,"ai_${currentProvider().name.lowercase()}_key") ?: "") }})
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

        populateModels(currentProvider())
        provider.onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p:android.widget.AdapterView<*>?){}
            override fun onItemSelected(p:android.widget.AdapterView<*>?,v:android.view.View?,pos:Int,id:Long){populateModels(currentProvider())}
        }
        model.onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p:android.widget.AdapterView<*>?){}
            override fun onItemSelected(p:android.widget.AdapterView<*>?,v:android.view.View?,pos:Int,id:Long){
                val picked = (model.adapter as? ArrayAdapter<String>)?.getItem(pos)
                if (picked == ADD_NEW) promptForNewModel()
            }
        }
    }

    private fun lp()=LinearLayout.LayoutParams(-1,-2).apply{topMargin=8}
    private fun currentProvider() = AiProvider.values()[provider.selectedItemPosition]

    /** Known models for this provider, then anything the user added, then the "+" row. */
    private fun populateModels(p: AiProvider, selectAt: Int? = null) {
        val items = (p.models + AiCustomModelStore.all(this, p) + ADD_NEW)
        model.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
        model.setSelection(selectAt ?: 0)
    }

    private fun promptForNewModel() {
        val p = currentProvider()
        val input = EditText(this).apply {
            hint = "Model ID, e.g. ${p.defaultModel}"
        }
        AlertDialog.Builder(this)
            .setTitle("Add a new model")
            .setMessage("For a model released after this app's model list was last updated — paste its exact ID from ${p.label}'s docs.")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val id = input.text.toString().trim()
                if (id.isBlank()) {
                    // nothing typed — fall back to the first real model instead of leaving "+" selected
                    populateModels(p)
                } else {
                    AiCustomModelStore.add(this, p, id)
                    val items = p.models + AiCustomModelStore.all(this, p) + ADD_NEW
                    populateModels(p, items.indexOf(id).coerceAtLeast(0))
                }
            }
            .setNegativeButton("Cancel") { _, _ -> populateModels(p) }
            .setOnCancelListener { populateModels(p) }
            .show()
    }

    private fun selectedModelId(): String {
        val picked = (model.adapter as? ArrayAdapter<String>)?.getItem(model.selectedItemPosition)
        return picked?.takeIf { it != ADD_NEW } ?: currentProvider().defaultModel
    }

    private fun save(){val p=currentProvider();SecureStore.put(this,"ai_${p.name.lowercase()}_key",key.text.toString());if(profileName.text.isNotBlank())AiProfileStore.save(this,AiProfile(profileName.text.toString(),system.text.toString(),defaultBox.isChecked));output.text="Saved on-device."}
    private fun send(){val p=currentProvider();val k=key.text.toString().trim();val modelId=selectedModelId();if(k.isBlank()){output.text="API key required.";return};if(user.text.toString().isBlank()){output.text="Enter a prompt first.";return};output.text="Thinking…";Thread{try{val r=AiClient.chat(p,k,modelId,system.text.toString().ifBlank{"You are a helpful browsing copilot."},user.text.toString());runOnUiThread{output.text=r.text}}catch(e:Exception){runOnUiThread{output.text=e.message ?: "Request failed"}}}.start()}
}
