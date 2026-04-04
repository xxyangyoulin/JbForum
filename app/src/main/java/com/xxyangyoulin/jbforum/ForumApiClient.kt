package com.xxyangyoulin.jbforum

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Dispatcher
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * 论坛 API 客户端
 * 负责 HTTP 请求、Cookie 管理、会话管理
 */
class ForumApiClient {
    val desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36"
    private val requestMutex = Mutex()

    val cookieJar = object : CookieJar {
        private val store = CookiePersistence.load()

        private fun reloadFromPersistence() {
            store.clear()
            store.addAll(CookiePersistence.load())
        }

        override fun saveFromResponse(url: okhttp3.HttpUrl, cookies: List<Cookie>) {
            synchronized(store) {
                cookies.forEach { incoming ->
                    store.removeAll { existing ->
                        existing.name == incoming.name &&
                            existing.domain == incoming.domain &&
                            existing.path == incoming.path
                    }
                    store.add(incoming)
                }
                CookiePersistence.save(store)
                Log.d(AppConstants.LOG_TAG, "saveCookies url=$url count=${store.size}")
            }
        }

        override fun loadForRequest(url: okhttp3.HttpUrl): List<Cookie> {
            synchronized(store) {
                reloadFromPersistence()
                val now = System.currentTimeMillis()
                store.removeAll { it.expiresAt < now }
                val matched = store.filter { it.matches(url) }
                Log.d(AppConstants.LOG_TAG, "loadCookies url=$url matched=${matched.map { it.name }}")
                return matched
            }
        }

        fun clear() {
            synchronized(store) {
                store.clear()
                CookiePersistence.clear()
            }
        }

        fun hasCookie(name: String): Boolean {
            synchronized(store) {
                reloadFromPersistence()
                return store.any { it.name == name }
            }
        }

        fun isEmpty(): Boolean {
            synchronized(store) {
                reloadFromPersistence()
                val now = System.currentTimeMillis()
                store.removeAll { it.expiresAt < now }
                return store.isEmpty()
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", desktopUserAgent)
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .build()
            chain.proceed(request)
        })
        .cookieJar(cookieJar)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    private val imageDispatcher = Dispatcher().apply {
        maxRequests = AppConstants.OKHTTP_MAX_REQUESTS
        maxRequestsPerHost = AppConstants.OKHTTP_MAX_REQUESTS_PER_HOST
    }

    @Volatile
    private var forumSessionReady = false

    /**
     * 获取用于图片加载的 OkHttpClient
     */
    fun imageClient(): OkHttpClient {
        return client.newBuilder()
            .followRedirects(true)
            .followSslRedirects(true)
            .dispatcher(imageDispatcher)
            .addInterceptor(Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Referer", ForumDomainConfig.requireBaseUrl())
                    .build()
                chain.proceed(request)
            })
            .build()
    }

    /**
     * 确保 forum session 已建立
     */
    suspend fun ensureForumSession() {
        if (forumSessionReady) return
        if (cookieJar.isEmpty()) {
            getDocument("member.php?mod=logging&action=login")
        } else {
            getDocument("forum.php")
        }
        forumSessionReady = true
        Log.d(AppConstants.LOG_TAG, "ensureForumSession ready=true")
    }

    /**
     * 清除 session
     */
    suspend fun clearSession() {
        cookieJar.clear()
        forumSessionReady = false
    }

    /**
     * 执行 GET 请求并返回 HTML Document
     */
    suspend fun getDocument(pathOrUrl: String, referer: String? = null): org.jsoup.nodes.Document {
        val url = absoluteUrl(pathOrUrl)
        val requestBuilder = Request.Builder().url(url)
        referer?.takeIf { it.isNotBlank() }?.let { requestBuilder.header("Referer", it) }
        val request = requestBuilder.build()
        val html = executeForHtml(request)
        Log.d(AppConstants.LOG_TAG, "GET url=$url size=${html.length}")
        return org.jsoup.Jsoup.parse(html, ForumDomainConfig.requireBaseUrl())
    }

    /**
     * 执行 POST 请求并返回 HTML
     */
    suspend fun postHtml(url: String, body: FormBody, referer: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Referer", referer)
            .post(body)
            .build()
        val html = executeForHtml(request)
        Log.d(AppConstants.LOG_TAG, "POST url=$url size=${html.length}")
        return html
    }

    /**
     * 执行请求并返回字节数组
     */
    suspend fun executeForBytes(request: Request, depth: Int = 0): ByteArray {
        val response = client.newCall(request).execute()
        response.use {
            val bodyBytes = it.body?.bytes() ?: ByteArray(0)
            if (shouldFollowRedirect(it, bodyBytes.isEmpty(), depth)) {
                val location = it.header("Location").orEmpty()
                return executeForBytes(
                    request.newBuilder()
                        .url(absoluteUrl(location))
                        .build(),
                    depth + 1
                )
            }
            return bodyBytes
        }
    }

    private suspend fun executeForHtml(request: Request, depth: Int = 0): String {
        val response = client.newCall(request).execute()
        response.use {
            val body = it.body?.string().orEmpty()
            if (shouldFollowRedirect(it, body.isBlank(), depth)) {
                val location = it.header("Location").orEmpty()
                return executeForHtml(
                    request.newBuilder()
                        .url(absoluteUrl(location))
                        .get()
                        .build(),
                    depth + 1
                )
            }
            if (!it.isSuccessful && body.isBlank()) error("请求失败: ${it.code}")
            return body
        }
    }

    private fun shouldFollowRedirect(response: Response, bodyEmpty: Boolean, depth: Int): Boolean {
        return depth < AppConstants.PARSER_REDIRECT_MAX_DEPTH && response.isRedirect && bodyEmpty && !response.header("Location").isNullOrBlank()
    }

    /**
     * 将相对 URL 转换为绝对 URL
     */
    fun absoluteUrl(pathOrUrl: String): String {
        if (pathOrUrl.isBlank()) return ""
        return if (pathOrUrl.startsWith("http")) {
            pathOrUrl
        } else {
            ForumDomainConfig.requireBaseUrl().toHttpUrlOrNull()?.resolve(pathOrUrl)?.toString().orEmpty()
        }
    }

    /**
     * 使用互斥锁执行操作
     */
    suspend fun <T> withRequestLock(block: suspend () -> T): T {
        return requestMutex.withLock { block() }
    }
}

/**
 * CookieJar isEmpty 扩展函数
 */
fun okhttp3.CookieJar.isEmpty(): Boolean {
    return (this as? ForumApiClient)?.cookieJar?.isEmpty() ?: true
}

/**
 * CookieJar clear 扩展函数
 */
fun okhttp3.CookieJar.clear() {
    (this as? ForumApiClient)?.cookieJar?.clear()
}

/**
 * CookieJar hasCookie 扩展函数
 */
fun okhttp3.CookieJar.hasCookie(name: String): Boolean {
    return (this as? ForumApiClient)?.cookieJar?.hasCookie(name) ?: false
}
