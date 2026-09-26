package com.privbrowse.app.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.*
import com.privbrowse.app.video.VideoDownloadStore
import java.util.Locale

class VideoDownloadsActivity : androidx.appcompat.app.AppCompatActivity() {
    private lateinit var urls: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 20, 20, 20) }
        root.addView(TextView(this).apply { text = "Phase 8 — Generic video downloads"; textSize = 22f })
        root.addView(TextView(this).apply { text = "Only directly retrievable generic video URLs are offered. YouTube/Instagram/TikTok remain excluded."; setPadding(0, 12, 0, 12) })
        urls = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this)
        scroll.addView(urls)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(Button(this).apply { text = "Close"; setOnClickListener { finish() } })
        setContentView(root)
        render()
    }

    private fun render() {
        urls.removeAllViews()
        val items = VideoDownloadStore.all(this)
        if (items.isEmpty()) urls.addView(TextView(this).apply { text = "No queued downloads yet." })
        items.forEach { item ->
            val label = "#${item.id}  ${item.mimeType ?: "video"}\n${item.url.take(100)}"
            urls.addView(TextView(this).apply { text = label; setPadding(0, 12, 0, 12) })
        }
    }

    companion object {
        private val ALLOWED_MIME_PREFIXES = listOf("video/")
        private val ALLOWED_EXTENSIONS = setOf("mp4", "webm", "m4v", "mov", "ogv", "mkv")

        private fun isExcludedHost(url: String): Boolean {
            val host = Uri.parse(url).host?.lowercase(Locale.US).orEmpty()
            return host == "youtu.be" || host == "youtube.com" || host.endsWith(".youtube.com") ||
                host == "instagram.com" || host.endsWith(".instagram.com") ||
                host == "tiktok.com" || host.endsWith(".tiktok.com")
        }

        fun looksLikeVideo(url: String, mimeType: String?): Boolean {
            if (isExcludedHost(url)) return false
            val mime = mimeType?.substringBefore(';')?.lowercase(Locale.US)
            if (mime != null && mime != "video/*" && ALLOWED_MIME_PREFIXES.any(mime::startsWith)) return true
            val path = Uri.parse(url).path.orEmpty().lowercase(Locale.US)
            val ext = MimeTypeMap.getFileExtensionFromUrl(path).lowercase(Locale.US)
            return ext in ALLOWED_EXTENSIONS
        }

        fun enqueue(context: Context, url: String, mimeType: String? = null): Long? {
            if (!looksLikeVideo(url, mimeType)) return null
            return runCatching {
                val uri = Uri.parse(url)
                val type = mimeType?.substringBefore(';')?.lowercase(Locale.US)
                    ?.takeIf { it != "video/*" && it.startsWith("video/") }
                    ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        MimeTypeMap.getFileExtensionFromUrl(url).lowercase(Locale.US)
                    )
                val ext = MimeTypeMap.getFileExtensionFromMimeType(type)
                    ?: MimeTypeMap.getFileExtensionFromUrl(url).takeIf { it.isNotBlank() }
                    ?: "mp4"
                val safeExt = ext.lowercase(Locale.US).filter { it.isLetterOrDigit() }.take(8).ifBlank { "mp4" }
                val name = "PrivBrowse-${System.currentTimeMillis()}.$safeExt"
                val req = DownloadManager.Request(uri).apply {
                    setTitle("PrivBrowse video")
                    setDescription("Generic video download")
                    type?.let(::setMimeType)
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
                    setAllowedOverMetered(true)
                    setAllowedOverRoaming(true)
                }
                val id = (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
                VideoDownloadStore.add(context, url, id, type)
                id
            }.getOrNull()
        }
    }
}
