package com.xxyangyoulin.jbforum

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

data class ThreadHistoryItem(
    val id: String,
    val title: String,
    val url: String,
    val viewedAt: Long
)

object ThreadBrowseHistory {
    private const val keyItems = "items"
    private const val keyMigrated = "migrated_to_db"
    private const val maxItems = 50

    @Volatile
    private var appContext: Context? = null
    @Volatile
    private var dao: ThreadHistoryDao? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        dao = AppDatabase.get(context.applicationContext).threadHistoryDao()
        migrateFromPrefsIfNeeded(context.applicationContext)
    }

    fun load(): List<ThreadHistoryItem> {
        val dbDao = dao ?: return emptyList()
        return runBlocking {
            withContext(Dispatchers.IO) {
                dbDao.loadTop(maxItems).map {
                    ThreadHistoryItem(
                        id = it.id,
                        title = it.title,
                        url = it.url,
                        viewedAt = it.viewedAt
                    )
                }
            }
        }
    }

    fun add(title: String, url: String) {
        val normalizedUrl = url.trim()
        if (normalizedUrl.isBlank()) return
        val item = ThreadHistoryItem(
            id = normalizedUrl.substringAfter("tid=").substringBefore('&').ifBlank { normalizedUrl },
            title = title.ifBlank { "帖子详情" },
            url = normalizedUrl,
            viewedAt = System.currentTimeMillis()
        )
        val dbDao = dao ?: return
        runBlocking {
            withContext(Dispatchers.IO) {
                dbDao.upsert(
                    ThreadHistoryEntity(
                        url = item.url,
                        id = item.id,
                        title = item.title,
                        viewedAt = item.viewedAt
                    )
                )
                dbDao.trimTo(maxItems)
            }
        }
    }

    fun clear() {
        val dbDao = dao ?: return
        runBlocking {
            withContext(Dispatchers.IO) {
                dbDao.clearAll()
            }
        }
    }

    fun replaceAll(items: List<ThreadHistoryItem>) {
        val dbDao = dao ?: return
        runBlocking {
            withContext(Dispatchers.IO) {
                dbDao.clearAll()
                items.sortedByDescending { it.viewedAt }.take(maxItems).forEach { item ->
                    dbDao.upsert(
                        ThreadHistoryEntity(
                            url = item.url,
                            id = item.id,
                            title = item.title,
                            viewedAt = item.viewedAt
                        )
                    )
                }
                dbDao.trimTo(maxItems)
            }
        }
    }

    private fun decode(raw: String): ThreadHistoryItem? {
        val parts = raw.split("||")
        if (parts.size != 4) return null
        return ThreadHistoryItem(
            id = parts[0],
            title = parts[1],
            url = parts[2],
            viewedAt = parts[3].toLongOrNull() ?: return null
        )
    }

    private fun migrateFromPrefsIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_THREAD_HISTORY, Context.MODE_PRIVATE)
        if (prefs.getBoolean(keyMigrated, false)) return
        val rawItems = prefs.getStringSet(keyItems, emptySet()).orEmpty()
        if (rawItems.isNotEmpty()) {
            val dbDao = dao ?: return
            val parsed = rawItems.mapNotNull(::decode)
            runBlocking {
                withContext(Dispatchers.IO) {
                    parsed.forEach { item ->
                        dbDao.upsert(
                            ThreadHistoryEntity(
                                url = item.url,
                                id = item.id,
                                title = item.title,
                                viewedAt = item.viewedAt
                            )
                        )
                    }
                    dbDao.trimTo(maxItems)
                }
            }
        }
        prefs.edit().putBoolean(keyMigrated, true).remove(keyItems).apply()
    }
}
