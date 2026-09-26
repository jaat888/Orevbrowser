package com.privbrowse.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class LinkItem(val id: Long, val title: String, val url: String, val timestamp: Long)

/** Phase 3 — one row of the Network Transparency Log. */
data class NetworkLogEntry(
    val id: Long,
    val pageOrigin: String,
    val host: String,
    val blocked: Boolean,
    val category: String?, // "tracker" | "fingerprint" | null (allowed third-party contact)
    val timestamp: Long
)

/** Phase 3 — totals used by the Weekly Privacy Report notification. */
data class WeeklyStats(val trackersBlocked: Int, val fingerprintBlocked: Int)

/**
 * Local-only SQLite store for bookmarks, history, and (Phase 3) the
 * network transparency log.
 * Zero-Cloud Guarantee: nothing here ever leaves the device.
 */
class DbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "privbrowse.db"
        private const val DB_VERSION = 3

        const val TABLE_BOOKMARKS = "bookmarks"
        const val TABLE_HISTORY = "history"
        const val TABLE_NETWORK_LOG = "network_log"
        const val TABLE_READING_LIST = "reading_list"

        private const val CREATE_READING_LIST_SQL =
            "CREATE TABLE IF NOT EXISTS $TABLE_READING_LIST (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT, " +
                "url TEXT NOT NULL, " +
                "timestamp INTEGER NOT NULL)"

        private const val CREATE_NETWORK_LOG_SQL =
            "CREATE TABLE IF NOT EXISTS $TABLE_NETWORK_LOG (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "page_origin TEXT NOT NULL, " +
                "host TEXT NOT NULL, " +
                "blocked INTEGER NOT NULL, " +
                "category TEXT, " +
                "timestamp INTEGER NOT NULL)"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_BOOKMARKS (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT, " +
                "url TEXT NOT NULL, " +
                "timestamp INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE $TABLE_HISTORY (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT, " +
                "url TEXT NOT NULL, " +
                "timestamp INTEGER NOT NULL)"
        )
        db.execSQL(CREATE_NETWORK_LOG_SQL)
        db.execSQL(CREATE_READING_LIST_SQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Additive only — earlier versions dropped bookmarks/history on any
        // bump, which would silently wipe user data the moment Phase 3's
        // schema change shipped. Existing tables are left alone; only the
        // new Phase 3 table is added for anyone upgrading from version 1.
        if (oldVersion < 2) {
            db.execSQL(CREATE_NETWORK_LOG_SQL)
        }
        if (oldVersion < 3) {
            db.execSQL(CREATE_READING_LIST_SQL)
        }
    }

    fun addBookmark(title: String, url: String) {
        val values = ContentValues().apply {
            put("title", title)
            put("url", url)
            put("timestamp", System.currentTimeMillis())
        }
        writableDatabase.insert(TABLE_BOOKMARKS, null, values)
    }

    fun isBookmarked(url: String): Boolean {
        readableDatabase.query(
            TABLE_BOOKMARKS, arrayOf("id"), "url = ?", arrayOf(url), null, null, null
        ).use { c -> return c.count > 0 }
    }

    fun removeBookmark(id: Long) {
        writableDatabase.delete(TABLE_BOOKMARKS, "id = ?", arrayOf(id.toString()))
    }

    fun getBookmarks(): List<LinkItem> = queryAll(TABLE_BOOKMARKS)

    fun addHistoryEntry(title: String, url: String) {
        val values = ContentValues().apply {
            put("title", title)
            put("url", url)
            put("timestamp", System.currentTimeMillis())
        }
        writableDatabase.insert(TABLE_HISTORY, null, values)
    }

    fun getHistory(): List<LinkItem> = queryAll(TABLE_HISTORY)

    fun clearHistory() {
        writableDatabase.delete(TABLE_HISTORY, null, null)
    }

    fun addReadingList(title: String, url: String) {
        if (url.isBlank()) return
        val values = ContentValues().apply {
            put("title", title)
            put("url", url)
            put("timestamp", System.currentTimeMillis())
        }
        writableDatabase.insert(TABLE_READING_LIST, null, values)
    }

    fun getReadingList(): List<LinkItem> = queryAll(TABLE_READING_LIST)

    fun removeReadingList(id: Long) {
        writableDatabase.delete(TABLE_READING_LIST, "id = ?", arrayOf(id.toString()))
    }

    fun clearReadingList() {
        writableDatabase.delete(TABLE_READING_LIST, null, null)
    }

    private fun queryAll(table: String): List<LinkItem> {
        val list = ArrayList<LinkItem>()
        readableDatabase.query(
            table,
            arrayOf("id", "title", "url", "timestamp"),
            null, null, null, null,
            "timestamp DESC"
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    LinkItem(
                        id = c.getLong(0),
                        title = c.getString(1) ?: "",
                        url = c.getString(2),
                        timestamp = c.getLong(3)
                    )
                )
            }
        }
        return list
    }

    // ---------- Phase 3: Network Transparency Log ----------

    /** Records one contacted host — blocked (tracker/fingerprint) or allowed third-party. */
    fun logNetworkEvent(pageOrigin: String, host: String, blocked: Boolean, category: String?) {
        val values = ContentValues().apply {
            put("page_origin", pageOrigin)
            put("host", host)
            put("blocked", if (blocked) 1 else 0)
            put("category", category)
            put("timestamp", System.currentTimeMillis())
        }
        writableDatabase.insert(TABLE_NETWORK_LOG, null, values)
    }

    /** Newest-first log, optionally filtered to one page's origin (e.g. "https://example.com"). */
    fun getNetworkLog(originFilter: String? = null, limit: Int = 300): List<NetworkLogEntry> {
        val list = ArrayList<NetworkLogEntry>()
        val selection = if (originFilter != null) "page_origin = ?" else null
        val selectionArgs = if (originFilter != null) arrayOf(originFilter) else null
        readableDatabase.query(
            TABLE_NETWORK_LOG,
            arrayOf("id", "page_origin", "host", "blocked", "category", "timestamp"),
            selection, selectionArgs, null, null,
            "timestamp DESC",
            limit.toString()
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    NetworkLogEntry(
                        id = c.getLong(0),
                        pageOrigin = c.getString(1),
                        host = c.getString(2),
                        blocked = c.getInt(3) == 1,
                        category = c.getString(4),
                        timestamp = c.getLong(5)
                    )
                )
            }
        }
        return list
    }

    fun clearNetworkLog() {
        writableDatabase.delete(TABLE_NETWORK_LOG, null, null)
    }

    /** Deletes log entries older than [maxAgeMillis] so the on-device log doesn't grow forever. */
    fun pruneNetworkLog(maxAgeMillis: Long) {
        val cutoff = System.currentTimeMillis() - maxAgeMillis
        writableDatabase.delete(TABLE_NETWORK_LOG, "timestamp < ?", arrayOf(cutoff.toString()))
    }

    /** Blocked-event totals since [sinceMillis], split tracker vs fingerprinting — for the Weekly Privacy Report. */
    fun getBlockedStatsSince(sinceMillis: Long): WeeklyStats {
        var trackers = 0
        var fingerprint = 0
        readableDatabase.query(
            TABLE_NETWORK_LOG,
            arrayOf("category", "COUNT(*)"),
            "blocked = 1 AND timestamp >= ?",
            arrayOf(sinceMillis.toString()),
            "category", null, null
        ).use { c ->
            while (c.moveToNext()) {
                val category = c.getString(0)
                val count = c.getInt(1)
                if (category == "fingerprint") fingerprint += count else trackers += count
            }
        }
        return WeeklyStats(trackers, fingerprint)
    }
}
