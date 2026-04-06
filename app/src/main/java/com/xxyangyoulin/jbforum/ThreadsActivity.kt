package com.xxyangyoulin.jbforum

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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.xxyangyoulin.jbforum.ui.components.CachedRemoteDisplayImage
import com.xxyangyoulin.jbforum.ui.components.ClickableName
import com.xxyangyoulin.jbforum.ui.components.ForumMessageAction
import com.xxyangyoulin.jbforum.ui.components.HeroCard
import com.xxyangyoulin.jbforum.ui.components.PagingFooterStatus
import com.xxyangyoulin.jbforum.ui.components.RefreshContainer
import com.xxyangyoulin.jbforum.ui.components.StyledDropdownMenu
import com.xxyangyoulin.jbforum.ui.components.UserIdentity
import com.xxyangyoulin.jbforum.ui.components.appTopBarHaze
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
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

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
    ExperimentalMaterial3Api::class
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val imageLoader = rememberForumImageLoader()
    val imageDownloadClient = rememberForumImageDownloadClient()
    var topMenuExpanded by remember { mutableStateOf(false) }
    var threadRuleMenuExpanded by remember { mutableStateOf(false) }
    var searchDialogOpen by remember { mutableStateOf(false) }
    var historyPanelOpen by remember { mutableStateOf(false) }
    var historyItems by remember { mutableStateOf(ThreadBrowseHistory.load()) }
    var hideComposeFab by remember { mutableStateOf(false) }
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

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSession()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                modifier = Modifier.appTopBarHaze(hazeState),
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
                        Icon(Icons.AutoMirrored.Outlined.ArrowBackIos, contentDescription = null, tint = TitleText)
                    }
                },
                actions = {
                    if (searchKeyword.isNullOrBlank() && (state.selectedBoard?.url ?: boardUrl).contains("forumdisplay")) {
                        Box {
                            IconButton(onClick = { threadRuleMenuExpanded = true }) {
                                Icon(Icons.Outlined.FilterList, contentDescription = null, tint = TitleText)
                            }
                            StyledDropdownMenu(
                                expanded = threadRuleMenuExpanded,
                                onDismissRequest = { threadRuleMenuExpanded = false }
                            ) {
                                ThreadFilterRule.entries.forEach { rule ->
                                    DropdownMenuItem(
                                        text = { Text(rule.label) },
                                        onClick = {
                                            threadRuleMenuExpanded = false
                                            val currentBoard = state.selectedBoard ?: Board(boardTitle, boardDescription, boardUrl)
                                            val targetUrl = applyThreadFilterRule(currentBoard.url, rule)
                                            viewModel.openBoard(
                                                currentBoard.copy(url = targetUrl),
                                                forceRefresh = true
                                            )
                                        }
                                    )
                                }
                            }
                        }
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
                        IconButton(onClick = { topMenuExpanded = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = null, tint = TitleText)
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
                                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("网页打开") },
                                onClick = {
                                    topMenuExpanded = false
                                    val targetUrl = state.selectedBoard?.url ?: boardUrl
                                    if (targetUrl.isNotBlank()) {
                                        context.startActivity(
                                            ThreadWebViewActivity.createIntent(
                                                context = context,
                                                url = targetUrl,
                                                title = state.selectedBoard?.title ?: boardTitle.ifBlank { "帖子列表" }
                                            )
                                        )
                                    } else {
                                        Toast.makeText(context, "当前列表链接为空", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                leadingIcon = { Icon(Icons.Outlined.OpenInBrowser, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("浏览历史") },
                                onClick = {
                                    topMenuExpanded = false
                                    historyItems = ThreadBrowseHistory.load()
                                    historyPanelOpen = true
                                },
                                leadingIcon = { Icon(Icons.Outlined.History, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("本地收藏") },
                                onClick = {
                                    topMenuExpanded = false
                                    context.startActivity(LocalFavoritesActivity.createIntent(context))
                                },
                                leadingIcon = { Icon(Icons.Outlined.FavoriteBorder, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("个人中心") },
                                onClick = {
                                    topMenuExpanded = false
                                    context.startActivity(UserCenterActivity.createIntent(context))
                                },
                                enabled = state.session != null,
                                leadingIcon = { Icon(Icons.Outlined.AccountCircle, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("设置") },
                                onClick = {
                                    topMenuExpanded = false
                                    context.startActivity(android.content.Intent(context, SettingsActivity::class.java))
                                },
                                leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) }
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
                    SmallFloatingActionButton(
                        onClick = viewModel::prepareNewThread,
                        containerColor = AccentGreen,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 6.dp,
                            focusedElevation = 4.dp,
                            hoveredElevation = 4.dp
                        ),
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
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
                    loadingMore = state.threadsLoadingMore,
                    loadMoreError = state.threadsLoadMoreError,
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
                    modifier = Modifier.hazeSource(state = hazeState),
                    drawBehindTopBar = true
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

private enum class ThreadFilterRule(val label: String) {
    Latest("最新"),
    Hot("熱門"),
    HotPost("熱帖"),
    Digest("精華")
}

private fun applyThreadFilterRule(url: String, rule: ThreadFilterRule): String {
    val parsed = url.toHttpUrlOrNull() ?: return url
    val builder = parsed.newBuilder()
        .removeAllQueryParameters("filter")
        .removeAllQueryParameters("orderby")
        .removeAllQueryParameters("digest")
        .removeAllQueryParameters("page")
    when (rule) {
        ThreadFilterRule.Latest -> {
            builder.addQueryParameter("filter", "lastpost")
            builder.addQueryParameter("orderby", "lastpost")
        }
        ThreadFilterRule.Hot -> {
            builder.addQueryParameter("filter", "heat")
            builder.addQueryParameter("orderby", "heats")
        }
        ThreadFilterRule.HotPost -> {
            builder.addQueryParameter("filter", "hot")
        }
        ThreadFilterRule.Digest -> {
            builder.addQueryParameter("filter", "digest")
            builder.addQueryParameter("digest", "1")
        }
    }
    return builder.build().toString()
}

@Composable
internal fun ThreadListScreen(
    board: Board,
    threads: List<ThreadSummary>,
    nextPageUrl: String?,
    loadingMore: Boolean,
    loadMoreError: String?,
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
    LaunchedEffect(board, threads, nextPageUrl, loadingMore, loadMoreError) {
        adapter.submit(
            board = board,
            threads = threads,
            nextPageUrl = nextPageUrl,
            loadingMore = loadingMore,
            loadMoreError = loadMoreError
        )
    }
    DisposableEffect(adapter) {
        onDispose { adapter.release() }
    }

    RefreshContainer(
        refreshing = refreshing,
        onRefresh = onRefresh,
        contentVersion = "${threads.size}-${nextPageUrl.orEmpty()}-${board.url}",
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
                            outRect.top = 0
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
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (thread.isNewbiePost) {
                            ThreadTagBadge(text = "新人帖")
                        }
                        if (thread.hasNewReply) {
                            ThreadTagBadge(text = "New")
                        }
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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    thread.title,
                    modifier = Modifier
                        .weight(1f)
                        .alignByBaseline(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TitleText
                )
                thread.totalPages?.takeIf { it > 1 }?.let { totalPages ->
                    Text(
                        text = "共${totalPages}页",
                        modifier = Modifier.alignByBaseline(),
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

@Composable
private fun ThreadTagBadge(text: String) {
    Text(
        text = text,
        color = AccentGreen,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(AccentGreen.copy(alpha = 0.14f))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    )
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
    private var loadingMore: Boolean = false
    private var loadMoreError: String? = null
    private var autoRequestedNextPageUrl: String? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val prefetchedRefs = linkedSetOf<String>()
    var recyclerView: RecyclerView? = null
    var onBeforeOpenThread: (() -> Unit)? = null

    init {
        setHasStableIds(true)
    }

    fun submit(
        board: Board,
        threads: List<ThreadSummary>,
        nextPageUrl: String?,
        loadingMore: Boolean,
        loadMoreError: String?
    ) {
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
        this.loadingMore = loadingMore
        this.loadMoreError = loadMoreError
        if (this.nextPageUrl != nextPageUrl) {
            autoRequestedNextPageUrl = null
        }
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
        if (threads.isEmpty()) return
        val lastVisible = lm.findLastVisibleItemPosition().coerceAtLeast(0) - 1
        val start = (lastVisible + 1).coerceIn(0, threads.lastIndex)
        val end = (start + 2).coerceAtMost(threads.lastIndex)
        for (index in start..end) {
            threads[index].thumbnailUrls.forEach { imageRef ->
                if (!prefetchedRefs.add(imageRef)) return@forEach
                scope.launch {
                    runCatching { ThreadImageCache.ensureCached(imageDownloadClient, imageRef) }
                }
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

    override fun getItemCount(): Int = 1 + threads.size + if (threads.isNotEmpty()) 1 else 0

    override fun getItemId(position: Int): Long = when (getItemViewType(position)) {
        0 -> Long.MIN_VALUE
        2 -> Long.MAX_VALUE
        else -> threads[position - 1].id.hashCode().toLong()
    }

    override fun getItemViewType(position: Int): Int = when {
        position == 0 -> 0
        position == itemCount - 1 -> 2
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
                PagingFooterStatus(
                    loading = loadingMore,
                    error = loadMoreError,
                    hasMore = nextPageUrl != null,
                    onRetry = onLoadMore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
                if (nextPageUrl != null && !loadingMore && loadMoreError == null && autoRequestedNextPageUrl != nextPageUrl) {
                    autoRequestedNextPageUrl = nextPageUrl
                    holder.composeView.post { onLoadMore() }
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
