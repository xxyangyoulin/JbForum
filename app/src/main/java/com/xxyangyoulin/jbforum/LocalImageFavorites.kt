package com.xxyangyoulin.jbforum

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object LocalImageFavorites {
    private const val keyItems = "items"
    private val thumbnailMutex = Mutex()

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun load(): List<LocalFavoriteImage> {
        val context = appContext ?: return emptyList()
        val prefs = context.getSharedPreferences(AppConstants.PREFS_LOCAL_FAVORITES, Context.MODE_PRIVATE)
        return prefs.getStringSet(keyItems, emptySet())
            .orEmpty()
            .mapNotNull(::decode)
            .filter { File(it.filePath).exists() }
            .sortedByDescending { it.savedAt }
    }

    suspend fun add(
        context: Context,
        client: OkHttpClient,
        imageRef: String,
        sourceThreadTitle: String = "",
        sourceThreadUrl: String = ""
    ): LocalFavoriteImage {
        return withContext(Dispatchers.IO) {
            val existing = load().firstOrNull { it.originalUrl == imageRef || it.filePath == imageRef }
            if (existing != null) return@withContext existing

            val favoritesDir = File(context.filesDir, AppConstants.CACHE_DIR_FAVORITE_IMAGES).apply { mkdirs() }
            val thumbnailsDir = File(context.filesDir, AppConstants.CACHE_DIR_FAVORITE_THUMBNAILS).apply { mkdirs() }
            val bytes = if (imageRef.startsWith("http")) {
                client.newCall(
                    Request.Builder()
                        .url(imageRef)
                        .header("Referer", ForumDomainConfig.requireBaseUrl())
                        .build()
                ).execute().use { response ->
                    if (!response.isSuccessful) error("下载图片失败: ${response.code}")
                    response.body?.bytes() ?: error("图片内容为空")
                }
            } else {
                File(imageRef).takeIf { it.exists() }?.readBytes() ?: error("图片文件不存在")
            }
            val extension = extensionFor(imageRef)
            val id = sha1(imageRef)
            val target = File(favoritesDir, "$id.$extension")
            if (!target.exists()) {
                target.writeBytes(bytes)
            }
            val thumbnailTarget = when {
                shouldCompressStaticImage(target) -> staticThumbnailFile(thumbnailsDir, id).also { displayFile ->
                    if (!displayFile.exists()) createThumbnail(bytes, displayFile)
                }
                else -> target
            }
            val item = LocalFavoriteImage(
                id = id,
                filePath = target.absolutePath,
                thumbnailPath = thumbnailTarget.absolutePath,
                originalUrl = imageRef,
                savedAt = System.currentTimeMillis(),
                sourceThreadTitle = sourceThreadTitle,
                sourceThreadUrl = sourceThreadUrl
            )
            save(load().filterNot { it.id == item.id } + item)
            Log.d(AppConstants.LOG_TAG, "local favorite saved path=${target.absolutePath}")
            item
        }
    }

    suspend fun remove(ids: Set<String>) {
        withContext(Dispatchers.IO) {
            if (ids.isEmpty()) return@withContext
            val keptItems = load().filterNot { it.id in ids }
            load().filter { it.id in ids }.forEach { item ->
                File(item.filePath).takeIf { it.exists() }?.delete()
                File(item.thumbnailPath).takeIf { it.exists() }?.delete()
                legacyAnimatedDisplayFile(item.id).takeIf { it.exists() }?.delete()
                staticThumbnailFile(item.id).takeIf { it.exists() }?.delete()
            }
            save(keptItems)
        }
    }

    suspend fun clearAll() {
        remove(load().map { it.id }.toSet())
        save(emptyList())
    }

    fun replaceAllFromBackup(items: List<LocalFavoriteImage>) {
        save(items)
    }

    suspend fun ensureThumbnail(item: LocalFavoriteImage): String? {
        return withContext(Dispatchers.IO) {
            val sourceFile = File(item.filePath)
            if (!sourceFile.exists()) return@withContext null
            val displayFile = when {
                shouldCompressStaticImage(sourceFile) -> staticThumbnailFile(item.id)
                else -> return@withContext sourceFile.absolutePath
            }
            if (!displayFile.exists()) {
                thumbnailMutex.withLock {
                    if (!displayFile.exists()) {
                        displayFile.parentFile?.mkdirs()
                        createThumbnail(sourceFile, displayFile)
                    }
                }
            }
            displayFile.takeIf { it.exists() }?.absolutePath
        }
    }

    fun thumbnailOrOriginal(item: LocalFavoriteImage): String? {
        val sourceFile = File(item.filePath)
        if (!sourceFile.exists()) return null
        if (!shouldCompressStaticImage(sourceFile)) return sourceFile.absolutePath
        return staticThumbnailFile(item.id).takeIf { it.exists() }?.absolutePath
    }

    fun phase(item: LocalFavoriteImage): CachedImagePhase {
        val sourceFile = File(item.filePath)
        if (!sourceFile.exists()) return CachedImagePhase.Loading
        if (!shouldCompressStaticImage(sourceFile)) return CachedImagePhase.Ready
        return if (staticThumbnailFile(item.id).exists()) CachedImagePhase.Ready else CachedImagePhase.Compressing
    }

    private fun save(items: List<LocalFavoriteImage>) {
        val context = appContext ?: return
        context.getSharedPreferences(AppConstants.PREFS_LOCAL_FAVORITES, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(keyItems, items.map(::encode).toSet())
            .apply()
    }

    private fun encode(item: LocalFavoriteImage): String {
        return listOf(
            item.id,
            item.filePath,
            item.thumbnailPath,
            item.originalUrl,
            item.savedAt.toString(),
            item.sourceThreadTitle,
            item.sourceThreadUrl
        ).joinToString("||")
    }

    private fun decode(raw: String): LocalFavoriteImage? {
        val parts = raw.split("||")
        if (parts.size != 4 && parts.size != 6 && parts.size != 7) return null
        val filePath = parts[1]
        val thumbnailPath = when (parts.size) {
            7 -> parts[2]
            else -> filePath
        }
        val originalUrlIndex = if (parts.size == 7) 3 else 2
        val savedAtIndex = if (parts.size == 7) 4 else 3
        val sourceTitleIndex = if (parts.size == 7) 5 else 4
        val sourceUrlIndex = if (parts.size == 7) 6 else 5
        return LocalFavoriteImage(
            id = parts[0],
            filePath = filePath,
            thumbnailPath = thumbnailPath,
            originalUrl = parts[originalUrlIndex],
            savedAt = parts[savedAtIndex].toLongOrNull() ?: return null,
            sourceThreadTitle = parts.getOrNull(sourceTitleIndex).orEmpty(),
            sourceThreadUrl = parts.getOrNull(sourceUrlIndex).orEmpty()
        )
    }

    private fun createThumbnail(bytes: ByteArray, target: File) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, AppConstants.THUMBNAIL_SIZE_FAVORITES)
        val bitmap = BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
        ) ?: return
        target.outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, AppConstants.THUMBNAIL_JPEG_QUALITY_FAVORITES, stream)
        }
        bitmap.recycle()
    }

    private fun createThumbnail(sourceFile: File, target: File) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(sourceFile.absolutePath, bounds)
        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, AppConstants.THUMBNAIL_SIZE_FAVORITES)
        val bitmap = BitmapFactory.decodeFile(
            sourceFile.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
        ) ?: return
        target.outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, AppConstants.THUMBNAIL_JPEG_QUALITY_FAVORITES, stream)
        }
        bitmap.recycle()
    }

    private fun calculateInSampleSize(width: Int, height: Int, targetSize: Int): Int {
        var sampleSize = 1
        var currentWidth = width
        var currentHeight = height
        while (currentWidth > targetSize * 2 || currentHeight > targetSize * 2) {
            sampleSize *= 2
            currentWidth /= 2
            currentHeight /= 2
        }
        return sampleSize.coerceAtLeast(1)
    }

    private fun extensionFor(imageRef: String): String {
        val normalized = imageRef.substringBefore('?').lowercase()
        return when {
            normalized.endsWith(".gif") -> "gif"
            normalized.endsWith(".webp") -> "webp"
            normalized.endsWith(".png") -> "png"
            else -> "jpg"
        }
    }

    private fun legacyAnimatedDisplayFile(id: String): File {
        val context = appContext ?: error("LocalImageFavorites not initialized")
        return File(File(context.filesDir, AppConstants.CACHE_DIR_FAVORITE_THUMBNAILS), "$id.mp4")
    }

    private fun staticThumbnailFile(id: String): File {
        val context = appContext ?: error("LocalImageFavorites not initialized")
        return staticThumbnailFile(File(context.filesDir, AppConstants.CACHE_DIR_FAVORITE_THUMBNAILS), id)
    }

    private fun staticThumbnailFile(baseDir: File, id: String): File {
        return File(baseDir, "$id.jpg")
    }

    private fun shouldCompressStaticImage(sourceFile: File): Boolean {
        if (extensionFor(sourceFile.absolutePath) == "gif") return false
        return sourceFile.length() > AppConstants.IMAGE_COMPRESS_THRESHOLD_BYTES
    }

    private fun sha1(value: String): String {
        return MessageDigest.getInstance("SHA-1")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
