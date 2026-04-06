package com.xxyangyoulin.jbforum

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

enum class CachedImagePhase {
    Loading,
    Compressing,
    Ready
}

object ThreadImageCache {
    private val cacheMutex = Mutex()
    private val failedRefs = mutableSetOf<String>()
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    suspend fun ensureCached(client: OkHttpClient, imageRef: String): String? {
        val context = appContext ?: return null
        return withContext(Dispatchers.IO) {
            val originalFile = originalFile(context, imageRef)
            if (imageRef.startsWith("http") && !originalFile.exists()) {
                cacheMutex.withLock {
                    if (!originalFile.exists()) {
                        val bytes = client.newCall(
                            Request.Builder()
                                .url(imageRef)
                                .header("Referer", ForumDomainConfig.requireBaseUrl())
                                .build()
                        ).execute().use { response ->
                            if (!response.isSuccessful) error("下载图片失败: ${response.code}")
                            response.body?.bytes() ?: error("图片内容为空")
                        }
                        originalFile.parentFile?.mkdirs()
                        originalFile.writeBytes(bytes)
                    }
                }
            }

            val sourceFile = if (imageRef.startsWith("http")) originalFile else File(imageRef)
            if (!sourceFile.exists()) return@withContext null

            if (shouldCompressStaticImage(sourceFile, imageRef)) {
                val thumbnailFile = thumbnailFile(context, imageRef)
                if (!thumbnailFile.exists()) {
                    cacheMutex.withLock {
                        if (!thumbnailFile.exists()) {
                            thumbnailFile.parentFile?.mkdirs()
                            createThumbnail(sourceFile, thumbnailFile)
                        }
                    }
                }
                return@withContext thumbnailFile.takeIf { it.exists() }?.absolutePath ?: sourceFile.absolutePath
            }
            sourceFile.absolutePath
        }
    }

    fun previewRef(imageRef: String): String {
        val context = appContext ?: return imageRef
        val originalFile = originalFile(context, imageRef)
        return if (imageRef.startsWith("http") && originalFile.exists()) {
            originalFile.absolutePath
        } else {
            imageRef
        }
    }

    fun thumbnailRef(imageRef: String): String? {
        val context = appContext ?: return null
        val originalFile = originalFile(context, imageRef)
        if (originalFile.exists() && !shouldCompressStaticImage(originalFile, imageRef)) {
            return originalFile.absolutePath
        }
        val thumbnailFile = thumbnailFile(context, imageRef)
        return thumbnailFile.takeIf { it.exists() }?.absolutePath
    }

    fun phase(imageRef: String): CachedImagePhase {
        val context = appContext ?: return CachedImagePhase.Loading
        val originalFile = originalFile(context, imageRef)
        if (!originalFile.exists()) return CachedImagePhase.Loading
        if (!shouldCompressStaticImage(originalFile, imageRef)) return CachedImagePhase.Ready
        val thumbnailFile = thumbnailFile(context, imageRef)
        return if (thumbnailFile.exists()) CachedImagePhase.Ready else CachedImagePhase.Compressing
    }

    fun clearCached(imageRef: String) {
        val context = appContext ?: return
        originalFile(context, imageRef).takeIf { it.exists() }?.delete()
        thumbnailFile(context, imageRef).takeIf { it.exists() }?.delete()
    }

    fun markFailed(imageRef: String) {
        synchronized(failedRefs) {
            failedRefs += imageRef
        }
    }

    fun clearFailed(imageRef: String) {
        synchronized(failedRefs) {
            failedRefs -= imageRef
        }
    }

    fun isFailed(imageRef: String): Boolean {
        return synchronized(failedRefs) {
            imageRef in failedRefs
        }
    }

    private fun originalFile(context: Context, imageRef: String): File {
        val extension = extensionFor(imageRef)
        return File(context.cacheDir, "${AppConstants.CACHE_DIR_THREAD_ORIGINALS}/${sha1(imageRef)}.$extension")
    }

    private fun thumbnailFile(context: Context, imageRef: String): File {
        return File(context.cacheDir, "${AppConstants.CACHE_DIR_THREAD_THUMBNAILS}/${sha1(imageRef)}.jpg")
    }

    private fun createThumbnail(sourceFile: File, target: File) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(sourceFile.absolutePath, bounds)
        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, AppConstants.THUMBNAIL_SIZE_THREAD)
        val bitmap = BitmapFactory.decodeFile(
            sourceFile.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
        ) ?: return
        target.outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, AppConstants.THUMBNAIL_JPEG_QUALITY_THREAD, stream)
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

    private fun shouldCompressStaticImage(sourceFile: File, imageRef: String): Boolean {
        if (isGifRef(imageRef)) return false
        return sourceFile.length() > AppConstants.IMAGE_COMPRESS_THRESHOLD_BYTES
    }

    private fun isGifRef(imageRef: String): Boolean {
        return extensionFor(imageRef) == "gif"
    }

    private fun sha1(value: String): String {
        return MessageDigest.getInstance("SHA-1")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
