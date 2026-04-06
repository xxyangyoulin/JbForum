package com.xxyangyoulin.jbforum

import android.content.Context
import java.io.File
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

object ThreadDetailDiskCache {
    private const val LIMIT = 20

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun load(threadUrl: String): ThreadDetail? {
        val context = appContext ?: return null
        val file = cacheFile(context, threadUrl)
        if (!file.exists()) return null
        val raw = runCatching { file.readText() }.getOrNull().orEmpty()
        if (raw.isBlank()) return null
        return runCatching {
            val root = JSONObject(raw)
            val title = root.optString("title")
            val url = root.optString("url")
            if (url.isBlank()) return@runCatching null
            val postsArray = root.optJSONArray("posts") ?: JSONArray()
            val posts = buildList {
                for (i in 0 until postsArray.length()) {
                    val item = postsArray.optJSONObject(i) ?: continue
                    add(
                        PostItem(
                            pid = item.optString("pid"),
                            author = item.optString("author"),
                            authorUid = item.optString("authorUid"),
                            authorAvatarUrl = item.optString("authorAvatarUrl").ifBlank { null },
                            time = item.optString("time"),
                            floor = item.optString("floor"),
                            content = item.optString("content"),
                            contentBlocks = parseContentBlocks(item.optJSONArray("contentBlocks")),
                            imageUrls = parseStringArray(item.optJSONArray("imageUrls")),
                            remarks = parseRemarks(item.optJSONArray("remarks"))
                        )
                    )
                }
            }
            ThreadDetail(
                title = title,
                author = root.optString("author"),
                authorUid = root.optString("authorUid"),
                publishedAt = root.optString("publishedAt"),
                repliesText = root.optString("repliesText"),
                viewsText = root.optString("viewsText"),
                posts = posts,
                url = url,
                nextPageUrl = root.optString("nextPageUrl").ifBlank { null },
                currentPage = root.optInt("currentPage").coerceAtLeast(1),
                totalPages = root.optInt("totalPages").coerceAtLeast(1),
                favoriteCount = root.optString("favoriteCount"),
                favoriteActionUrl = root.optString("favoriteActionUrl").ifBlank { null },
                replyAction = root.optString("replyAction").ifBlank { null },
                replyFields = parseStringMap(root.optJSONObject("replyFields"))
            )
        }.getOrNull()
    }

    fun save(detail: ThreadDetail) {
        val context = appContext ?: return
        val file = cacheFile(context, detail.url)
        val root = JSONObject().apply {
            put("title", detail.title)
            put("author", detail.author)
            put("authorUid", detail.authorUid)
            put("publishedAt", detail.publishedAt)
            put("repliesText", detail.repliesText)
            put("viewsText", detail.viewsText)
            put("url", detail.url)
            put("nextPageUrl", detail.nextPageUrl ?: "")
            put("currentPage", detail.currentPage)
            put("totalPages", detail.totalPages)
            put("favoriteCount", detail.favoriteCount)
            put("favoriteActionUrl", detail.favoriteActionUrl ?: "")
            put("replyAction", detail.replyAction ?: "")
            put("replyFields", JSONObject().apply {
                detail.replyFields.forEach { (k, v) -> put(k, v) }
            })
            put("posts", JSONArray().apply {
                detail.posts.forEach { post ->
                    put(
                        JSONObject().apply {
                            put("pid", post.pid)
                            put("author", post.author)
                            put("authorUid", post.authorUid)
                            put("authorAvatarUrl", post.authorAvatarUrl ?: "")
                            put("time", post.time)
                            put("floor", post.floor)
                            put("content", post.content)
                            put("contentBlocks", JSONArray().apply {
                                post.contentBlocks.forEach { block ->
                                    put(
                                        JSONObject().apply {
                                            put("text", block.text ?: "")
                                            put("imageUrl", block.imageUrl ?: "")
                                            put("imageIndex", block.imageIndex ?: -1)
                                            put("linkUrl", block.linkUrl ?: "")
                                            put("quoteHeader", block.quoteHeader ?: "")
                                            put("quoteText", block.quoteText ?: "")
                                            put("quoteLinkUrl", block.quoteLinkUrl ?: "")
                                            put("inlineSegments", JSONArray().apply {
                                                block.inlineSegments.forEach { segment ->
                                                    put(
                                                        JSONObject().apply {
                                                            put("text", segment.text)
                                                            put("linkUrl", segment.linkUrl ?: "")
                                                        }
                                                    )
                                                }
                                            })
                                        }
                                    )
                                }
                            })
                            put("imageUrls", JSONArray(post.imageUrls))
                            put("remarks", JSONArray().apply {
                                post.remarks.forEach { remark ->
                                    put(
                                        JSONObject().apply {
                                            put("author", remark.author)
                                            put("authorUid", remark.authorUid)
                                            put("authorAvatarUrl", remark.authorAvatarUrl ?: "")
                                            put("time", remark.time)
                                            put("content", remark.content)
                                        }
                                    )
                                }
                            })
                        }
                    )
                }
            })
            put("savedAt", System.currentTimeMillis())
        }
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(root.toString())
            trim(context)
        }
    }

    private fun parseContentBlocks(array: JSONArray?): List<PostContentBlock> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                add(
                    PostContentBlock(
                        text = item.optString("text").ifBlank { null },
                        imageUrl = item.optString("imageUrl").ifBlank { null },
                        imageIndex = item.optInt("imageIndex").takeIf { it >= 0 },
                        linkUrl = item.optString("linkUrl").ifBlank { null },
                        inlineSegments = parseInlineSegments(item.optJSONArray("inlineSegments")),
                        quoteHeader = item.optString("quoteHeader").ifBlank { null },
                        quoteText = item.optString("quoteText").ifBlank { null },
                        quoteLinkUrl = item.optString("quoteLinkUrl").ifBlank { null }
                    )
                )
            }
        }
    }

    private fun parseInlineSegments(array: JSONArray?): List<PostInlineSegment> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val text = item.optString("text")
                if (text.isBlank()) continue
                add(PostInlineSegment(text = text, linkUrl = item.optString("linkUrl").ifBlank { null }))
            }
        }
    }

    private fun parseRemarks(array: JSONArray?): List<PostRemark> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                add(
                    PostRemark(
                        author = item.optString("author"),
                        authorUid = item.optString("authorUid"),
                        authorAvatarUrl = item.optString("authorAvatarUrl").ifBlank { null },
                        time = item.optString("time"),
                        content = item.optString("content")
                    )
                )
            }
        }
    }

    private fun parseStringArray(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val value = array.optString(i).trim()
                if (value.isNotBlank()) add(value)
            }
        }
    }

    private fun parseStringMap(obj: JSONObject?): Map<String, String> {
        if (obj == null) return emptyMap()
        return buildMap {
            obj.keys().forEach { key ->
                put(key, obj.optString(key))
            }
        }
    }

    private fun trim(context: Context) {
        val dir = File(context.cacheDir, AppConstants.CACHE_DIR_THREAD_DETAILS)
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        if (files.size <= LIMIT) return
        files.drop(LIMIT).forEach { it.delete() }
    }

    private fun cacheFile(context: Context, threadUrl: String): File {
        return File(context.cacheDir, "${AppConstants.CACHE_DIR_THREAD_DETAILS}/${sha1(threadUrl)}.json")
    }

    private fun sha1(value: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
