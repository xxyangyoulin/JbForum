package com.xxyangyoulin.jbforum

import android.content.Context

object MetaTubeConfig {
    private const val KEY_SERVER = "server"
    private const val KEY_TOKEN = "token"

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun getServer(): String {
        val context = appContext ?: return ""
        return context.getSharedPreferences(AppConstants.PREFS_METATUBE, Context.MODE_PRIVATE)
            .getString(KEY_SERVER, "")
            .orEmpty()
            .trim()
    }

    fun getToken(): String {
        val context = appContext ?: return ""
        return context.getSharedPreferences(AppConstants.PREFS_METATUBE, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, "")
            .orEmpty()
            .trim()
    }

    fun save(server: String, token: String) {
        val context = appContext ?: return
        context.getSharedPreferences(AppConstants.PREFS_METATUBE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SERVER, server.trim())
            .putString(KEY_TOKEN, token.trim())
            .apply()
    }

    fun isConfigured(): Boolean = getServer().isNotBlank()
}
