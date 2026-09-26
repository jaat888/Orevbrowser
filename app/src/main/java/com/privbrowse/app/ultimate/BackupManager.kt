package com.privbrowse.app.ultimate

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.privbrowse.app.BuildConfig
import com.privbrowse.app.ui.MainActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.GZIPInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPOutputStream

object BackupManager {
    fun createLocalBackup(context: Context): File {
        val root = JSONObject()
        root.put("version", 2)
        root.put("createdAt", System.currentTimeMillis())
        root.put("profile", context.getSharedPreferences("privbrowse_profile", Context.MODE_PRIVATE).getString("active_profile", "personal"))
        val mainPrefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
        val prefJson = JSONObject()
        val prefTypes = JSONObject()
        for ((k, v) in mainPrefs.all) {
            when (v) {
                is Boolean -> { prefJson.put(k, v); prefTypes.put(k, "boolean") }
                is Int -> { prefJson.put(k, v); prefTypes.put(k, "int") }
                is Long -> { prefJson.put(k, v); prefTypes.put(k, "long") }
                is Float -> { prefJson.put(k, v.toDouble()); prefTypes.put(k, "float") }
                is String -> { prefJson.put(k, v); prefTypes.put(k, "string") }
                is Set<*> -> { prefJson.put(k, JSONArray(v.filterIsInstance<String>())); prefTypes.put(k, "set") }
            }
        }
        root.put("preferences", prefJson)
        root.put("preferenceTypes", prefTypes)
        val ultimatePrefs = UltimateFeatureStore.prefs(context)
        val ultimateJson = JSONObject()
        val ultimateTypes = JSONObject()
        for ((k, v) in ultimatePrefs.all) {
            when (v) {
                is Boolean -> { ultimateJson.put(k, v); ultimateTypes.put(k, "boolean") }
                is Int -> { ultimateJson.put(k, v); ultimateTypes.put(k, "int") }
                is Long -> { ultimateJson.put(k, v); ultimateTypes.put(k, "long") }
                is Float -> { ultimateJson.put(k, v.toDouble()); ultimateTypes.put(k, "float") }
                is String -> { ultimateJson.put(k, v); ultimateTypes.put(k, "string") }
                is Set<*> -> { ultimateJson.put(k, JSONArray(v.filterIsInstance<String>())); ultimateTypes.put(k, "set") }
            }
        }
        root.put("ultimate", ultimateJson)
        root.put("ultimateTypes", ultimateTypes)
        root.put("note", "Local PrivBrowse backup. Browser databases remain on-device and are not copied into this JSON snapshot.")
        val dir = File(context.getExternalFilesDir("Backups") ?: context.filesDir, "PrivBrowse").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val file = File(dir, "privbrowse-backup-$stamp.json.gz")
        GZIPOutputStream(file.outputStream()).bufferedWriter(Charsets.UTF_8).use { it.write(root.toString(2)) }
        return file
    }

    fun restoreFromUri(context: Context, uri: Uri): Boolean {
        return runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return false
            val raw = if (bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()) {
                GZIPInputStream(bytes.inputStream()).bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                bytes.toString(Charsets.UTF_8)
            }
            restoreJson(context, raw)
        }.getOrDefault(false)
    }

    private fun restoreJson(context: Context, raw: String): Boolean {
        val root = JSONObject(raw)
        if (root.optInt("version", 1) !in 1..2) return false
        val mainValues = root.optJSONObject("preferences") ?: return false
        restorePrefs(context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE), mainValues, root.optJSONObject("preferenceTypes"))
        restorePrefs(UltimateFeatureStore.prefs(context), root.optJSONObject("ultimate"), root.optJSONObject("ultimateTypes"))
        return true
    }

    private fun restorePrefs(prefs: android.content.SharedPreferences, values: JSONObject?, types: JSONObject?) {
        if (values == null) return
        val editor = prefs.edit()
        val keys = values.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = values.opt(key)
            when (types?.optString(key, "") ?: "") {
                "boolean" -> editor.putBoolean(key, values.optBoolean(key))
                "int" -> editor.putInt(key, values.optInt(key))
                "long" -> editor.putLong(key, values.optLong(key))
                "float" -> editor.putFloat(key, values.optDouble(key).toFloat())
                "string" -> editor.putString(key, values.optString(key))
                "set" -> editor.putStringSet(key, (value as? JSONArray)?.let { a -> (0 until a.length()).mapNotNull { i -> a.optString(i, null) }.toSet() } ?: emptySet())
                else -> when (value) {
                    value === JSONObject.NULL -> editor.remove(key)
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Double -> editor.putFloat(key, value.toFloat())
                    is String -> editor.putString(key, value)
                    is JSONArray -> editor.putStringSet(key, (0 until value.length()).mapNotNull { i -> value.optString(i, null) }.toSet())
                }
            }
        }
        editor.apply()
    }

    fun shareLatest(context: Context): Boolean {
        val file = runCatching { createLocalBackup(context) }.getOrNull() ?: return false
        val uri: Uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.files", file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "application/gzip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Share PrivBrowse backup"))
        return true
    }
}
