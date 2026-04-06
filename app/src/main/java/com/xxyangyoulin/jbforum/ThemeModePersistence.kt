package com.xxyangyoulin.jbforum

import android.content.Context
import android.app.UiModeManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode(val value: String, val label: String) {
    System("system", "跟随系统"),
    Light("light", "浅色模式"),
    Dark("dark", "深色模式");

    companion object {
        fun fromValue(value: String?): ThemeMode {
            return entries.firstOrNull { it.value == value } ?: System
        }
    }
}

object ThemeModePersistence {
    private const val keyThemeMode = "theme_mode"

    @Volatile
    private var appContext: Context? = null

    var mode by mutableStateOf(ThemeMode.System)
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = appContext!!.getSharedPreferences(AppConstants.PREFS_SETTINGS, Context.MODE_PRIVATE)
        mode = ThemeMode.fromValue(prefs.getString(keyThemeMode, ThemeMode.System.value))
        applyNightMode(mode)
    }

    fun updateMode(value: ThemeMode) {
        mode = value
        val context = appContext ?: return
        context.getSharedPreferences(AppConstants.PREFS_SETTINGS, Context.MODE_PRIVATE)
            .edit()
            .putString(keyThemeMode, value.value)
            .apply()
        applyNightMode(value)
    }

    private fun applyNightMode(mode: ThemeMode) {
        val context = appContext ?: return
        val uiModeManager = context.getSystemService(UiModeManager::class.java) ?: return
        uiModeManager.setApplicationNightMode(
            when (mode) {
                ThemeMode.System -> UiModeManager.MODE_NIGHT_AUTO
                ThemeMode.Light -> UiModeManager.MODE_NIGHT_NO
                ThemeMode.Dark -> UiModeManager.MODE_NIGHT_YES
            }
        )
    }
}
