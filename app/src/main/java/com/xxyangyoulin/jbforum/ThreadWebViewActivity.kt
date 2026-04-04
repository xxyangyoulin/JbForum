package com.xxyangyoulin.jbforum

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.xxyangyoulin.jbforum.ui.theme.ForumTheme
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl

class ThreadWebViewActivity : ComponentActivity() {
    private var threadUrl: String = ""
    private var threadTitle: String = ""
    private var webView: WebView? = null
    private var cookiesPersisted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookiePersistence.init(applicationContext)
        LoginPersistence.init(applicationContext)
        LocalImageFavorites.init(applicationContext)
        enableEdgeToEdge()
        threadUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        threadTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "原网页" }
        if (threadUrl.isBlank()) {
            finish()
            return
        }
        setContent {
            ForumTheme {
                ThreadWebViewScreen(
                    title = threadTitle,
                    url = threadUrl,
                    onBack = {
                        val current = webView
                        if (current != null && current.canGoBack()) current.goBack() else finish()
                    },
                    onWebViewReady = { created ->
                        webView = created
                    }
                )
            }
        }
    }

    override fun finish() {
        persistWebViewCookiesIfNeeded()
        super.finish()
    }

    override fun onDestroy() {
        persistWebViewCookiesIfNeeded()
        webView?.destroy()
        webView = null
        super.onDestroy()
    }

    private fun persistWebViewCookiesIfNeeded() {
        if (cookiesPersisted || threadUrl.isBlank()) return
        val cookieManager = CookieManager.getInstance()
        val rawCookie = cookieManager.getCookie(threadUrl).orEmpty()
        val existing = CookiePersistence.load()
        val nonForumCookies = existing.filterNot { ForumDomainConfig.isForumDomain(it.domain) }
        val baseUrl = ForumDomainConfig.requireBaseUrl().toHttpUrl()
        val forumCookies = rawCookie.split(';')
            .mapNotNull { token ->
                val pair = token.trim()
                if (pair.isEmpty() || !pair.contains("=")) return@mapNotNull null
                val name = pair.substringBefore('=').trim()
                val value = pair.substringAfter('=').trim()
                if (name.isEmpty()) return@mapNotNull null
                Cookie.parse(baseUrl, "$name=$value; Domain=${ForumDomainConfig.forumCookieDomain()}; Path=/")
            }
        val merged = (nonForumCookies + forumCookies)
            .distinctBy { Triple(it.name, it.domain, it.path) }
        CookiePersistence.save(merged)
        cookieManager.flush()
        cookiesPersisted = true
    }

    companion object {
        private const val EXTRA_URL = "url"
        private const val EXTRA_TITLE = "title"

        fun createIntent(context: Context, url: String, title: String): Intent {
            return Intent(context, ThreadWebViewActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TITLE, title)
            }
        }

        fun injectAppCookiesIntoWebView(url: String) {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            CookiePersistence.load()
                .filter { ForumDomainConfig.isForumDomain(it.domain) }
                .forEach { cookie ->
                    val path = cookie.path.ifBlank { "/" }
                    cookieManager.setCookie(url, "${cookie.name}=${cookie.value}; Domain=${cookie.domain}; Path=$path")
                }
            cookieManager.flush()
        }

        fun webHeaders(referer: String = ForumDomainConfig.requireBaseUrl()): Map<String, String> {
            return mapOf(
                "User-Agent" to ForumApiClient().desktopUserAgent,
                "Accept-Language" to "zh-CN,zh;q=0.9,en;q=0.8",
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                "Referer" to referer
            )
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThreadWebViewScreen(
    title: String,
    url: String,
    onBack: () -> Unit,
    onWebViewReady: (WebView) -> Unit
) {
    Scaffold(
        containerColor = Color(0xFFF5F6F8),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    scrolledContainerColor = Color.White
                ),
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        color = Color(0xFF1F2937),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(0xFF1F2937))
                    }
                }
            )
        }
    ) { padding ->
        ThreadWebViewContent(
            padding = padding,
            url = url,
            onWebViewReady = onWebViewReady
        )
    }
}

@Composable
private fun ThreadWebViewContent(
    padding: PaddingValues,
    url: String,
    onWebViewReady: (WebView) -> Unit
) {
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        factory = { context ->
            WebView(context).apply {
                ThreadWebViewActivity.injectAppCookiesIntoWebView(url)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                settings.userAgentString = ForumApiClient().desktopUserAgent
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowContentAccess = true
                settings.allowFileAccess = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        if (!request.isForMainFrame) return false
                        view.loadUrl(
                            request.url.toString(),
                            ThreadWebViewActivity.webHeaders(referer = request.requestHeaders["Referer"] ?: ForumDomainConfig.requireBaseUrl())
                        )
                        return true
                    }
                }
                webChromeClient = WebChromeClient()
                onWebViewReady(this)
                loadUrl(url, ThreadWebViewActivity.webHeaders())
            }
        }
    )
}
