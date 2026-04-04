package com.xxyangyoulin.jbforum

import android.content.Context
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object ForumDomainConfig {
    private const val keyForumDomain = "forum_domain"
    private const val keyOpenThreadInWeb = "open_thread_in_web"

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun getDomain(): String {
        val context = appContext ?: return ""
        return context.getSharedPreferences(AppConstants.PREFS_SETTINGS, Context.MODE_PRIVATE)
            .getString(keyForumDomain, "")
            .orEmpty()
            .trim()
    }

    fun setDomain(input: String) {
        val context = appContext ?: return
        val normalized = normalizeDomain(input)
        context.getSharedPreferences(AppConstants.PREFS_SETTINGS, Context.MODE_PRIVATE)
            .edit()
            .putString(keyForumDomain, normalized)
            .apply()
    }

    fun openThreadInWebDefault(): Boolean {
        val context = appContext ?: return false
        return context.getSharedPreferences(AppConstants.PREFS_SETTINGS, Context.MODE_PRIVATE)
            .getBoolean(keyOpenThreadInWeb, false)
    }

    fun setOpenThreadInWebDefault(enabled: Boolean) {
        val context = appContext ?: return
        context.getSharedPreferences(AppConstants.PREFS_SETTINGS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(keyOpenThreadInWeb, enabled)
            .apply()
    }

    fun baseUrl(): String {
        val domain = getDomain()
        if (domain.isBlank()) return ""
        return "https://$domain/forum/"
    }

    fun requireBaseUrl(): String {
        return baseUrl().ifBlank { error("请先在设置中填写论坛域名") }
    }

    fun forumHost(): String {
        return requireBaseUrl().toHttpUrlOrNull()?.host.orEmpty()
    }

    fun forumCookieDomain(): String {
        val host = forumHost()
        return if (host.startsWith(".")) host else ".$host"
    }

    fun isForumDomain(domain: String): Boolean {
        val host = forumHost()
        val normalized = domain.trim().removePrefix(".").lowercase()
        val normalizedHost = host.lowercase()
        return normalized == normalizedHost || normalized.endsWith(".$normalizedHost")
    }

    private fun normalizeDomain(input: String): String {
        val raw = input.trim()
        if (raw.isBlank()) return ""
        val withScheme = if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) raw else "https://$raw"
        val parsed = withScheme.toHttpUrlOrNull() ?: error("域名格式不正确")
        return parsed.host
    }
}
