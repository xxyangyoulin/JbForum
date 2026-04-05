package com.xxyangyoulin.jbforum

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class GitHubReleaseInfo(
    val tagName: String,
    val htmlUrl: String,
    val name: String
)

object GitHubUpdateChecker {
    private val client = OkHttpClient()
    private const val latestReleaseApi = "https://api.github.com/repos/xxyangyoulin/JbForum/releases/latest"
    private const val PREFS_NAME = "github_update_prefs"
    private const val KEY_LAST_CHECK_TIME = "last_check_time"
    private const val MIN_INTERVAL_MS = 20 * 60 * 60 * 1000L // 20 hours

    private lateinit var prefs: android.content.SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun shouldAutoCheck(): Boolean {
        if (!::prefs.isInitialized) return false
        val last = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
        return System.currentTimeMillis() - last >= MIN_INTERVAL_MS
    }

    fun recordCheckTime() {
        if (!::prefs.isInitialized) return
        prefs.edit().putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis()).apply()
    }

    suspend fun latestRelease(): GitHubReleaseInfo = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(latestReleaseApi)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()
        val jsonText = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("检查更新失败：${response.code}")
            response.body?.string().orEmpty().ifBlank { error("检查更新失败：响应为空") }
        }
        val json = JSONObject(jsonText)
        val tagName = json.optString("tag_name").trim()
        val htmlUrl = json.optString("html_url").trim()
        val name = json.optString("name").trim()
        if (tagName.isBlank() || htmlUrl.isBlank()) error("检查更新失败：发布信息不完整")
        GitHubReleaseInfo(
            tagName = tagName,
            htmlUrl = htmlUrl,
            name = name.ifBlank { tagName }
        )
    }

    fun hasNewVersion(currentVersion: String, latestTag: String): Boolean {
        return compareVersion(normalize(currentVersion), normalize(latestTag)) < 0
    }

    private fun normalize(value: String): String {
        return value.trim().removePrefix("v").removePrefix("V")
    }

    private fun compareVersion(a: String, b: String): Int {
        val av = a.split('.', '-', '_').map { it.toIntOrNull() ?: 0 }
        val bv = b.split('.', '-', '_').map { it.toIntOrNull() ?: 0 }
        val max = maxOf(av.size, bv.size)
        for (i in 0 until max) {
            val ai = av.getOrElse(i) { 0 }
            val bi = bv.getOrElse(i) { 0 }
            if (ai != bi) return ai.compareTo(bi)
        }
        return 0
    }
}
