package com.xxyangyoulin.jbforum

import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

object MetaTubeApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun fetchCodeMetadata(server: String, token: String, code: String): LocalCodeMetadata? {
        return withContext(Dispatchers.IO) {
            val base = server.trim().trimEnd('/')
            if (base.isBlank()) return@withContext null
            val targetCode = normalizeCode(code)
            if (targetCode.isBlank()) return@withContext null

            val url = "$base/v1/movies/search?q=${URLEncoder.encode(targetCode, "UTF-8")}"
            val requestBuilder = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "JbForum/Android")
            if (token.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }

            val body = client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body?.string().orEmpty()
            }
            if (body.isBlank()) return@withContext null

            val root = JSONObject(body)
            val list = root.optJSONArray("data") ?: JSONArray()
            if (list.length() == 0) return@withContext null
            val picked = pickBestMatch(list, targetCode) ?: return@withContext null
            val actors = picked.optJSONArray("actors")?.toStringList().orEmpty()
            val number = picked.optString("number").ifBlank { targetCode }
            val provider = picked.optString("provider")
            val providerId = picked.optString("id")
            val rawCoverUrl = picked.optString("cover_url")
            val rawThumbUrl = picked.optString("thumb_url")
            LocalCodeMetadata(
                code = number,
                provider = provider,
                providerId = providerId,
                title = picked.optString("title"),
                coverUrl = buildProxyImageUrl(base, provider, providerId, rawCoverUrl, "primary")
                    ?: rawCoverUrl,
                backdropUrl = buildProxyImageUrl(base, provider, providerId, rawCoverUrl, "backdrop")
                    ?: rawCoverUrl,
                thumbUrl = buildProxyImageUrl(base, provider, providerId, rawThumbUrl, "thumb")
                    ?: rawThumbUrl,
                releaseDate = picked.optString("release_date"),
                actors = actors,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    fun resolveDisplayCoverUrl(
        server: String,
        provider: String,
        providerId: String,
        coverUrl: String,
        thumbUrl: String
    ): String {
        if (File(coverUrl).isAbsolute) return coverUrl
        if (File(thumbUrl).isAbsolute) return thumbUrl
        if (coverUrl.contains("/v1/images/") || thumbUrl.contains("/v1/images/")) {
            return coverUrl.ifBlank { thumbUrl }
        }
        val base = server.trim().trimEnd('/')
        return buildProxyImageUrl(base, provider, providerId, coverUrl, "primary")
            ?: buildProxyImageUrl(base, provider, providerId, thumbUrl, "thumb")
            ?: coverUrl.ifBlank { thumbUrl }
    }

    fun resolveDisplayBackdropUrl(
        server: String,
        provider: String,
        providerId: String,
        backdropUrl: String,
        coverUrl: String
    ): String {
        if (File(backdropUrl).isAbsolute) return backdropUrl
        if (File(coverUrl).isAbsolute) return coverUrl
        if (backdropUrl.contains("/v1/images/")) return backdropUrl
        val base = server.trim().trimEnd('/')
        return buildProxyImageUrl(base, provider, providerId, backdropUrl.ifBlank { coverUrl }, "backdrop")
            ?: backdropUrl.ifBlank { coverUrl }
    }

    private fun pickBestMatch(list: JSONArray, targetCode: String): JSONObject? {
        var fallback: JSONObject? = null
        for (i in 0 until list.length()) {
            val item = list.optJSONObject(i) ?: continue
            if (fallback == null) fallback = item
            val itemCode = normalizeCode(item.optString("number"))
            if (itemCode == targetCode) return item
        }
        return fallback
    }

    private fun normalizeCode(raw: String): String {
        return raw.trim()
            .uppercase()
            .replace('_', '-')
            .replace(' ', '-')
            .replace(Regex("-+"), "-")
    }

    private fun buildProxyImageUrl(
        baseServer: String,
        provider: String,
        providerId: String,
        sourceUrl: String,
        type: String
    ): String? {
        if (baseServer.isBlank() || provider.isBlank() || providerId.isBlank()) return null
        val encoded = URLEncoder.encode(sourceUrl, "UTF-8")
        return "$baseServer/v1/images/$type/$provider/$providerId?url=$encoded&auto=true&pos=1.0&quality=95"
    }

    private fun JSONArray.toStringList(): List<String> {
        val result = ArrayList<String>(length())
        for (i in 0 until length()) {
            val value = optString(i)
            if (value.isNotBlank()) result += value
        }
        return result
    }
}
