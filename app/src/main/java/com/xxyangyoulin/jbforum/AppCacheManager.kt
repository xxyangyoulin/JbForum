package com.xxyangyoulin.jbforum

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CacheStats(
    val totalBytes: Long,
    val favoriteBytes: Long,
    val codeMetadataBytes: Long = 0L
)

object AppCacheManager {
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    suspend fun stats(): CacheStats {
        val context = appContext ?: return CacheStats(0L, 0L)
        return withContext(Dispatchers.IO) {
            val favoriteDirs = listOf(
                File(context.filesDir, AppConstants.CACHE_DIR_FAVORITE_IMAGES),
                File(context.filesDir, AppConstants.CACHE_DIR_FAVORITE_THUMBNAILS)
            )
            val otherDirs = listOf(
                File(context.cacheDir, AppConstants.CACHE_DIR_COIL),
                File(context.cacheDir, AppConstants.CACHE_DIR_THREAD_ORIGINALS),
                File(context.cacheDir, AppConstants.CACHE_DIR_THREAD_THUMBNAILS),
                File(context.cacheDir, AppConstants.CACHE_DIR_THREAD_DETAILS)
            )
            val favoriteBytes = favoriteDirs.sumOf(::directorySize)
            val codeMetadataBytes = File(context.getDatabasePath("jb_forum.db").path).let { dbFile ->
                if (dbFile.exists()) dbFile.length() else 0L
            }
            CacheStats(
                totalBytes = favoriteBytes + otherDirs.sumOf(::directorySize) + codeMetadataBytes,
                favoriteBytes = favoriteBytes,
                codeMetadataBytes = codeMetadataBytes
            )
        }
    }

    suspend fun clearNonFavoriteCaches() {
        val context = appContext ?: return
        withContext(Dispatchers.IO) {
            listOf(
                File(context.filesDir, AppConstants.CACHE_DIR_FAVORITE_THUMBNAILS),
                File(context.cacheDir, AppConstants.CACHE_DIR_COIL),
                File(context.cacheDir, AppConstants.CACHE_DIR_THREAD_ORIGINALS),
                File(context.cacheDir, AppConstants.CACHE_DIR_THREAD_THUMBNAILS),
                File(context.cacheDir, AppConstants.CACHE_DIR_THREAD_DETAILS)
            ).forEach(::deleteRecursively)
        }
    }

    private fun directorySize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf(::directorySize) ?: 0L
    }

    private fun deleteRecursively(file: File) {
        if (!file.exists()) return
        if (file.isDirectory) {
            file.listFiles()?.forEach(::deleteRecursively)
        }
        file.delete()
    }
}
