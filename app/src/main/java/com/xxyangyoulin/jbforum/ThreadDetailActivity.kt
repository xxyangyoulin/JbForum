package com.xxyangyoulin.jbforum

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.xxyangyoulin.jbforum.ui.components.UserIdentity
import com.xxyangyoulin.jbforum.ui.theme.ForumTheme
import com.xxyangyoulin.jbforum.ui.theme.rememberForumImageDownloadClient
import com.xxyangyoulin.jbforum.ui.theme.rememberForumImageLoader
import com.xxyangyoulin.jbforum.util.openThreadByPreference
import kotlin.math.roundToInt

class ThreadDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookiePersistence.init(applicationContext)
        LoginPersistence.init(applicationContext)
        BoardDiskCache.init(applicationContext)
        LocalImageFavorites.init(applicationContext)
        ThreadBrowseHistory.init(applicationContext)
        enableEdgeToEdge()
        val thread = ThreadSummary(
            id = intent.getStringExtra(EXTRA_THREAD_ID).orEmpty(),
            title = intent.getStringExtra(EXTRA_THREAD_TITLE).orEmpty(),
            author = intent.getStringExtra(EXTRA_THREAD_AUTHOR).orEmpty(),
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
        private const val EXTRA_THREAD_URL = "thread_url"

        fun createIntent(context: android.content.Context, thread: ThreadSummary): android.content.Intent {
            return android.content.Intent(context, ThreadDetailActivity::class.java).apply {
                putExtra(EXTRA_THREAD_ID, thread.id)
                putExtra(EXTRA_THREAD_TITLE, thread.title)
                putExtra(EXTRA_THREAD_AUTHOR, thread.author)
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
                    Text(
                        text = state.selectedThread?.title ?: thread.title.ifBlank { "帖子详情" },
                        maxLines = 1,
                        color = TitleText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
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
            if (canReply || detectedLinks.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(end = FloatingButtonEdgePadding),
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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(detail.title, style = MaterialTheme.typography.titleLarge, color = TitleText, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        ClickableName(
                            name = detail.author,
                            uid = detail.authorUid,
                            color = MutedText,
                            style = MaterialTheme.typography.bodySmall,
                            suffix = detail.publishedAt.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty(),
                            onOpenUserCenter = onOpenUserCenter
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            items(
                count = segments.size,
                key = { i -> "${segments[i].postId}-${segments[i].segmentIndex}" }
            ) { index ->
                val segment = segments[index]
                val prevIsSamePost = index > 0 && segments[index - 1].postId == segment.postId
                val nextIsSamePost = index < segments.size - 1 && segments[index + 1].postId == segment.postId
                val isLastOfPost = !nextIsSamePost

                if (!prevIsSamePost) {
                    Spacer(Modifier.height(12.dp))
                }

                val topRadius = if (prevIsSamePost) 0.dp else 24.dp
                val bottomRadius = if (nextIsSamePost) 0.dp else 24.dp
                val shape = when {
                    topRadius > 0.dp && bottomRadius > 0.dp -> RoundedCornerShape(24.dp)
                    topRadius > 0.dp -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    bottomRadius > 0.dp -> RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    else -> RoundedCornerShape(0.dp)
                }
                val segKey = "${segment.postId}-${segment.segmentIndex}"
                val cachedHeight = segmentHeights[segKey]

                val layoutDirection = LocalLayoutDirection.current

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
                        .drawBehind {
                            val stroke = 1f.dp.toPx()
                            val half = stroke / 2
                            val outline = shape.createOutline(size, layoutDirection, this)
                            // Draw border as inset path following the exact shape outline
                            val borderPath = when (outline) {
                                is Outline.Rectangle -> Path().apply {
                                    addRect(Rect(half, half, size.width - half, size.height - half))
                                }
                                is Outline.Rounded -> Path().apply {
                                    val rr = outline.roundRect
                                    addRoundRect(
                                        RoundRect(
                                            left = half, top = half,
                                            right = size.width - half, bottom = size.height - half,
                                            topLeftCornerRadius = rr.topLeftCornerRadius,
                                            topRightCornerRadius = rr.topRightCornerRadius,
                                            bottomLeftCornerRadius = rr.bottomLeftCornerRadius,
                                            bottomRightCornerRadius = rr.bottomRightCornerRadius
                                        )
                                    )
                                }
                                is Outline.Generic -> outline.path
                            }
                            drawPath(borderPath, color = CardBorder, style = Stroke(width = stroke))
                            // Overdraw internal edges with background color to hide border
                            if (!prevIsSamePost && nextIsSamePost) {
                                drawRect(CardBackground, Offset(0f, size.height - stroke), Size(size.width, stroke))
                            } else if (prevIsSamePost && nextIsSamePost) {
                                drawRect(CardBackground, Offset(0f, 0f), Size(size.width, stroke))
                                drawRect(CardBackground, Offset(0f, size.height - stroke), Size(size.width, stroke))
                            } else if (prevIsSamePost && !nextIsSamePost) {
                                drawRect(CardBackground, Offset(0f, 0f), Size(size.width, stroke))
                            }
                        }
                ) {
                    when (segment) {
                        is PostSegment.Whole -> {
                            Column(modifier = Modifier.padding(18.dp)) {
                                PostHeader(segment.post, imageLoader, onOpenUserCenter)
                                Spacer(Modifier.height(12.dp))
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
                        is PostSegment.First -> {
                            Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 0.5.dp)) {
                                PostHeader(segment.post, imageLoader, onOpenUserCenter)
                                Spacer(Modifier.height(12.dp))
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
                        is PostSegment.Middle -> {
                            Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 0.5.dp, bottom = 0.5.dp)) {
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
                        is PostSegment.Tail -> {
                            Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 0.5.dp, bottom = 18.dp)) {
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
            if (detail.nextPageUrl != null) {
                item {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onLoadMoreReplies,
                        modifier = Modifier.fillMaxWidth()
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
            avatarSize = 40.dp,
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
            if (index < blocks.lastIndex) Spacer(Modifier.height(10.dp))
        }
        block.imageUrl?.let { imageUrl ->
            var launchSource by remember(imageUrl) { mutableStateOf<PreviewLaunchSource?>(null) }
            val cachedRatio = imageAspectRatios[imageUrl]
            CachedRemoteDisplayImage(
                imageRef = imageUrl,
                imageLoader = imageLoader,
                imageDownloadClient = imageDownloadClient,
                modifier = Modifier
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
            if (index < blocks.lastIndex) Spacer(Modifier.height(10.dp))
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
        Spacer(Modifier.height(12.dp))
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
    Spacer(Modifier.height(14.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            "点评",
            color = TitleText,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.clickable { onRemark(post) }
        )
    }
}
