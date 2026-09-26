package com.privbrowse.app.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AiProfile(val name: String, val prompt: String, val isDefault: Boolean)

object AiProfileStore {
    private const val PREFS="privbrowse_ai"; private const val PROFILES="profiles"
    private fun prefs(c: Context)=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
    fun save(c:Context, profile:AiProfile){
        val arr=JSONArray(prefs(c).getString(PROFILES,"[]") ?: "[]")
        var i = arr.length() - 1
        while (i >= 0) {
            if (arr.optJSONObject(i)?.optString("name") == profile.name) arr.remove(i)
            i--
        }
        if(profile.isDefault) for(i in 0 until arr.length()) arr.optJSONObject(i)?.put("default",false)
        arr.put(JSONObject().put("name",profile.name).put("prompt",profile.prompt).put("default",profile.isDefault))
        prefs(c).edit().putString(PROFILES,arr.toString()).apply()
    }
    fun all(c:Context):List<AiProfile>{
        val raw = prefs(c).getString(PROFILES,"[]") ?: "[]"
        return runCatching {
            val arr=JSONArray(raw)
            List(arr.length()){i-> val o=arr.optJSONObject(i) ?: JSONObject(); AiProfile(o.optString("name"),o.optString("prompt"),o.optBoolean("default")) }
        }.getOrElse { emptyList() }
    }
    fun default(c:Context)=all(c).firstOrNull{it.isDefault}
}
