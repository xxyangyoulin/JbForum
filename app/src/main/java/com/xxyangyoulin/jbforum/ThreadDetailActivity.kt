package com.xxyangyoulin.jbforum

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.GestureDetector
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.xxyangyoulin.jbforum.model.PostSegment
import com.xxyangyoulin.jbforum.model.splitPostSegments
import com.xxyangyoulin.jbforum.ui.components.CachedRemoteDisplayImage
import com.xxyangyoulin.jbforum.ui.components.ClickableName
import com.xxyangyoulin.jbforum.ui.components.EndOfListIndicator
import com.xxyangyoulin.jbforum.ui.components.RefreshContainer
import com.xxyangyoulin.jbforum.ui.components.RemarkCard
import com.xxyangyoulin.jbforum.ui.components.AuthorAvatar
import com.xxyangyoulin.jbforum.ui.components.ForumLinkActionDialog
import com.xxyangyoulin.jbforum.ui.components.ForumMessageAction
import com.xxyangyoulin.jbforum.ui.components.appTopBarHaze
import com.xxyangyoulin.jbforum.ui.components.PagingFooterStatus
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
    ExperimentalLayoutApi::class
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
    val handleBack = onBack
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
        topBarPost?.authorUid?.ifBlank { topBarDetail.authorUid }.orEmpty()
    }
    val topBarAvatarUrl = thread.authorAvatarUrl ?: topBarPost?.authorAvatarUrl
    val topBarAuthor = thread.author.ifBlank { topBarDetail?.author.orEmpty() }
    val topBarPublishedAt = thread.publishedAt.ifBlank { topBarDetail?.publishedAt.orEmpty() }
    var pendingScrollPid by remember { mutableStateOf<String?>(null) }
    val hazeState = remember { HazeState() }

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets.navigationBarsIgnoringVisibility,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBarsIgnoringVisibility,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                modifier = Modifier.appTopBarHaze(hazeState),
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
                        Icon(Icons.AutoMirrored.Outlined.ArrowBackIos, contentDescription = null, tint = TitleText)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::favoriteThread) {
                        Icon(Icons.Outlined.FavoriteBorder, contentDescription = "收藏", tint = TitleText)
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
                            Icon(Icons.Outlined.MoreVert, contentDescription = null, tint = TitleText)
                        }
                        StyledDropdownMenu(
                            expanded = detailMenuExpanded,
                            onDismissRequest = { detailMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("浏览历史") },
                                leadingIcon = { Icon(Icons.Outlined.History, contentDescription = null) },
                                onClick = {
                                    detailMenuExpanded = false
                                    historyItems = ThreadBrowseHistory.load()
                                    historyPanelOpen = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("网页打开") },
                                leadingIcon = { Icon(Icons.Outlined.OpenInBrowser, contentDescription = null) },
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
                        SmallFloatingActionButton(
                            onClick = { linksDialogOpen = true },
                            containerColor = CardBackground,
                            contentColor = TitleText,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 6.dp,
                                focusedElevation = 4.dp,
                                hoveredElevation = 4.dp
                            )
                        ) {
                            Icon(Icons.Outlined.Link, contentDescription = null)
                        }
                    }
                    if (canReply) {
                        SmallFloatingActionButton(
                            onClick = { replyDialogOpen = true },
                            containerColor = AccentGreen,
                            contentColor = Color.White,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 6.dp,
                                focusedElevation = 4.dp,
                                hoveredElevation = 4.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Message,
                                contentDescription = null
                            )
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
                loadingMoreReplies = state.repliesLoadingMore,
                loadMoreRepliesError = state.repliesLoadMoreError,
                padding = padding,
                modifier = Modifier.hazeSource(state = hazeState),
                drawBehindTopBar = true,
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
                contentVersion = state.threadDetail?.url.orEmpty(),
                indicatorTopPadding = padding.calculateTopPadding(),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = padding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                        end = padding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                        bottom = padding.calculateBottomPadding(),
                        top = 0.dp
                    )
                    .hazeSource(state = hazeState)
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
    loadingMoreReplies: Boolean,
    loadMoreRepliesError: String?,
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
    LaunchedEffect(nearBottom, detail.nextPageUrl, loadingMoreReplies, loadMoreRepliesError) {
        if (nearBottom && detail.nextPageUrl != null && !loadingMoreReplies && loadMoreRepliesError == null) {
            onLoadMoreReplies()
        }
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
        contentVersion = "${detail.url}-${segments.size}-${detail.posts.size}",
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
                    PagingFooterStatus(
                        loading = loadingMoreReplies,
                        error = loadMoreRepliesError,
                        hasMore = true,
                        onRetry = onLoadMoreReplies,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, top = 20.dp, end = 20.dp)
                    )
                }
            } else if (detail.posts.isNotEmpty()) {
                item {
                    EndOfListIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    )
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
        ForumLinkActionDialog(
            link = menuLink,
            type = detectedMenuType,
            message = menuLabel.ifBlank { menuLink },
            onDismiss = {
                detectedMenuLink = null
                detectedMenuType = null
                detectedMenuLabel = null
            },
            onOpen = {
                detectedMenuLink = null
                detectedMenuType = null
                detectedMenuLabel = null
                onOpenLink(menuLink, menuLabel.ifBlank { menuLink })
            },
            onExternalOpen = { externalLink ->
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(externalLink)))
                detectedMenuLink = null
                detectedMenuType = null
                detectedMenuLabel = null
            },
            onFavorite = {
                onFavoriteText(menuLink, detectedMenuType)
                detectedMenuLink = null
                detectedMenuType = null
                detectedMenuLabel = null
            },
            onOpenCode = if (detectedMenuType == "番号") {
                {
                    val domain = ForumDomainConfig.getDomain()
                    if (domain.isBlank()) {
                        Toast.makeText(context, "请先在设置中填写论坛域名", Toast.LENGTH_SHORT).show()
                    } else {
                        val code = menuLink.trim()
                        val targetUrl = "https://$domain/${Uri.encode(code)}"
                        context.startActivity(
                            ThreadWebViewActivity.createIntent(
                                context = context,
                                url = targetUrl,
                                title = code
                            )
                        )
                    }
                    detectedMenuLink = null
                    detectedMenuType = null
                    detectedMenuLabel = null
                }
            } else null,
            onCopy = {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                clipboard?.setPrimaryClip(ClipData.newPlainText("link", menuLink))
                Toast.makeText(context, "已复制链接", Toast.LENGTH_SHORT).show()
                detectedMenuLink = null
                detectedMenuType = null
                detectedMenuLabel = null
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
    val style = MaterialTheme.typography.bodyLarge
    val textSizeSp = style.fontSize.takeIf { it != androidx.compose.ui.unit.TextUnit.Unspecified } ?: 16.sp
    val plainText = remember(segments) { segments.joinToString(separator = "") { it.text } }
    val linkRanges = remember(segments) {
        buildList {
            var cursor = 0
            segments.forEach { segment ->
                val start = cursor
                cursor += segment.text.length
                val end = cursor
                val link = segment.linkUrl
                if (!link.isNullOrBlank() && end > start) {
                    add(
                        InlineLinkRange(
                            start = start,
                            end = end,
                            url = link,
                            label = segment.text
                        )
                    )
                }
            }
        }
    }
    val latestRanges by rememberUpdatedState(linkRanges)
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            androidx.appcompat.widget.AppCompatTextView(context).apply {
                setTextIsSelectable(true)
                setTextColor(TitleText.toArgb())
                textSize = textSizeSp.value
                val gestureDetector = GestureDetector(
                    context,
                    object : GestureDetector.SimpleOnGestureListener() {
                        private fun detectLink(event: android.view.MotionEvent): InlineLinkRange? {
                            val textLayout = layout ?: return null
                            val x = (event.x - totalPaddingLeft + scrollX).toInt()
                            val y = (event.y - totalPaddingTop + scrollY).toInt()
                            val line = textLayout.getLineForVertical(y)
                            val offset = textLayout.getOffsetForHorizontal(line, x.toFloat())
                            return latestRanges.firstOrNull { offset in it.start until it.end }
                        }

                        override fun onSingleTapUp(e: android.view.MotionEvent): Boolean {
                            val match = detectLink(e) ?: return false
                            onOpenLink(match.url, match.label)
                            return true
                        }

                        override fun onLongPress(e: android.view.MotionEvent) {
                            val match = detectLink(e) ?: return
                            onLongPressLink(match.url, match.label)
                        }
                    }
                )
                setOnTouchListener { _, event ->
                    gestureDetector.onTouchEvent(event)
                }
            }
        },
        update = { textView ->
            val spanned = SpannableString(plainText)
            linkRanges.forEach { range ->
                spanned.setSpan(
                    ForegroundColorSpan(AccentGreen.toArgb()),
                    range.start,
                    range.end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spanned.setSpan(
                    UnderlineSpan(),
                    range.start,
                    range.end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            textView.text = spanned
            textView.setTextColor(TitleText.toArgb())
            textView.textSize = textSizeSp.value
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
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/JbForum")
            put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("无法创建相册文件")
        resolver.openOutputStream(uri)?.use { it.write(payload.bytes) } ?: error("无法写入相册文件")
        resolver.update(uri, android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
        }, null, null)
    }
}

private data class ThreadGalleryImagePayload(
    val bytes: ByteArray,
    val extension: String,
    val mimeType: String
)

private data class InlineLinkRange(
    val start: Int,
    val end: Int,
    val url: String,
    val label: String
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
            imageVector = Icons.AutoMirrored.Filled.Message,
            contentDescription = "点评",
            tint = MutedText,
            modifier = Modifier
                .size(18.dp)
                .clickable { onRemark(post) }
        )
    }
}
