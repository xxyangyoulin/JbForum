package com.xxyangyoulin.jbforum

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import com.xxyangyoulin.jbforum.model.PostSegment
import com.xxyangyoulin.jbforum.model.splitPostSegments
import com.xxyangyoulin.jbforum.ui.components.CachedRemoteDisplayImage
import com.xxyangyoulin.jbforum.ui.components.ClickableName
import com.xxyangyoulin.jbforum.ui.components.RefreshContainer
import com.xxyangyoulin.jbforum.ui.components.RemarkCard
import com.xxyangyoulin.jbforum.ui.components.AuthorAvatar
import com.xxyangyoulin.jbforum.ui.components.UserIdentity
import com.xxyangyoulin.jbforum.ui.theme.ForumTheme
import com.xxyangyoulin.jbforum.ui.theme.rememberForumImageDownloadClient
import com.xxyangyoulin.jbforum.ui.theme.rememberForumImageLoader
import com.xxyangyoulin.jbforum.util.openThreadByPreference
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets.navigationBarsIgnoringVisibility,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBarsIgnoringVisibility,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardBackground,
                    scrolledContainerColor = CardBackground
                ),
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
                    Box {
                        IconButton(onClick = { detailMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null, tint = TitleText)
                        }
                        DropdownMenu(
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
                onRefresh = { viewModel.openThread(thread) },
                onOpenUserCenter = { uid ->
                    context.startActivity(UserCenterActivity.createIntent(context, uid))
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
                }
            )
        } else {
            RefreshContainer(
                refreshing = state.loading,
                onRefresh = { viewModel.openThread(thread) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
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
    onRefresh: () -> Unit,
    onOpenUserCenter: (String) -> Unit,
    onOpenImage: (List<String>, Int, PreviewLaunchSource?) -> Unit,
    onLoadMoreReplies: () -> Unit,
    onNearBottomChanged: (Boolean) -> Unit,
    onRemark: (PostItem) -> Unit,
    onFavoriteText: (String, String?) -> Unit
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

    RefreshContainer(
        refreshing = refreshing,
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(0.dp),
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            cachedHeight?.let { Modifier.heightIn(min = with(LocalDensity.current) { it.toDp() }) }
                                ?: Modifier
                        )
                        .onSizeChanged { segmentHeights[segKey] = it.height }
                        .background(CardBackground, shape)
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
                                        onOpenImage = onOpenImage,
                                        onFavoriteText = onFavoriteText
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
                                        onOpenImage = onOpenImage,
                                        onFavoriteText = onFavoriteText
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
                                        onOpenImage = onOpenImage,
                                        onFavoriteText = onFavoriteText
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
                                            onOpenImage = onOpenImage,
                                            onFavoriteText = onFavoriteText
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
internal fun PostContentBlocks(
    post: PostItem,
    blocks: List<PostContentBlock>,
    imageLoader: ImageLoader,
    imageDownloadClient: okhttp3.OkHttpClient,
    detailImageResizeWidthPx: Int,
    imageAspectRatios: SnapshotStateMap<String, Float>,
    onOpenImage: (List<String>, Int, PreviewLaunchSource?) -> Unit,
    onFavoriteText: (String, String?) -> Unit
) {
    val orderedImages = remember(post.pid) { post.contentBlocks.mapNotNull { it.imageUrl } }
    val previewImages = remember(orderedImages) { orderedImages.map(ThreadImageCache::previewRef) }
    blocks.forEachIndexed { index, block ->
        block.text?.takeIf { it.isNotBlank() }?.let { text ->
            SelectableFavoriteText(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = TitleText,
                onFavorite = onFavoriteText
            )
            if (index < blocks.lastIndex) Spacer(Modifier.height(ThreadDetailSectionSpacing))
        }
        block.imageUrl?.let { imageUrl ->
            var launchSource by remember(imageUrl) { mutableStateOf<PreviewLaunchSource?>(null) }
            val cachedRatio = imageAspectRatios[imageUrl]
            CachedRemoteDisplayImage(
                imageRef = imageUrl,
                imageLoader = imageLoader,
                imageDownloadClient = imageDownloadClient,
                modifier = Modifier
                    .padding(vertical = ThreadDetailImageVerticalPadding)
                    .onGloballyPositioned { coordinates ->
                        val bounds = coordinates.boundsInWindow()
                        launchSource = PreviewLaunchSource(
                            left = bounds.left.roundToInt(),
                            top = bounds.top.roundToInt(),
                            width = bounds.width.roundToInt(),
                            height = bounds.height.roundToInt()
                        )
                        if (coordinates.size.width > 0 && coordinates.size.height > 200) {
                            imageAspectRatios[imageUrl] = coordinates.size.width.toFloat() / coordinates.size.height.toFloat()
                        }
                    }
                    .fillMaxWidth()
                    .then(
                        cachedRatio?.let { Modifier.aspectRatio(it) } ?: Modifier
                    )
                    .background(Color(0xFFEDEFF2))
                    .clickable {
                        onOpenImage(
                            previewImages.map(ThreadImageCache::previewRef),
                            block.imageIndex ?: orderedImages.indexOf(imageUrl).coerceAtLeast(0),
                            launchSource
                        )
                    },
                resizeWidthPx = detailImageResizeWidthPx,
                showOriginalDirectly = false
            )
        }
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
            imageVector = Icons.Outlined.ChatBubbleOutline,
            contentDescription = "点评",
            tint = MutedText,
            modifier = Modifier
                .size(18.dp)
                .clickable { onRemark(post) }
        )
    }
}
