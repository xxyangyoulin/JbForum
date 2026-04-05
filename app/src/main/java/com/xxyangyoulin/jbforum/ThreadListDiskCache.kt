package com.xxyangyoulin.jbforum

import android.content.Context
import java.io.File
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

object ThreadListDiskCache {
    private const val CACHE_DIR = "thread_list_cache"
    private const val LIMIT = 20

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun load(boardUrl: String): ThreadListPage? {
        val context = appContext ?: return null
        val file = cacheFile(context, boardUrl)
        if (!file.exists()) return null
        val raw = runCatching { file.readText() }.getOrNull().orEmpty()
        if (raw.isBlank()) return null
        return runCatching {
            val root = JSONObject(raw)
            val nextPageUrl = root.optString("nextPageUrl").ifBlank { null }
            val threadsArray = root.optJSONArray("threads") ?: JSONArray()
            val threads = buildList {
                for (i in 0 until threadsArray.length()) {
                    val item = threadsArray.optJSONObject(i) ?: continue
                    val url = item.optString("url")
                    if (url.isBlank()) continue
                    add(
                        ThreadSummary(
                            id = item.optString("id"),
                            title = item.optString("title"),
                            author = item.optString("author"),
                            authorUid = item.optString("authorUid"),
                            authorAvatarUrl = item.optString("authorAvatarUrl").ifBlank { null },
                            publishedAt = item.optString("publishedAt"),
                            titleIconUrls = jsonArrayToStringList(item.optJSONArray("titleIconUrls")),
                            totalPages = item.optInt("totalPages").takeIf { it > 0 },
                            url = url,
                            thumbnailUrls = jsonArrayToStringList(item.optJSONArray("thumbnailUrls")),
                            viewsText = item.optString("viewsText"),
                            repliesText = item.optString("repliesText"),
                            lastReplyAuthor = item.optString("lastReplyAuthor"),
                            lastReplyTime = item.optString("lastReplyTime"),
                            metaText = item.optString("metaText")
                        )
                    )
                }
            }
            ThreadListPage(threads = threads, nextPageUrl = nextPageUrl)
        }.getOrNull()
    }

    fun save(boardUrl: String, page: ThreadListPage) {
        val context = appContext ?: return
        val file = cacheFile(context, boardUrl)
        val root = JSONObject().apply {
            put("nextPageUrl", page.nextPageUrl ?: "")
            put("savedAt", System.currentTimeMillis())
            put("threads", JSONArray().apply {
                page.threads.forEach { thread ->
                    put(
                        JSONObject().apply {
                            put("id", thread.id)
                            put("title", thread.title)
                            put("author", thread.author)
                            put("authorUid", thread.authorUid)
                            put("authorAvatarUrl", thread.authorAvatarUrl ?: "")
                            put("publishedAt", thread.publishedAt)
                            put("titleIconUrls", JSONArray(thread.titleIconUrls))
                            put("totalPages", thread.totalPages ?: 0)
                            put("url", thread.url)
                            put("thumbnailUrls", JSONArray(thread.thumbnailUrls))
                            put("viewsText", thread.viewsText)
                            put("repliesText", thread.repliesText)
                            put("lastReplyAuthor", thread.lastReplyAuthor)
                            put("lastReplyTime", thread.lastReplyTime)
                            put("metaText", thread.metaText)
                        }
                    )
                }
            })
        }
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(root.toString())
            trim(context)
        }
    }

    private fun trim(context: Context) {
        val dir = File(context.cacheDir, CACHE_DIR)
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        if (files.size <= LIMIT) return
        files.drop(LIMIT).forEach { it.delete() }
    }

    private fun cacheFile(context: Context, boardUrl: String): File {
        val name = sha1(boardUrl) + ".json"
        return File(context.cacheDir, "$CACHE_DIR/$name")
    }

    private fun jsonArrayToStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val value = array.optString(i).trim()
                if (value.isNotBlank()) add(value)
            }
        }
    }

    private fun sha1(value: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
