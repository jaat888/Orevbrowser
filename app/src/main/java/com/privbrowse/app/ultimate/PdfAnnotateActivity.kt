package com.privbrowse.app.ultimate

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.Gravity
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.privbrowse.app.R
import java.io.File
import java.io.FileOutputStream

/** Multi-page local PDF viewer with per-page raster annotations and full-document export. */
class PdfAnnotateActivity : AppCompatActivity() {
    private var renderer: PdfRenderer? = null
    private var fd: ParcelFileDescriptor? = null
    private var pageIndex = 0
    private lateinit var image: ImageView
    private lateinit var pageLabel: TextView
    private var mode = Paint.Style.STROKE
    private val annotations = mutableMapOf<Int, Bitmap>()
    private var displayBitmap: Bitmap? = null

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        build()
        b?.getString("uri")?.let { open(Uri.parse(it)) } ?: picker.launch(arrayOf("application/pdf"))
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun build() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.background_light))
        }
        val bar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        fun btn(label: String, click: () -> Unit) = TextView(this).apply {
            text = label
            textSize = 13f
            setTextColor(getColor(R.color.accent_dark))
            setPadding(dp(8), dp(11), dp(8), dp(11))
            setOnClickListener { click() }
        }
        bar.addView(btn("Open") { picker.launch(arrayOf("application/pdf")) })
        bar.addView(btn("‹") { showPage(pageIndex - 1) })
        bar.addView(btn("›") { showPage(pageIndex + 1) })
        pageLabel = TextView(this).apply {
            textSize = 12f
            setTextColor(getColor(R.color.text_muted_light))
            gravity = Gravity.CENTER
        }
        bar.addView(pageLabel, LinearLayout.LayoutParams(0, dp(48), 1f))
        bar.addView(btn("Highlight") { mode = Paint.Style.FILL })
        bar.addView(btn("Pen") { mode = Paint.Style.STROKE })
        bar.addView(btn("Save PDF") { save() })
        root.addView(bar)
        image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.WHITE)
        }
        root.addView(image, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun open(uri: Uri) {
        closeDocument()
        runCatching {
            fd = contentResolver.openFileDescriptor(uri, "r")
            renderer = PdfRenderer(fd!!)
            pageIndex = 0
            annotations.clear()
            showPage(0)
        }.onFailure {
            Toast.makeText(this, "Cannot open PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPage(index: Int) {
        val r = renderer ?: return
        if (index !in 0 until r.pageCount) return
        saveCurrentAnnotation()
        pageIndex = index
        val page = r.openPage(index)
        val width = 1200
        val height = (page.height.toFloat() / page.width.toFloat() * width).toInt().coerceAtLeast(900)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        annotations[index]?.let { overlay -> Canvas(bitmap).drawBitmap(overlay, 0f, 0f, null) }
        displayBitmap?.let { if (it !== bitmap) runCatching { it.recycle() } }
        displayBitmap = bitmap
        image.setImageBitmap(bitmap)
        pageLabel.text = "Page ${index + 1}/${r.pageCount}"
        image.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    ensureAnnotation(width, height)
                    val x = event.x / view.width.toFloat() * width
                    val y = event.y / view.height.toFloat() * height
                    paintPoint(x, y)
                    true
                }
                else -> true
            }
        }
    }

    private fun ensureAnnotation(width: Int, height: Int) {
        val bitmap = annotations[pageIndex]
        if (bitmap == null || bitmap.width != width || bitmap.height != height) {
            annotations[pageIndex] = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
    }

    private fun saveCurrentAnnotation() {
        // Draw operations already land in annotations[pageIndex], so no extra copy is needed.
    }

    private fun paintPoint(x: Float, y: Float) {
        val overlay = annotations[pageIndex] ?: return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (mode == Paint.Style.FILL) Color.argb(80, 255, 230, 0) else Color.RED
            style = mode
            strokeWidth = if (mode == Paint.Style.FILL) 70f else 6f
        }
        fun drawStroke(target: Bitmap) {
            val canvas = Canvas(target)
            if (mode == Paint.Style.FILL) canvas.drawRect(x - 45f, y - 20f, x + 45f, y + 20f, paint)
            else canvas.drawCircle(x, y, 3f, paint)
        }
        drawStroke(overlay)
        displayBitmap?.let(::drawStroke)
        image.invalidate()
    }

    private fun renderForExport(pageNumber: Int): Bitmap {
        val r = renderer ?: error("No document")
        val page = r.openPage(pageNumber)
        val width = 1200
        val height = (page.height.toFloat() / page.width.toFloat() * width).toInt().coerceAtLeast(900)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        annotations[pageNumber]?.let { Canvas(bitmap).drawBitmap(it, 0f, 0f, null) }
        return bitmap
    }

    private fun save() {
        val r = renderer ?: run { Toast.makeText(this, "Open a PDF first", Toast.LENGTH_SHORT).show(); return }
        saveCurrentAnnotation()
        val file = File(getExternalFilesDir("Documents"), "annotated-${System.currentTimeMillis()}.pdf")
        val doc = PdfDocument()
        try {
            for (i in 0 until r.pageCount) {
                val bitmap = renderForExport(i)
                val info = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, i + 1).create()
                val page = doc.startPage(info)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                doc.finishPage(page)
                bitmap.recycle()
            }
            FileOutputStream(file).use { doc.writeTo(it) }
            Toast.makeText(this, "Saved ${file.name}", Toast.LENGTH_SHORT).show()
        } finally {
            doc.close()
        }
    }

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(::open)
    }

    private fun closeDocument() {
        renderer?.close()
        renderer = null
        fd?.close()
        fd = null
        annotations.values.forEach { runCatching { it.recycle() } }
        annotations.clear()
        displayBitmap?.let { runCatching { it.recycle() } }
        displayBitmap = null
    }

    override fun onDestroy() {
        closeDocument()
        super.onDestroy()
    }
}
