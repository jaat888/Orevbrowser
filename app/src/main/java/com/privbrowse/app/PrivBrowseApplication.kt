package com.privbrowse.app

import android.app.Application
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrivBrowseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val dir = File(filesDir, "crashes").apply { mkdirs() }
                val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                File(dir, "crash-$stamp.txt").writeText(buildString {
                    appendLine("PrivBrowse crash log")
                    appendLine("time=$stamp")
                    appendLine("thread=${thread.name}")
                    appendLine("app=${BuildConfig.VERSION_NAME}")
                    appendLine("webview=${android.webkit.WebView.getCurrentWebViewPackage()?.versionName ?: "unknown"}")
                    appendLine()
                    append(throwable.stackTraceToString())
                })
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
