package com.xxyangyoulin.jbforum

import android.content.Context

object LoginPersistence {
    private const val keyUsername = "username"
    private const val keyPassword = "password"

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun load(): Pair<String, String> {
        val context = appContext ?: return "" to ""
        val prefs = context.getSharedPreferences(AppConstants.PREFS_LOGIN, Context.MODE_PRIVATE)
        return prefs.getString(keyUsername, "").orEmpty() to prefs.getString(keyPassword, "").orEmpty()
    }

    fun save(username: String, password: String) {
        val context = appContext ?: return
        context.getSharedPreferences(AppConstants.PREFS_LOGIN, Context.MODE_PRIVATE)
            .edit()
            .putString(keyUsername, username)
            .putString(keyPassword, password)
            .apply()
    }
}
