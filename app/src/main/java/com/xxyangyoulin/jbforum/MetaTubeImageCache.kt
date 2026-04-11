package com.xxyangyoulin.jbforum

import android.content.Context
import java.io.File
import java.net.URI
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object MetaTubeImageCache {
    @Volatile
    private var appContext: Context? = null

    private val client = OkHttpClient()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun resolve(url: String): String {
        if (url.isBlank()) return url
        if (File(url).isAbsolute && File(url).exists()) return url
        val file = cachedFile(url)
        return file?.takeIf { it.exists() }?.absolutePath ?: url
    }

    suspend fun cache(url: String, token: String): String {
        if (url.isBlank()) return url
        if (File(url).isAbsolute) return url
        val target = cachedFile(url) ?: return url
        if (!target.exists() || target.length() <= 0L) {
            withContext(Dispatchers.IO) {
                val tmp = File(target.absolutePath + ".tmp")
                runCatching {
                    target.parentFile?.mkdirs()
                    val requestBuilder = Request.Builder().url(url)
                    if (token.isNotBlank()) {
                        requestBuilder.header("Authorization", "Bearer $token")
                    }
                    client.newCall(requestBuilder.build()).execute().use { response ->
                        if (!response.isSuccessful) return@use
                        val body = response.body ?: return@use
                        tmp.outputStream().use { output ->
                            body.byteStream().copyTo(output)
                        }
                        if (tmp.exists() && tmp.length() > 0L) {
                            tmp.renameTo(target)
                        }
                    }
                }.onFailure {
                    tmp.delete()
                }
            }
        }
        return target.takeIf { it.exists() && it.length() > 0L }?.absolutePath ?: url
    }

    suspend fun prefetch(url: String, token: String) {
        cache(url, token)
    }

    suspend fun prefetch(urls: List<String>, token: String) {
        urls.filter { it.isNotBlank() }.distinct().forEach { url ->
            cache(url, token)
        }
    }

    private fun cachedFile(url: String): File? {
        val context = appContext ?: return null
        val dir = File(context.cacheDir, AppConstants.CACHE_DIR_METATUBE_IMAGES).apply { mkdirs() }
        val path = runCatching { URI(url).path.orEmpty() }.getOrDefault(url.substringBefore('?'))
        val fileName = path.substringAfterLast('/').substringBefore('?')
        val extension = fileName.substringAfterLast('.', "jpg")
            .lowercase()
            .takeIf { it.matches(Regex("[a-z0-9]{1,5}")) }
            ?: "jpg"
        return File(dir, "${sha1(url)}.$extension")
    }

    private fun sha1(value: String): String {
        return MessageDigest.getInstance("SHA-1")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
