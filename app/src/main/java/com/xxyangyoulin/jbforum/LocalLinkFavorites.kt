package com.xxyangyoulin.jbforum

import android.content.Context
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

object LocalLinkFavorites {
    private const val keyItems = "items"
    private const val keyMigrated = "migrated_to_db"

    @Volatile
    private var appContext: Context? = null
    @Volatile
    private var dao: LinkFavoriteDao? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        dao = AppDatabase.get(context.applicationContext).linkFavoriteDao()
        migrateFromPrefsIfNeeded(context.applicationContext)
    }

    fun load(): List<LocalFavoriteLink> {
        val dbDao = dao ?: return emptyList()
        return runBlocking {
            withContext(Dispatchers.IO) {
                dbDao.loadAll().map {
                    LocalFavoriteLink(
                        id = it.id,
                        value = it.value,
                        type = it.type,
                        savedAt = it.savedAt,
                        sourceThreadTitle = it.sourceThreadTitle,
                        sourceThreadUrl = it.sourceThreadUrl
                    )
                }
            }
        }
    }

    fun exists(value: String): Boolean {
        val id = sha1(value.lowercase())
        return load().any { it.id == id }
    }

    fun add(
        value: String,
        type: String,
        sourceThreadTitle: String = "",
        sourceThreadUrl: String = ""
    ): LocalFavoriteLink {
        val normalized = value.trim()
        require(normalized.isNotBlank()) { "链接为空" }
        val id = sha1(normalized.lowercase())
        val item = LocalFavoriteLink(
            id = id,
            value = normalized,
            type = type,
            savedAt = System.currentTimeMillis(),
            sourceThreadTitle = sourceThreadTitle,
            sourceThreadUrl = sourceThreadUrl
        )
        val dbDao = dao ?: return item
        runBlocking {
            withContext(Dispatchers.IO) {
                dbDao.upsert(
                    LinkFavoriteEntity(
                        id = item.id,
                        value = item.value,
                        type = item.type,
                        savedAt = item.savedAt,
                        sourceThreadTitle = item.sourceThreadTitle,
                        sourceThreadUrl = item.sourceThreadUrl
                    )
                )
            }
        }
        return item
    }

    fun remove(ids: Set<String>) {
        if (ids.isEmpty()) return
        val dbDao = dao ?: return
        runBlocking {
            withContext(Dispatchers.IO) {
                dbDao.deleteByIds(ids.toList())
            }
        }
    }

    fun replaceAll(items: List<LocalFavoriteLink>) {
        val dbDao = dao ?: return
        runBlocking {
            withContext(Dispatchers.IO) {
                dbDao.clearAll()
                items.forEach { item ->
                    dbDao.upsert(
                        LinkFavoriteEntity(
                            id = item.id,
                            value = item.value,
                            type = item.type,
                            savedAt = item.savedAt,
                            sourceThreadTitle = item.sourceThreadTitle,
                            sourceThreadUrl = item.sourceThreadUrl
                        )
                    )
                }
            }
        }
    }

    private fun decode(raw: String): LocalFavoriteLink? {
        val parts = raw.split("||")
        if (parts.size != 6) return null
        return LocalFavoriteLink(
            id = parts[0],
            value = parts[1],
            type = parts[2],
            savedAt = parts[3].toLongOrNull() ?: return null,
            sourceThreadTitle = parts[4],
            sourceThreadUrl = parts[5]
        )
    }

    private fun migrateFromPrefsIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_LOCAL_LINK_FAVORITES, Context.MODE_PRIVATE)
        if (prefs.getBoolean(keyMigrated, false)) return
        val rawItems = prefs.getStringSet(keyItems, emptySet()).orEmpty()
        if (rawItems.isNotEmpty()) {
            val dbDao = dao ?: return
            val parsed = rawItems.mapNotNull(::decode)
            runBlocking {
                withContext(Dispatchers.IO) {
                    parsed.forEach { item ->
                        dbDao.upsert(
                            LinkFavoriteEntity(
                                id = item.id,
                                value = item.value,
                                type = item.type,
                                savedAt = item.savedAt,
                                sourceThreadTitle = item.sourceThreadTitle,
                                sourceThreadUrl = item.sourceThreadUrl
                            )
                        )
                    }
                }
            }
        }
        prefs.edit().putBoolean(keyMigrated, true).remove(keyItems).apply()
    }

    private fun sha1(value: String): String {
        return MessageDigest.getInstance("SHA-1")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
