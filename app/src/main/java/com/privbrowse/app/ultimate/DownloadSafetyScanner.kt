package com.privbrowse.app.ultimate

import java.io.File
import java.io.RandomAccessFile

/** Small offline heuristic scanner: no cloud upload and no claim of AV-grade detection. */
object DownloadSafetyScanner {
    data class Result(val suspicious: Boolean, val reason: String)

    fun inspect(file: File, mime: String): Result {
        val name = file.name.lowercase()
        val extRisk = listOf(".apk", ".aab", ".exe", ".msi", ".scr", ".bat", ".cmd", ".com", ".ps1", ".jar", ".sh").firstOrNull { name.endsWith(it) }
        if (extRisk != null) return Result(true, "Executable/script extension $extRisk")
        if (!file.exists() || file.length() == 0L) return Result(false, "No local sample")
        val header = ByteArray(8)
        RandomAccessFile(file, "r").use { it.read(header) }
        if (header.size >= 4 && header[0] == 0x7f && header[1] == 'E'.code.toByte() && header[2] == 'L'.code.toByte() && header[3] == 'F'.code.toByte()) {
            return Result(true, "ELF executable signature")
        }
        if (header.size >= 2 && header[0] == 'M'.code.toByte() && header[1] == 'Z'.code.toByte()) {
            return Result(true, "Windows executable signature")
        }
        if (mime.contains("javascript", true) || mime.contains("x-sh", true)) return Result(true, "Script MIME type")
        return Result(false, "No obvious executable signature")
    }
}
