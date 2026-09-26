package com.privbrowse.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.privbrowse.app.R
import com.privbrowse.app.ai.AiClient
import com.privbrowse.app.ai.AiCustomModelStore
import com.privbrowse.app.ai.AiProfileStore
import com.privbrowse.app.ai.AiProvider
import com.privbrowse.app.privacy.SecureStore
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Full AI workspace. It deliberately has no app-owned AI backend: the device
 * talks directly to the provider selected by the user.
 */
class AiCopilotActivity : AppCompatActivity() {
    private lateinit var prompt: TextInputEditText
    private lateinit var chatBox: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var send: MaterialButton
    private lateinit var stop: MaterialButton
    private lateinit var contextSwitch: MaterialSwitch
    private lateinit var status: TextView
    private lateinit var connectionPill: TextView
    private lateinit var modelLabel: TextView

    private var providerSpinner: Spinner? = null
    private var modelSpinner: Spinner? = null
    private var keyField: TextInputEditText? = null
    private var endpointField: TextInputEditText? = null
    private var setupDialog: AlertDialog? = null

    private var pageTitle = ""
    private var pageUrl = ""
    private var pageText = ""
    private var lastAnswer = ""
    private var lastUserPrompt = ""
    private var activeSystemPrompt = ""
    private var textToSpeech: TextToSpeech? = null
    private var aiThread: Thread? = null
    private val cancelRequested = AtomicBoolean(false)
    private val chat = mutableListOf<Pair<String, String>>()
    private val addCustomModel = "+ Add custom model"

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun color(id: Int) = ContextCompat.getColor(this, id)
    private fun prefs() = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE)
    private fun aiPrefs() = getSharedPreferences("privbrowse_ai_ui", MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageTitle = intent.getStringExtra(EXTRA_PAGE_TITLE).orEmpty()
        pageUrl = intent.getStringExtra(EXTRA_PAGE_URL).orEmpty()
        pageText = intent.getStringExtra(EXTRA_PAGE_TEXT).orEmpty()
        activeSystemPrompt = aiPrefs().getString("active_profile", AiProfileStore.default(this).prompt)
            ?: AiProfileStore.default(this).prompt
        buildUi()
        restoreChat()
        updateHeader()
        intent.getStringExtra(EXTRA_PROMPT)?.takeIf(String::isNotBlank)?.let {
            prompt.setText(it)
            prompt.requestFocus()
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.background_light))
        }
        root.addView(toolbar())

        scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(dp(12), dp(2), dp(12), dp(12))
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), dp(4), dp(2), dp(20))
        }
        content.addView(heroCard())
        content.addView(pageCard(), lp().apply { topMargin = dp(10) })
        content.addView(toolDeck(), lp().apply { topMargin = dp(10) })
        chatBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(chatBox, lp().apply { topMargin = dp(12) })
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(composer())
        setContentView(root)
    }

    private fun toolbar(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(7), dp(5), dp(7), dp(5))
        addView(TextView(this@AiCopilotActivity).apply {
            text = "‹"
            textSize = 38f
            gravity = Gravity.CENTER
            setTextColor(color(R.color.text_light))
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(44), dp(50)))
        addView(LinearLayout(this@AiCopilotActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@AiCopilotActivity).apply {
                text = "PrivBrowse AI"
                textSize = 20f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(color(R.color.text_light))
            })
            modelLabel = TextView(this@AiCopilotActivity).apply {
                textSize = 11.5f
                setTextColor(color(R.color.text_muted_light))
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }
            addView(modelLabel)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        connectionPill = TextView(this@AiCopilotActivity).apply {
            text = "SETUP"
            textSize = 10f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(7), dp(10), dp(7))
            setTextColor(color(R.color.accent_dark))
            setBackgroundColor(color(R.color.surface_light_alt))
            setOnClickListener { showSetup() }
        }
        addView(connectionPill, LinearLayout.LayoutParams(-2, dp(38)).apply { rightMargin = dp(5) })
        addView(TextView(this@AiCopilotActivity).apply {
            text = "⋮"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(color(R.color.text_light))
            setOnClickListener { showMore() }
        }, LinearLayout.LayoutParams(dp(42), dp(48)))
    }

    private fun heroCard(): View = MaterialCardView(this).apply {
        radius = dp(20).toFloat()
        cardElevation = 0f
        strokeWidth = dp(1)
        strokeColor = color(R.color.divider)
        setCardBackgroundColor(color(R.color.surface_light))
        val box = LinearLayout(this@AiCopilotActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
        }
        val top = LinearLayout(this@AiCopilotActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val icon = TextView(this@AiCopilotActivity).apply {
            text = "AI"
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(color(R.color.on_accent))
            setBackgroundColor(color(R.color.accent))
        }
        top.addView(icon, LinearLayout.LayoutParams(dp(44), dp(44)))
        top.addView(LinearLayout(this@AiCopilotActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
            addView(TextView(this@AiCopilotActivity).apply {
                text = "Ask, explain, summarize"
                textSize = 17f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(color(R.color.text_light))
            })
            addView(TextView(this@AiCopilotActivity).apply {
                text = "Use page context when it helps, or just chat normally."
                textSize = 12f
                setTextColor(color(R.color.text_muted_light))
                setPadding(0, dp(3), 0, 0)
            })
        }, LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(top)
        addView(box)
    }

    private fun pageCard(): View = MaterialCardView(this).apply {
        radius = dp(18).toFloat()
        cardElevation = 0f
        strokeWidth = dp(1)
        strokeColor = color(R.color.divider)
        setCardBackgroundColor(color(R.color.surface_light))
        val box = LinearLayout(this@AiCopilotActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        val row = LinearLayout(this@AiCopilotActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(LinearLayout(this@AiCopilotActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@AiCopilotActivity).apply {
                text = "Page context"
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(color(R.color.text_light))
            })
            addView(TextView(this@AiCopilotActivity).apply {
                text = pageTitle.ifBlank { "No page attached" }
                textSize = 11.5f
                setTextColor(color(R.color.text_muted_light))
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            })
        }, LinearLayout.LayoutParams(0, -2, 1f))
        contextSwitch = MaterialSwitch(this@AiCopilotActivity).apply {
            text = "Use page"
            isChecked = prefs().getBoolean(MainActivity.KEY_AI_CONTEXT, true) && pageText.isNotBlank()
            setOnCheckedChangeListener { _, checked ->
                prefs().edit().putBoolean(MainActivity.KEY_AI_CONTEXT, checked).apply()
            }
        }
        row.addView(contextSwitch)
        box.addView(row)
        if (pageUrl.isNotBlank()) box.addView(TextView(this@AiCopilotActivity).apply {
            text = pageUrl
            textSize = 10.5f
            setTextColor(color(R.color.text_muted_light))
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.MIDDLE
            setPadding(0, dp(3), 0, 0)
        })
        addView(box)
    }

    private fun toolDeck(): View = MaterialCardView(this).apply {
        radius = dp(18).toFloat()
        cardElevation = 0f
        strokeWidth = dp(1)
        strokeColor = color(R.color.divider)
        setCardBackgroundColor(color(R.color.surface_light))
        val box = LinearLayout(this@AiCopilotActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(11))
        }
        box.addView(TextView(this@AiCopilotActivity).apply {
            text = "AI tools"
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(color(R.color.text_light))
            setPadding(dp(2), 0, 0, dp(5))
        })
        val actions = listOf(
            "Summarize", "Key points", "Explain", "Facts", "Hindi", "Rewrite", "Study",
            "Compare", "Checklist", "Questions", "Glossary", "Timeline", "Pros/cons", "Outline",
            "Action items", "FAQs", "Entities", "JSON", "Code explain", "Title ideas"
        )
        val scroller = HorizontalScrollView(this@AiCopilotActivity).apply { isHorizontalScrollBarEnabled = false }
        val chips = LinearLayout(this@AiCopilotActivity).apply { orientation = LinearLayout.HORIZONTAL }
        actions.forEach { label ->
            chips.addView(Chip(this@AiCopilotActivity).apply {
                text = label
                isCheckable = false
                setOnClickListener { sendAction(label) }
                layoutParams = LinearLayout.LayoutParams(-2, dp(38)).apply { rightMargin = dp(6) }
            })
        }
        scroller.addView(chips)
        box.addView(scroller)
        addView(box)
    }

    private fun composer(): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(7), dp(10), dp(10))
            setBackgroundColor(color(R.color.surface_light))
        }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.BOTTOM }
        val inputWrap = TextInputLayout(this@AiCopilotActivity).apply {
            hint = "Ask PrivBrowse AI…"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            isCounterEnabled = false
        }
        prompt = TextInputEditText(this).apply {
            minLines = 1
            maxLines = 5
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setTextColor(color(R.color.text_light))
            setPadding(dp(12), dp(7), dp(12), dp(7))
        }
        inputWrap.addView(prompt, LinearLayout.LayoutParams(-1, -2))
        row.addView(inputWrap, LinearLayout.LayoutParams(0, -2, 1f))
        send = MaterialButton(this).apply {
            text = "Send"
            minWidth = dp(70)
            setOnClickListener { sendMessage() }
        }
        stop = MaterialButton(this).apply {
            text = "Stop"
            minWidth = dp(70)
            visibility = View.GONE
            setOnClickListener { stopRequest() }
        }
        row.addView(send, LinearLayout.LayoutParams(dp(76), dp(54)).apply { leftMargin = dp(6) })
        row.addView(stop, LinearLayout.LayoutParams(dp(76), dp(54)).apply { leftMargin = dp(4) })
        panel.addView(row)
        status = TextView(this).apply {
            text = "Set a provider key in Setup to start."
            textSize = 11f
            maxLines = 2
            setTextColor(color(R.color.text_muted_light))
            setPadding(dp(4), dp(4), dp(2), 0)
        }
        panel.addView(status)
        return panel
    }

    private fun showSetup() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(3), 0, dp(3), 0)
        }
        val provider = Spinner(this)
        provider.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, AiProvider.entries.map { it.label })
        providerSpinner = provider
        root.addView(label("Provider"))
        root.addView(provider, lp().apply { topMargin = dp(3) })

        val model = Spinner(this)
        modelSpinner = model
        root.addView(label("Model"), lp().apply { topMargin = dp(7) })
        root.addView(model, lp().apply { topMargin = dp(2) })

        keyField = TextInputEditText(this).apply {
            hint = "Paste API key"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        root.addView(TextInputLayout(this).apply {
            hint = "API key"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            addView(keyField)
        }, lp().apply { topMargin = dp(8) })

        endpointField = TextInputEditText(this).apply {
            hint = "https://your-host/v1/chat/completions"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }
        val endpointWrap = TextInputLayout(this).apply {
            hint = "Custom endpoint (Custom provider only)"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            addView(endpointField)
        }
        root.addView(endpointWrap, lp().apply { topMargin = dp(7) })

        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row1.addView(setupButton("Load") { loadKey() }, weightLp())
        row1.addView(setupButton("Test") { testConnection() }, weightLp().apply { leftMargin = dp(6) })
        row1.addView(setupButton("Save") { saveConfig() }, weightLp().apply { leftMargin = dp(6) })
        root.addView(row1, lp().apply { topMargin = dp(6) })

        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row2.addView(setupButton("Refresh models") { refreshModels() }, weightLp())
        row2.addView(setupButton("Custom model") { promptCustomModel(currentProvider()) }, weightLp().apply { leftMargin = dp(6) })
        root.addView(row2, lp().apply { topMargin = dp(6) })

        root.addView(setupButton("Prompt profiles") { dismissSetup(); showProfiles() }, lp().apply { topMargin = dp(6) })

        setupDialog = AlertDialog.Builder(this)
            .setTitle("AI setup")
            .setView(root)
            .setNegativeButton(R.string.close, null)
            .create()
        setupDialog?.setOnDismissListener { setupDialog = null }
        setupDialog?.show()
        wireSetupProvider(provider, model, endpointWrap)
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        textSize = 11.5f
        setTextColor(color(R.color.text_muted_light))
    }

    private fun weightLp() = LinearLayout.LayoutParams(0, dp(46), 1f)
    private fun setupButton(text: String, click: () -> Unit) = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
        this.text = text
        setOnClickListener { click() }
    }

    private fun wireSetupProvider(provider: Spinner, model: Spinner, endpointWrap: TextInputLayout) {
        val saved = aiPrefs().getString("provider", AiProvider.GROQ.name) ?: AiProvider.GROQ.name
        provider.setSelection(AiProvider.entries.indexOfFirst { it.name == saved }.coerceAtLeast(0))
        provider.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                populateModels(currentProvider(), model)
                updateEndpointVisibility(endpointWrap, currentProvider())
                loadKey()
            }
        }
        populateModels(currentProvider(), model)
        updateEndpointVisibility(endpointWrap, currentProvider())
        loadKey()
    }

    private fun updateEndpointVisibility(wrap: TextInputLayout, provider: AiProvider) {
        wrap.visibility = if (provider == AiProvider.CUSTOM) View.VISIBLE else View.GONE
        endpointField?.setText(aiPrefs().getString("custom_endpoint", AiProvider.CUSTOM.endpoint).orEmpty())
    }

    private fun populateModels(provider: AiProvider, spinner: Spinner, preferred: String? = null, remote: List<String>? = null) {
        val options = ((remote ?: emptyList()) + provider.models + AiCustomModelStore.all(this, provider) + addCustomModel)
            .filter(String::isNotBlank)
            .distinct()
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        val wanted = preferred ?: aiPrefs().getString("model_${provider.name}", provider.defaultModel) ?: provider.defaultModel
        spinner.setSelection(options.indexOf(wanted).takeIf { it >= 0 } ?: 0)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val value = options.getOrNull(position).orEmpty()
                if (value == addCustomModel) promptCustomModel(provider) else updateHeader(provider, value)
            }
        }
        updateHeader(provider, wanted)
    }

    private fun currentProvider(): AiProvider {
        val position = providerSpinner?.selectedItemPosition ?: 0
        return AiProvider.entries.getOrElse(position) { AiProvider.GROQ }
    }

    private fun currentModel(): String {
        val spinner = modelSpinner ?: return currentProvider().defaultModel
        return spinner.selectedItem?.toString()?.takeIf { it != addCustomModel }?.ifBlank { null }
            ?: currentProvider().defaultModel
    }

    private fun customEndpoint(): String? = endpointField?.text?.toString()?.trim()?.takeIf(String::isNotBlank)
        ?: aiPrefs().getString("custom_endpoint", null)

    private fun updateHeader(provider: AiProvider, model: String = aiPrefs().getString("model_${provider.name}", provider.defaultModel).orEmpty()) {
        if (!::modelLabel.isInitialized) return
        val show = prefs().getBoolean(MainActivity.KEY_AI_SHOW_MODEL, true)
        modelLabel.text = if (show) "${provider.label} • ${model.ifBlank { provider.defaultModel }}" else "Ready"
        val savedKey = SecureStore.get(this, "ai_${provider.name.lowercase()}_key").orEmpty()
        connectionPill.text = if (savedKey.isBlank()) "SETUP" else "READY"
    }

    private fun loadSavedConfiguration() = updateHeader(savedProvider())

    private fun savedProvider(): AiProvider {
        val name = aiPrefs().getString("provider", AiProvider.GROQ.name) ?: AiProvider.GROQ.name
        return AiProvider.entries.firstOrNull { it.name == name } ?: AiProvider.GROQ
    }

    private fun loadKey() {
        val p = currentProvider()
        keyField?.setText(SecureStore.get(this, "ai_${p.name.lowercase()}_key").orEmpty())
        updateHeader(p, currentModel())
    }

    private fun saveConfig() {
        val p = currentProvider()
        val key = keyField?.text?.toString()?.trim().orEmpty()
        SecureStore.put(this, "ai_${p.name.lowercase()}_key", key)
        val selectedModel = currentModel()
        val edit = aiPrefs().edit().putString("provider", p.name).putString("model_${p.name}", selectedModel)
        if (p == AiProvider.CUSTOM) edit.putString("custom_endpoint", customEndpoint().orEmpty())
        edit.apply()
        activeSystemPrompt = aiPrefs().getString("active_profile", activeSystemPrompt).orEmpty()
        updateHeader(p, selectedModel)
        status.text = if (key.isBlank()) "Saved without a key — add one before sending." else "Saved securely on this device."
        Toast.makeText(this, "AI setup saved", Toast.LENGTH_SHORT).show()
    }

    private fun refreshModels() {
        val p = currentProvider()
        val key = keyField?.text?.toString()?.trim().orEmpty()
        if (key.isBlank()) { status.text = "Enter an API key first."; return }
        status.text = "Loading ${p.label} models…"
        Thread {
            try {
                val models = AiClient.listModels(p, key, customEndpoint())
                runOnUiThread {
                    val previous = currentModel()
                    val spinner = modelSpinner ?: return@runOnUiThread
                    populateModels(p, spinner, previous, models)
                    status.text = "${models.size} models available"
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = cleanError(e.message ?: "Model refresh failed") }
            }
        }.start()
    }

    private fun testConnection() {
        val p = currentProvider()
        val key = keyField?.text?.toString()?.trim().orEmpty()
        if (key.isBlank()) { status.text = "API key required."; return }
        status.text = "Testing ${p.label}…"
        Thread {
            try {
                AiClient.chat(p, key, currentModel(), "You are a connectivity test. Reply with exactly OK.", "OK", 64, customEndpoint())
                runOnUiThread { status.text = "Connection OK • model responded." }
            } catch (e: Exception) {
                runOnUiThread { status.text = cleanError(e.message ?: "Connection failed") }
            }
        }.start()
    }

    private fun sendAction(label: String) {
        val promptText = when (label) {
            "Summarize" -> "Summarize the current page in a concise set of bullets. Preserve important numbers and caveats."
            "Key points" -> "Extract the 5–10 most important points from the current page."
            "Explain" -> "Explain the current page simply, defining difficult terms."
            "Facts" -> "Extract concrete names, dates, numbers, definitions and other verifiable facts from the current page."
            "Hindi" -> "Explain the main useful content of the current page in clear Hindi."
            "Rewrite" -> "Turn the important content of the current page into clean notes without adding unsupported facts."
            "Study" -> "Create a short study guide from the current page with definitions, key ideas, examples and self-check questions."
            "Compare" -> "Find the main options or positions on the current page and compare them neutrally in a compact table."
            "Checklist" -> "Convert useful instructions or steps from the current page into a practical checklist."
            "Questions" -> "Create 10 useful questions and answers from the current page."
            "Glossary" -> "Build a glossary of important terms from the current page with simple definitions."
            "Timeline" -> "Extract dated events from the current page and arrange them chronologically."
            "Pros/cons" -> "Extract the main advantages and disadvantages explicitly supported by the current page."
            "Outline" -> "Create a clean outline of the current page's structure and ideas."
            "Action items" -> "Extract concrete action items or next steps from the current page."
            "FAQs" -> "Create a concise FAQ based only on the current page."
            "Entities" -> "Extract important people, organizations, products, places and other named entities from the current page."
            "JSON" -> "Return a compact JSON object containing the main title, topic, key points, dates and entities from the current page."
            "Code explain" -> "Explain any code on the current page in simple terms and point out important behavior or risks."
            "Title ideas" -> "Suggest 8 clear titles for the main topic of the current page."
            else -> label
        }
        prompt.setText(promptText)
        sendMessage()
    }

    private fun sendMessage() {
        if (aiThread?.isAlive == true) return
        val userText = prompt.text?.toString()?.trim().orEmpty()
        if (userText.isBlank()) return
        val provider = savedProvider()
        val key = SecureStore.get(this, "ai_${provider.name.lowercase()}_key").orEmpty()
        if (key.isBlank()) {
            status.text = "Add an API key in Setup."
            showSetup()
            return
        }
        val model = aiPrefs().getString("model_${provider.name}", provider.defaultModel)?.trim().orEmpty().ifBlank { provider.defaultModel }
        val includePage = contextSwitch.isChecked && pageText.isNotBlank()
        val pageContext = if (includePage) {
            "\n\nCURRENT PAGE CONTEXT\nTitle: $pageTitle\nURL: $pageUrl\nReadable text:\n${pageText.take(14000)}"
        } else ""
        val history = if (chat.isNotEmpty()) {
            "\n\nRECENT CHAT\n" + chat.takeLast(8).joinToString("\n") { "${it.first}: ${it.second}" }.take(9000)
        } else ""
        val system = activeSystemPrompt.ifBlank {
            "You are PrivBrowse AI, a careful browser assistant. Answer directly and clearly. Distinguish page facts from uncertainty. Never invent citations or pretend to have opened a page you did not receive."
        }
        lastUserPrompt = userText
        addBubble("You", userText, false)
        prompt.setText("")
        send.visibility = View.GONE
        stop.visibility = View.VISIBLE
        status.text = "Thinking…"
        cancelRequested.set(false)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        val endpoint = aiPrefs().getString("custom_endpoint", null)
        aiThread = Thread {
            try {
                val reply = AiClient.chat(provider, key, model, system, userText + pageContext + history, 3072, endpoint)
                if (cancelRequested.get()) return@Thread
                lastAnswer = reply.text
                runOnUiThread {
                    addBubble(provider.label, reply.text, true)
                    setIdle("Done")
                    saveChat()
                }
            } catch (e: Exception) {
                if (cancelRequested.get()) return@Thread
                val message = cleanError(e.message ?: "Request failed")
                runOnUiThread {
                    addBubble("PrivBrowse", message, true)
                    setIdle("Request failed")
                }
            }
        }
        aiThread?.start()
    }

    private fun stopRequest() {
        cancelRequested.set(true)
        aiThread?.interrupt()
        aiThread = null
        send.visibility = View.VISIBLE
        stop.visibility = View.GONE
        status.text = "Request stopped"
    }

    private fun setIdle(text: String) {
        aiThread = null
        send.visibility = View.VISIBLE
        stop.visibility = View.GONE
        status.text = text
    }

    private fun addBubble(who: String, text: String, withActions: Boolean) {
        chat.add(who to text)
        addBubbleView(who, text, withActions)
    }

    private fun addBubbleView(who: String, text: String, withActions: Boolean) {
        val card = MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = color(R.color.divider)
            setCardBackgroundColor(color(R.color.surface_light))
            layoutParams = lp().apply { bottomMargin = dp(8) }
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        box.addView(TextView(this).apply {
            this.text = who
            textSize = 10.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(color(if (who == "You") R.color.text_muted_light else R.color.accent_dark))
        })
        box.addView(TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(color(R.color.text_light))
            setTextIsSelectable(true)
            setPadding(0, dp(4), 0, 0)
        })
        if (withActions) {
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            actions.addView(smallButton("Copy") { copyText(text) })
            actions.addView(smallButton("Share") { shareText(text) })
            actions.addView(smallButton("Speak") { speak(text) })
            box.addView(actions, lp().apply { topMargin = dp(6) })
        }
        card.addView(box)
        chatBox.addView(card)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun smallButton(label: String, action: () -> Unit) = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
        text = label
        minWidth = dp(60)
        minHeight = dp(38)
        setOnClickListener { action() }
    }

    private fun copyText(value: String) {
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("PrivBrowse AI", value))
        status.text = "Copied"
    }

    private fun shareText(value: String) {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, value)
        }, "Share AI answer"))
    }

    private fun speak(value: String) {
        if (textToSpeech == null) {
            textToSpeech = TextToSpeech(this) { result ->
                if (result == TextToSpeech.SUCCESS) textToSpeech?.language = java.util.Locale.getDefault()
            }
        }
        textToSpeech?.speak(value.take(8000), TextToSpeech.QUEUE_FLUSH, null, "privbrowse-ai")
    }

    private fun showMore() {
        val labels = arrayOf(
            "Paste clipboard into prompt", "Use current page again", "Prompt profiles",
            "Retry last", "Copy last answer", "Share last answer", "Clear chat", "Export chat"
        )
        AlertDialog.Builder(this).setTitle("AI actions").setItems(labels) { _, which ->
            when (which) {
                0 -> pasteClipboard()
                1 -> { contextSwitch.isChecked = true; status.text = "Page context enabled" }
                2 -> showProfiles()
                3 -> if (lastUserPrompt.isNotBlank()) { prompt.setText(lastUserPrompt); sendMessage() }
                4 -> if (lastAnswer.isNotBlank()) copyText(lastAnswer)
                5 -> if (lastAnswer.isNotBlank()) shareText(lastAnswer)
                6 -> clearChat()
                7 -> exportChat()
            }
        }.show()
    }

    private fun pasteClipboard() {
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        if (clip.isBlank()) { status.text = "Clipboard is empty"; return }
        prompt.setText(clip.take(12000))
        prompt.setSelection(prompt.text?.length ?: 0)
        status.text = "Clipboard text pasted"
    }

    private fun showProfiles() {
        val profiles = AiProfileStore.all(this)
        val names = profiles.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Prompt profiles")
            .setItems(names) { _, which ->
                val selected = profiles[which]
                activeSystemPrompt = selected.prompt
                aiPrefs().edit().putString("active_profile", selected.prompt).apply()
                status.text = "Profile: ${selected.name}"
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun promptCustomModel(provider: AiProvider) {
        val field = EditText(this).apply {
            hint = "Exact model ID"
            setSingleLine(true)
        }
        AlertDialog.Builder(this)
            .setTitle("Add custom model")
            .setView(field)
            .setPositiveButton("Add") { _, _ ->
                val id = field.text?.toString()?.trim().orEmpty()
                if (id.isNotBlank()) {
                    AiCustomModelStore.add(this, provider, id)
                    modelSpinner?.let { populateModels(provider, it, id) }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearChat() {
        chat.clear()
        lastAnswer = ""
        lastUserPrompt = ""
        chatBox.removeAllViews()
        aiPrefs().edit().remove("chat").apply()
        status.text = "Chat cleared"
    }

    private fun saveChat() {
        if (!prefs().getBoolean(MainActivity.KEY_AI_HISTORY, true)) return
        val array = JSONArray()
        chat.takeLast(24).forEach { (who, text) -> array.put(JSONObject().put("who", who).put("text", text)) }
        aiPrefs().edit().putString("chat", array.toString()).apply()
    }

    private fun restoreChat() {
        if (!prefs().getBoolean(MainActivity.KEY_AI_HISTORY, true)) return
        runCatching {
            val array = JSONArray(aiPrefs().getString("chat", "[]") ?: "[]")
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val who = item.optString("who")
                val text = item.optString("text")
                if (who.isNotBlank() && text.isNotBlank()) {
                    chat.add(who to text)
                    addBubbleView(who, text, who != "You")
                    if (who != "You") lastAnswer = text
                }
            }
        }.onFailure { chat.clear() }
    }

    private fun exportChat() {
        val text = chat.joinToString("\n\n") { "${it.first}:\n${it.second}" }
        if (text.isBlank()) { status.text = "No chat to export"; return }
        shareText(text)
    }

    private fun cleanError(raw: String): String {
        val compact = raw.replace('\n', ' ').replace(Regex("\\s+"), " ").trim()
        return when {
            compact.contains("HTTP 401") -> "401: API key rejected. Check the key and provider."
            compact.contains("HTTP 403") -> "403: Provider rejected access. Check key permissions and model access."
            compact.contains("HTTP 404") -> "404: Model or endpoint not found. Refresh models or set the exact model ID."
            compact.contains("HTTP 405") -> "405: Endpoint does not support this request method. Check the endpoint."
            compact.contains("HTTP 429") -> "429: Rate limit reached. Try later or use another available model."
            compact.contains("HTTP 5") -> "Provider server error. Try again in a moment."
            compact.length > 320 -> compact.take(320) + "…"
            else -> compact
        }
    }

    private fun updateHeader() {
        updateHeader(savedProvider())
    }

    private fun dismissSetup() = setupDialog?.dismiss()

    private fun lp() = LinearLayout.LayoutParams(-1, -2)

    override fun onDestroy() {
        cancelRequested.set(true)
        aiThread?.interrupt()
        aiThread = null
        textToSpeech?.shutdown()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PAGE_TITLE = "page_title"
        const val EXTRA_PAGE_URL = "page_url"
        const val EXTRA_PAGE_TEXT = "page_text"
        const val EXTRA_PROMPT = "prompt"
    }
}
