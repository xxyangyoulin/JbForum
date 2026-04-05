package com.xxyangyoulin.jbforum

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object BoardDiskCache {
    private const val keyBoards = "boards"

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun load(): List<Board> {
        val context = appContext ?: return emptyList()
        val raw = context.getSharedPreferences(AppConstants.PREFS_SETTINGS, Context.MODE_PRIVATE)
            .getString(keyBoards, "")
            .orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val url = item.optString("url")
                    if (url.isBlank()) continue
                    add(
                        Board(
                            title = item.optString("title"),
                            description = item.optString("description"),
                            url = url,
                            latestThreadTitle = item.optString("latestThreadTitle"),
                            latestThreadUrl = item.optString("latestThreadUrl")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(boards: List<Board>) {
        val context = appContext ?: return
        val array = JSONArray()
        boards.forEach { board ->
            array.put(
                JSONObject().apply {
                    put("title", board.title)
                    put("description", board.description)
                    put("url", board.url)
                    put("latestThreadTitle", board.latestThreadTitle)
                    put("latestThreadUrl", board.latestThreadUrl)
                }
            )
        }
        context.getSharedPreferences(AppConstants.PREFS_SETTINGS, Context.MODE_PRIVATE)
            .edit()
            .putString(keyBoards, array.toString())
            .apply()
    }
}
