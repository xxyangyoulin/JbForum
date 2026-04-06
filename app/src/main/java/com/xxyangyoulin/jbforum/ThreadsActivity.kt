package com.xxyangyoulin.jbforum

import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import com.xxyangyoulin.jbforum.ui.components.CachedRemoteDisplayImage
import com.xxyangyoulin.jbforum.ui.components.ClickableName
import com.xxyangyoulin.jbforum.ui.components.ForumMessageAction
import com.xxyangyoulin.jbforum.ui.components.HeroCard
import com.xxyangyoulin.jbforum.ui.components.RefreshContainer
import com.xxyangyoulin.jbforum.ui.components.StyledDropdownMenu
import com.xxyangyoulin.jbforum.ui.components.UserIdentity
import com.xxyangyoulin.jbforum.ui.theme.Dimens
import com.xxyangyoulin.jbforum.ui.theme.ForumTheme
import com.xxyangyoulin.jbforum.ui.theme.rememberForumImageDownloadClient
import com.xxyangyoulin.jbforum.ui.theme.rememberForumImageLoader
import com.xxyangyoulin.jbforum.util.openThreadByPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ThreadsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookiePersistence.init(applicationContext)
        LoginPersistence.init(applicationContext)
        BoardDiskCache.init(applicationContext)
        ThreadListDiskCache.init(applicationContext)
        LocalImageFavorites.init(applicationContext)
        ThreadBrowseHistory.init(applicationContext)
        enableEdgeToEdge()
        val boardTitle = intent.getStringExtra(EXTRA_BOARD_TITLE).orEmpty()
        val boardDescription = intent.getStringExtra(EXTRA_BOARD_DESCRIPTION).orEmpty()
        val boardUrl = intent.getStringExtra(EXTRA_BOARD_URL).orEmpty()
        val searchKeyword = intent.getStringExtra(EXTRA_SEARCH_KEYWORD)
        setContent {
            val viewModel: MainViewModel = viewModel()
            DisposableEffect(Unit) {
                onDispose { viewModel.clearThreadListScroll() }
            }
            ForumTheme {
                ThreadsActivityScreen(
                    viewModel = viewModel,
                    boardTitle = boardTitle,
                    boardDescription = boardDescription,
                    boardUrl = boardUrl,
                    searchKeyword = searchKeyword,
                    onBack = { finish() }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_BOARD_TITLE = "board_title"
        private const val EXTRA_BOARD_DESCRIPTION = "board_description"
        private const val EXTRA_BOARD_URL = "board_url"
        private const val EXTRA_SEARCH_KEYWORD = "search_keyword"

        fun createIntent(context: android.content.Context, board: Board): android.content.Intent {
            return android.content.Intent(context, ThreadsActivity::class.java).apply {
                putExtra(EXTRA_BOARD_TITLE, board.title)
                putExtra(EXTRA_BOARD_DESCRIPTION, board.description)
                putExtra(EXTRA_BOARD_URL, board.url)
            }
        }

        fun createSearchIntent(context: android.content.Context, keyword: String): android.content.Intent {
            return android.content.Intent(context, ThreadsActivity::class.java).apply {
                putExtra(EXTRA_SEARCH_KEYWORD, keyword)
            }
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi::class
)
@Composable
internal fun ThreadsActivityScreen(
    viewModel: MainViewModel,
    boardTitle: String,
    boardDescription: String,
    boardUrl: String,
    searchKeyword: String?,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val imageLoader = rememberForumImageLoader()
    val imageDownloadClient = rememberForumImageDownloadClient()
    var topMenuExpanded by remember { mutableStateOf(false) }
    var searchDialogOpen by remember { mutableStateOf(false) }
    var historyPanelOpen by remember { mutableStateOf(false) }
    var historyItems by remember { mutableStateOf(ThreadBrowseHistory.load()) }
    var hideComposeFab by remember { mutableStateOf(false) }
    val supportsHaze = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val hazeState = remember { HazeState() }
    val imagePreviewLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.let { data ->
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

    LaunchedEffect(boardUrl, searchKeyword) {
        if (!searchKeyword.isNullOrBlank()) {
            viewModel.searchThreads(searchKeyword)
        } else if (boardUrl.isNotBlank()) {
            viewModel.openBoard(Board(boardTitle, boardDescription, boardUrl))
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
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
                } else {
                    Modifier
                },
                title = {
                    Column {
                        Text(
                            text = state.selectedBoard?.title ?: boardTitle.ifBlank { "帖子列表" },
                            maxLines = 1,
                            color = TitleText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = state.session?.username?.let { "已登录 · $it" } ?: "匿名浏览",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedText
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TitleText)
                    }
                },
                actions = {
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
                        IconButton(onClick = { topMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null, tint = TitleText)
                        }
                        StyledDropdownMenu(
                            expanded = topMenuExpanded,
                            onDismissRequest = { topMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("搜索") },
                                onClick = {
                                    topMenuExpanded = false
                                    searchDialogOpen = true
                                },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("浏览历史") },
                                onClick = {
                                    topMenuExpanded = false
                                    historyItems = ThreadBrowseHistory.load()
                                    historyPanelOpen = true
                                },
                                leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("本地收藏") },
                                onClick = {
                                    topMenuExpanded = false
                                    context.startActivity(LocalFavoritesActivity.createIntent(context))
                                },
                                leadingIcon = { Icon(Icons.Default.Collections, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("个人中心") },
                                onClick = {
                                    topMenuExpanded = false
                                    context.startActivity(UserCenterActivity.createIntent(context))
                                },
                                enabled = state.session != null,
                                leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("设置") },
                                onClick = {
                                    topMenuExpanded = false
                                    context.startActivity(android.content.Intent(context, SettingsActivity::class.java))
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimatedVisibility(
                    visible = state.session != null && state.selectedBoard?.url?.contains("forumdisplay") == true && !hideComposeFab,
                    enter = fadeIn() + scaleIn(initialScale = 0.85f),
                    exit = fadeOut() + scaleOut(targetScale = 0.85f)
                ) {
                    FloatingActionButton(
                        onClick = viewModel::prepareNewThread,
                        containerColor = AccentGreen,
                        contentColor = Color.White,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            state.selectedBoard?.let { board ->
                ThreadListScreen(
                    board = board,
                    threads = state.threads,
                    nextPageUrl = state.threadsNextPageUrl,
                    imageLoader = imageLoader,
                    imageDownloadClient = imageDownloadClient,
                    initialFirstVisibleItemIndex = state.threadListFirstVisibleItemIndex,
                    initialFirstVisibleItemScrollOffset = state.threadListFirstVisibleItemScrollOffset,
                    refreshing = state.loading,
                    padding = padding,
                    onRefresh = {
                        if (!searchKeyword.isNullOrBlank()) viewModel.searchThreads(searchKeyword) else viewModel.openBoard(board, forceRefresh = true)
                    },
                    onOpenUserCenter = { uid ->
                        context.startActivity(UserCenterActivity.createIntent(context, uid))
                    },
                    onOpenThread = {
                        openThreadByPreference(context, it)
                    },
                    onListScrollChanged = viewModel::updateThreadListScroll,
                    onLoadMore = viewModel::loadMoreThreads,
                    onNearBottomChanged = { hideComposeFab = it },
                    onCompose = {
                        if (state.session != null) viewModel.prepareNewThread() else viewModel.prepareLogin()
                    },
                    modifier = if (supportsHaze) Modifier.hazeSource(state = hazeState) else Modifier,
                    drawBehindTopBar = supportsHaze
                )
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

    if (state.challenge != null) {
        LoginDialog(
            challenge = state.challenge!!,
            loading = state.loading,
            onDismiss = viewModel::clearChallenge,
            onRefreshCaptcha = viewModel::prepareLogin,
            onLogin = viewModel::login
        )
    }

    if (state.composeForm != null) {
        ComposeDialog(
            form = state.composeForm!!,
            onDismiss = viewModel::clearCompose,
            onSubmit = viewModel::submitNewThread
        )
    }

    if (searchDialogOpen) {
        SearchDialog(
            onDismiss = { searchDialogOpen = false },
            onSearch = {
                searchDialogOpen = false
                context.startActivity(ThreadsActivity.createSearchIntent(context, it))
            }
        )
    }
}

@Composable
internal fun ThreadListScreen(
    board: Board,
    threads: List<ThreadSummary>,
    nextPageUrl: String?,
    imageLoader: ImageLoader,
    imageDownloadClient: okhttp3.OkHttpClient,
    initialFirstVisibleItemIndex: Int,
    initialFirstVisibleItemScrollOffset: Int,
    refreshing: Boolean,
    padding: PaddingValues,
    onRefresh: () -> Unit,
    onOpenUserCenter: (String) -> Unit,
    onOpenThread: (ThreadSummary) -> Unit,
    onListScrollChanged: (Int, Int) -> Unit,
    onLoadMore: () -> Unit,
    onNearBottomChanged: (Boolean) -> Unit,
    onCompose: () -> Unit,
    modifier: Modifier = Modifier,
    drawBehindTopBar: Boolean = false
) {
    val context = LocalContext.current
    val isSearchResult = board.url.contains("search.php?mod=forum")
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val adapter = remember(
        board,
        imageLoader,
        imageDownloadClient,
        isSearchResult,
        onOpenUserCenter,
        onOpenThread,
        onLoadMore
    ) {
        ThreadListAdapter(
            board = board,
            imageLoader = imageLoader,
            imageDownloadClient = imageDownloadClient,
            isSearchResult = isSearchResult,
            onOpenUserCenter = onOpenUserCenter,
            onOpenThread = onOpenThread,
            onLoadMore = onLoadMore
        )
    }
    LaunchedEffect(board, threads, nextPageUrl) {
        adapter.submit(board = board, threads = threads, nextPageUrl = nextPageUrl)
    }
    DisposableEffect(adapter) {
        onDispose { adapter.release() }
    }

    RefreshContainer(
        refreshing = refreshing,
        onRefresh = onRefresh,
        indicatorTopPadding = if (drawBehindTopBar) padding.calculateTopPadding() else 0.dp,
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = padding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                end = padding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                bottom = padding.calculateBottomPadding(),
                top = if (drawBehindTopBar) 0.dp else padding.calculateTopPadding()
            )
    ) {
        AndroidView(
            factory = { androidContext ->
                RecyclerView(androidContext).apply {
                    clipToPadding = false
                    itemAnimator = null
                    layoutManager = LinearLayoutManager(androidContext)
                    setPadding(
                        0,
                        if (drawBehindTopBar) androidContext.resources.displayMetrics.density.times(
                            (Dimens.contentCardPadding + padding.calculateTopPadding()).value
                        ).toInt() else 0,
                        0,
                        androidContext.resources.displayMetrics.density.times(Dimens.contentCardPadding.value).toInt()
                    )
                    addItemDecoration(object : RecyclerView.ItemDecoration() {
                        override fun getItemOffsets(
                            outRect: android.graphics.Rect,
                            view: android.view.View,
                            parent: RecyclerView,
                            state: RecyclerView.State
                        ) {
                            val position = parent.getChildAdapterPosition(view)
                            if (position == RecyclerView.NO_POSITION) return
                            outRect.left = androidContext.resources.displayMetrics.density.times(Dimens.contentCardPadding.value).toInt()
                            outRect.right = outRect.left
                            outRect.bottom = androidContext.resources.displayMetrics.density.times(Dimens.contentCardSpacing.value).toInt()
                            outRect.top = if (position == 0) androidContext.resources.displayMetrics.density.times(Dimens.contentCardPadding.value).toInt() else 0
                        }
                    })
                    this.adapter = adapter.also { threadAdapter ->
                        threadAdapter.recyclerView = this
                    }
                    if (initialFirstVisibleItemIndex > 0 || initialFirstVisibleItemScrollOffset > 0) {
                        post {
                            (layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
                                initialFirstVisibleItemIndex.coerceAtLeast(0),
                                -initialFirstVisibleItemScrollOffset
                            )
                        }
                    }
                    addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                            val lastVisible = lm.findLastVisibleItemPosition()
                            val totalCount = recyclerView.adapter?.itemCount ?: 0
                            if (lastVisible == totalCount - 1 && recyclerView.childCount > 0) {
                                val lastView = recyclerView.getChildAt(recyclerView.childCount - 1) ?: return
                                val thresholdPx = with(density) { 20.dp.roundToPx() }
                                val remaining = recyclerView.height - recyclerView.paddingBottom - lastView.bottom
                                onNearBottomChanged(remaining <= thresholdPx)
                            } else {
                                onNearBottomChanged(false)
                            }
                            adapter.prefetchAroundVisible()
                        }
                    })
                }
            },
            update = { recyclerView ->
                if (recyclerView.adapter !== adapter) {
                    recyclerView.adapter = adapter
                }
                adapter.recyclerView = recyclerView
                adapter.onBeforeOpenThread = {
                    val lm = recyclerView.layoutManager as? LinearLayoutManager
                    if (lm != null) {
                        val first = lm.findFirstVisibleItemPosition().coerceAtLeast(0)
                        val firstView = lm.findViewByPosition(first)
                        onListScrollChanged(first, -(firstView?.top ?: 0))
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ThreadListItemCard(
    thread: ThreadSummary,
    isSearchResult: Boolean,
    imageLoader: ImageLoader,
    imageDownloadClient: okhttp3.OkHttpClient,
    onOpenUserCenter: (String) -> Unit,
    onOpenThread: () -> Unit
) {
    val context = LocalContext.current
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenThread),
        shape = RoundedCornerShape(Dimens.contentCardCorner),
        colors = CardDefaults.outlinedCardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(Dimens.contentCardPadding)) {
            if (isSearchResult) {
                ClickableName(
                    name = thread.author,
                    uid = thread.authorUid,
                    color = TitleText,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    suffix = thread.metaText.ifBlank { "刚刚发布" }.let { " · $it" },
                    onOpenUserCenter = onOpenUserCenter
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserIdentity(
                        imageLoader = imageLoader,
                        imageUrl = thread.authorAvatarUrl,
                        name = thread.author,
                        uid = thread.authorUid,
                        avatarSize = 38.dp,
                        nameTextStyle = MaterialTheme.typography.labelLarge,
                        metaText = thread.publishedAt.ifBlank { "刚刚发布" },
                        onOpenUserCenter = onOpenUserCenter
                    )
                    Spacer(Modifier.weight(1f))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        thread.titleIconUrls.forEach { iconUrl ->
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(iconUrl)
                                    .build(),
                                imageLoader = imageLoader,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    thread.title,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TitleText
                )
                thread.totalPages?.takeIf { it > 1 }?.let { totalPages ->
                    Text(
                        text = "共${totalPages}页",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentGreen
                    )
                }
            }
            if (thread.thumbnailUrls.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val gap = 3.dp
                    val thumbWidth = (maxWidth - gap * 2) / 3
                    val thumbWidthPx = with(LocalDensity.current) { thumbWidth.roundToPx() }
                    Row(
                        modifier = if (thread.thumbnailUrls.size == 3) {
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFEDEFF2))
                        } else {
                            Modifier
                                .wrapContentWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFEDEFF2))
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        thread.thumbnailUrls.forEachIndexed { index, url ->
                            Box(
                                modifier = Modifier
                                    .width(thumbWidth)
                                    .height(108.dp)
                                    .clip(RoundedCornerShape(0.dp))
                                    .background(Color(0xFFEDEFF2))
                            ) {
                                CachedRemoteDisplayImage(
                                    imageRef = url,
                                    imageLoader = imageLoader,
                                    imageDownloadClient = imageDownloadClient,
                                    contentScale = ContentScale.Crop,
                                    placeholderMinHeight = 108.dp,
                                    trackVisibility = false,
                                    modifier = Modifier.fillMaxSize(),
                                    resizeWidthPx = thumbWidthPx
                                )
                            }
                            if (index != thread.thumbnailUrls.lastIndex) {
                                Spacer(modifier = Modifier.width(gap))
                            }
                        }
                    }
                }
            }
            if (!isSearchResult) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = listOf(
                            thread.viewsText.takeIf { it.isNotBlank() }?.let { "浏览 $it" },
                            thread.repliesText.takeIf { it.isNotBlank() }?.let { "回复 $it" },
                            thread.lastReplyAuthor.takeIf { it.isNotBlank() }?.let { author ->
                                listOf(
                                    author,
                                    thread.lastReplyTime.takeIf { it.isNotBlank() }
                                ).joinToString(" · ")
                            }
                        ).filterNotNull().joinToString("   "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText
                    )
                }
            }
        }
    }
}

private class ThreadListAdapter(
    private var board: Board,
    private val imageLoader: ImageLoader,
    private val imageDownloadClient: okhttp3.OkHttpClient,
    private val isSearchResult: Boolean,
    private val onOpenUserCenter: (String) -> Unit,
    private val onOpenThread: (ThreadSummary) -> Unit,
    private val onLoadMore: () -> Unit
) : RecyclerView.Adapter<ThreadListAdapter.ComposeViewHolder>() {
    private var threads: List<ThreadSummary> = emptyList()
    private var nextPageUrl: String? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val prefetchedRefs = linkedSetOf<String>()
    var recyclerView: RecyclerView? = null
    var onBeforeOpenThread: (() -> Unit)? = null

    fun submit(board: Board, threads: List<ThreadSummary>, nextPageUrl: String?) {
        val oldBoard = this.board
        val oldThreads = this.threads
        val oldNextPageUrl = this.nextPageUrl
        val oldCount = itemCount
        val appended =
            board == oldBoard &&
            oldThreads.isNotEmpty() &&
                threads.size >= oldThreads.size &&
                threads.subList(0, oldThreads.size) == oldThreads

        this.board = board
        this.threads = threads
        this.nextPageUrl = nextPageUrl

        if (appended) {
            val insertedCount = threads.size - oldThreads.size
            if (oldNextPageUrl != null) {
                if (nextPageUrl == null) {
                    notifyDataSetChanged()
                    return
                }
                if (insertedCount > 0) {
                    notifyItemRangeInserted(oldThreads.size + 1, insertedCount)
                }
                if (nextPageUrl != oldNextPageUrl) {
                    notifyItemChanged(itemCount - 1)
                }
            } else if (insertedCount > 0) {
                notifyItemRangeInserted(oldThreads.size + 1, insertedCount)
                if (nextPageUrl != null) notifyItemInserted(itemCount - 1)
            } else {
                notifyDataSetChanged()
            }
            recyclerView?.post { prefetchAroundVisible() }
            return
        }

        if (oldCount == 0 || oldCount != itemCount) {
            notifyDataSetChanged()
        } else {
            notifyItemRangeChanged(0, itemCount)
        }
        recyclerView?.post { prefetchAroundVisible() }
    }

    fun prefetchAroundVisible() {
        val recycler = recyclerView ?: return
        val lm = recycler.layoutManager as? LinearLayoutManager ?: return
        val nextThreadIndex = (lm.findLastVisibleItemPosition() + 1) - 1
        val nextThread = threads.getOrNull(nextThreadIndex) ?: return
        nextThread.thumbnailUrls.forEach { imageRef ->
            if (!prefetchedRefs.add(imageRef)) return@forEach
            scope.launch {
                runCatching { ThreadImageCache.ensureCached(imageDownloadClient, imageRef) }
            }
        }
        while (prefetchedRefs.size > 120) {
            val iterator = prefetchedRefs.iterator()
            if (!iterator.hasNext()) break
            iterator.next()
            iterator.remove()
        }
    }

    fun release() {
        scope.cancel()
    }

    override fun getItemCount(): Int = 1 + threads.size + if (nextPageUrl != null) 1 else 0

    override fun getItemViewType(position: Int): Int = when {
        position == 0 -> 0
        position == itemCount - 1 && nextPageUrl != null -> 2
        else -> 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComposeViewHolder {
        val composeView = ComposeView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        }
        return ComposeViewHolder(composeView)
    }

    override fun onBindViewHolder(holder: ComposeViewHolder, position: Int) {
        when (getItemViewType(position)) {
            0 -> holder.composeView.setContent {
                HeroCard(title = board.title, subtitle = board.description.ifBlank { "讨论列表" })
            }
            2 -> holder.composeView.setContent {
                OutlinedButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) {
                    Text("浏览更多", color = TitleText)
                }
            }
            else -> {
                val thread = threads[position - 1]
                holder.composeView.setContent {
                    ThreadListItemCard(
                        thread = thread,
                        isSearchResult = isSearchResult,
                        imageLoader = imageLoader,
                        imageDownloadClient = imageDownloadClient,
                        onOpenUserCenter = onOpenUserCenter,
                        onOpenThread = {
                            onBeforeOpenThread?.invoke()
                            onOpenThread(thread)
                        }
                    )
                }
            }
        }
    }

    class ComposeViewHolder(val composeView: ComposeView) : RecyclerView.ViewHolder(composeView)
}
