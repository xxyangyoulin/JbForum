package com.xxyangyoulin.jbforum

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.app.DownloadManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebResourceError
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.xxyangyoulin.jbforum.ui.theme.ForumTheme
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl

class ThreadWebViewActivity : ComponentActivity() {
    private var threadUrl: String = ""
    private var threadTitle: String = ""
    private var webView: WebView? = null
    private var cookiesPersisted = false
    private var restoredWebViewState: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookiePersistence.init(applicationContext)
        LoginPersistence.init(applicationContext)
        LocalImageFavorites.init(applicationContext)
        enableEdgeToEdge()
        threadUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        threadTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "原网页" }
        restoredWebViewState = savedInstanceState?.getBundle(STATE_WEB_VIEW)
        if (threadUrl.isBlank()) {
            finish()
            return
        }
        setContent {
            ForumTheme {
                ThreadWebViewScreen(
                    title = threadTitle,
                    url = threadUrl,
                    restoredState = restoredWebViewState,
                    onBack = {
                        val current = webView
                        if (current != null && current.canGoBack()) current.goBack() else finish()
                    },
                    onRefresh = {
                        webView?.reload()
                    },
                    onOpenInBrowser = {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webView?.url ?: threadUrl)))
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val state = Bundle()
        if (webView?.saveState(state) != null) {
            outState.putBundle(STATE_WEB_VIEW, state)
        }
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
        private const val STATE_WEB_VIEW = "web_view_state"

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
    restoredState: Bundle?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onWebViewReady: (WebView) -> Unit
) {
    var pageTitle by rememberSaveable(url) { mutableStateOf(title) }
    var progress by remember { mutableIntStateOf(0) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var longPressTarget by remember { mutableStateOf<WebLongPressTarget?>(null) }

    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = Color(0xFFF5F6F8),
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        scrolledContainerColor = Color.White
                    ),
                    title = {
                        Text(
                            text = pageTitle,
                            maxLines = 1,
                            color = Color(0xFF1F2937),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(0xFF1F2937))
                        }
                    },
                    actions = {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF1F2937))
                        }
                        IconButton(onClick = onOpenInBrowser) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF1F2937))
                        }
                    }
                )
                if (progress in 1..99 && loadError == null) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF2563EB),
                        trackColor = Color(0xFFE5E7EB)
                    )
                }
            }
        }
    ) { padding ->
        ThreadWebViewContent(
            padding = padding,
            url = url,
            restoredState = restoredState,
            onWebViewReady = onWebViewReady,
            onProgressChanged = { progress = it },
            onPageTitleChanged = { received ->
                if (received.isNotBlank()) {
                    pageTitle = received
                }
            },
            onPageError = { loadError = it },
            onLongPressTarget = { longPressTarget = it }
        )
        if (loadError != null) {
            WebViewErrorOverlay(
                message = loadError.orEmpty(),
                onRetry = {
                    loadError = null
                    onRefresh()
                }
            )
        }
        longPressTarget?.let { target ->
            WebLongPressDialog(
                target = target,
                onDismiss = { longPressTarget = null }
            )
        }
    }
}

@Composable
private fun ThreadWebViewContent(
    padding: PaddingValues,
    url: String,
    restoredState: Bundle?,
    onWebViewReady: (WebView) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onPageTitleChanged: (String) -> Unit,
    onPageError: (String?) -> Unit,
    onLongPressTarget: (WebLongPressTarget?) -> Unit
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
                setDownloadListener { downloadUrl, userAgent, contentDisposition, mimeType, _ ->
                    enqueueWebDownload(
                        context = context,
                        downloadUrl = downloadUrl,
                        userAgent = userAgent ?: settings.userAgentString,
                        contentDisposition = contentDisposition,
                        mimeType = mimeType,
                        referer = url ?: ForumDomainConfig.requireBaseUrl()
                    )
                }
                setOnLongClickListener {
                    val target = longPressTarget()
                    onLongPressTarget(target)
                    target != null
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                        onPageError(null)
                    }

                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        if (!request.isForMainFrame) return false
                        view.loadUrl(
                            request.url.toString(),
                            ThreadWebViewActivity.webHeaders(referer = request.requestHeaders["Referer"] ?: ForumDomainConfig.requireBaseUrl())
                        )
                        return true
                    }

                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError
                    ) {
                        if (request.isForMainFrame) {
                            onPageError(error.description?.toString().orEmpty().ifBlank { "页面加载失败" })
                        }
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView, newProgress: Int) {
                        onProgressChanged(newProgress)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        onPageTitleChanged(title.orEmpty())
                    }
                }
                onWebViewReady(this)
                if (restoredState != null) {
                    restoreState(restoredState)
                } else {
                    loadUrl(url, ThreadWebViewActivity.webHeaders())
                }
            }
        }
    )
}

private data class WebLongPressTarget(
    val url: String,
    val isImage: Boolean
)

private fun enqueueWebDownload(
    context: Context,
    downloadUrl: String,
    userAgent: String,
    contentDisposition: String?,
    mimeType: String?,
    referer: String
) {
    val fileName = URLUtil.guessFileName(downloadUrl, contentDisposition, mimeType)
    val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
        setMimeType(mimeType)
        addRequestHeader("User-Agent", userAgent)
        addRequestHeader("Referer", referer)
        CookieManager.getInstance().getCookie(downloadUrl)?.takeIf { it.isNotBlank() }?.let {
            addRequestHeader("Cookie", it)
        }
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        setTitle(fileName)
        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
    }
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    downloadManager.enqueue(request)
    android.widget.Toast.makeText(context, "已加入下载队列", android.widget.Toast.LENGTH_SHORT).show()
}

private fun WebView.longPressTarget(): WebLongPressTarget? {
    val result = hitTestResult ?: return null
    return when (result.type) {
        WebView.HitTestResult.SRC_ANCHOR_TYPE -> result.extra?.takeIf { it.isNotBlank() }?.let {
            WebLongPressTarget(url = it, isImage = false)
        }
        WebView.HitTestResult.IMAGE_TYPE,
        WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> result.extra?.takeIf { it.isNotBlank() }?.let {
            WebLongPressTarget(url = it, isImage = true)
        }
        else -> null
    }
}

@Composable
private fun WebViewErrorOverlay(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "加载失败", color = Color(0xFF1F2937), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, color = Color(0xFF6B7280), style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}

@Composable
private fun WebLongPressDialog(
    target: WebLongPressTarget,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (target.isImage) "图片操作" else "链接操作")
        },
        text = {
            Column {
                Text(
                    text = if (target.isImage) "预览图片" else "打开链接",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (target.isImage) {
                                context.startActivity(
                                    ImagePreviewActivity.createIntent(
                                        context = context,
                                        images = listOf(PreviewImageItem(imageRef = target.url, canFavorite = false)),
                                        initialIndex = 0
                                    )
                                )
                            } else {
                                context.startActivity(
                                    ThreadWebViewActivity.createIntent(
                                        context = context,
                                        url = target.url,
                                        title = "网页"
                                    )
                                )
                            }
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    color = Color(0xFF1F2937)
                )
                Text(
                    text = "复制链接",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("url", target.url))
                            android.widget.Toast.makeText(context, "已复制链接", android.widget.Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    color = Color(0xFF1F2937)
                )
                Text(
                    text = "外部浏览器打开",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target.url)))
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    color = Color(0xFF1F2937)
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
