package com.xxyangyoulin.jbforum

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

object LocalCodeMetadataStore {
    @Volatile
    private var dao: CodeMetadataDao? = null

    fun init(context: Context) {
        dao = AppDatabase.get(context.applicationContext).codeMetadataDao()
    }

    suspend fun loadByCodes(codes: Set<String>): Map<String, LocalCodeMetadata> {
        if (codes.isEmpty()) return emptyMap()
        val dbDao = dao ?: return emptyMap()
        return withContext(Dispatchers.IO) {
            dbDao.loadByCodes(codes.toList()).associate { entity ->
                entity.code to LocalCodeMetadata(
                    code = entity.code,
                    provider = entity.provider,
                    providerId = entity.providerId,
                    title = entity.title,
                    coverUrl = entity.coverUrl,
                    backdropUrl = entity.backdropUrl,
                    thumbUrl = entity.thumbUrl,
                    releaseDate = entity.releaseDate,
                    actors = entity.actorsCsv.split('|').filter { it.isNotBlank() },
                    updatedAt = entity.updatedAt
                )
            }
        }
    }

    fun upsert(item: LocalCodeMetadata) {
        val dbDao = dao ?: return
        runBlocking {
            withContext(Dispatchers.IO) {
                dbDao.upsert(
                    CodeMetadataEntity(
                        code = item.code,
                        provider = item.provider,
                        providerId = item.providerId,
                        title = item.title,
                        coverUrl = item.coverUrl,
                        backdropUrl = item.backdropUrl,
                        thumbUrl = item.thumbUrl,
                        releaseDate = item.releaseDate,
                        actorsCsv = item.actors.joinToString("|"),
                        updatedAt = item.updatedAt
                    )
                )
            }
        }
    }

    fun deleteByCodes(codes: Set<String>) {
        if (codes.isEmpty()) return
        val dbDao = dao ?: return
        runBlocking {
            withContext(Dispatchers.IO) {
                dbDao.deleteByCodes(codes.toList())
            }
        }
    }

    suspend fun deleteByCodesSuspend(codes: Set<String>) {
        if (codes.isEmpty()) return
        val dbDao = dao ?: return
        withContext(Dispatchers.IO) {
            dbDao.deleteByCodes(codes.toList())
        }
    }

    fun clearAll() {
        val dbDao = dao ?: return
        runBlocking {
            withContext(Dispatchers.IO) {
                dbDao.clearAll()
            }
        }
    }

    suspend fun clearAllSuspend() {
        val dbDao = dao ?: return
        withContext(Dispatchers.IO) {
            dbDao.clearAll()
        }
    }
}
