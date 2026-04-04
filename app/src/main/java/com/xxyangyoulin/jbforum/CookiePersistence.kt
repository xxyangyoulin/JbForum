package com.xxyangyoulin.jbforum

import android.content.Context
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl

object CookiePersistence {
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun load(): MutableList<Cookie> {
        val context = appContext ?: return mutableListOf()
        val saved = context.getSharedPreferences(AppConstants.PREFS_COOKIES, Context.MODE_PRIVATE)
            .getStringSet("cookies", emptySet())
            .orEmpty()
        val baseUrl = ForumDomainConfig.baseUrl()
        if (baseUrl.isBlank()) return mutableListOf()
        val url = baseUrl.toHttpUrl()
        return saved.mapNotNull { Cookie.parse(url, it) }.toMutableList()
    }

    fun save(cookies: List<Cookie>) {
        val context = appContext ?: return
        context.getSharedPreferences(AppConstants.PREFS_COOKIES, Context.MODE_PRIVATE)
            .edit()
            .putStringSet("cookies", cookies.map { it.toString() }.toSet())
            .apply()
    }

    fun clear() {
        val context = appContext ?: return
        context.getSharedPreferences(AppConstants.PREFS_COOKIES, Context.MODE_PRIVATE)
            .edit()
            .remove("cookies")
            .apply()
    }
}
