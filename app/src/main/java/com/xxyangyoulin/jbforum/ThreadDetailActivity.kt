package com.xxyangyoulin.jbforum

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import com.xxyangyoulin.jbforum.model.PostSegment
import com.xxyangyoulin.jbforum.model.splitPostSegments
import com.xxyangyoulin.jbforum.ui.components.CachedRemoteDisplayImage
import com.xxyangyoulin.jbforum.ui.components.ClickableName
import com.xxyangyoulin.jbforum.ui.components.RefreshContainer
import com.xxyangyoulin.jbforum.ui.components.RemarkCard
import com.xxyangyoulin.jbforum.ui.components.AuthorAvatar
import com.xxyangyoulin.jbforum.ui.components.ForumMessageAction
import com.xxyangyoulin.jbforum.ui.components.StyledDropdownMenu
import com.xxyangyoulin.jbforum.ui.components.UserIdentity
import com.xxyangyoulin.jbforum.ui.theme.ForumTheme
import com.xxyangyoulin.jbforum.ui.theme.rememberForumImageDownloadClient
import com.xxyangyoulin.jbforum.ui.theme.rememberForumImageLoader
import com.xxyangyoulin.jbforum.util.openThreadByPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import kotlin.math.roundToInt

private val ThreadPostAvatarSize = 28.dp
private val ThreadPostHeaderGap = 10.dp
private val ThreadPostContentIndent = ThreadPostAvatarSize + ThreadPostHeaderGap
private val ThreadDetailHorizontalPadding = 15.dp
private val ThreadDetailTitlePadding = 15.dp
private val ThreadDetailSectionSpacing = 10.dp
private val ThreadDetailImageVerticalPadding = 5.dp
private val ThreadDetailRemarksTopSpacing = 12.dp
private val ThreadDetailFooterTopSpacing = 14.dp

class ThreadDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookiePersistence.init(applicationContext)
        LoginPersistence.init(applicationContext)
        BoardDiskCache.init(applicationContext)
        ThreadDetailDiskCache.init(applicationContext)
        LocalImageFavorites.init(applicationContext)
        ThreadBrowseHistory.init(applicationContext)
        enableEdgeToEdge()
        val thread = ThreadSummary(
            id = intent.getStringExtra(EXTRA_THREAD_ID).orEmpty(),
            title = intent.getStringExtra(EXTRA_THREAD_TITLE).orEmpty(),
            author = intent.getStringExtra(EXTRA_THREAD_AUTHOR).orEmpty(),
            authorUid = intent.getStringExtra(EXTRA_THREAD_AUTHOR_UID).orEmpty(),
            authorAvatarUrl = intent.getStringExtra(EXTRA_THREAD_AUTHOR_AVATAR_URL).orEmpty().ifBlank { null },
            publishedAt = intent.getStringExtra(EXTRA_THREAD_PUBLISHED_AT).orEmpty(),
            url = intent.getStringExtra(EXTRA_THREAD_URL).orEmpty()
        )
        setContent {
            val viewModel: MainViewModel = viewModel()
            ForumTheme {
                ThreadDetailActivityScreen(
                    viewModel = viewModel,
                    thread = thread,
                    onBack = { finish() }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_THREAD_ID = "thread_id"
        private const val EXTRA_THREAD_TITLE = "thread_title"
        private const val EXTRA_THREAD_AUTHOR = "thread_author"
        private const val EXTRA_THREAD_AUTHOR_UID = "thread_author_uid"
        private const val EXTRA_THREAD_AUTHOR_AVATAR_URL = "thread_author_avatar_url"
        private const val EXTRA_THREAD_PUBLISHED_AT = "thread_published_at"
        private const val EXTRA_THREAD_URL = "thread_url"

        fun createIntent(context: android.content.Context, thread: ThreadSummary): android.content.Intent {
            return android.content.Intent(context, ThreadDetailActivity::class.java).apply {
                putExtra(EXTRA_THREAD_ID, thread.id)
                putExtra(EXTRA_THREAD_TITLE, thread.title)
                putExtra(EXTRA_THREAD_AUTHOR, thread.author)
                putExtra(EXTRA_THREAD_AUTHOR_UID, thread.authorUid)
                putExtra(EXTRA_THREAD_AUTHOR_AVATAR_URL, thread.authorAvatarUrl)
                putExtra(EXTRA_THREAD_PUBLISHED_AT, thread.publishedAt)
                putExtra(EXTRA_THREAD_URL, thread.url)
            }
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi::class
)
@Composable
internal fun ThreadDetailActivityScreen(
    viewModel: MainViewModel,
    thread: ThreadSummary,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val imageLoader = rememberForumImageLoader()
    val imageDownloadClient = rememberForumImageDownloadClient()
    val snackbarHostState = remember { SnackbarHostState() }
    var replyDialogOpen by remember { mutableStateOf(false) }
    var detailMenuExpanded by remember { mutableStateOf(false) }
    var hideFloatingButtons by remember { mutableStateOf(false) }
    val imagePreviewLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.let { data ->
            if (data.getBooleanExtra(ImagePreviewActivity.EXTRA_REFRESH_FAVORITES, false)) {
                viewModel.refreshLocalFavorites()
            }
            val threadUrl = data.getStringExtra(ImagePreviewActivity.EXTRA_OPEN_THREAD_URL).orEmpty()
            if (threadUrl.isNotBlank()) {
                openThreadByPreference(
                    context,
                    ThreadSummary(
                        id = threadUrl.substringAfter("tid=").substringBefore('&'),
                        title = data.getStringExtra(ImagePreviewActivity.EXTRA_OPEN_THREAD_TITLE).orEmpty().ifBlank { "帖子详情" },
                        author = "",
                        url = threadUrl
                    )
                )
            }
        }
    }
    val threadWebLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refreshSession()
    }
    val detectedLinks = state.detectedLinks
    var linksDialogOpen by remember { mutableStateOf(false) }
    var historyPanelOpen by remember { mutableStateOf(false) }
    var historyItems by remember { mutableStateOf(ThreadBrowseHistory.load()) }
    val handleBack = {
        viewModel.closeThread()
        onBack()
    }
    val openUserCenter: (String) -> Unit = { uid ->
        context.startActivity(UserCenterActivity.createIntent(context, uid))
    }
    val openPostLink: (String, String) -> Unit = { url, label ->
        openThreadDetailLink(
            context = context,
            url = url,
            label = label,
            openUserCenter = openUserCenter,
            openThreadWeb = { targetUrl, title ->
                threadWebLauncher.launch(
                    ThreadWebViewActivity.createIntent(
                        context = context,
                        url = targetUrl,
                        title = title
                    )
                )
            },
            openImage = { imageUrl ->
                imagePreviewLauncher.launch(
                    ImagePreviewActivity.createIntent(
                        context = context,
                        images = listOf(PreviewImageItem(imageRef = imageUrl)),
                        initialIndex = 0
                    )
                )
            }
        )
    }

    LaunchedEffect(thread.url) {
        if (thread.url.isNotBlank()) viewModel.openThread(thread)
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(state.threadDetail?.url) {
        val detail = state.threadDetail ?: return@LaunchedEffect
        ThreadBrowseHistory.add(
            title = state.selectedThread?.title ?: detail.title,
            url = detail.url
        )
    }

    BackHandler(onBack = handleBack)

    val topBarDetail = state.threadDetail
    val topBarPost = topBarDetail?.posts?.firstOrNull()
    val topBarUid = thread.authorUid.ifBlank {
        topBarPost?.authorUid?.ifBlank { topBarDetail?.authorUid.orEmpty() }.orEmpty()
    }
    val topBarAvatarUrl = thread.authorAvatarUrl ?: topBarPost?.authorAvatarUrl
    val topBarAuthor = thread.author.ifBlank { topBarDetail?.author.orEmpty() }
    val topBarPublishedAt = thread.publishedAt.ifBlank { topBarDetail?.publishedAt.orEmpty() }
    var pendingScrollPid by remember { mutableStateOf<String?>(null) }
    val supportsHaze = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val hazeState = remember { HazeState() }

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets.navigationBarsIgnoringVisibility,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBarsIgnoringVisibility,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (supportsHaze) Color.Transparent else CardBackground,
                    scrolledContainerColor = if (supportsHaze) Color.Transparent else CardBackground
                ),
                modifier = if (supportsHaze) {
                    Modifier.hazeEffect(
                        state = hazeState,
                        style = HazeMaterials.thin()
                    ) {
                        inputScale = HazeInputScale.Fixed(0.5f)
                    }
                } else Modifier,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (topBarAvatarUrl != null || topBarAuthor.isNotBlank() || topBarPublishedAt.isNotBlank()) {
                            AuthorAvatar(
                                imageLoader = imageLoader,
                                imageUrl = topBarAvatarUrl,
                                name = topBarAuthor,
                                modifier = if (topBarUid.isNotBlank()) Modifier.clickable { openUserCenter(topBarUid) } else Modifier,
                                size = 34.dp
                            )
                        }
                        if (topBarAuthor.isNotBlank() || topBarPublishedAt.isNotBlank()) {
                            Column(horizontalAlignment = Alignment.Start) {
                                if (topBarAuthor.isNotBlank()) {
                                    Text(
                                        text = topBarAuthor,
                                        maxLines = 1,
                                        color = TitleText,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = if (topBarUid.isNotBlank()) Modifier.clickable { openUserCenter(topBarUid) } else Modifier
                                    )
                                }
                                if (topBarPublishedAt.isNotBlank()) {
                                    Text(
                                        text = topBarPublishedAt,
                                        maxLines = 1,
                                        color = MutedText,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TitleText)
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::favoriteThread) {
                        Text("收藏", color = TitleText)
                    }
                    ForumMessageAction(
                        status = state.forumMessageStatus,
                        onClick = {
                            val targetUrl = state.forumMessageStatus.noticeUrl
                                .ifBlank { ForumDomainConfig.baseUrl() + "home.php?mod=space&do=notice" }
                            if (targetUrl.isNotBlank()) {
                                context.startActivity(
                                    ForumNoticeActivity.createIntent(context, targetUrl)
                                )
                            }
                        }
                    )
                    Box {
                        IconButton(onClick = { detailMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null, tint = TitleText)
                        }
                        StyledDropdownMenu(
                            expanded = detailMenuExpanded,
                            onDismissRequest = { detailMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("浏览历史") },
                                leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                                onClick = {
                                    detailMenuExpanded = false
                                    historyItems = ThreadBrowseHistory.load()
                                    historyPanelOpen = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("打开原网页") },
                                leadingIcon = { Icon(Icons.Default.OpenInBrowser, contentDescription = null) },
                                onClick = {
                                    detailMenuExpanded = false
                                    val targetUrl = state.selectedThread?.url
                                        ?.takeIf { it.isNotBlank() }
                                        ?: state.threadDetail?.url.orEmpty()
                                    if (targetUrl.isBlank()) {
                                        Toast.makeText(context, "帖子链接为空", Toast.LENGTH_SHORT).show()
                                    } else {
                                        threadWebLauncher.launch(
                                            ThreadWebViewActivity.createIntent(
                                                context = context,
                                                url = targetUrl,
                                                title = state.selectedThread?.title ?: thread.title.ifBlank { "帖子详情" }
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            val canReply = state.threadDetail?.replyAction != null
            AnimatedVisibility(
                visible = (canReply || detectedLinks.isNotEmpty()) && !hideFloatingButtons,
                enter = fadeIn() + scaleIn(initialScale = 0.85f),
                exit = fadeOut() + scaleOut(targetScale = 0.85f)
            ) {
                Column(
                    modifier = Modifier.navigationBarsPadding(),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (detectedLinks.isNotEmpty()) {
                        FloatingActionButton(
                            onClick = { linksDialogOpen = true },
                            containerColor = CardBackground,
                            contentColor = TitleText
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null)
                        }
                    }
                    if (canReply) {
                        FloatingActionButton(
                            onClick = { replyDialogOpen = true },
                            containerColor = AccentGreen,
                            contentColor = Color.White
                        ) {
                            Text("✎")
                        }
                    }
                }
            }
        }
    ) { padding ->
        val detail = state.threadDetail
        if (detail != null) {
            ThreadDetailScreen(
                detail = detail,
                imageLoader = imageLoader,
                imageDownloadClient = imageDownloadClient,
                refreshing = state.threadRefreshing,
                padding = padding,
                modifier = if (supportsHaze) Modifier.hazeSource(state = hazeState) else Modifier,
                drawBehindTopBar = supportsHaze,
                onRefresh = { viewModel.openThread(thread) },
                onOpenUserCenter = { uid ->
                    context.startActivity(UserCenterActivity.createIntent(context, uid))
                },
                onOpenLink = { url, label ->
                    val currentThreadId = thread.id.ifBlank { extractThreadId(detail.url) }
                    val targetThreadId = extractThreadId(url)
                    val targetPostId = extractPostId(url)
                    if (url.contains("goto=findpost") &&
                        currentThreadId.isNotBlank() &&
                        currentThreadId == targetThreadId &&
                        !targetPostId.isNullOrBlank() &&
                        detail.posts.any { it.pid == targetPostId }
                    ) {
                        pendingScrollPid = targetPostId
                    } else {
                        openPostLink(url, label)
                    }
                },
                onOpenImage = { images, index, launchSource ->
                    val sourceThread = state.selectedThread
                    imagePreviewLauncher.launch(
                        ImagePreviewActivity.createIntent(
                            context = context,
                            images = images.map {
                                PreviewImageItem(
                                    imageRef = it,
                                    sourceThreadTitle = sourceThread?.title.orEmpty(),
                                    sourceThreadUrl = sourceThread?.url.orEmpty(),
                                    canFavorite = true
                                )
                            },
                            initialIndex = index,
                            launchSource = launchSource
                        ),
                        ActivityOptionsCompat.makeCustomAnimation(context, 0, 0)
                    )
                },
                onLoadMoreReplies = viewModel::loadMoreReplies,
                onNearBottomChanged = { hideFloatingButtons = it },
                onRemark = {
                    if (state.session != null) {
                        viewModel.prepareRemark(it)
                    } else {
                        viewModel.prepareLogin()
                    }
                },
                onFavoriteText = { selected, detectedType ->
                    runCatching {
                        LocalLinkFavorites.add(
                            value = selected,
                            type = detectedType ?: LinkCategory.TEXT,
                            sourceThreadTitle = state.selectedThread?.title ?: state.threadDetail?.title.orEmpty(),
                            sourceThreadUrl = state.selectedThread?.url ?: state.threadDetail?.url.orEmpty()
                        )
                    }.onSuccess {
                        Toast.makeText(
                            context,
                            if (detectedType != null) "已收藏${detectedType}链接" else "已收藏文本",
                            Toast.LENGTH_SHORT
                        ).show()
                    }.onFailure {
                        Toast.makeText(context, it.message ?: "收藏失败", Toast.LENGTH_SHORT).show()
                    }
                },
                targetPostId = pendingScrollPid,
                onTargetPostHandled = { pendingScrollPid = null }
            )
        } else {
            RefreshContainer(
                refreshing = state.loading,
                onRefresh = { viewModel.openThread(thread) },
                indicatorTopPadding = if (supportsHaze) padding.calculateTopPadding() else 0.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = padding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                        end = padding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                        bottom = padding.calculateBottomPadding(),
                        top = if (supportsHaze) 0.dp else padding.calculateTopPadding()
                    )
                    .then(if (supportsHaze) Modifier.hazeSource(state = hazeState) else Modifier)
            ) {
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }

    ThreadHistoryPanel(
        visible = historyPanelOpen,
        items = historyItems,
        onDismiss = { historyPanelOpen = false },
        onClear = {
            ThreadBrowseHistory.clear()
            historyItems = emptyList()
        },
        onOpen = { item ->
            historyPanelOpen = false
            openThreadByPreference(
                context,
                ThreadSummary(
                    id = item.id,
                    title = item.title,
                    author = "",
                    url = item.url
                )
            )
        }
    )


    if (replyDialogOpen && state.threadDetail?.replyAction != null) {
        QuickReplyDialog(
            title = "快速回复",
            label = "回复内容",
            onDismiss = { replyDialogOpen = false },
            onSubmit = {
                replyDialogOpen = false
                viewModel.submitReply(it)
            }
        )
    }

    if (state.remarkForm != null) {
        QuickReplyDialog(
            title = "点评",
            label = "点评内容",
            hint = "当前点评：${state.remarkForm!!.targetLabel}",
            onDismiss = viewModel::clearRemark,
            onSubmit = {
                viewModel.submitRemark(it)
            }
        )
    }

    if (linksDialogOpen) {
        ThreadLinksDialog(
            links = detectedLinks,
            sourceThreadTitle = state.selectedThread?.title ?: state.threadDetail?.title.orEmpty(),
            sourceThreadUrl = state.selectedThread?.url ?: state.threadDetail?.url.orEmpty(),
            onDismiss = { linksDialogOpen = false },
            onOpenFavorites = {
                linksDialogOpen = false
                context.startActivity(
                    LocalFavoritesActivity.createIntent(
                        context,
                        LocalFavoritesActivity.TAB_LINK
                    )
                )
            }
        )
    }
}

@Composable
internal fun ThreadDetailScreen(
    detail: ThreadDetail,
    imageLoader: ImageLoader,
    imageDownloadClient: okhttp3.OkHttpClient,
    refreshing: Boolean,
    padding: PaddingValues,
    modifier: Modifier = Modifier,
    drawBehindTopBar: Boolean = false,
    onRefresh: () -> Unit,
    onOpenUserCenter: (String) -> Unit,
    onOpenLink: (String, String) -> Unit,
    onOpenImage: (List<String>, Int, PreviewLaunchSource?) -> Unit,
    onLoadMoreReplies: () -> Unit,
    onNearBottomChanged: (Boolean) -> Unit,
    onRemark: (PostItem) -> Unit,
    onFavoriteText: (String, String?) -> Unit,
    targetPostId: String? = null,
    onTargetPostHandled: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val detailImageResizeWidthPx = remember(configuration, density) {
        with(density) {
            (configuration.screenWidthDp.dp - 68.dp).roundToPx()
        }
    }
    val segments = remember(detail.posts) { splitPostSegments(detail.posts) }
    val segmentHeights = remember { mutableStateMapOf<String, Int>() }
    val imageAspectRatios = remember { mutableStateMapOf<String, Float>() }
    var highlightedPostId by remember { mutableStateOf<String?>(null) }
    val nearBottom by remember(listState, density) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
            if (lastVisible.index != listState.layoutInfo.totalItemsCount - 1) return@derivedStateOf false
            val thresholdPx = with(density) { 20.dp.roundToPx() }
            val remaining = listState.layoutInfo.viewportEndOffset - (lastVisible.offset + lastVisible.size)
            remaining <= thresholdPx
        }
    }

    LaunchedEffect(nearBottom) {
        onNearBottomChanged(nearBottom)
    }
    LaunchedEffect(targetPostId, segments) {
        val pid = targetPostId ?: return@LaunchedEffect
        val targetIndex = segments.indexOfFirst { it.postId == pid }
        if (targetIndex >= 0) {
            listState.animateScrollToItem(targetIndex)
            highlightedPostId = pid
            onTargetPostHandled()
        }
    }
    LaunchedEffect(highlightedPostId) {
        if (highlightedPostId != null) {
            delay(800)
            highlightedPostId = null
        }
    }

    RefreshContainer(
        refreshing = refreshing,
        onRefresh = onRefresh,
        indicatorTopPadding = if (drawBehindTopBar) padding.calculateTopPadding() else 0.dp,
        modifier = Modifier
            .then(modifier)
            .fillMaxSize()
            .padding(
                start = padding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                end = padding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                bottom = padding.calculateBottomPadding(),
                top = if (drawBehindTopBar) 0.dp else padding.calculateTopPadding()
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(
                    top = if (drawBehindTopBar) padding.calculateTopPadding() else 0.dp
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardBackground)
                            .padding(horizontal = ThreadDetailTitlePadding, vertical = 8.dp)
                    ) {
                        Text(detail.title, style = MaterialTheme.typography.titleLarge, color = TitleText, fontWeight = FontWeight.SemiBold)
                    }
                }
            items(
                count = segments.size,
                key = { i -> "${segments[i].postId}-${segments[i].segmentIndex}" }
            ) { index ->
                val segment = segments[index]
                val prevIsSamePost = index > 0 && segments[index - 1].postId == segment.postId
                val nextIsSamePost = index < segments.size - 1 && segments[index + 1].postId == segment.postId
                val isLastOfPost = !nextIsSamePost

                val shape = RoundedCornerShape(0.dp)
                val segKey = "${segment.postId}-${segment.segmentIndex}"
                val cachedHeight = segmentHeights[segKey]
                val highlightAlpha by animateFloatAsState(
                    targetValue = if (segment.postId == highlightedPostId) 1f else 0f,
                    animationSpec = tween(durationMillis = 180),
                    label = "post_highlight"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            cachedHeight?.let { Modifier.heightIn(min = with(LocalDensity.current) { it.toDp() }) }
                                ?: Modifier
                        )
                        .onSizeChanged { segmentHeights[segKey] = it.height }
                        .background(CardBackground, shape)
                        .drawBehind {
                            if (highlightAlpha > 0f) {
                                drawRect(
                                    color = AccentGreen.copy(alpha = 0.12f * highlightAlpha)
                                )
                            }
                        }
                        .clip(shape)
                ) {
                    val isFirstPost = segment.postId == detail.posts.firstOrNull()?.pid
                    when (segment) {
                        is PostSegment.Whole -> {
                            Column(modifier = Modifier.padding(start = ThreadDetailHorizontalPadding, end = ThreadDetailHorizontalPadding, top = ThreadDetailTitlePadding, bottom = ThreadDetailTitlePadding)) {
                                if (!isFirstPost) {
                                    PostHeader(segment.post, imageLoader, onOpenUserCenter)
                                    Spacer(Modifier.height(ThreadDetailSectionSpacing))
                                }
                                Column(modifier = if (!isFirstPost) Modifier.padding(start = ThreadPostContentIndent) else Modifier) {
                                    PostContentBlocks(
                                        post = segment.post,
                                        blocks = segment.post.contentBlocks,
                                        imageLoader = imageLoader,
                                        imageDownloadClient = imageDownloadClient,
                                        detailImageResizeWidthPx = detailImageResizeWidthPx,
                                        imageAspectRatios = imageAspectRatios,
                                        onOpenLink = onOpenLink,
                                        onOpenImage = onOpenImage,
                                        onFavoriteText = onFavoriteText,
                                        sourceThreadTitle = detail.title,
                                        sourceThreadUrl = detail.url
                                    )
                                    PostFooter(segment.post, imageLoader, onOpenUserCenter, onRemark)
                                }
                            }
                        }
                        is PostSegment.First -> {
                            Column(modifier = Modifier.padding(start = ThreadDetailHorizontalPadding, end = ThreadDetailHorizontalPadding, top = ThreadDetailTitlePadding, bottom = 0.dp)) {
                                if (!isFirstPost) {
                                    PostHeader(segment.post, imageLoader, onOpenUserCenter)
                                    Spacer(Modifier.height(ThreadDetailSectionSpacing))
                                }
                                Column(modifier = if (!isFirstPost) Modifier.padding(start = ThreadPostContentIndent) else Modifier) {
                                    PostContentBlocks(
                                        post = segment.post,
                                        blocks = segment.blocks,
                                        imageLoader = imageLoader,
                                        imageDownloadClient = imageDownloadClient,
                                        detailImageResizeWidthPx = detailImageResizeWidthPx,
                                        imageAspectRatios = imageAspectRatios,
                                        onOpenLink = onOpenLink,
                                        onOpenImage = onOpenImage,
                                        onFavoriteText = onFavoriteText,
                                        sourceThreadTitle = detail.title,
                                        sourceThreadUrl = detail.url
                                    )
                                }
                            }
                        }
                        is PostSegment.Middle -> {
                            Column(modifier = Modifier.padding(start = ThreadDetailHorizontalPadding, end = ThreadDetailHorizontalPadding, top = 0.dp, bottom = 0.dp)) {
                                Column(modifier = if (!isFirstPost) Modifier.padding(start = ThreadPostContentIndent) else Modifier) {
                                    PostContentBlocks(
                                        post = segment.post,
                                        blocks = segment.blocks,
                                        imageLoader = imageLoader,
                                        imageDownloadClient = imageDownloadClient,
                                        detailImageResizeWidthPx = detailImageResizeWidthPx,
                                        imageAspectRatios = imageAspectRatios,
                                        onOpenLink = onOpenLink,
                                        onOpenImage = onOpenImage,
                                        onFavoriteText = onFavoriteText,
                                        sourceThreadTitle = detail.title,
                                        sourceThreadUrl = detail.url
                                    )
                                }
                            }
                        }
                        is PostSegment.Tail -> {
                            Column(modifier = Modifier.padding(start = ThreadDetailHorizontalPadding, end = ThreadDetailHorizontalPadding, top = 0.dp, bottom = ThreadDetailTitlePadding)) {
                                Column(modifier = if (!isFirstPost) Modifier.padding(start = ThreadPostContentIndent) else Modifier) {
                                    if (segment.blocks.isNotEmpty()) {
                                        PostContentBlocks(
                                            post = segment.post,
                                            blocks = segment.blocks,
                                            imageLoader = imageLoader,
                                            imageDownloadClient = imageDownloadClient,
                                            detailImageResizeWidthPx = detailImageResizeWidthPx,
                                            imageAspectRatios = imageAspectRatios,
                                            onOpenLink = onOpenLink,
                                            onOpenImage = onOpenImage,
                                            onFavoriteText = onFavoriteText,
                                            sourceThreadTitle = detail.title,
                                            sourceThreadUrl = detail.url
                                        )
                                    }
                                    PostFooter(segment.post, imageLoader, onOpenUserCenter, onRemark)
                                }
                            }
                        }
                    }
                }
            }
            if (detail.nextPageUrl != null) {
                item {
                    OutlinedButton(
                        onClick = onLoadMoreReplies,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, top = 20.dp, end = 20.dp)
                    ) {
                        val remainingPages = (detail.totalPages - detail.currentPage).coerceAtLeast(1)
                        Text("继续加载更多评论，还剩 $remainingPages 页", color = TitleText)
                    }
                }
            }
            }
        }
    }
}

@Composable
internal fun PostHeader(
    post: PostItem,
    imageLoader: ImageLoader,
    onOpenUserCenter: (String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        UserIdentity(
            imageLoader = imageLoader,
            imageUrl = post.authorAvatarUrl,
            name = post.author.ifBlank { "匿名" },
            uid = post.authorUid,
            avatarSize = ThreadPostAvatarSize,
            nameTextStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            metaText = listOf(post.floor, post.time).filter { it.isNotBlank() }.joinToString(" · "),
            onOpenUserCenter = onOpenUserCenter
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun PostContentBlocks(
    post: PostItem,
    blocks: List<PostContentBlock>,
    imageLoader: ImageLoader,
    imageDownloadClient: okhttp3.OkHttpClient,
    detailImageResizeWidthPx: Int,
    imageAspectRatios: SnapshotStateMap<String, Float>,
    onOpenLink: (String, String) -> Unit,
    onOpenImage: (List<String>, Int, PreviewLaunchSource?) -> Unit,
    onFavoriteText: (String, String?) -> Unit,
    sourceThreadTitle: String,
    sourceThreadUrl: String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val orderedImages = remember(post.pid) { post.contentBlocks.mapNotNull { it.imageUrl } }
    val failedImages = remember(post.pid) { mutableStateMapOf<String, Boolean>() }
    var actionImageUrl by remember(post.pid) { mutableStateOf<String?>(null) }
    var detectedMenuLink by remember(post.pid) { mutableStateOf<String?>(null) }
    var detectedMenuType by remember(post.pid) { mutableStateOf<String?>(null) }
    var detectedMenuLabel by remember(post.pid) { mutableStateOf<String?>(null) }
    blocks.forEachIndexed { index, block ->
        block.quoteText?.takeIf { it.isNotBlank() }?.let { quoteText ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F6F8))
                    .drawBehind {
                        drawRect(
                            color = AccentGreen.copy(alpha = 0.22f),
                            topLeft = Offset.Zero,
                            size = Size(6.dp.toPx(), size.height)
                        )
                    }
                    .padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
            ) {
                block.quoteHeader?.takeIf { it.isNotBlank() }?.let { header ->
                    Text(
                        text = header,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText,
                        modifier = if (!block.quoteLinkUrl.isNullOrBlank()) {
                            Modifier.clickable { onOpenLink(block.quoteLinkUrl, header) }
                        } else {
                            Modifier
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = quoteText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText
                )
            }
            if (index < blocks.lastIndex || !block.text.isNullOrBlank() || !block.imageUrl.isNullOrBlank()) {
                Spacer(Modifier.height(ThreadDetailSectionSpacing))
            }
        }
        block.text?.takeIf { it.isNotBlank() }?.let { text ->
            val inlineSegments = block.inlineSegments
            val hasInlineLink = inlineSegments.any { !it.linkUrl.isNullOrBlank() }
            if (!hasInlineLink && block.linkUrl.isNullOrBlank()) {
                SelectableFavoriteText(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TitleText,
                    onFavorite = onFavoriteText,
                    onDetectedLinkAction = { value, type ->
                        detectedMenuLink = value
                        detectedMenuType = type
                    }
                )
            } else if (hasInlineLink) {
                LinkablePostText(
                    segments = inlineSegments.ifEmpty { listOf(PostInlineSegment(text = text, linkUrl = block.linkUrl)) },
                    onOpenLink = onOpenLink,
                    onLongPressLink = { link, label ->
                        detectedMenuLink = link
                        detectedMenuType = ThreadLinkRecognizer.detectTypeForSelection(link)
                        detectedMenuLabel = label
                    }
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AccentGreen,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        block.linkUrl?.let { onOpenLink(it, text) }
                    }
                )
            }
            if (index < blocks.lastIndex) Spacer(Modifier.height(ThreadDetailSectionSpacing))
        }
        block.imageUrl?.let { imageUrl ->
            var layoutCoordinates by remember(imageUrl) { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
            val cachedRatio = imageAspectRatios[imageUrl]
            val isFailed = failedImages[imageUrl] ?: ThreadImageCache.isFailed(imageUrl)
            val previewImageRefs = orderedImages.filterNot { failedImages[it] ?: ThreadImageCache.isFailed(it) }
            CachedRemoteDisplayImage(
                imageRef = imageUrl,
                imageLoader = imageLoader,
                imageDownloadClient = imageDownloadClient,
                modifier = Modifier
                    .padding(vertical = ThreadDetailImageVerticalPadding)
                    .onGloballyPositioned { coordinates -> layoutCoordinates = coordinates }
                    .fillMaxWidth()
                    .then(
                        cachedRatio?.let { Modifier.aspectRatio(it) } ?: Modifier
                    )
                    .background(Color(0xFFEDEFF2))
                    .combinedClickable(
                        enabled = !isFailed,
                        onClick = {
                            val previewIndex = previewImageRefs.indexOf(imageUrl)
                            if (previewIndex >= 0) {
                                val launchSource = layoutCoordinates?.let { coordinates ->
                                    val bounds = coordinates.boundsInWindow()
                                    PreviewLaunchSource(
                                        left = bounds.left.roundToInt(),
                                        top = bounds.top.roundToInt(),
                                        width = bounds.width.roundToInt(),
                                        height = bounds.height.roundToInt()
                                    )
                                }
                                onOpenImage(
                                    previewImageRefs.map(ThreadImageCache::previewRef),
                                    previewIndex,
                                    launchSource
                                )
                            }
                        },
                        onLongClick = {
                            actionImageUrl = imageUrl
                        }
                    ),
                resizeWidthPx = detailImageResizeWidthPx,
                showOriginalDirectly = false,
                onImageReady = { width, height ->
                    if (width > 0 && height > 0) {
                        imageAspectRatios[imageUrl] = width.toFloat() / height.toFloat()
                    }
                },
                onFailedStateChanged = { failed ->
                    failedImages[imageUrl] = failed
                }
            )
        }
    }
    if (detectedMenuLink != null) {
        val menuLink = detectedMenuLink.orEmpty()
        val menuLabel = detectedMenuLabel.orEmpty()
        val canOpenInternally = shouldShowInternalOpen(menuLink, detectedMenuType)
        val canOpenExternally = shouldShowExternalOpen(menuLink, detectedMenuType)
        val externalOpenLink = normalizeExternalDetectedLink(menuLink)
        AlertDialog(
            onDismissRequest = {
                detectedMenuLink = null
                detectedMenuType = null
                detectedMenuLabel = null
            },
            title = { Text("链接操作") },
            text = { Text(menuLabel.ifBlank { menuLink }) },
            confirmButton = {
                if (canOpenInternally) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        OutlinedButton(
                            onClick = {
                                detectedMenuLink = null
                                detectedMenuType = null
                                detectedMenuLabel = null
                                onOpenLink(menuLink, menuLabel.ifBlank { menuLink })
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("打开")
                        }
                    }
                }
            },
            dismissButton = {
                Row {
                    if (canOpenExternally) {
                        TextButton(
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(externalOpenLink)))
                                detectedMenuLink = null
                                detectedMenuType = null
                                detectedMenuLabel = null
                            }
                        ) {
                            Text("外部打开")
                        }
                    }
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard?.setPrimaryClip(ClipData.newPlainText("link", menuLink))
                            Toast.makeText(context, "已复制链接", Toast.LENGTH_SHORT).show()
                            detectedMenuLink = null
                            detectedMenuType = null
                            detectedMenuLabel = null
                        }
                    ) {
                        Text("复制")
                    }
                    TextButton(
                        onClick = {
                            onFavoriteText(menuLink, detectedMenuType)
                            detectedMenuLink = null
                            detectedMenuType = null
                            detectedMenuLabel = null
                        }
                    ) {
                        Text("收藏")
                    }
                    TextButton(
                        onClick = {
                            detectedMenuLink = null
                            detectedMenuType = null
                            detectedMenuLabel = null
                        }
                    ) {
                        Text("取消")
                    }
                }
            }
        )
    }
    actionImageUrl?.let { imageUrl ->
        AlertDialog(
            onDismissRequest = { actionImageUrl = null },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            actionImageUrl = null
                            coroutineScope.launch {
                                val message = runCatching {
                                    saveThreadImageToGallery(context, imageDownloadClient, imageUrl)
                                }.fold(
                                    onSuccess = { "图片已保存到相册" },
                                    onFailure = { it.message ?: "保存失败" }
                                )
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存图片")
                    }
                    OutlinedButton(
                        onClick = {
                            actionImageUrl = null
                            coroutineScope.launch {
                                val message = runCatching {
                                    LocalImageFavorites.add(
                                        context = context,
                                        client = imageDownloadClient,
                                        imageRef = imageUrl,
                                        sourceThreadTitle = sourceThreadTitle,
                                        sourceThreadUrl = sourceThreadUrl
                                    )
                                }.fold(
                                    onSuccess = { "图片已收藏到本地" },
                                    onFailure = { it.message ?: "收藏失败" }
                                )
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("收藏图片")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { actionImageUrl = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun LinkablePostText(
    segments: List<PostInlineSegment>,
    onOpenLink: (String, String) -> Unit,
    onLongPressLink: (String, String) -> Unit
) {
    val annotated = remember(segments) {
        buildAnnotatedString {
            segments.forEach { segment ->
                val start = length
                append(segment.text)
                val end = length
                if (!segment.linkUrl.isNullOrBlank()) {
                    addStyle(
                        SpanStyle(color = AccentGreen, textDecoration = TextDecoration.Underline),
                        start = start,
                        end = end
                    )
                    addStringAnnotation(
                        tag = "link",
                        annotation = segment.linkUrl,
                        start = start,
                        end = end
                    )
                }
            }
        }
    }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    BasicText(
        text = annotated,
        style = MaterialTheme.typography.bodyLarge.copy(color = TitleText),
        onTextLayout = { textLayoutResult = it },
        modifier = Modifier.pointerInput(annotated) {
            detectTapGestures(
                onTap = { position ->
                    val layout = textLayoutResult ?: return@detectTapGestures
                    val offset = layout.getOffsetForPosition(position)
                    annotated
                        .getStringAnnotations(tag = "link", start = offset, end = offset)
                        .firstOrNull()
                        ?.let { annotation ->
                            val label = segments.firstOrNull { it.linkUrl == annotation.item }?.text.orEmpty()
                            onOpenLink(annotation.item, label)
                        }
                },
                onLongPress = { position ->
                    val layout = textLayoutResult ?: return@detectTapGestures
                    val offset = layout.getOffsetForPosition(position)
                    annotated
                        .getStringAnnotations(tag = "link", start = offset, end = offset)
                        .firstOrNull()
                        ?.let { annotation ->
                            onLongPressLink(
                                annotation.item,
                                segments.firstOrNull { it.linkUrl == annotation.item }?.text.orEmpty()
                            )
                        }
                }
            )
        }
    )
}

private fun openThreadDetailLink(
    context: android.content.Context,
    url: String,
    label: String,
    openUserCenter: (String) -> Unit,
    openThreadWeb: (String, String) -> Unit,
    openImage: (String) -> Unit
) {
    val normalized = ForumHtmlParser.absoluteUrl(url)
    if (normalized.isBlank()) return
    val httpUrl = normalized.toHttpUrlOrNull()
    val forumHost = runCatching { ForumDomainConfig.forumHost() }.getOrNull().orEmpty()
    val isForumLink = httpUrl != null && forumHost.isNotBlank() &&
        (httpUrl.host == forumHost || httpUrl.host.endsWith(".$forumHost"))
    if (!isForumLink) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalized)))
        return
    }

    if (isImageLink(normalized)) {
        openImage(normalized)
        return
    }

    if (normalized.contains("viewthread") || Regex("""thread-\d+-\d+-\d+""").containsMatchIn(normalized)) {
        openThreadByPreference(
            context,
            ThreadSummary(
                id = extractThreadId(normalized),
                title = label.ifBlank { "帖子详情" },
                author = "",
                url = normalized
            )
        )
        return
    }

    if (normalized.contains("forumdisplay") || Regex("""forum-\d+-\d+""").containsMatchIn(normalized)) {
        context.startActivity(
            ThreadsActivity.createIntent(
                context,
                Board(
                    title = label.ifBlank { "板块" },
                    description = "",
                    url = normalized
                )
            )
        )
        return
    }

    if (normalized.contains("home.php?mod=space") && normalized.contains("uid=")) {
        openUserCenter(ForumHtmlParser.extractUid(normalized))
        return
    }

    openThreadWeb(normalized, label.ifBlank { "原网页" })
}

private fun shouldShowInternalOpen(link: String, type: String?): Boolean {
    if (type == "番号" || type == "磁力") return false
    return link.startsWith("http://", ignoreCase = true) ||
        link.startsWith("https://", ignoreCase = true) ||
        link.startsWith("ed2k://", ignoreCase = true) ||
        isCloudShareLink(link)
}

private fun shouldShowExternalOpen(link: String, type: String?): Boolean {
    if (type == "番号") return false
    return link.startsWith("http://", ignoreCase = true) ||
        link.startsWith("https://", ignoreCase = true) ||
        link.startsWith("magnet:", ignoreCase = true) ||
        link.startsWith("ed2k://", ignoreCase = true) ||
        isCloudShareLink(link)
}

private fun normalizeExternalDetectedLink(link: String): String {
    return if (isCloudShareLink(link) && !link.startsWith("http://", ignoreCase = true) && !link.startsWith("https://", ignoreCase = true)) {
        "https://$link"
    } else {
        link
    }
}

private fun isCloudShareLink(link: String): Boolean {
    val normalized = link.lowercase()
    return normalized.startsWith("pan.baidu.com/") ||
        normalized.startsWith("pan.quark.cn/") ||
        normalized.startsWith("aliyundrive.com/") ||
        normalized.startsWith("www.aliyundrive.com/") ||
        normalized.startsWith("alipan.com/") ||
        normalized.startsWith("www.alipan.com/") ||
        normalized.startsWith("drive.uc.cn/") ||
        normalized.startsWith("115.com/") ||
        normalized.startsWith("pan.xunlei.com/") ||
        normalized.startsWith("123pan.com/") ||
        normalized.startsWith("www.123pan.com/") ||
        normalized.startsWith("share.weiyun.com/") ||
        normalized.matches(Regex("""lanzou[a-z0-9]*\.com/.*""")) ||
        normalized.matches(Regex("""www\.lanzou[a-z0-9]*\.com/.*"""))
}

private fun extractThreadId(url: String): String {
    val tid = url.substringAfter("tid=", "").substringBefore('&').trim()
    if (tid.isNotBlank()) return tid
    return Regex("""thread-(\d+)-""").find(url)?.groupValues?.getOrNull(1).orEmpty()
}

private fun extractPostId(url: String): String? {
    return url.substringAfter("pid=", "").substringBefore('&').trim().ifBlank { null }
}

private fun isImageLink(url: String): Boolean {
    val normalized = url.substringBefore('?').lowercase()
    return normalized.endsWith(".jpg") ||
        normalized.endsWith(".jpeg") ||
        normalized.endsWith(".png") ||
        normalized.endsWith(".gif") ||
        normalized.endsWith(".webp")
}

private suspend fun saveThreadImageToGallery(
    context: android.content.Context,
    client: okhttp3.OkHttpClient,
    imageUrl: String
) {
    val payload = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (imageUrl.startsWith("http")) {
            client.newCall(
                okhttp3.Request.Builder()
                    .url(imageUrl)
                    .header("Referer", ForumDomainConfig.requireBaseUrl())
                    .build()
            ).execute().use { response ->
                if (!response.isSuccessful) error("下载图片失败: ${response.code}")
                val bytes = response.body?.bytes() ?: error("图片内容为空")
                val fallback = threadImageFormatFor(imageUrl)
                val mimeType = response.body?.contentType()?.toString()?.substringBefore(';')
                    ?.takeIf { it.startsWith("image/") }
                    ?: fallback.mimeType
                val extension = threadImageExtensionForMimeType(mimeType) ?: fallback.extension
                ThreadGalleryImagePayload(bytes, extension, mimeType)
            }
        } else {
            val file = java.io.File(imageUrl).takeIf { it.exists() } ?: error("图片内容为空")
            val format = threadImageFormatFor(imageUrl)
            ThreadGalleryImagePayload(file.readBytes(), format.extension, format.mimeType)
        }
    }
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val resolver = context.contentResolver
        val filename = "jbforum_${System.currentTimeMillis()}.${payload.extension}"
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, payload.mimeType)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/JbForum")
                put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("无法创建相册文件")
        resolver.openOutputStream(uri)?.use { it.write(payload.bytes) } ?: error("无法写入相册文件")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            resolver.update(uri, android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
            }, null, null)
        }
    }
}

private data class ThreadGalleryImagePayload(
    val bytes: ByteArray,
    val extension: String,
    val mimeType: String
)

private fun threadImageFormatFor(imageRef: String): ThreadGalleryImagePayload {
    val normalized = imageRef.substringBefore('?').lowercase()
    return when {
        normalized.endsWith(".gif") -> ThreadGalleryImagePayload(ByteArray(0), "gif", "image/gif")
        normalized.endsWith(".png") -> ThreadGalleryImagePayload(ByteArray(0), "png", "image/png")
        normalized.endsWith(".webp") -> ThreadGalleryImagePayload(ByteArray(0), "webp", "image/webp")
        normalized.endsWith(".jpeg") -> ThreadGalleryImagePayload(ByteArray(0), "jpeg", "image/jpeg")
        else -> ThreadGalleryImagePayload(ByteArray(0), "jpg", "image/jpeg")
    }
}

private fun threadImageExtensionForMimeType(mimeType: String): String? {
    return when (mimeType.lowercase()) {
        "image/gif" -> "gif"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/jpeg" -> "jpeg"
        else -> null
    }
}

@Composable
internal fun PostFooter(
    post: PostItem,
    imageLoader: ImageLoader,
    onOpenUserCenter: (String) -> Unit,
    onRemark: (PostItem) -> Unit
) {
    if (post.remarks.isNotEmpty()) {
        Spacer(Modifier.height(ThreadDetailRemarksTopSpacing))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            post.remarks.forEach { remark ->
                RemarkCard(
                    remark = remark,
                    imageLoader = imageLoader,
                    onOpenUserCenter = onOpenUserCenter
                )
            }
        }
    }
    Spacer(Modifier.height(ThreadDetailFooterTopSpacing))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Icon(
            imageVector = Icons.Outlined.Message,
            contentDescription = "点评",
            tint = MutedText,
            modifier = Modifier
                .size(18.dp)
                .clickable { onRemark(post) }
        )
    }
}
