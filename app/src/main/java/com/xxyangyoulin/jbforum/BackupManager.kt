package com.xxyangyoulin.jbforum

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object BackupManager {
    suspend fun exportToDownloads(context: Context): String = withContext(Dispatchers.IO) {
        LocalLinkFavorites.init(context.applicationContext)
        ThreadBrowseHistory.init(context.applicationContext)

        val images = LocalImageFavorites.load()
        val links = LocalLinkFavorites.load()
        val history = ThreadBrowseHistory.load()
        val cookies = context.getSharedPreferences(AppConstants.PREFS_COOKIES, Context.MODE_PRIVATE)
            .getStringSet("cookies", emptySet())
            .orEmpty()
            .toList()
        val login = LoginPersistence.load()

        val imageMetaArray = JSONArray()
        val imageFiles = mutableListOf<Pair<String, File>>()
        images.forEach { item ->
            val file = File(item.filePath)
            if (!file.exists()) return@forEach
            val ext = file.extension.ifBlank { "jpg" }
            val entryName = "images/${item.id}.$ext"
            imageMetaArray.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("entry", entryName)
                    put("originalUrl", item.originalUrl)
                    put("savedAt", item.savedAt)
                    put("sourceThreadTitle", item.sourceThreadTitle)
                    put("sourceThreadUrl", item.sourceThreadUrl)
                }
            )
            imageFiles += entryName to file
        }

        val linkArray = JSONArray()
        links.forEach { item ->
            linkArray.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("value", item.value)
                    put("type", item.type)
                    put("savedAt", item.savedAt)
                    put("sourceThreadTitle", item.sourceThreadTitle)
                    put("sourceThreadUrl", item.sourceThreadUrl)
                }
            )
        }

        val historyArray = JSONArray()
        history.forEach { item ->
            historyArray.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("url", item.url)
                    put("viewedAt", item.viewedAt)
                }
            )
        }

        val root = JSONObject().apply {
            put("schemaVersion", 1)
            put("exportedAt", System.currentTimeMillis())
            put("domain", ForumDomainConfig.getDomain())
            put("openThreadInWeb", ForumDomainConfig.openThreadInWebDefault())
            put("username", login.first)
            put("password", login.second)
            put("cookies", JSONArray(cookies))
            put("imageFavorites", imageMetaArray)
            put("linkFavorites", linkArray)
            put("history", historyArray)
        }

        val tempZip = File(context.cacheDir, "jbforum_backup_temp.zip")
        ZipOutputStream(FileOutputStream(tempZip)).use { zip ->
            zip.putNextEntry(ZipEntry("backup.json"))
            zip.write(root.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            imageFiles.forEach { (entryName, file) ->
                zip.putNextEntry(ZipEntry(entryName))
                FileInputStream(file).use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }

        val fileName = "jbforum_backup_${
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        }.zip"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/zip")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("创建导出文件失败")
        resolver.openOutputStream(uri)?.use { out ->
            FileInputStream(tempZip).use { input -> input.copyTo(out) }
        } ?: error("写入导出文件失败")
        tempZip.delete()
        fileName
    }

    suspend fun importFromUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        LocalLinkFavorites.init(context.applicationContext)
        ThreadBrowseHistory.init(context.applicationContext)

        val imageBytes = mutableMapOf<String, ByteArray>()
        var backupJson: String? = null
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) {
                        zip.closeEntry()
                        continue
                    }
                    val bytes = zip.readBytes()
                    if (entry.name == "backup.json") {
                        backupJson = bytes.toString(Charsets.UTF_8)
                    } else if (entry.name.startsWith("images/")) {
                        imageBytes[entry.name] = bytes
                    }
                    zip.closeEntry()
                }
            }
        } ?: error("读取备份文件失败")

        val json = JSONObject(backupJson ?: error("备份文件缺少 backup.json"))
        val domain = json.optString("domain")
        val openThreadInWeb = json.optBoolean("openThreadInWeb", false)
        val username = json.optString("username")
        val password = json.optString("password")
        val cookies = mutableSetOf<String>()
        val cookieArray = json.optJSONArray("cookies") ?: JSONArray()
        for (i in 0 until cookieArray.length()) {
            cookieArray.optString(i).takeIf { it.isNotBlank() }?.let(cookies::add)
        }

        ForumDomainConfig.setDomain(domain)
        ForumDomainConfig.setOpenThreadInWebDefault(openThreadInWeb)
        LoginPersistence.save(username, password)
        context.getSharedPreferences(AppConstants.PREFS_COOKIES, Context.MODE_PRIVATE)
            .edit()
            .putStringSet("cookies", cookies)
            .apply()

        val importedLinks = mutableListOf<LocalFavoriteLink>()
        val linkArray = json.optJSONArray("linkFavorites") ?: JSONArray()
        for (i in 0 until linkArray.length()) {
            val item = linkArray.optJSONObject(i) ?: continue
            val value = item.optString("value")
            if (value.isBlank()) continue
            val normalizedValue = value.trim()
            importedLinks += LocalFavoriteLink(
                id = item.optString("id").ifBlank { sha1(normalizedValue.lowercase()) },
                value = normalizedValue,
                type = item.optString("type").ifBlank { LinkCategory.OTHER },
                savedAt = item.optLong("savedAt", System.currentTimeMillis()),
                sourceThreadTitle = item.optString("sourceThreadTitle"),
                sourceThreadUrl = item.optString("sourceThreadUrl")
            )
        }
        LocalLinkFavorites.replaceAll(importedLinks)

        val importedHistory = mutableListOf<ThreadHistoryItem>()
        val historyArray = json.optJSONArray("history") ?: JSONArray()
        for (i in 0 until historyArray.length()) {
            val item = historyArray.optJSONObject(i) ?: continue
            val url = item.optString("url")
            if (url.isBlank()) continue
            importedHistory += ThreadHistoryItem(
                id = item.optString("id").ifBlank { url.substringAfter("tid=").substringBefore('&').ifBlank { url } },
                title = item.optString("title").ifBlank { "帖子详情" },
                url = url,
                viewedAt = item.optLong("viewedAt", System.currentTimeMillis())
            )
        }
        ThreadBrowseHistory.replaceAll(importedHistory)

        LocalImageFavorites.clearAll()
        val favoritesDir = File(context.filesDir, AppConstants.CACHE_DIR_FAVORITE_IMAGES).apply { mkdirs() }
        val thumbnailsDir = File(context.filesDir, AppConstants.CACHE_DIR_FAVORITE_THUMBNAILS)
        if (thumbnailsDir.exists()) thumbnailsDir.deleteRecursively()
        thumbnailsDir.mkdirs()

        val importedImages = mutableListOf<LocalFavoriteImage>()
        val imageArray = json.optJSONArray("imageFavorites") ?: JSONArray()
        for (i in 0 until imageArray.length()) {
            val item = imageArray.optJSONObject(i) ?: continue
            val id = item.optString("id")
            val entry = item.optString("entry")
            val bytes = imageBytes[entry] ?: continue
            if (id.isBlank()) continue
            val ext = entry.substringAfterLast('.', "").ifBlank { "jpg" }
            val target = File(favoritesDir, "$id.$ext")
            target.writeBytes(bytes)
            importedImages += LocalFavoriteImage(
                id = id,
                filePath = target.absolutePath,
                thumbnailPath = target.absolutePath,
                originalUrl = item.optString("originalUrl"),
                savedAt = item.optLong("savedAt", System.currentTimeMillis()),
                sourceThreadTitle = item.optString("sourceThreadTitle"),
                sourceThreadUrl = item.optString("sourceThreadUrl")
            )
        }
        LocalImageFavorites.replaceAllFromBackup(importedImages)

        "已导入：图片${importedImages.size}，链接${importedLinks.size}，历史${importedHistory.size}"
    }

    private fun sha1(value: String): String {
        return MessageDigest.getInstance("SHA-1")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
