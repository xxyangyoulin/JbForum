package com.xxyangyoulin.jbforum

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import com.xxyangyoulin.jbforum.model.PostSegment
import com.xxyangyoulin.jbforum.model.splitPostSegments
import com.xxyangyoulin.jbforum.util.buildHighlightedText
import com.xxyangyoulin.jbforum.util.openThreadByPreference
import com.xxyangyoulin.jbforum.util.tryNavigate
import com.xxyangyoulin.jbforum.util.readAppVersionName
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
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
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
import com.xxyangyoulin.jbforum.ui.theme.Dimens
import com.xxyangyoulin.jbforum.ui.theme.rememberForumImageDownloadClient
import com.xxyangyoulin.jbforum.ui.theme.rememberForumImageLoader

internal val AppBackground = Color(0xFFF5F6F8)
internal val CardBackground = Color(0xFFFFFFFF)
internal val CardBorder = Color(0xFFE8EBEF)
internal val MutedText = Color(0xFF8B94A1)
internal val TitleText = Color(0xFF1F2937)
internal val AccentGreen = Color(0xFFCB0000)
internal val InputBackground = Color(0xFFF1F3F6)
internal val FloatingButtonEdgePadding = 16.dp
internal val FloatingButtonStackSpacing = 66.dp
private const val LogTag = "JbForum"


class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookiePersistence.init(applicationContext)
        LoginPersistence.init(applicationContext)
        BoardDiskCache.init(applicationContext)
        ThreadListDiskCache.init(applicationContext)
        ThreadDetailDiskCache.init(applicationContext)
        LocalImageFavorites.init(applicationContext)
        ThreadBrowseHistory.init(applicationContext)
        GitHubUpdateChecker.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            viewModel = viewModel()
            ForumApp(viewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (::viewModel.isInitialized) {
            viewModel.refreshSession()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ForumApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var searchDialogOpen by remember { mutableStateOf(false) }
    var topMenuExpanded by remember { mutableStateOf(false) }
    var logoutConfirmOpen by remember { mutableStateOf(false) }
    var historyPanelOpen by remember { mutableStateOf(false) }
    var historyItems by remember { mutableStateOf(ThreadBrowseHistory.load()) }
    var autoUpdateInfo by remember { mutableStateOf<GitHubReleaseInfo?>(null) }
    var autoHasNewVersion by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (GitHubUpdateChecker.shouldAutoCheck()) {
            runCatching { GitHubUpdateChecker.latestRelease() }
                .onSuccess { latest ->
                    GitHubUpdateChecker.recordCheckTime()
                    val currentVersion = readAppVersionName(context)
                    if (GitHubUpdateChecker.hasNewVersion(currentVersion, latest.tagName)) {
                        autoHasNewVersion = true
                        autoUpdateInfo = latest
                    }
                }
        }
    }

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
                                        viewModel.refreshLocalFavorites()
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
                                    text = { Text("网站首页") },
                                    onClick = {
                                        topMenuExpanded = false
                                        val configuredDomain = ForumDomainConfig.getDomain()
                                        if (configuredDomain.isBlank()) {
                                            Toast.makeText(context, "请先在设置中填写论坛域名", Toast.LENGTH_SHORT).show()
                                        } else {
                                            context.startActivity(
                                                ThreadWebViewActivity.createIntent(
                                                    context = context,
                                                    url = "https://$configuredDomain",
                                                    title = "网站首页"
                                                )
                                            )
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("设置") },
                                    onClick = {
                                        topMenuExpanded = false
                                        context.startActivity(android.content.Intent(context, SettingsActivity::class.java))
                                    },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
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
                            tryNavigate {
                                context.startActivity(ThreadsActivity.createIntent(context, it))
                            }
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

        if (autoUpdateInfo != null && autoHasNewVersion) {
            UpdateResultDialog(
                latest = autoUpdateInfo!!,
                currentVersion = readAppVersionName(context),
                hasNewVersion = true,
                onDismiss = { autoUpdateInfo = null }
            )
        }
    }
}


internal class LocalFavoriteViewHolder(
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
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setMinimumHeight(cardView.context.resources.displayMetrics.density.times(120).roundToInt())
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

    private fun applyItemHeight(item: LocalFavoriteImage, columnWidthPx: Int) {
        // 始终从原始文件读取尺寸（原始文件始终存在，缩略图可能未生成）
        val sourceFile = File(item.filePath)
        if (!sourceFile.exists()) {
            contentFrame.layoutParams = contentFrame.layoutParams.apply {
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            return
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(sourceFile.absolutePath, bounds)
        val imgWidth = bounds.outWidth
        val imgHeight = bounds.outHeight
        if (imgWidth <= 0 || imgHeight <= 0) {
            contentFrame.layoutParams = contentFrame.layoutParams.apply {
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            return
        }
        val measuredWidth = contentFrame.width.takeIf { it > 0 } ?: itemView.width
        val colWidth = when {
            columnWidthPx > 0 -> columnWidthPx
            measuredWidth > 0 -> measuredWidth
            else -> 0
        }
        if (colWidth <= 0) {
            contentFrame.layoutParams = contentFrame.layoutParams.apply {
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            return
        }
        val calculatedHeight = (colWidth * imgHeight.toFloat() / imgWidth.toFloat()).toInt()
        contentFrame.layoutParams = contentFrame.layoutParams.apply {
            height = calculatedHeight
        }
    }

    fun bind(
        item: LocalFavoriteImage,
        selected: Boolean,
        columnWidthPx: Int,
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
        // 根据图片实际尺寸预设高度，防止 StaggeredGrid 布局跳动
        applyItemHeight(item, columnWidthPx)
        if (columnWidthPx <= 0 && contentFrame.width <= 0 && itemView.width <= 0) {
            itemView.post {
                if (boundItemId != item.id) return@post
                applyItemHeight(item, 0)
            }
        }
        imageView.setImageDrawable(null)

        val displayRef = LocalImageFavorites.thumbnailOrOriginal(item)
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
            placeholder.visibility = View.GONE
            imageRequestDisposable = imageLoader.enqueue(
                ImageRequest.Builder(itemView.context)
                    .data(File(displayRef))
                    .crossfade(300)
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
            if (selected) android.graphics.Color.parseColor("#CB0000") else android.graphics.Color.TRANSPARENT
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

internal class LocalFavoritesAdapter(
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
    private var columnWidthPx: Int = 0

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

    fun updateColumnWidth(widthPx: Int) {
        if (widthPx <= 0 || widthPx == columnWidthPx) return
        columnWidthPx = widthPx
        notifyDataSetChanged()
        recyclerView?.post { rebindVisiblePlayers() }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocalFavoriteViewHolder {
        val cardView = MaterialCardView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = 0
                rightMargin = 0
                topMargin = 0
                bottomMargin = 0
            }
            radius = 0f
            strokeWidth = 0
            setCardBackgroundColor(android.graphics.Color.WHITE)
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
            columnWidthPx = columnWidthPx,
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
internal fun BoardListScreen(
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
                contentPadding = PaddingValues(Dimens.contentCardPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.contentCardSpacing)
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
                        shape = RoundedCornerShape(Dimens.contentCardCorner),
                        colors = CardDefaults.outlinedCardColors(containerColor = CardBackground),
                        border = androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent)
                    ) {
                        Column(modifier = Modifier.padding(Dimens.contentCardPadding)) {
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
internal fun SearchDialog(
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
internal fun LoadMoreButton(
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
internal fun SelectablePill(text: String, selected: Boolean, onClick: () -> Unit) {
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
internal fun QuickReplyDialog(
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
internal fun ThreadLinksDialog(
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
                            border = androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent)
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
internal fun ThreadHistoryPanel(
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
                                        border = androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent)
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
internal fun LoginDialog(
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
internal fun ComposeDialog(
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
