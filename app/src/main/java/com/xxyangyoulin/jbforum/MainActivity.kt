package com.xxyangyoulin.jbforum

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.ActionMode
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.card.MaterialCardView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import kotlin.math.roundToInt
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView
import com.xxyangyoulin.jbforum.ui.components.CachedRemoteDisplayImage
import com.xxyangyoulin.jbforum.ui.components.ClickableName
import com.xxyangyoulin.jbforum.ui.components.HeaderPill
import com.xxyangyoulin.jbforum.ui.components.HeroCard
import com.xxyangyoulin.jbforum.ui.components.RefreshContainer
import com.xxyangyoulin.jbforum.ui.components.RemarkCard
import com.xxyangyoulin.jbforum.ui.components.UserIdentity
import com.xxyangyoulin.jbforum.ui.theme.ForumTheme
import com.xxyangyoulin.jbforum.ui.theme.rememberForumImageDownloadClient
import com.xxyangyoulin.jbforum.ui.theme.rememberForumImageLoader

private val AppBackground = Color(0xFFF5F6F8)
private val CardBackground = Color(0xFFFFFFFF)
private val CardBorder = Color(0xFFE8EBEF)
private val MutedText = Color(0xFF8B94A1)
private val TitleText = Color(0xFF1F2937)
private val AccentGreen = Color(0xFFCB0000)
private val InputBackground = Color(0xFFF1F3F6)
private val FloatingButtonEdgePadding = 16.dp
private val FloatingButtonStackSpacing = 66.dp
private const val LogTag = "JbForum"


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookiePersistence.init(applicationContext)
        LoginPersistence.init(applicationContext)
        LocalImageFavorites.init(applicationContext)
        ThreadBrowseHistory.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            ForumApp(viewModel)
        }
    }
}

private fun openThreadByPreference(context: android.content.Context, thread: ThreadSummary) {
    if (ForumDomainConfig.openThreadInWebDefault() && ForumDomainConfig.baseUrl().isNotBlank()) {
        context.startActivity(
            ThreadWebViewActivity.createIntent(
                context = context,
                url = thread.url,
                title = thread.title.ifBlank { "帖子详情" }
            )
        )
    } else {
        context.startActivity(ThreadDetailActivity.createIntent(context, thread))
    }
}

class ThreadsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookiePersistence.init(applicationContext)
        LoginPersistence.init(applicationContext)
        LocalImageFavorites.init(applicationContext)
        ThreadBrowseHistory.init(applicationContext)
        enableEdgeToEdge()
        val boardTitle = intent.getStringExtra(EXTRA_BOARD_TITLE).orEmpty()
        val boardDescription = intent.getStringExtra(EXTRA_BOARD_DESCRIPTION).orEmpty()
        val boardUrl = intent.getStringExtra(EXTRA_BOARD_URL).orEmpty()
        val searchKeyword = intent.getStringExtra(EXTRA_SEARCH_KEYWORD)
        setContent {
            val viewModel: MainViewModel = viewModel()
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

class ThreadDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookiePersistence.init(applicationContext)
        LoginPersistence.init(applicationContext)
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

class UserCenterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookiePersistence.init(applicationContext)
        LoginPersistence.init(applicationContext)
        LocalImageFavorites.init(applicationContext)
        ThreadBrowseHistory.init(applicationContext)
        enableEdgeToEdge()
        val uid = intent.getStringExtra(EXTRA_UID)
        setContent {
            val viewModel: MainViewModel = viewModel()
            ForumTheme {
                UserCenterActivityScreen(
                    viewModel = viewModel,
                    uid = uid,
                    onBack = { finish() }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_UID = "uid"

        fun createIntent(context: android.content.Context, uid: String? = null): android.content.Intent {
            return android.content.Intent(context, UserCenterActivity::class.java).apply {
                putExtra(EXTRA_UID, uid)
            }
        }
    }
}

class LocalFavoritesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookiePersistence.init(applicationContext)
        LoginPersistence.init(applicationContext)
        LocalImageFavorites.init(applicationContext)
        LocalLinkFavorites.init(applicationContext)
        ThreadBrowseHistory.init(applicationContext)
        enableEdgeToEdge()
        val initialTab = intent.getStringExtra(EXTRA_INITIAL_TAB).orEmpty().ifBlank { TAB_IMAGE }
        setContent {
            val viewModel: MainViewModel = viewModel()
            ForumTheme {
                LocalFavoritesActivityScreen(
                    viewModel = viewModel,
                    initialTab = initialTab,
                    onBack = { finish() }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_INITIAL_TAB = "initial_tab"
        const val TAB_IMAGE = "image"
        const val TAB_LINK = "link"

        fun createIntent(context: android.content.Context, initialTab: String = TAB_IMAGE): android.content.Intent {
            return android.content.Intent(context, LocalFavoritesActivity::class.java).apply {
                putExtra(EXTRA_INITIAL_TAB, initialTab)
            }
        }
    }
}

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookiePersistence.init(applicationContext)
        LoginPersistence.init(applicationContext)
        LocalImageFavorites.init(applicationContext)
        ThreadBrowseHistory.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            ForumTheme {
                SettingsActivityScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForumApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var searchDialogOpen by remember { mutableStateOf(false) }
    var topMenuExpanded by remember { mutableStateOf(false) }
    var logoutConfirmOpen by remember { mutableStateOf(false) }
    var historyPanelOpen by remember { mutableStateOf(false) }
    var historyItems by remember { mutableStateOf(ThreadBrowseHistory.load()) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

        ForumTheme {
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
                                text = "JbForum",
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
                                    text = { Text("浏览历史") },
                                    onClick = {
                                        topMenuExpanded = false
                                        historyItems = ThreadBrowseHistory.load()
                                        historyPanelOpen = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
                                )
                                if (state.session == null) {
                                    DropdownMenuItem(
                                        text = { Text("登录") },
                                        onClick = {
                                            topMenuExpanded = false
                                            viewModel.prepareLogin()
                                        },
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                                    )
                                }
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
                                    text = { Text("搜索") },
                                    onClick = {
                                        topMenuExpanded = false
                                        searchDialogOpen = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("本地收藏") },
                                    onClick = {
                                        topMenuExpanded = false
                                        viewModel.refreshLocalFavorites()
                                        context.startActivity(LocalFavoritesActivity.createIntent(context))
                                    },
                                    leadingIcon = { Icon(Icons.Default.Collections, contentDescription = null) }
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
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize(),
                color = AppBackground
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    BoardListScreen(
                        boards = state.boards,
                        refreshing = state.loading,
                        padding = padding,
                        onRefresh = viewModel::refreshBoards,
                        onOpenBoard = {
                            context.startActivity(ThreadsActivity.createIntent(context, it))
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

        if (searchDialogOpen) {
            SearchDialog(
                onDismiss = { searchDialogOpen = false },
                onSearch = {
                    searchDialogOpen = false
                    context.startActivity(ThreadsActivity.createSearchIntent(context, it))
                }
            )
        }

        if (logoutConfirmOpen) {
            AlertDialog(
                onDismissRequest = { logoutConfirmOpen = false },
                title = { Text("注销登录") },
                text = { Text("确认先注销当前账号，再打开登录界面吗？") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            logoutConfirmOpen = false
                            viewModel.logoutAndPrepareLogin()
                        }
                    ) {
                        Text("确认")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { logoutConfirmOpen = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThreadsActivityScreen(
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
    var historyPanelOpen by remember { mutableStateOf(false) }
    var historyItems by remember { mutableStateOf(ThreadBrowseHistory.load()) }
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
                                text = { Text("浏览历史") },
                                onClick = {
                                    topMenuExpanded = false
                                    historyItems = ThreadBrowseHistory.load()
                                    historyPanelOpen = true
                                },
                                leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
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
                                text = { Text("本地收藏") },
                                onClick = {
                                    topMenuExpanded = false
                                    context.startActivity(LocalFavoritesActivity.createIntent(context))
                                },
                                leadingIcon = { Icon(Icons.Default.Collections, contentDescription = null) }
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
                        if (!searchKeyword.isNullOrBlank()) viewModel.searchThreads(searchKeyword) else viewModel.openBoard(board)
                    },
                    onOpenUserCenter = { uid ->
                        context.startActivity(UserCenterActivity.createIntent(context, uid))
                    },
                    onOpenThread = {
                        openThreadByPreference(context, it)
                    },
                    onListScrollChanged = viewModel::updateThreadListScroll,
                    onLoadMore = viewModel::loadMoreThreads,
                    onCompose = {
                        if (state.session != null) viewModel.prepareNewThread() else viewModel.prepareLogin()
                    }
                )
            }
            if (state.session != null && state.selectedBoard?.url?.contains("forumdisplay") == true) {
                FloatingActionButton(
                    onClick = viewModel::prepareNewThread,
                    containerColor = AccentGreen,
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = FloatingButtonEdgePadding)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ThreadDetailActivityScreen(
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
                    IconButton(onClick = onBack) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserCenterActivityScreen(
    viewModel: MainViewModel,
    uid: String?,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val imageLoader = rememberForumImageLoader()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uid, state.session?.uid) {
        if (state.session != null) {
            viewModel.openUserCenter(uid)
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = AppBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardBackground,
                    scrolledContainerColor = CardBackground
                ),
                title = { Text("用户中心", color = TitleText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TitleText)
                    }
                }
            )
        }
    ) { padding ->
        UserCenterScreen(
            profile = state.userProfile,
            isOwnProfile = state.session?.uid?.isNotBlank() == true && state.session?.uid == state.userCenterUid,
            favorites = state.userFavorites,
            favoritesNextPageUrl = state.userFavoritesNextPageUrl,
            threads = state.userThreads,
            threadsNextPageUrl = state.userThreadsNextPageUrl,
            replies = state.userReplies,
            repliesNextPageUrl = state.userRepliesNextPageUrl,
            imageLoader = imageLoader,
            refreshing = state.loading,
            padding = padding,
            onRefresh = { viewModel.openUserCenter(state.userCenterUid.ifBlank { uid }) },
            onLoadMoreFavorites = viewModel::loadMoreUserFavorites,
            onLoadMoreThreads = viewModel::loadMoreUserThreads,
            onLoadMoreReplies = viewModel::loadMoreUserReplies,
            onDeleteFavorite = viewModel::deleteUserFavorite,
            onOpenThread = {
                openThreadByPreference(
                    context,
                    ThreadSummary(
                        id = it.url.substringAfter("tid=").substringBefore('&'),
                        title = it.title,
                        author = state.session?.username.orEmpty(),
                        url = it.url
                    )
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun LocalFavoritesActivityScreen(
    viewModel: MainViewModel,
    initialTab: String,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val imageLoader = rememberForumImageLoader()
    var tab by rememberSaveable { mutableStateOf(if (initialTab == LocalFavoritesActivity.TAB_LINK) LocalFavoritesActivity.TAB_LINK else LocalFavoritesActivity.TAB_IMAGE) }
    var linkItems by remember { mutableStateOf(LocalLinkFavorites.load()) }
    val imagePreviewLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.let { data ->
            if (data.getBooleanExtra(ImagePreviewActivity.EXTRA_REFRESH_FAVORITES, false)) {
                viewModel.refreshLocalFavorites()
                linkItems = LocalLinkFavorites.load()
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

    LaunchedEffect(Unit) {
        viewModel.refreshLocalFavorites()
        linkItems = LocalLinkFavorites.load()
    }

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets.navigationBarsIgnoringVisibility,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBarsIgnoringVisibility,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardBackground,
                    scrolledContainerColor = CardBackground
                ),
                title = { Text("本地收藏", color = TitleText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TitleText)
                    }
                },
                actions = {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { tab = LocalFavoritesActivity.TAB_IMAGE },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                "图片",
                                color = if (tab == LocalFavoritesActivity.TAB_IMAGE) TitleText else MutedText,
                                fontWeight = if (tab == LocalFavoritesActivity.TAB_IMAGE) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                        TextButton(
                            onClick = {
                                tab = LocalFavoritesActivity.TAB_LINK
                                linkItems = LocalLinkFavorites.load()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                "链接",
                                color = if (tab == LocalFavoritesActivity.TAB_LINK) TitleText else MutedText,
                                fontWeight = if (tab == LocalFavoritesActivity.TAB_LINK) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (tab == LocalFavoritesActivity.TAB_IMAGE) {
            LocalFavoritesScreen(
                images = state.localFavoriteImages,
                imageLoader = imageLoader,
                refreshing = state.loading,
                padding = padding,
                onRefresh = viewModel::refreshLocalFavorites,
                onDeleteSelected = viewModel::deleteLocalFavorites,
                onOpenImage = { index, launchSource ->
                    imagePreviewLauncher.launch(
                        ImagePreviewActivity.createIntent(
                            context = context,
                            images = state.localFavoriteImages.map {
                                PreviewImageItem(
                                    imageRef = it.filePath,
                                    sourceThreadTitle = it.sourceThreadTitle,
                                    sourceThreadUrl = it.sourceThreadUrl,
                                    canFavorite = false
                                )
                            },
                            initialIndex = index,
                            launchSource = launchSource
                        ),
                        ActivityOptionsCompat.makeCustomAnimation(context, 0, 0)
                    )
                }
            )
        } else {
            LocalLinkFavoritesContent(
                items = linkItems,
                padding = padding,
                onLinksChanged = { linkItems = LocalLinkFavorites.load() },
                onOpenThread = { item ->
                    val sourceUrl = item.sourceThreadUrl
                    if (sourceUrl.isBlank()) return@LocalLinkFavoritesContent
                    openThreadByPreference(
                        context,
                        ThreadSummary(
                            id = sourceUrl.substringAfter("tid=").substringBefore('&'),
                            title = item.sourceThreadTitle.ifBlank { "帖子详情" },
                            author = "",
                            url = sourceUrl
                        )
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsActivityScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardBackground,
                    scrolledContainerColor = CardBackground
                ),
                title = { Text("设置", color = TitleText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TitleText)
                    }
                }
            )
        }
    ) { padding ->
        SettingsScreen(
            loggedIn = state.session != null,
            padding = padding,
            onCacheCleared = viewModel::refreshLocalFavorites,
            onLogout = viewModel::logout
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
private fun LocalLinkFavoritesContent(
    items: List<LocalFavoriteLink>,
    padding: PaddingValues,
    onLinksChanged: () -> Unit,
    onOpenThread: (LocalFavoriteLink) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf(LinkCategory.ALL) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val filteredItems = remember(items, selectedTab) {
        items.filter { LinkCategory.matches(selectedTab, it.type) }
    }
    val selectedItems = remember(filteredItems, selectedIds) {
        filteredItems.filter { selectedIds.contains(it.id) }
    }
    val selectionMode = selectedIds.isNotEmpty()

    fun toggleSelection(id: String) {
        selectedIds = if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
    }

    fun copyLinks(targets: List<LocalFavoriteLink>) {
        val content = targets.joinToString("\n") { it.value }
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("links", content))
        Toast.makeText(context, "已复制 ${targets.size} 条链接", Toast.LENGTH_SHORT).show()
    }

    fun shareLinks(targets: List<LocalFavoriteLink>) {
        val content = targets.joinToString("\n") { it.value }
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, content)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "分享链接"))
    }

    fun deleteLinks(targets: List<LocalFavoriteLink>) {
        if (targets.isEmpty()) return
        LocalLinkFavorites.remove(targets.map { it.id }.toSet())
        selectedIds = emptySet()
        onLinksChanged()
        Toast.makeText(context, "已删除 ${targets.size} 条收藏", Toast.LENGTH_SHORT).show()
    }

    if (items.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("还没有收藏内容", color = MutedText, textAlign = TextAlign.Center)
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = if (selectionMode) 84.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        LinkCategory.tabs.forEach { tab ->
                            SelectablePill(
                                text = tab,
                                selected = selectedTab == tab,
                                onClick = {
                                    selectedTab = tab
                                    selectedIds = emptySet()
                                }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.heightIn(min = 30.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val allSelected = filteredItems.isNotEmpty() && filteredItems.all { selectedIds.contains(it.id) }
                        TextButton(
                            onClick = {
                                if (filteredItems.isEmpty()) return@TextButton
                                selectedIds = if (allSelected) {
                                    selectedIds - filteredItems.map { it.id }.toSet()
                                } else {
                                    selectedIds + filteredItems.map { it.id }.toSet()
                                }
                            },
                            enabled = filteredItems.isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.heightIn(min = 26.dp)
                        ) {
                            Text(if (allSelected) "取消" else "全选", fontSize = 12.sp)
                        }
                        Text(
                            text = if (selectionMode) "${selectedIds.size}" else "",
                            color = MutedText,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            if (filteredItems.isEmpty()) {
                item {
                    Text(
                        "该分类暂无收藏内容",
                        color = MutedText,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            items(filteredItems, key = { it.id }) { item ->
                val selected = selectedIds.contains(item.id)
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                if (selectionMode) toggleSelection(item.id)
                            },
                            onLongClick = { toggleSelection(item.id) }
                        ),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (selected) AccentGreen.copy(alpha = 0.08f) else CardBackground
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selected) AccentGreen else CardBorder
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 3.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.type, color = MutedText, style = MaterialTheme.typography.labelSmall)
                            Text(
                                java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(item.savedAt)),
                                color = MutedText,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        SelectionContainer {
                            Text(item.value, color = TitleText, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (item.sourceThreadTitle.isNotBlank()) "来源：${item.sourceThreadTitle}" else "",
                                color = MutedText,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { copyLinks(listOf(item)) },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                modifier = Modifier.heightIn(min = 26.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                            }
                            TextButton(
                                onClick = { shareLinks(listOf(item)) },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                modifier = Modifier.heightIn(min = 26.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                            }
                            TextButton(
                                onClick = { onOpenThread(item) },
                                enabled = item.sourceThreadUrl.isNotBlank(),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier.heightIn(min = 26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("原帖", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.size(8.dp)) }
        }

        AnimatedVisibility(
            visible = selectionMode,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CardBackground,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { copyLinks(selectedItems) },
                        enabled = selectedItems.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.heightIn(min = 30.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("复制")
                    }
                    OutlinedButton(
                        onClick = { shareLinks(selectedItems) },
                        enabled = selectedItems.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.heightIn(min = 30.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("分享")
                    }
                    OutlinedButton(
                        onClick = { deleteLinks(selectedItems) },
                        enabled = selectedItems.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.heightIn(min = 30.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("删除")
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableFavoriteText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    onFavorite: (String, String?) -> Unit
) {
    val textSizeSp = style.fontSize.takeIf { it != androidx.compose.ui.unit.TextUnit.Unspecified } ?: 16.sp
    AndroidView(
        factory = {
            androidx.appcompat.widget.AppCompatTextView(it).apply {
                setTextColor(color.toArgb())
                textSize = textSizeSp.value
                setTextIsSelectable(true)
                customSelectionActionModeCallback = object : ActionMode.Callback {
                    private val favoriteId = 0x7f0f1234
                    private fun selectedText(): String {
                        val fullText = this@apply.text?.toString().orEmpty()
                        val start = selectionStart.coerceAtLeast(0)
                        val end = selectionEnd.coerceAtLeast(0)
                        val from = minOf(start, end)
                        val to = maxOf(start, end)
                        return if (to > from && to <= fullText.length) fullText.substring(from, to).trim() else ""
                    }

                    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                        return true
                    }

                    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                        val selected = selectedText()
                        val detectedType = ThreadLinkRecognizer.detectTypeForSelection(selected)
                        val title = if (detectedType != null) "收藏$detectedType" else "收藏文本"
                        val item = menu?.findItem(favoriteId) ?: menu?.add(Menu.NONE, favoriteId, Menu.NONE, title)
                        item?.title = title
                        return true
                    }

                    override fun onActionItemClicked(mode: ActionMode?, item: android.view.MenuItem?): Boolean {
                        if (item?.itemId == favoriteId) {
                            val selected = selectedText()
                            if (selected.isNotBlank()) {
                                onFavorite(selected, ThreadLinkRecognizer.detectTypeForSelection(selected))
                            }
                            mode?.finish()
                            return true
                        }
                        if (item?.itemId == android.R.id.copy) {
                            onTextContextMenuItem(android.R.id.copy)
                            mode?.finish()
                            return true
                        }
                        if (item?.itemId == android.R.id.selectAll) {
                            onTextContextMenuItem(android.R.id.selectAll)
                            return true
                        }
                        return false
                    }

                    override fun onDestroyActionMode(mode: ActionMode?) = Unit
                }
            }
        },
        update = { textView ->
            textView.text = buildHighlightedText(text, AccentGreen.toArgb())
            textView.setTextColor(color.toArgb())
            textView.textSize = textSizeSp.value
        },
        modifier = Modifier.fillMaxWidth()
    )
}

private fun buildHighlightedText(text: String, accentColor: Int): CharSequence {
    val matches = ThreadLinkRecognizer.extractDisplayMatches(text)
    if (matches.isEmpty()) return text
    val spannable = SpannableString(text)
    matches.forEach { range ->
        val start = range.first
        val endExclusive = range.last + 1
        if (start in text.indices && endExclusive <= text.length && endExclusive > start) {
            spannable.setSpan(UnderlineSpan(), start, endExclusive, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(ForegroundColorSpan(accentColor), start, endExclusive, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
    return spannable
}

private class LocalFavoriteViewHolder(
    cardView: MaterialCardView,
    private val imageLoader: ImageLoader,
    private val onClick: (Int, PreviewLaunchSource?) -> Unit,
    private val onLongClick: (Int) -> Unit
) : RecyclerView.ViewHolder(cardView) {
    private val imageView = AppCompatImageView(cardView.context).apply {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        adjustViewBounds = true
        scaleType = ImageView.ScaleType.FIT_CENTER
    }
    private val placeholder = TextView(cardView.context).apply {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            cardView.context.resources.displayMetrics.density.times(180).roundToInt()
        )
        gravity = Gravity.CENTER
        setTextColor(android.graphics.Color.parseColor("#8B94A1"))
        textSize = 13f
    }
    private val selectionOverlay = View(cardView.context).apply {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(android.graphics.Color.parseColor("#2ECB0000"))
        visibility = View.GONE
    }
    private val contentFrame = FrameLayout(cardView.context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        addView(imageView)
        addView(placeholder)
        addView(selectionOverlay)
    }
    private val scope = kotlinx.coroutines.CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var imageRequestDisposable: coil.request.Disposable? = null
    private var prepareJob: Job? = null
    private var gifJob: Job? = null
    private var lockedAspectRatio: Float? = null
    var boundItemId: String = ""
        private set
    var currentGifRef: String? = null
        private set
    private var gifDrawable: GifDrawable? = null

    init {
        cardView.addView(contentFrame)
        cardView.setOnClickListener {
            onClick(bindingAdapterPosition, currentLaunchSource())
        }
        cardView.setOnLongClickListener {
            onLongClick(bindingAdapterPosition)
            true
        }
    }

    fun bind(
        item: LocalFavoriteImage,
        selected: Boolean,
        cachedAspectRatio: Float?,
        onAspectRatioResolved: (Float) -> Unit,
        onPrepared: () -> Unit
    ) {
        boundItemId = item.id
        updateSelectionState(selected)
        imageRequestDisposable?.dispose()
        prepareJob?.cancel()
        gifJob?.cancel()
        gifDrawable?.stop()
        gifDrawable = null
        currentGifRef = null
        imageView.setImageDrawable(null)

        val displayRef = LocalImageFavorites.thumbnailOrOriginal(item)
        val hasCachedAspectRatio = cachedAspectRatio != null && cachedAspectRatio > 0f
        cachedAspectRatio?.takeIf { it > 0f }?.let(::applyAspectRatio)
        val phase = if (displayRef != null) CachedImagePhase.Ready else LocalImageFavorites.phase(item)
        if (displayRef == null) {
            showPlaceholder(
                when (phase) {
                    CachedImagePhase.Loading -> "加载中..."
                    CachedImagePhase.Compressing -> "压缩中..."
                    CachedImagePhase.Ready -> "加载中..."
                }
            )
            prepareJob = scope.launch {
                runCatching { LocalImageFavorites.ensureThumbnail(item) }
                onPrepared()
            }
            return
        }

        if (isGifImage(displayRef)) {
            currentGifRef = displayRef
            showPlaceholder("加载中...")
            gifJob = scope.launch(Dispatchers.IO) {
                val drawable = runCatching { GifDrawable(File(displayRef)) }.getOrNull()
                withContext(Dispatchers.Main) {
                    if (boundItemId != item.id) return@withContext
                    gifDrawable = drawable
                    drawable?.let {
                        if (!hasCachedAspectRatio) {
                            val aspectRatio = it.intrinsicWidth.toFloat() / it.intrinsicHeight.coerceAtLeast(1).toFloat()
                            applyAspectRatio(aspectRatio)
                            onAspectRatioResolved(aspectRatio)
                        }
                        it.stop()
                        placeholder.visibility = View.GONE
                        imageView.setImageDrawable(it)
                        if (isReadyToPlay()) {
                            it.start()
                        }
                    } ?: showPlaceholder("加载中...")
                    onPrepared()
                }
            }
        } else {
            if (!hasCachedAspectRatio) {
                lockedAspectRatio = null
            }
            placeholder.visibility = View.GONE
            if (!hasCachedAspectRatio) {
                gifJob = scope.launch(Dispatchers.IO) {
                    resolveLocalImageAspectRatio(displayRef)?.let { ratio ->
                        withContext(Dispatchers.Main) {
                            if (boundItemId == item.id) {
                                applyAspectRatio(ratio)
                                onAspectRatioResolved(ratio)
                            }
                        }
                    }
                }
            }
            imageRequestDisposable = imageLoader.enqueue(
                ImageRequest.Builder(itemView.context)
                    .data(File(displayRef))
                    .allowHardware(false)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .target(imageView)
                    .build()
            )
        }
    }

    fun updateSelectionState(selected: Boolean) {
        selectionOverlay.visibility = if (selected) View.VISIBLE else View.GONE
        (itemView as MaterialCardView).strokeColor =
            if (selected) android.graphics.Color.parseColor("#CB0000") else android.graphics.Color.parseColor("#E8EBEF")
    }

    fun recycle() {
        imageRequestDisposable?.dispose()
        imageRequestDisposable = null
        prepareJob?.cancel()
        gifJob?.cancel()
        gifDrawable?.stop()
        gifDrawable = null
        currentGifRef = null
        imageView.setImageDrawable(null)
    }

    fun release() {
        recycle()
        scope.cancel()
    }

    private fun showPlaceholder(text: String) {
        imageView.setImageDrawable(null)
        placeholder.text = text
        placeholder.visibility = View.VISIBLE
    }

    private fun applyAspectRatio(aspectRatio: Float) {
        if (aspectRatio <= 0f) return
        if (lockedAspectRatio == null || kotlin.math.abs(lockedAspectRatio!! - aspectRatio) > 0.02f) {
            lockedAspectRatio = aspectRatio
        }
        val contentWidth = sequenceOf(contentFrame.width, itemView.width, itemView.measuredWidth)
            .firstOrNull { it > 0 }
        if (contentWidth == null) {
            contentFrame.post { applyAspectRatio(aspectRatio) }
            return
        }
        val finalAspectRatio = lockedAspectRatio?.takeIf { it > 0f } ?: aspectRatio
        val targetHeight = (contentWidth / finalAspectRatio.coerceAtLeast(0.1f)).roundToInt()
            .coerceAtLeast(1)
        contentFrame.layoutParams = contentFrame.layoutParams.apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = targetHeight
        }
        imageView.layoutParams = (imageView.layoutParams as FrameLayout.LayoutParams).apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        placeholder.layoutParams = (placeholder.layoutParams as FrameLayout.LayoutParams).apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        selectionOverlay.layoutParams = (selectionOverlay.layoutParams as FrameLayout.LayoutParams).apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        contentFrame.requestLayout()
    }

    fun startGif() {
        if (isReadyToPlay()) {
            gifDrawable?.start()
        } else {
            gifDrawable?.stop()
        }
    }

    fun stopGif() {
        gifDrawable?.stop()
    }

    fun isReadyToPlay(): Boolean {
        val rect = android.graphics.Rect()
        val visible = itemView.getGlobalVisibleRect(rect)
        val totalArea = itemView.width * itemView.height
        val visibleArea = rect.width() * rect.height()
        return visible && totalArea > 0 && visibleArea.toFloat() / totalArea.toFloat() >= 0.3f
    }

    private fun resolveLocalImageAspectRatio(path: String): Float? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) return null
        return width.toFloat() / height.toFloat()
    }

    private fun currentLaunchSource(): PreviewLaunchSource? {
        val rect = android.graphics.Rect()
        if (!imageView.getGlobalVisibleRect(rect)) return null
        return PreviewLaunchSource(
            left = rect.left,
            top = rect.top,
            width = rect.width(),
            height = rect.height()
        )
    }
}

private class LocalFavoritesAdapter(
    private val imageLoader: ImageLoader,
    private val onOpenImage: (Int, PreviewLaunchSource?) -> Unit,
    private val onToggleSelection: (String) -> Unit
) : RecyclerView.Adapter<LocalFavoriteViewHolder>() {
    private var items: List<LocalFavoriteImage> = emptyList()
    private var selectedIds: Set<String> = emptySet()
    var recyclerView: RecyclerView? = null
    private val selectionPayload = Any()
    private val scope = kotlinx.coroutines.CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val prefetchedIds = linkedSetOf<String>()
    private var lastVisibleGifIds: List<String> = emptyList()
    private val aspectRatioCache = mutableMapOf<String, Float>()

    init {
        setHasStableIds(true)
    }

    fun submitItems(items: List<LocalFavoriteImage>) {
        this.items = items
        notifyDataSetChanged()
        recyclerView?.post { rebindVisiblePlayers() }
    }

    fun updateSelection(selectedIds: Set<String>) {
        val changedIds = (this.selectedIds - selectedIds) + (selectedIds - this.selectedIds)
        this.selectedIds = selectedIds
        if (changedIds.isEmpty()) return
        changedIds.forEach { id ->
            val index = items.indexOfFirst { it.id == id }
            if (index >= 0) notifyItemChanged(index, selectionPayload)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocalFavoriteViewHolder {
        val cardView = MaterialCardView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                val margin = context.resources.displayMetrics.density.roundToInt()
                leftMargin = margin
                rightMargin = margin
                topMargin = margin
                bottomMargin = margin
            }
            radius = 0f
            strokeWidth = parent.context.resources.displayMetrics.density.roundToInt() * 2
            setCardBackgroundColor(android.graphics.Color.WHITE)
            strokeColor = android.graphics.Color.parseColor("#E8EBEF")
        }
        return LocalFavoriteViewHolder(
            cardView = cardView,
            imageLoader = imageLoader,
            onClick = { position, launchSource ->
                if (position !in items.indices) return@LocalFavoriteViewHolder
                val item = items[position]
                if (selectedIds.isEmpty()) onOpenImage(position, launchSource) else onToggleSelection(item.id)
            },
            onLongClick = { position ->
                if (position !in items.indices) return@LocalFavoriteViewHolder
                onToggleSelection(items[position].id)
            }
        )
    }

    override fun onBindViewHolder(holder: LocalFavoriteViewHolder, position: Int) {
        val item = items[position]
        holder.bind(
            item = item,
            selected = item.id in selectedIds,
            cachedAspectRatio = aspectRatioCache[item.id],
            onAspectRatioResolved = { ratio ->
                if (ratio > 0f) {
                    aspectRatioCache[item.id] = ratio
                }
            },
            onPrepared = {
                recyclerView?.post { rebindVisiblePlayers() }
            }
        )
    }

    override fun onBindViewHolder(
        holder: LocalFavoriteViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(selectionPayload)) {
            holder.updateSelectionState(items[position].id in selectedIds)
            return
        }
        onBindViewHolder(holder, position)
    }

    override fun onViewRecycled(holder: LocalFavoriteViewHolder) {
        holder.recycle()
        recyclerView?.post { rebindVisiblePlayers() }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long = items[position].id.hashCode().toLong()

    fun rebindVisiblePlayers() {
        val recycler = recyclerView ?: return
        val visibleHolders = buildList {
            for (index in 0 until recycler.childCount) {
                val holder = recycler.getChildViewHolder(recycler.getChildAt(index))
                if (holder is LocalFavoriteViewHolder) add(holder)
            }
        }
        val visibleGifIds = visibleHolders
            .filter { it.currentGifRef != null && it.itemView.height > 0 && it.isReadyToPlay() }
            .sortedBy { it.itemView.top }
            .map { it.boundItemId }
        if (visibleGifIds == lastVisibleGifIds) {
            prefetchAfter(lastVisiblePosition(recycler))
            return
        }
        lastVisibleGifIds = visibleGifIds
        visibleHolders.forEach { holder ->
            if (holder.boundItemId in visibleGifIds) holder.startGif() else holder.stopGif()
        }
        prefetchAfter(lastVisiblePosition(recycler))
    }

    fun setScrolling(scrolling: Boolean) {
        recyclerView?.post { rebindVisiblePlayers() }
    }

    fun release() {
        scope.cancel()
    }

    private fun prefetchAfter(lastVisiblePosition: Int) {
        if (lastVisiblePosition < 0) return
        val preloadRange = ((lastVisiblePosition + 1)..(lastVisiblePosition + 1)).filter { it in items.indices }
        preloadRange.forEach { index ->
            val item = items[index]
            if (!prefetchedIds.add(item.id)) return@forEach
            scope.launch {
                runCatching { LocalImageFavorites.ensureThumbnail(item) }
            }
        }
        if (prefetchedIds.size > 24) {
            val keepIds = items
                .drop(lastVisiblePosition.coerceAtLeast(0))
                .take(12)
                .mapTo(linkedSetOf()) { it.id }
            prefetchedIds.retainAll(keepIds)
        }
    }

    private fun lastVisiblePosition(recycler: RecyclerView): Int {
        val layoutManager = recycler.layoutManager
        return when (layoutManager) {
            is LinearLayoutManager -> layoutManager.findLastVisibleItemPosition()
            is StaggeredGridLayoutManager -> layoutManager.findLastVisibleItemPositions(null).maxOrNull() ?: -1
            else -> -1
        }
    }
}

@Composable
private fun ThumbnailGeneratingPlaceholder(
    text: String = "缩略图生成中...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MutedText,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun BoardListScreen(
    boards: List<Board>,
    refreshing: Boolean,
    padding: PaddingValues,
    onRefresh: () -> Unit,
    onOpenBoard: (Board) -> Unit
) {
    val listState = rememberLazyListState()
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    HeroCard(
                        title = "讨论板块",
                        subtitle = "论坛内容已整理成移动端浏览界面"
                    )
                }
                items(boards) { board ->
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenBoard(board) },
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = CardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(board.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TitleText)
                            Spacer(Modifier.height(6.dp))
                            Text(board.description, style = MaterialTheme.typography.bodyMedium, color = MutedText)
                            if (board.latestThreadTitle.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Text("最新 · ${board.latestThreadTitle}", style = MaterialTheme.typography.labelMedium, color = AccentGreen)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadListScreen(
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
    onCompose: () -> Unit
) {
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = CardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                            model = iconUrl,
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

@Composable
private fun SearchDialog(
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit
) {
    var keyword by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("搜索帖子") },
        text = {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                label = { Text("关键词") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onSearch(keyword) }, enabled = keyword.isNotBlank()) {
                Text("搜索")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun SettingsScreen(
    loggedIn: Boolean,
    padding: PaddingValues,
    onCacheCleared: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf(CacheStats(0L, 0L)) }
    var loading by remember { mutableStateOf(true) }
    var confirmClear by remember { mutableStateOf(false) }
    var forumDomain by remember { mutableStateOf(ForumDomainConfig.getDomain()) }
    var openThreadInWeb by remember { mutableStateOf(ForumDomainConfig.openThreadInWebDefault()) }

    suspend fun refreshStats() {
        loading = true
        stats = AppCacheManager.stats()
        loading = false
    }

    LaunchedEffect(Unit) {
        refreshStats()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = CardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("论坛域名", color = TitleText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = forumDomain,
                    onValueChange = { forumDomain = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("例如 www.xxx.com") }
                )
                OutlinedButton(
                    onClick = {
                        runCatching {
                            ForumDomainConfig.setDomain(forumDomain)
                            CookiePersistence.clear()
                        }.onSuccess {
                            Toast.makeText(context, "域名已保存", Toast.LENGTH_SHORT).show()
                        }.onFailure {
                            Toast.makeText(context, it.message ?: "域名保存失败", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("保存域名", color = TitleText)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("默认网页浏览帖子", color = TitleText)
                    Switch(
                        checked = openThreadInWeb,
                        onCheckedChange = {
                            openThreadInWeb = it
                            ForumDomainConfig.setOpenThreadInWebDefault(it)
                        }
                    )
                }
            }
        }
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = CardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("缓存信息", color = TitleText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (loading) {
                    CircularProgressIndicator(color = AccentGreen, strokeWidth = 2.dp)
                } else {
                    Text("当前全部缓存大小：${formatBytes(stats.totalBytes)}", color = TitleText)
                    Text("收藏缓存大小：${formatBytes(stats.favoriteBytes)}", color = TitleText)
                }
            }
        }
        OutlinedButton(
            onClick = { confirmClear = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("清除缓存", color = TitleText)
        }
        if (loggedIn) {
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("注销", color = TitleText)
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清除缓存") },
            text = { Text("将清除所有缩略图、旧 MP4 展示缓存和非本地收藏的图片缓存，本地收藏原图会保留。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        scope.launch {
                            loading = true
                            AppCacheManager.clearNonFavoriteCaches()
                            onCacheCleared()
                            refreshStats()
                        }
                    }
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ThreadDetailScreen(
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = CardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
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
            }
            items(detail.posts) { post ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = CardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
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
                        Spacer(Modifier.height(12.dp))
                        val orderedImages = post.contentBlocks.mapNotNull { it.imageUrl }
                        val previewImages = remember(orderedImages) {
                            orderedImages.map(ThreadImageCache::previewRef)
                        }
                        post.contentBlocks.forEach { block ->
                            block.text?.takeIf { it.isNotBlank() }?.let { text ->
                                SelectableFavoriteText(
                                    text = text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TitleText,
                                    onFavorite = onFavoriteText
                                )
                                Spacer(Modifier.height(10.dp))
                            }
                            block.imageUrl?.let { imageUrl ->
                                var launchSource by remember(imageUrl) { mutableStateOf<PreviewLaunchSource?>(null) }
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
                                        }
                                        .fillMaxWidth()
                                        .background(Color(0xFFEDEFF2))
                                        .clickable {
                                            onOpenImage(
                                                previewImages.map(ThreadImageCache::previewRef),
                                                block.imageIndex ?: orderedImages.indexOf(imageUrl).coerceAtLeast(0),
                                                launchSource
                                            )
                                        },
                                    resizeWidthPx = detailImageResizeWidthPx,
                                    showOriginalDirectly = true
                                )
                                Spacer(Modifier.height(10.dp))
                            }
                        }
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
                }
            }
            if (detail.nextPageUrl != null) {
                item {
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
private fun UserCenterScreen(
    profile: UserProfile?,
    isOwnProfile: Boolean,
    favorites: List<UserThreadItem>,
    favoritesNextPageUrl: String?,
    threads: List<UserThreadItem>,
    threadsNextPageUrl: String?,
    replies: List<UserThreadItem>,
    repliesNextPageUrl: String?,
    imageLoader: ImageLoader,
    refreshing: Boolean,
    padding: PaddingValues,
    onRefresh: () -> Unit,
    onLoadMoreFavorites: () -> Unit,
    onLoadMoreThreads: () -> Unit,
    onLoadMoreReplies: () -> Unit,
    onOpenThread: (UserThreadItem) -> Unit,
    onDeleteFavorite: (UserThreadItem) -> Unit
) {
    var tab by rememberSaveable(isOwnProfile) { mutableStateOf(if (isOwnProfile) "favorite" else "thread") }
    val listState = rememberLazyListState()
    var confirmDeleteFavorite by remember { mutableStateOf<UserThreadItem?>(null) }
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            profile?.let {
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = CardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AuthorAvatar(imageLoader = imageLoader, imageUrl = it.avatarUrl, name = it.username, size = 52.dp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(it.username, color = TitleText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                    Text("UID: ${it.uid}", color = MutedText, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (isOwnProfile) {
                                    SelectablePill("收藏", tab == "favorite") { tab = "favorite" }
                                }
                                SelectablePill("主题", tab == "thread") { tab = "thread" }
                                SelectablePill("回复", tab == "reply") { tab = "reply" }
                                SelectablePill("资料", tab == "profile") { tab = "profile" }
                            }
                        }
                    }
                }
            }
            when (tab) {
                "favorite" -> {
                    items(favorites) { item ->
                        UserThreadCard(
                            item = item,
                            onOpenThread = onOpenThread,
                            showDeleteAction = isOwnProfile,
                            onDelete = { confirmDeleteFavorite = it }
                        )
                    }
                    if (favoritesNextPageUrl != null) {
                        item {
                            LoadMoreButton(
                                text = "加载更多收藏",
                                onClick = onLoadMoreFavorites
                            )
                        }
                    }
                }
                "thread" -> {
                    items(threads) { item ->
                        UserThreadCard(item = item, onOpenThread = onOpenThread)
                    }
                    if (threadsNextPageUrl != null) {
                        item {
                            LoadMoreButton(
                                text = "加载更多主题",
                                onClick = onLoadMoreThreads
                            )
                        }
                    }
                }
                "reply" -> {
                    items(replies) { item ->
                        UserThreadCard(item = item, onOpenThread = onOpenThread)
                    }
                    if (repliesNextPageUrl != null) {
                        item {
                            LoadMoreButton(
                                text = "加载更多回复",
                                onClick = onLoadMoreReplies
                            )
                        }
                    }
                }
                else -> {
                    profile?.let {
                        item { UserProfileCard(title = "基本信息", items = it.basics) }
                        item { UserProfileCard(title = "活跃概况", items = it.activity) }
                        item { UserProfileCard(title = "统计信息", items = it.credits + it.stats) }
                    }
                }
            }
            }
        }
    }
    if (confirmDeleteFavorite != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteFavorite = null },
            title = { Text("删除收藏") },
            text = { Text("确认删除这条收藏吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = confirmDeleteFavorite
                        confirmDeleteFavorite = null
                        if (target != null) onDeleteFavorite(target)
                    }
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteFavorite = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LocalFavoritesScreen(
    images: List<LocalFavoriteImage>,
    imageLoader: ImageLoader,
    refreshing: Boolean,
    padding: PaddingValues,
    onRefresh: () -> Unit,
    onDeleteSelected: (Set<String>) -> Unit,
    onOpenImage: (Int, PreviewLaunchSource?) -> Unit
) {
    val selectedIds = remember { mutableStateListOf<String>() }
    var confirmDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val adapter = remember(context, imageLoader) {
        LocalFavoritesAdapter(
            imageLoader = imageLoader,
            onOpenImage = onOpenImage,
            onToggleSelection = { id ->
                if (id in selectedIds) selectedIds.remove(id) else selectedIds.add(id)
            }
        )
    }
    LaunchedEffect(images) {
        val validIds = images.mapTo(linkedSetOf()) { it.id }
        selectedIds.retainAll(validIds)
    }
    LaunchedEffect(images) {
        adapter.submitItems(images)
        adapter.recyclerView?.post { adapter.rebindVisiblePlayers() }
    }
    LaunchedEffect(selectedIds.size) {
        adapter.updateSelection(selectedIds.toSet())
    }
    DisposableEffect(adapter) {
        onDispose {
            adapter.release()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { androidContext ->
                    RecyclerView(androidContext).apply {
                        clipToPadding = false
                        itemAnimator = null
                        setPadding(
                            (11 * resources.displayMetrics.density).roundToInt(),
                            (11 * resources.displayMetrics.density).roundToInt(),
                            (11 * resources.displayMetrics.density).roundToInt(),
                            (11 * resources.displayMetrics.density).roundToInt()
                        )
                        layoutManager = object : StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL) {
                            override fun supportsPredictiveItemAnimations(): Boolean = false
                        }
                        this.adapter = adapter.also { favoritesAdapter ->
                            favoritesAdapter.recyclerView = this
                            favoritesAdapter.submitItems(images)
                            favoritesAdapter.updateSelection(selectedIds.toSet())
                            post { favoritesAdapter.rebindVisiblePlayers() }
                        }
                        addOnScrollListener(object : RecyclerView.OnScrollListener() {
                            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                                adapter.setScrolling(newState != RecyclerView.SCROLL_STATE_IDLE)
                            }
                        })
                    }
                },
                update = { recyclerView ->
                    adapter.recyclerView = recyclerView
                },
                modifier = Modifier.fillMaxSize()
            )
            if (images.isEmpty()) {
                Text(
                    text = "还没有本地收藏图片",
                    color = MutedText,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            if (selectedIds.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { confirmDelete = true },
                    containerColor = AccentGreen,
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = FloatingButtonEdgePadding, bottom = FloatingButtonEdgePadding)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("取消收藏") },
            text = { Text("确认删除已选中的 ${selectedIds.size} 张收藏图片吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ids = selectedIds.toSet()
                        selectedIds.clear()
                        confirmDelete = false
                        onDeleteSelected(ids)
                    }
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun LoadMoreButton(
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(text)
    }
}

@Composable
private fun SelectablePill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) AccentGreen.copy(alpha = 0.16f) else InputBackground)
            .border(1.dp, if (selected) AccentGreen else CardBorder, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = if (selected) AccentGreen else MutedText, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun UserThreadCard(
    item: UserThreadItem,
    onOpenThread: (UserThreadItem) -> Unit,
    showDeleteAction: Boolean = false,
    onDelete: ((UserThreadItem) -> Unit)? = null
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenThread(item) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.title, color = TitleText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (showDeleteAction && onDelete != null && item.deleteActionUrl != null) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        "删除收藏",
                        color = MutedText,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.clickable { onDelete(item) }
                    )
                }
            }
            if (item.summary.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(item.summary, color = MutedText, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            Spacer(Modifier.height(8.dp))
            Text(listOf(item.board, item.time).filter { it.isNotBlank() }.joinToString(" · "), color = MutedText, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun UserProfileCard(
    title: String,
    items: List<Pair<String, String>>
) {
    if (items.isEmpty()) return
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = TitleText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            items.forEach { (key, value) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(key, color = MutedText, style = MaterialTheme.typography.bodySmall)
                    Text(value, color = TitleText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun AuthorAvatar(
    imageLoader: ImageLoader,
    imageUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp
) {
    val context = LocalContext.current
    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .build(),
            imageLoader = imageLoader,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFFECD6C3))
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFF8A2E2B)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1).ifBlank { "匿" },
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun QuickReplyDialog(
    title: String,
    label: String,
    hint: String = "",
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var reply by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (hint.isNotBlank()) {
                    Text(hint, style = MaterialTheme.typography.bodySmall, color = MutedText)
                }
                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it },
                    label = { Text(label) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(reply) }, enabled = reply.isNotBlank()) {
                Text("发送")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ThreadLinksDialog(
    links: List<DetectedLink>,
    sourceThreadTitle: String,
    sourceThreadUrl: String,
    onDismiss: () -> Unit,
    onOpenFavorites: () -> Unit
) {
    val context = LocalContext.current
    var favoriteIds by remember { mutableStateOf(LocalLinkFavorites.load().map { it.id }.toSet()) }
    var selectedTab by rememberSaveable { mutableStateOf(LinkCategory.ALL) }
    val clipboardManager = remember(context) {
        context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    }
    val filteredLinks = remember(links, selectedTab) {
        links.filter { LinkCategory.matches(selectedTab, it.type) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("识别到的链接（${links.size}）") },
        text = {
            if (links.isEmpty()) {
                Text("当前帖子未识别到可收藏链接")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LinkCategory.tabs.forEach { tab ->
                            SelectablePill(
                                text = tab,
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab }
                            )
                        }
                    }
                    filteredLinks.forEach { link ->
                        val linkId = java.security.MessageDigest.getInstance("SHA-1")
                            .digest(link.value.lowercase().toByteArray())
                            .joinToString("") { "%02x".format(it) }
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.outlinedCardColors(containerColor = CardBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(link.type, color = MutedText, style = MaterialTheme.typography.labelSmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        TextButton(
                                            onClick = {
                                                clipboardManager.setPrimaryClip(
                                                    android.content.ClipData.newPlainText("link", link.value)
                                                )
                                                Toast.makeText(context, "已复制链接", Toast.LENGTH_SHORT).show()
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            modifier = Modifier.heightIn(min = 28.dp)
                                        ) { Text("复制") }
                                        TextButton(
                                            onClick = {
                                                LocalLinkFavorites.add(
                                                    value = link.value,
                                                    type = link.type,
                                                    sourceThreadTitle = sourceThreadTitle,
                                                    sourceThreadUrl = sourceThreadUrl
                                                )
                                                favoriteIds = favoriteIds + linkId
                                                Toast.makeText(context, "链接已收藏", Toast.LENGTH_SHORT).show()
                                            },
                                            enabled = !favoriteIds.contains(linkId),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            modifier = Modifier.heightIn(min = 28.dp)
                                        ) {
                                            Text(if (favoriteIds.contains(linkId)) "已收藏" else "收藏")
                                        }
                                    }
                                }
                                SelectionContainer {
                                    Text(link.value, color = TitleText, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    if (filteredLinks.isEmpty()) {
                        Text(
                            text = "该分类暂无链接",
                            color = MutedText,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = {
                        val content = filteredLinks.joinToString("\n") { it.value }
                        clipboardManager.setPrimaryClip(android.content.ClipData.newPlainText("links", content))
                        Toast.makeText(context, "已复制全部链接", Toast.LENGTH_SHORT).show()
                    },
                    enabled = filteredLinks.isNotEmpty(),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.heightIn(min = 30.dp)
                ) {
                    Text("全部复制", fontSize = 12.sp)
                }
                TextButton(
                    onClick = {
                        var added = 0
                        filteredLinks.forEach { link ->
                            val id = java.security.MessageDigest.getInstance("SHA-1")
                                .digest(link.value.lowercase().toByteArray())
                                .joinToString("") { "%02x".format(it) }
                            if (!favoriteIds.contains(id)) {
                                LocalLinkFavorites.add(
                                    value = link.value,
                                    type = link.type,
                                    sourceThreadTitle = sourceThreadTitle,
                                    sourceThreadUrl = sourceThreadUrl
                                )
                                added++
                            }
                        }
                        favoriteIds = LocalLinkFavorites.load().map { it.id }.toSet()
                        Toast.makeText(context, if (added > 0) "已收藏 $added 条链接" else "全部已收藏", Toast.LENGTH_SHORT).show()
                    },
                    enabled = filteredLinks.isNotEmpty(),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.heightIn(min = 30.dp)
                ) {
                    Text("全部收藏", fontSize = 12.sp)
                }
                TextButton(
                    onClick = onOpenFavorites,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.heightIn(min = 30.dp)
                ) { Text("查看收藏") }
                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.heightIn(min = 30.dp)
                ) { Text("关闭") }
            }
        }
    )
}

@Composable
private fun ThreadHistoryPanel(
    visible: Boolean,
    items: List<ThreadHistoryItem>,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onOpen: (ThreadHistoryItem) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
                    .clickable(onClick = onDismiss)
            )
            AnimatedVisibility(
                visible = visible,
                modifier = Modifier.align(Alignment.CenterEnd),
                enter = slideInHorizontally { fullWidth -> fullWidth },
                exit = slideOutHorizontally { fullWidth -> fullWidth }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.56f),
                    color = CardBackground,
                    tonalElevation = 2.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(start = 12.dp, end = 8.dp, top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "浏览历史",
                                color = TitleText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${items.size}/50",
                                color = MutedText,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        if (items.isEmpty()) {
                            Text(
                                text = "暂无历史",
                                color = MutedText,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(items, key = { it.url }) { item ->
                                    OutlinedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpen(item) },
                                        colors = CardDefaults.outlinedCardColors(containerColor = CardBackground),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                            Text(
                                                text = item.title,
                                                color = TitleText,
                                                maxLines = 2,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
                                                    .format(java.util.Date(item.viewedAt)),
                                                color = MutedText,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = onClear,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text("一键清除")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginDialog(
    challenge: CaptchaChallenge,
    onDismiss: () -> Unit,
    onRefreshCaptcha: () -> Unit,
    onLogin: (String, String, String) -> Unit
) {
    val savedLogin = remember { LoginPersistence.load() }
    var username by rememberSaveable { mutableStateOf(savedLogin.first) }
    var password by rememberSaveable { mutableStateOf(savedLogin.second) }
    var captcha by rememberSaveable { mutableStateOf("") }
    val captchaBitmap = remember(challenge.imageBytes) {
        BitmapFactory.decodeByteArray(challenge.imageBytes, 0, challenge.imageBytes.size)?.asImageBitmap()
    }

    LaunchedEffect(challenge.imageBytes) {
        captcha = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("登录论坛") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("用户名") })
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    visualTransformation = PasswordVisualTransformation()
                )
                Text("验证码")
                captchaBitmap?.let {
                    androidx.compose.foundation.Image(
                        bitmap = it,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(112.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                OutlinedTextField(
                    value = captcha,
                    onValueChange = { captcha = it },
                    label = { Text("输入验证码") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                )
                TextButton(onClick = onRefreshCaptcha) {
                    Text("刷新验证码")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    LoginPersistence.save(username, password)
                    onLogin(username, password, captcha)
                },
                enabled = username.isNotBlank() && password.isNotBlank() && captcha.isNotBlank()
            ) {
                Text("登录")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ComposeDialog(
    form: ComposeForm,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var selectedTypeId by rememberSaveable(form.typeOptions) { mutableStateOf(form.typeOptions.firstOrNull()?.first.orEmpty()) }
    var subject by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发帖") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                form.typeOptions.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTypeId = value }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(if (selectedTypeId == value) Color(0xFF8A2E2B) else Color(0xFFD8C7B8), CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label)
                    }
                }
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("标题") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("正文") }, modifier = Modifier.fillMaxWidth(), minLines = 8)
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(selectedTypeId, subject, message) }, enabled = selectedTypeId.isNotBlank() && subject.isNotBlank() && message.isNotBlank()) {
                Text("提交")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
