package com.privbrowse.app.privacy

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object BreachChecker {
    data class Result(val ok: Boolean, val message: String)

    fun check(email: String, apiKey: String): Result {
        if (email.isBlank() || !email.contains('@')) return Result(false, "Enter a valid email address")
        if (apiKey.isBlank()) return Result(false, "Have I Been Pwned API key is required")
        return try {
            val url = URL("https://haveibeenpwned.com/api/v3/breachedaccount/${URLEncoder.encode(email, "UTF-8")}?truncateResponse=false")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("hibp-api-key", apiKey)
                setRequestProperty("user-agent", "PrivBrowse")
                setRequestProperty("accept", "application/json")
            }
            val code = conn.responseCode
            when (code) {
                200 -> Result(true, "This email appears in one or more reported breaches.")
                404 -> Result(true, "No breach was returned for this email.")
                401 -> Result(false, "The HIBP API key was rejected.")
                429 -> Result(false, "HIBP rate limit reached. Try again later.")
                else -> Result(false, "HIBP returned HTTP $code")
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result(false, "Breach check failed: ${e.message ?: "network error"}")
        }
    }
}
