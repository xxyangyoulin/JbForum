package com.xxyangyoulin.jbforum

import android.os.Bundle
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.xxyangyoulin.jbforum.ui.components.CachedRemoteDisplayImage
import com.xxyangyoulin.jbforum.ui.components.ClickableName
import com.xxyangyoulin.jbforum.ui.components.HeroCard
import com.xxyangyoulin.jbforum.ui.components.RefreshContainer
import com.xxyangyoulin.jbforum.ui.components.UserIdentity
import com.xxyangyoulin.jbforum.ui.theme.Dimens
import com.xxyangyoulin.jbforum.ui.theme.ForumTheme
import com.xxyangyoulin.jbforum.ui.theme.rememberForumImageDownloadClient
import com.xxyangyoulin.jbforum.ui.theme.rememberForumImageLoader
import com.xxyangyoulin.jbforum.util.openThreadByPreference

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

@OptIn(ExperimentalMaterial3Api::class)
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
                    containerColor = CardBackground,
                    scrolledContainerColor = CardBackground
                ),
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
                    Box {
                        IconButton(onClick = { topMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null, tint = TitleText)
                        }
                        DropdownMenu(
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
                    }
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
    onCompose: () -> Unit
) {
    val context = LocalContext.current
    val canCompose = board.url.contains("forumdisplay")
    val isSearchResult = board.url.contains("search.php?mod=forum")
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val thumbnailResizeWidthPx = remember(configuration, density) {
        with(density) {
            ((configuration.screenWidthDp.dp - 72.dp) / 3).roundToPx()
        }
    }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialFirstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = initialFirstVisibleItemScrollOffset
    )
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
                contentPadding = PaddingValues(Dimens.contentCardPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.contentCardSpacing)
            ) {
            item {
                HeroCard(title = board.title, subtitle = board.description.ifBlank { "讨论列表" })
            }
            items(
                items = threads,
                key = { thread -> thread.id.ifBlank { thread.url } }
            ) { thread ->
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onListScrollChanged(
                                listState.firstVisibleItemIndex,
                                listState.firstVisibleItemScrollOffset
                            )
                            onOpenThread(thread)
                        },
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
                                                .crossfade(150)
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                thread.thumbnailUrls.forEach { url ->
                                    CachedRemoteDisplayImage(
                                        imageRef = url,
                                        imageLoader = imageLoader,
                                        imageDownloadClient = imageDownloadClient,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(108.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFFEDEFF2)),
                                        resizeWidthPx = thumbnailResizeWidthPx
                                    )
                                }
                                repeat(3 - thread.thumbnailUrls.size) {
                                    Spacer(modifier = Modifier.weight(1f))
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
            if (nextPageUrl != null) {
                item {
                    OutlinedButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) {
                        Text("浏览更多", color = TitleText)
                    }
                }
            }
            }
        }
    }
}
