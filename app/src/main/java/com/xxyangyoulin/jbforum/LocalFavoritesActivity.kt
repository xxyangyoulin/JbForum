package com.xxyangyoulin.jbforum

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager
import coil.ImageLoader
import coil.compose.AsyncImage
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.outlined.Image
import coil.request.ImageRequest
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.xxyangyoulin.jbforum.ui.theme.ForumTheme
import com.xxyangyoulin.jbforum.ui.theme.PillShape
import com.xxyangyoulin.jbforum.ui.theme.rememberForumImageLoader
import com.xxyangyoulin.jbforum.ui.components.EndOfListIndicator
import com.xxyangyoulin.jbforum.ui.components.RefreshContainer
import com.xxyangyoulin.jbforum.ui.components.appTopBarHaze
import com.xxyangyoulin.jbforum.util.buildHighlightedText
import com.xxyangyoulin.jbforum.util.openThreadByPreference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LocalFavoritesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookiePersistence.init(applicationContext)
        LoginPersistence.init(applicationContext)
        BoardDiskCache.init(applicationContext)
        LocalImageFavorites.init(applicationContext)
        LocalLinkFavorites.init(applicationContext)
        MetaTubeConfig.init(applicationContext)
        LocalCodeMetadataStore.init(applicationContext)
        ThreadBrowseHistory.init(applicationContext)
        enableEdgeToEdge()
        val initialTab = intent.getStringExtra(EXTRA_INITIAL_TAB).orEmpty().ifBlank { TAB_CODE }
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
        const val TAB_CODE = "code"

        fun createIntent(context: android.content.Context, initialTab: String = TAB_CODE): android.content.Intent {
            return android.content.Intent(context, LocalFavoritesActivity::class.java).apply {
                putExtra(EXTRA_INITIAL_TAB, initialTab)
            }
        }
    }
}

internal enum class CodeDisplayMode { DETAIL, POSTER, BACKDROP }

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
internal fun LocalFavoritesActivityScreen(
    viewModel: MainViewModel,
    initialTab: String,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val imageLoader = rememberForumImageLoader()
    var tab by rememberSaveable {
        mutableStateOf(
            when (initialTab) {
                LocalFavoritesActivity.TAB_LINK -> LocalFavoritesActivity.TAB_LINK
                LocalFavoritesActivity.TAB_CODE -> LocalFavoritesActivity.TAB_CODE
                else -> LocalFavoritesActivity.TAB_IMAGE
            }
        )
    }
    val hazeState = remember { HazeState() }
    var linkItems by remember { mutableStateOf<List<LocalFavoriteLink>>(emptyList()) }
    var codeMetadataMap by remember { mutableStateOf<Map<String, LocalCodeMetadata>>(emptyMap()) }
    var scrapingCodes by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingCodes by remember { mutableStateOf<Set<String>>(emptySet()) }
    var failedCodes by remember { mutableStateOf<Set<String>>(emptySet()) }
    var codeDisplayMode by rememberSaveable { mutableStateOf(CodeDisplayMode.DETAIL) }
    var metaTubeSettingsOpen by remember { mutableStateOf(false) }
    var autoScrapeEnabled by rememberSaveable { mutableStateOf(true) }
    var codeRefreshing by remember { mutableStateOf(false) }
    val scrapeStatuses by CodeMetadataScraper.statuses.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    suspend fun refreshLinkItems() {
        linkItems = LocalLinkFavorites.loadSuspend()
    }
    val imagePreviewLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.let { data ->
            if (data.getBooleanExtra(ImagePreviewActivity.EXTRA_REFRESH_FAVORITES, false)) {
                viewModel.refreshLocalFavorites()
                coroutineScope.launch {
                    refreshLinkItems()
                    val codes = linkItems.filter { it.type == LinkCategory.CODE }
                        .map { it.value.trim().uppercase() }
                        .toSet()
                    codeMetadataMap = LocalCodeMetadataStore.loadByCodes(codes)
                }
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
        refreshLinkItems()
        val codes = linkItems.filter { it.type == LinkCategory.CODE }
            .map { it.value.trim().uppercase() }
            .toSet()
        codeMetadataMap = LocalCodeMetadataStore.loadByCodes(codes)
    }

    LaunchedEffect(tab, linkItems, codeMetadataMap, scrapeStatuses, MetaTubeConfig.getServer(), MetaTubeConfig.getToken()) {
        if (tab != LocalFavoritesActivity.TAB_CODE) return@LaunchedEffect
        val server = MetaTubeConfig.getServer()
        if (server.isBlank()) {
            scrapingCodes = emptySet()
            pendingCodes = emptySet()
            failedCodes = emptySet()
            return@LaunchedEffect
        }
        val token = MetaTubeConfig.getToken()
        val codeItems = linkItems.filter { it.type == LinkCategory.CODE }
        val targetCodes = codeItems.map { it.value.trim().uppercase() }.toSet()
        val missingCodes = targetCodes.filterNot { codeMetadataMap.containsKey(it) }
        val freshPendingCodes = missingCodes.filter { scrapeStatuses[it] == null }
        pendingCodes = targetCodes.filter {
            scrapeStatuses[it] == CodeMetadataScraper.Status.PENDING
        }.toSet()
        scrapingCodes = targetCodes.filter { scrapeStatuses[it] == CodeMetadataScraper.Status.RUNNING }.toSet()
        failedCodes = targetCodes.filter {
            !codeMetadataMap.containsKey(it) && scrapeStatuses[it] == CodeMetadataScraper.Status.FAILED
        }.toSet()
        if (autoScrapeEnabled && freshPendingCodes.isNotEmpty()) {
            CodeMetadataScraper.enqueue(freshPendingCodes.toSet(), server, token)
        }
    }

    LaunchedEffect(tab, scrapeStatuses, linkItems) {
        if (tab != LocalFavoritesActivity.TAB_CODE) return@LaunchedEffect
        val codes = linkItems.filter { it.type == LinkCategory.CODE }
            .map { it.value.trim().uppercase() }
            .toSet()
        codeMetadataMap = LocalCodeMetadataStore.loadByCodes(codes)
        pendingCodes = codes.filter { scrapeStatuses[it] == CodeMetadataScraper.Status.PENDING }.toSet()
        scrapingCodes = codes.filter { scrapeStatuses[it] == CodeMetadataScraper.Status.RUNNING }.toSet()
        failedCodes = codes.filter {
            !codeMetadataMap.containsKey(it) && scrapeStatuses[it] == CodeMetadataScraper.Status.FAILED
        }.toSet()
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

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets.navigationBarsIgnoringVisibility,
        topBar = {
            Column(modifier = Modifier.appTopBarHaze(hazeState)) {
                TopAppBar(
                    windowInsets = WindowInsets.statusBarsIgnoringVisibility,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    title = { Text("本地收藏", color = TitleText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBackIos, contentDescription = null, tint = TitleText)
                        }
                    },
                    actions = {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    tab = LocalFavoritesActivity.TAB_CODE
                                    coroutineScope.launch {
                                        autoScrapeEnabled = true
                                        refreshLinkItems()
                                        val codes = linkItems.filter { it.type == LinkCategory.CODE }
                                            .map { it.value.trim().uppercase() }
                                            .toSet()
                                        codeMetadataMap = LocalCodeMetadataStore.loadByCodes(codes)
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    "番号",
                                    color = if (tab == LocalFavoritesActivity.TAB_CODE) TitleText else MutedText,
                                    fontWeight = if (tab == LocalFavoritesActivity.TAB_CODE) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
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
                                    coroutineScope.launch { refreshLinkItems() }
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
                if (tab == LocalFavoritesActivity.TAB_CODE) {
                    Surface(
                        color = Color.Transparent,
                        shadowElevation = 0.dp,
                        modifier = Modifier.appTopBarHaze(hazeState)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { metaTubeSettingsOpen = true },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Settings,
                                        contentDescription = "MetaTube 设置",
                                        tint = MutedText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "番号收藏",
                                    color = TitleText,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (scrapingCodes.isNotEmpty()) {
                                    Text(
                                        text = "刮削中 ${scrapingCodes.size}",
                                        color = MutedText,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                BlurHeaderPill(
                                    text = "详细",
                                    selected = codeDisplayMode == CodeDisplayMode.DETAIL,
                                    onClick = { codeDisplayMode = CodeDisplayMode.DETAIL }
                                )
                                BlurHeaderPill(
                                    text = "海报",
                                    selected = codeDisplayMode == CodeDisplayMode.POSTER,
                                    onClick = { codeDisplayMode = CodeDisplayMode.POSTER }
                                )
                                BlurHeaderPill(
                                    text = "横幅",
                                    selected = codeDisplayMode == CodeDisplayMode.BACKDROP,
                                    onClick = { codeDisplayMode = CodeDisplayMode.BACKDROP }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (tab == LocalFavoritesActivity.TAB_IMAGE) {
            LocalFavoritesScreen(
                images = state.localFavoriteImages,
                imageLoader = imageLoader,
                refreshing = state.loading,
                padding = padding,
                modifier = Modifier.hazeSource(state = hazeState),
                drawBehindTopBar = true,
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
            val openSourceThread: (LocalFavoriteLink) -> Unit = { item ->
                val sourceUrl = item.sourceThreadUrl
                if (sourceUrl.isNotBlank()) {
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
            }
            if (tab == LocalFavoritesActivity.TAB_LINK) {
                LocalLinkFavoritesContent(
                    items = linkItems.filter { it.type != LinkCategory.CODE },
                    padding = padding,
                    modifier = Modifier.hazeSource(state = hazeState),
                    drawBehindTopBar = true,
                    showCategoryTabs = true,
                    onLinksChanged = { coroutineScope.launch { refreshLinkItems() } },
                    onOpenThread = openSourceThread
                )
            } else {
                LocalLinkFavoritesContent(
                    items = linkItems.filter { it.type == LinkCategory.CODE },
                    codeMetadataMap = codeMetadataMap,
                    pendingCodes = pendingCodes,
                    scrapingCodes = scrapingCodes,
                    failedCodes = failedCodes,
                    refreshing = codeRefreshing,
                    padding = padding,
                    modifier = Modifier.hazeSource(state = hazeState),
                    drawBehindTopBar = true,
                    showCategoryTabs = false,
                    codeDisplayMode = codeDisplayMode,
                    showCodeHeaderInContent = false,
                    onCodeDisplayModeChange = { codeDisplayMode = it },
                    emptyHint = "还没有收藏番号",
                    onOpenSettings = { metaTubeSettingsOpen = true },
                    onLinksChanged = {
                        coroutineScope.launch {
                            autoScrapeEnabled = true
                            refreshLinkItems()
                            val codes = linkItems.filter { it.type == LinkCategory.CODE }
                                .map { it.value.trim().uppercase() }
                                .toSet()
                            codeMetadataMap = LocalCodeMetadataStore.loadByCodes(codes)
                        }
                    },
                    onRefresh = {
                        coroutineScope.launch {
                            if (codeRefreshing) return@launch
                            codeRefreshing = true
                            autoScrapeEnabled = true
                            refreshLinkItems()
                            val codes = linkItems.filter { it.type == LinkCategory.CODE }
                                .map { it.value.trim().uppercase() }
                                .toSet()
                            codeMetadataMap = LocalCodeMetadataStore.loadByCodes(codes)
                            val unscrapedCodes = codes.filter {
                                !codeMetadataMap.containsKey(it) && scrapeStatuses[it] == null
                            }.toSet()
                            if (unscrapedCodes.isNotEmpty()) {
                                CodeMetadataScraper.enqueue(
                                    unscrapedCodes,
                                    MetaTubeConfig.getServer(),
                                    MetaTubeConfig.getToken()
                                )
                            }
                            delay(300)
                            codeRefreshing = false
                        }
                    },
                    onDeleteLinks = { deleted ->
                        coroutineScope.launch {
                            LocalCodeMetadataStore.deleteByCodesSuspend(deleted.map { it.value.trim().uppercase() }.toSet())
                            refreshLinkItems()
                            val remainingCodes = linkItems.filter { it.type == LinkCategory.CODE }
                                .map { it.value.trim().uppercase() }
                                .toSet()
                            codeMetadataMap = LocalCodeMetadataStore.loadByCodes(remainingCodes)
                        }
                    },
                    onPreviewImage = { currentItem ->
                        val previewItems = linkItems
                            .filter { it.type == LinkCategory.CODE }
                            .mapNotNull { item ->
                                val metadata = codeMetadataMap[item.value.trim().uppercase()] ?: return@mapNotNull null
                                val imageRef = MetaTubeImageCache.resolve(
                                    MetaTubeApiClient.resolveDisplayBackdropUrl(
                                        server = MetaTubeConfig.getServer(),
                                        provider = metadata.provider,
                                        providerId = metadata.providerId,
                                        backdropUrl = metadata.backdropUrl,
                                        coverUrl = metadata.coverUrl
                                    )
                                ).ifBlank {
                                    MetaTubeImageCache.resolve(
                                        MetaTubeApiClient.resolveDisplayCoverUrl(
                                            server = MetaTubeConfig.getServer(),
                                            provider = metadata.provider,
                                            providerId = metadata.providerId,
                                            coverUrl = metadata.coverUrl,
                                            thumbUrl = metadata.thumbUrl
                                        )
                                    )
                                }
                                if (imageRef.isBlank()) return@mapNotNull null
                                item.id to PreviewImageItem(
                                    imageRef = imageRef,
                                    sourceThreadTitle = metadata.title.ifBlank { metadata.code },
                                    canFavorite = false
                                )
                            }
                        val initialIndex = previewItems.indexOfFirst { it.first == currentItem.id }
                        if (initialIndex < 0) return@LocalLinkFavoritesContent
                        imagePreviewLauncher.launch(
                            ImagePreviewActivity.createIntent(
                                context = context,
                                images = previewItems.map { it.second },
                                initialIndex = initialIndex,
                                launchSource = null
                            ),
                            ActivityOptionsCompat.makeCustomAnimation(context, 0, 0)
                        )
                    },
                    onOpenThread = openSourceThread
                )
            }
        }
    }

    if (metaTubeSettingsOpen) {
        MetaTubeSettingsDialog(
            initialServer = MetaTubeConfig.getServer(),
            initialToken = MetaTubeConfig.getToken(),
            onDismiss = { metaTubeSettingsOpen = false },
            onClearData = {
                coroutineScope.launch {
                    LocalCodeMetadataStore.clearAllSuspend()
                    CodeMetadataScraper.clearStatuses()
                    autoScrapeEnabled = false
                    codeMetadataMap = emptyMap()
                    pendingCodes = emptySet()
                    scrapingCodes = emptySet()
                    failedCodes = emptySet()
                    Toast.makeText(context, "已清除番号刮削数据", Toast.LENGTH_SHORT).show()
                }
            },
            onSave = { server, token ->
                MetaTubeConfig.save(server, token)
                CodeMetadataScraper.clearStatuses()
                autoScrapeEnabled = true
                metaTubeSettingsOpen = false
                coroutineScope.launch {
                    refreshLinkItems()
                    val codes = linkItems.filter { it.type == LinkCategory.CODE }
                        .map { it.value.trim().uppercase() }
                        .toSet()
                    codeMetadataMap = LocalCodeMetadataStore.loadByCodes(codes)
                }
                scrapingCodes = emptySet()
                pendingCodes = emptySet()
                failedCodes = emptySet()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun LocalLinkFavoritesContent(
    items: List<LocalFavoriteLink>,
    padding: PaddingValues,
    modifier: Modifier = Modifier,
    drawBehindTopBar: Boolean = false,
    showCategoryTabs: Boolean = true,
    emptyHint: String = "还没有收藏内容",
    codeMetadataMap: Map<String, LocalCodeMetadata> = emptyMap(),
    pendingCodes: Set<String> = emptySet(),
    scrapingCodes: Set<String> = emptySet(),
    failedCodes: Set<String> = emptySet(),
    refreshing: Boolean = false,
    codeDisplayMode: CodeDisplayMode = CodeDisplayMode.DETAIL,
    showCodeHeaderInContent: Boolean = true,
    onCodeDisplayModeChange: (CodeDisplayMode) -> Unit = {},
    onOpenSettings: (() -> Unit)? = null,
    onDeleteLinks: (List<LocalFavoriteLink>) -> Unit = {},
    onPreviewImage: ((LocalFavoriteLink) -> Unit)? = null,
    onRefresh: () -> Unit = {},
    onLinksChanged: () -> Unit,
    onOpenThread: (LocalFavoriteLink) -> Unit
) {
    val context = LocalContext.current
    val isCodePage = !showCategoryTabs
    val useOverlayHeader = showCategoryTabs || (isCodePage && showCodeHeaderInContent)
    val pageSize = 80
    val preloadThreshold = 16
    var selectedTab by rememberSaveable { mutableStateOf(LinkCategory.ALL) }
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var addCodeDialogOpen by rememberSaveable { mutableStateOf(false) }
    var pendingCode by rememberSaveable { mutableStateOf("") }
    var headerVisible by rememberSaveable { mutableStateOf(true) }
    var headerHeightPx by remember { mutableStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val filteredItems = remember(items, selectedTab, searchQuery, codeMetadataMap) {
        val keyword = searchQuery.trim().uppercase()
        items.filter { item ->
            LinkCategory.matches(selectedTab, item.type) && (keyword.isBlank() || run {
                val value = item.value.trim().uppercase()
                if (value.contains(keyword)) return@run true
                val meta = codeMetadataMap[item.value.trim().uppercase()]
                if (meta != null) {
                    if (meta.title.uppercase().contains(keyword)) return@run true
                    if (meta.actors.any { it.uppercase().contains(keyword) }) return@run true
                    if (meta.releaseDate.contains(keyword)) return@run true
                }
                false
            })
        }
    }
    var loadedCount by remember(filteredItems) { mutableStateOf(filteredItems.size.coerceAtMost(pageSize)) }
    val visibleItems = remember(filteredItems, loadedCount) { filteredItems.take(loadedCount) }
    val listState = rememberLazyListState()
    val selectedItems = remember(filteredItems, selectedIds) {
        filteredItems.filter { selectedIds.contains(it.id) }
    }
    var lastListScrollIndex by remember { mutableStateOf(0) }
    var lastListScrollOffset by remember { mutableStateOf(0) }
    val headerTopPadding = if (drawBehindTopBar) padding.calculateTopPadding() else 0.dp
    val headerContentTopPadding = 12.dp + headerTopPadding
    var searchBarHeightPx by remember { mutableStateOf(0) }
    val overlayContentTopPadding = with(androidx.compose.ui.platform.LocalDensity.current) {
        if (useOverlayHeader) headerHeightPx.toDp() + 8.dp else 0.dp
    }
    val listBackgroundColor = AppBackground.toArgb()
    val recyclerTopPaddingPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        val base = headerTopPadding + overlayContentTopPadding
        val searchExtra = if (isCodePage && searchActive && searchBarHeightPx > 0) {
            searchBarHeightPx.toDp() - headerTopPadding
        } else 0.dp
        (base + searchExtra).roundToPx()
    }

    LaunchedEffect(isCodePage) {
        if (isCodePage) selectionMode = false
    }

    BackHandler(enabled = searchActive && isCodePage) {
        searchActive = false
        searchQuery = ""
    }

    BackHandler(enabled = selectionMode && !isCodePage) {
        selectionMode = false
        selectedIds = emptySet()
    }

    LaunchedEffect(filteredItems, loadedCount, showCategoryTabs) {
        if (!showCategoryTabs) return@LaunchedEffect
        if (loadedCount >= filteredItems.size) return@LaunchedEffect
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@LaunchedEffect
        if (lastVisibleIndex >= visibleItems.size - preloadThreshold) {
            loadedCount = (loadedCount + pageSize).coerceAtMost(filteredItems.size)
        }
    }

    LaunchedEffect(showCategoryTabs, listState) {
        if (!useOverlayHeader) return@LaunchedEffect
        while (true) {
            val index = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            val scrollingUp = index < lastListScrollIndex || (index == lastListScrollIndex && offset < lastListScrollOffset)
            val scrollingDown = index > lastListScrollIndex || (index == lastListScrollIndex && offset > lastListScrollOffset)
            when {
                index == 0 && offset <= 4 -> headerVisible = true
                scrollingUp -> headerVisible = true
                scrollingDown -> headerVisible = false
            }
            lastListScrollIndex = index
            lastListScrollOffset = offset
            delay(50)
        }
    }

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
        onDeleteLinks(targets)
        selectionMode = false
        selectedIds = emptySet()
        onLinksChanged()
        Toast.makeText(context, "已删除 ${targets.size} 条收藏", Toast.LENGTH_SHORT).show()
    }

    val codeAdapter = remember(context) {
        CodeFavoritesAdapter()
    }
    LaunchedEffect(visibleItems, selectedIds, selectionMode, codeDisplayMode, codeMetadataMap, pendingCodes, scrapingCodes, failedCodes, showCategoryTabs) {
        if (!showCategoryTabs) {
            codeAdapter.submit(
                items = visibleItems,
                showEmpty = filteredItems.isEmpty(),
                showEnd = loadedCount >= filteredItems.size && filteredItems.isNotEmpty(),
                selectedIds = selectedIds,
                selectionMode = false,
                displayMode = codeDisplayMode,
                codeMetadataMap = codeMetadataMap,
                pendingCodes = pendingCodes,
                scrapingCodes = scrapingCodes,
                failedCodes = failedCodes,
                onToggleSelection = ::toggleSelection,
                onCopy = { copyLinks(listOf(it)) },
                onShare = { shareLinks(listOf(it)) },
                onDelete = { deleteLinks(listOf(it)) },
                onOpenThread = onOpenThread,
                onPreviewImage = onPreviewImage
            )
        }
    }

    LaunchedEffect(searchQuery) {
        loadedCount = filteredItems.size.coerceAtMost(pageSize)
        codeAdapter.recyclerView?.scrollToPosition(0)
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
            Text(emptyHint, color = MutedText, textAlign = TextAlign.Center)
        }
        return
    }

    RefreshContainer(
        refreshing = refreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = padding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                end = padding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                bottom = padding.calculateBottomPadding(),
                top = if (drawBehindTopBar) 0.dp else padding.calculateTopPadding()
            ),
        indicatorTopPadding = if (showCategoryTabs) overlayContentTopPadding else (headerTopPadding + overlayContentTopPadding)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (showCategoryTabs) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            top = overlayContentTopPadding,
                            end = 12.dp,
                            bottom = when {
                                selectionMode -> 84.dp
                                !showCategoryTabs -> 84.dp
                                else -> 12.dp
                            }
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                        items(visibleItems, key = { it.id }) { item ->
                            val selected = selectedIds.contains(item.id)
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (selectionMode) {
                                            Modifier.combinedClickable(
                                                onClick = { toggleSelection(item.id) },
                                                onLongClick = { toggleSelection(item.id) }
                                            )
                                        } else {
                                            Modifier
                                        }
                                    ),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (selected) AccentGreen.copy(alpha = 0.08f) else CardBackground
                                ),
                                border = BorderStroke(0.dp, Color.Transparent)
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
                                            SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(item.savedAt)),
                                            color = MutedText,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    if (selectionMode) {
                                        Text(item.value, color = TitleText, style = MaterialTheme.typography.bodyMedium)
                                    } else {
                                        SelectionContainer {
                                            Text(item.value, color = TitleText, style = MaterialTheme.typography.bodyMedium)
                                        }
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
                                            enabled = !selectionMode,
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                            modifier = Modifier.heightIn(min = 26.dp)
                                        ) {
                                            Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                        }
                                        TextButton(
                                            onClick = { shareLinks(listOf(item)) },
                                            enabled = !selectionMode,
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                            modifier = Modifier.heightIn(min = 26.dp)
                                        ) {
                                            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                                        }
                                        TextButton(
                                            onClick = { onOpenThread(item) },
                                            enabled = !selectionMode && item.sourceThreadUrl.isNotBlank(),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                            modifier = Modifier.heightIn(min = 26.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.ArrowBackIos,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (loadedCount >= filteredItems.size && filteredItems.isNotEmpty()) {
                            item { EndOfListIndicator(modifier = Modifier.padding(vertical = 8.dp)) }
                        }
                        item { Spacer(modifier = Modifier.size(8.dp)) }
                    }
                } else {
                    AndroidView(
                    factory = { androidContext ->
                        RecyclerView(androidContext).apply {
                            itemAnimator = null
                            clipToPadding = false
                            setBackgroundColor(listBackgroundColor)
                            val gridSpacingPx = (3 * resources.displayMetrics.density).roundToInt()
                            val halfGridSpacingPx = gridSpacingPx / 2
                            layoutManager = if (codeDisplayMode == CodeDisplayMode.DETAIL) {
                                LinearLayoutManager(androidContext)
                            } else {
                                createCodeGridLayoutManager(androidContext, codeAdapter)
                            }
                            addItemDecoration(object : RecyclerView.ItemDecoration() {
                                override fun getItemOffsets(
                                    outRect: android.graphics.Rect,
                                    view: android.view.View,
                                    parent: RecyclerView,
                                    state: RecyclerView.State
                                ) {
                                    val layoutManager = parent.layoutManager as? GridLayoutManager ?: run {
                                        outRect.set(0, 0, 0, 0)
                                        return
                                    }
                                    if (layoutManager.spanCount != 2) {
                                        outRect.set(0, 0, 0, 0)
                                        return
                                    }
                                    val position = parent.getChildAdapterPosition(view)
                                    if (position == RecyclerView.NO_POSITION) {
                                        outRect.set(0, 0, 0, 0)
                                        return
                                    }
                                    val spanIndex = position % 2
                                    if (spanIndex == 0) {
                                        outRect.set(0, 0, halfGridSpacingPx, gridSpacingPx)
                                    } else {
                                        outRect.set(halfGridSpacingPx, 0, 0, gridSpacingPx)
                                    }
                                }
                            })
                            setPadding(
                                0,
                                recyclerTopPaddingPx,
                                0,
                                (
                                    (if (selectionMode) 84 else 84) * resources.displayMetrics.density
                                    ).roundToInt()
                            )
                            adapter = codeAdapter.also { it.recyclerView = this }
                            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                                    if (useOverlayHeader) {
                                        when {
                                            !recyclerView.canScrollVertically(-1) -> headerVisible = true
                                            dy < 0 -> headerVisible = true
                                            dy > 0 -> headerVisible = false
                                        }
                                    }
                                    if (loadedCount >= filteredItems.size) return
                                    val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                                    val lastVisible = lm.findLastVisibleItemPosition()
                                    if (lastVisible >= visibleItems.size - preloadThreshold) {
                                        loadedCount = (loadedCount + pageSize).coerceAtMost(filteredItems.size)
                                    }
                                }
                            })
                        }
                    },
                    update = { recyclerView ->
                        recyclerView.setBackgroundColor(listBackgroundColor)
                        val needGrid = codeDisplayMode != CodeDisplayMode.DETAIL
                        val current = recyclerView.layoutManager
                        if (needGrid && current !is GridLayoutManager) {
                            recyclerView.layoutManager = createCodeGridLayoutManager(recyclerView.context, codeAdapter)
                            recyclerView.scrollToPosition(0)
                        } else if (!needGrid && current is GridLayoutManager) {
                            recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)
                            recyclerView.scrollToPosition(0)
                        }
                        recyclerView.setPadding(
                            recyclerView.paddingLeft,
                            recyclerTopPaddingPx,
                            recyclerView.paddingRight,
                            ((if (selectionMode) 84 else 84) * recyclerView.resources.displayMetrics.density).roundToInt()
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                    )
                }
            }

            if (isCodePage && searchActive) {
                Surface(
                    color = CardBackground.copy(alpha = 0.96f),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .onSizeChanged { searchBarHeightPx = it.height }
                        .padding(
                            top = headerTopPadding,
                            bottom = 8.dp
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MutedText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        val focusRequester = remember { FocusRequester() }
                        DisposableEffect(Unit) {
                            focusRequester.requestFocus()
                            onDispose {}
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TitleText),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "搜索番号、标题、演员...",
                                            color = MutedText,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Cancel,
                                    contentDescription = null,
                                    tint = MutedText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (useOverlayHeader) {
                val headerProgress by animateFloatAsState(
                    targetValue = if (headerVisible) 1f else 0f,
                    animationSpec = tween(durationMillis = 220),
                    label = "favorites_header_progress"
                )
                Surface(
                    color = CardBackground.copy(alpha = 0.96f),
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .onSizeChanged { headerHeightPx = it.height }
                        .graphicsLayer {
                            alpha = headerProgress
                            translationY = -(1f - headerProgress) * headerHeightPx * 0.85f
                        }
                ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = headerContentTopPadding,
                        bottom = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (showCategoryTabs) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(end = 84.dp)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            LinkCategory.tabs.forEach { tab ->
                                SelectablePill(
                                    text = tab,
                                    selected = selectedTab == tab,
                                    onClick = {
                                        selectedTab = tab
                                        selectionMode = false
                                        selectedIds = emptySet()
                                    }
                                )
                            }
                        }
                    } else if (showCodeHeaderInContent) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(end = 148.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (onOpenSettings != null) {
                                IconButton(
                                    onClick = onOpenSettings,
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Settings,
                                        contentDescription = "MetaTube 设置",
                                        tint = MutedText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = "番号收藏",
                                color = TitleText,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (scrapingCodes.isNotEmpty()) {
                                Text(
                                    text = "刮削中 ${scrapingCodes.size}",
                                    color = MutedText,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .heightIn(min = 30.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCodePage && showCodeHeaderInContent) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                SelectablePill(
                                    text = "详细",
                                    selected = codeDisplayMode == CodeDisplayMode.DETAIL,
                                    onClick = { onCodeDisplayModeChange(CodeDisplayMode.DETAIL) }
                                )
                                SelectablePill(
                                    text = "海报",
                                    selected = codeDisplayMode == CodeDisplayMode.POSTER,
                                    onClick = { onCodeDisplayModeChange(CodeDisplayMode.POSTER) }
                                )
                                SelectablePill(
                                    text = "横幅",
                                    selected = codeDisplayMode == CodeDisplayMode.BACKDROP,
                                    onClick = { onCodeDisplayModeChange(CodeDisplayMode.BACKDROP) }
                                )
                            }
                        } else {
                            if (!selectionMode) {
                                TextButton(
                                    onClick = { selectionMode = true },
                                    enabled = filteredItems.isNotEmpty(),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    modifier = Modifier.heightIn(min = 26.dp)
                                ) {
                                    Text("选择", fontSize = 12.sp)
                                }
                            } else {
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
                                TextButton(
                                    onClick = {
                                        selectionMode = false
                                        selectedIds = emptySet()
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    modifier = Modifier.heightIn(min = 26.dp)
                                ) {
                                    Text("完成", fontSize = 12.sp)
                                }
                                Text(
                                    text = selectedIds.size.toString(),
                                    color = MutedText,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
                }
            }

            if (!showCategoryTabs && !selectionMode) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            searchActive = !searchActive
                            if (!searchActive) searchQuery = ""
                        },
                        containerColor = if (searchActive) AccentGreen else CardBackground,
                        contentColor = if (searchActive) Color.White else TitleText
                    ) {
                        Icon(
                            if (searchActive) Icons.Outlined.Cancel else Icons.Outlined.Search,
                            contentDescription = if (searchActive) "关闭搜索" else "搜索番号"
                        )
                    }
                    SmallFloatingActionButton(
                        onClick = { addCodeDialogOpen = true },
                        containerColor = CardBackground,
                        contentColor = TitleText
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "手动添加番号")
                    }
                }
            }

            if (selectionMode) {
                Surface(
                    shape = PillShape,
                    color = CardBackground,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    IconButton(
                        onClick = {
                            copyLinks(selectedItems)
                            selectionMode = false
                            selectedIds = emptySet()
                        },
                        enabled = selectedItems.isNotEmpty()
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = {
                            shareLinks(selectedItems)
                            selectionMode = false
                            selectedIds = emptySet()
                        },
                        enabled = selectedItems.isNotEmpty()
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { deleteLinks(selectedItems) },
                        enabled = selectedItems.isNotEmpty()
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    }
                }
            }
        }
    }

    if (!showCategoryTabs && addCodeDialogOpen) {
        AlertDialog(
            onDismissRequest = { addCodeDialogOpen = false },
            title = { Text("手动添加番号") },
            text = {
                OutlinedTextField(
                    value = pendingCode,
                    onValueChange = { pendingCode = it },
                    singleLine = true,
                    placeholder = { Text("请输入番号") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val input = pendingCode.trim()
                        if (input.isBlank()) {
                            Toast.makeText(context, "请输入番号", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        val normalized = input.uppercase().replace('_', '-').replace(' ', '-')
                        val detectedType = ThreadLinkRecognizer.detectTypeForSelection(normalized)
                        if (detectedType != LinkCategory.CODE) {
                            Toast.makeText(context, "请输入有效番号", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        runCatching {
                            LocalLinkFavorites.add(
                                value = normalized,
                                type = LinkCategory.CODE
                            )
                        }.onSuccess {
                            onLinksChanged()
                            pendingCode = ""
                            addCodeDialogOpen = false
                            Toast.makeText(context, "已添加番号", Toast.LENGTH_SHORT).show()
                        }.onFailure {
                            Toast.makeText(context, it.message ?: "添加失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(onClick = { addCodeDialogOpen = false }) {
                    Text("取消")
                }
            }
        )
    }
}

private class CodeFavoritesAdapter : RecyclerView.Adapter<CodeFavoritesAdapter.CodeViewHolder>() {
    private companion object {
        const val ID_EMPTY = Long.MIN_VALUE + 1
        const val ID_END = Long.MIN_VALUE + 2
        const val TYPE_EMPTY = 0
        const val TYPE_END = 1
        const val TYPE_DETAIL = 2
        const val TYPE_POSTER = 3
        const val TYPE_BACKDROP = 4
    }
    var recyclerView: RecyclerView? = null
    private var items: List<LocalFavoriteLink> = emptyList()
    private var showEmpty: Boolean = false
    private var showEnd: Boolean = false
    private var selectedIds: Set<String> = emptySet()
    private var selectionMode: Boolean = false
    private var displayMode: CodeDisplayMode = CodeDisplayMode.DETAIL
    private var metadataMap: Map<String, LocalCodeMetadata> = emptyMap()
    private var pendingCodes: Set<String> = emptySet()
    private var scrapingCodes: Set<String> = emptySet()
    private var failedCodes: Set<String> = emptySet()
    private var onToggleSelection: (String) -> Unit = {}
    private var onCopy: (LocalFavoriteLink) -> Unit = {}
    private var onShare: (LocalFavoriteLink) -> Unit = {}
    private var onDelete: (LocalFavoriteLink) -> Unit = {}
    private var onOpenThread: (LocalFavoriteLink) -> Unit = {}
    private var onPreviewImage: ((LocalFavoriteLink) -> Unit)? = null

    init {
        setHasStableIds(true)
    }

    fun submit(
        items: List<LocalFavoriteLink>,
        showEmpty: Boolean,
        showEnd: Boolean,
        selectedIds: Set<String>,
        selectionMode: Boolean,
        displayMode: CodeDisplayMode,
        codeMetadataMap: Map<String, LocalCodeMetadata>,
        pendingCodes: Set<String>,
        scrapingCodes: Set<String>,
        failedCodes: Set<String>,
        onToggleSelection: (String) -> Unit,
        onCopy: (LocalFavoriteLink) -> Unit,
        onShare: (LocalFavoriteLink) -> Unit,
        onDelete: (LocalFavoriteLink) -> Unit,
        onOpenThread: (LocalFavoriteLink) -> Unit,
        onPreviewImage: ((LocalFavoriteLink) -> Unit)?
    ) {
        this.items = items
        this.showEmpty = showEmpty
        this.showEnd = showEnd
        this.selectedIds = selectedIds
        this.selectionMode = selectionMode
        this.displayMode = displayMode
        this.metadataMap = codeMetadataMap
        this.pendingCodes = pendingCodes
        this.scrapingCodes = scrapingCodes
        this.failedCodes = failedCodes
        this.onToggleSelection = onToggleSelection
        this.onCopy = onCopy
        this.onShare = onShare
        this.onDelete = onDelete
        this.onOpenThread = onOpenThread
        this.onPreviewImage = onPreviewImage
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): CodeViewHolder {
        val composeView = ComposeView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
        }
        return CodeViewHolder(composeView)
    }

    override fun getItemCount(): Int {
        if (showEmpty) return 1
        return items.size + if (showEnd) 1 else 0
    }

    override fun getItemViewType(position: Int): Int {
        if (showEmpty) return TYPE_EMPTY
        if (showEnd && position == items.size) return TYPE_END
        return when (displayMode) {
            CodeDisplayMode.DETAIL -> TYPE_DETAIL
            CodeDisplayMode.POSTER -> TYPE_POSTER
            CodeDisplayMode.BACKDROP -> TYPE_BACKDROP
        }
    }

    override fun getItemId(position: Int): Long {
        if (showEmpty) return ID_EMPTY
        if (showEnd && position == items.size) return ID_END
        return items[position].id.hashCode().toLong()
    }

    override fun onBindViewHolder(holder: CodeViewHolder, position: Int) {
        if (showEmpty) {
            holder.composeView.setContent {
                Text(
                    "该分类暂无收藏内容",
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            }
            return
        }
        if (showEnd && position == items.size) {
            holder.composeView.setContent {
                EndOfListIndicator(modifier = Modifier.padding(vertical = 8.dp))
            }
            return
        }
        val item = items[position]
        val normalizedCode = item.value.trim().uppercase()
        val metadata = metadataMap[normalizedCode]
        val isPending = pendingCodes.contains(normalizedCode)
        val isScraping = scrapingCodes.contains(normalizedCode)
        val isFailed = failedCodes.contains(normalizedCode)
        val selected = selectedIds.contains(item.id)
        holder.composeView.setContent {
            CodeFavoriteCard(
                item = item,
                selected = selected,
                selectionMode = selectionMode,
                displayMode = displayMode,
                metadata = metadata,
                isPending = isPending,
                isScraping = isScraping,
                isFailed = isFailed,
                onToggleSelection = { onToggleSelection(item.id) },
                onCopy = { onCopy(item) },
                onShare = { onShare(item) },
                onDelete = { onDelete(item) },
                onOpenThread = { onOpenThread(item) },
                onPreviewImage = onPreviewImage
            )
        }
    }

    class CodeViewHolder(val composeView: ComposeView) : RecyclerView.ViewHolder(composeView)
}

private fun createCodeGridLayoutManager(context: android.content.Context, adapter: CodeFavoritesAdapter): GridLayoutManager {
    return GridLayoutManager(context, 2).apply {
        spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val itemId = adapter.getItemId(position)
                return if (itemId == Long.MIN_VALUE + 1 || itemId == Long.MIN_VALUE + 2) 2 else 1
            }
        }
    }
}

@Composable
private fun BlurHeaderPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) {
        AccentGreen.copy(alpha = 0.14f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }
    val borderColor = if (selected) {
        AccentGreen.copy(alpha = 0.45f)
    } else {
        CardBorder.copy(alpha = 0.28f)
    }
    val contentColor = if (selected) AccentGreen else MutedText
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(containerColor)
            .border(1.dp, borderColor, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun CodeImagePlaceholder(modifier: Modifier = Modifier) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val bgColor = if (isLight) Color(0xFFE8EDF3) else Color(0xFF262624)
    val contentColor = MutedText

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        val isWide = maxWidth / maxHeight > 1.2f
        val iconSize = if (isWide) 28.dp else 40.dp
        val innerPadding = if (isWide) 8.dp else 12.dp
        val verticalSpacing = if (isWide) 3.dp else 6.dp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            modifier = Modifier.padding(innerPadding)
        ) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(iconSize)
            )
            Text(
                "暂无封面",
                color = contentColor,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CodeFavoriteCard(
    item: LocalFavoriteLink,
    selected: Boolean,
    selectionMode: Boolean,
    displayMode: CodeDisplayMode,
    metadata: LocalCodeMetadata?,
    isPending: Boolean,
    isScraping: Boolean,
    isFailed: Boolean,
    onToggleSelection: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onOpenThread: () -> Unit,
    onPreviewImage: ((LocalFavoriteLink) -> Unit)?
) {
    val isDetailMode = displayMode == CodeDisplayMode.DETAIL
    val previewBackdropModel = metadata?.let {
        MetaTubeImageCache.resolve(
            MetaTubeApiClient.resolveDisplayBackdropUrl(
                server = MetaTubeConfig.getServer(),
                provider = it.provider,
                providerId = it.providerId,
                backdropUrl = it.backdropUrl,
                coverUrl = it.coverUrl
            )
        )
    }.orEmpty()
    var actionMenuExpanded by remember(item.id) { mutableStateOf(false) }
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    when {
                        selectionMode -> onToggleSelection()
                        isDetailMode -> actionMenuExpanded = true
                    }
                }
            )
            .then(
                if (selectionMode) {
                    Modifier.combinedClickable(
                        onClick = onToggleSelection,
                        onLongClick = onToggleSelection
                    )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) {
                AccentGreen.copy(alpha = 0.08f)
            } else if (isDetailMode) {
                CardBackground
            } else {
                CardBackground
            }
        ),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(0.dp, Color.Transparent)
    ) {
        Column(
            modifier = if (isDetailMode) {
                Modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 3.dp)
            } else {
                Modifier.fillMaxWidth()
            },
            verticalArrangement = if (isDetailMode) Arrangement.spacedBy(8.dp) else Arrangement.spacedBy(0.dp)
        ) {
            if (metadata != null && isDetailMode) {
                val coverModel = MetaTubeApiClient.resolveDisplayCoverUrl(
                    server = MetaTubeConfig.getServer(),
                    provider = metadata.provider,
                    providerId = metadata.providerId,
                    coverUrl = metadata.coverUrl,
                    thumbUrl = metadata.thumbUrl
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 82.dp, height = 112.dp)
                            .clickable(
                                enabled = !selectionMode && previewBackdropModel.isNotBlank() && onPreviewImage != null
                            ) {
                                onPreviewImage?.invoke(item)
                            }
                    ) {
                        CodeImagePlaceholder(modifier = Modifier.fillMaxSize())
                        if (coverModel.isNotBlank()) {
                            AsyncImage(
                                model = coverModel,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = metadata.title.ifBlank { item.value },
                            color = TitleText,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = metadata.code,
                            color = AccentGreen,
                            style = MaterialTheme.typography.labelMedium
                        )
                        if (metadata.releaseDate.isNotBlank()) {
                            Text(
                                text = metadata.releaseDate,
                                color = MutedText,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (metadata.actors.isNotEmpty()) {
                            Text(
                                text = metadata.actors.joinToString(" / "),
                                color = MutedText,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else if (isDetailMode) {
                if (selectionMode) {
                    Text(item.value, color = TitleText, style = MaterialTheme.typography.bodyMedium)
                } else {
                    SelectionContainer {
                        Text(item.value, color = TitleText, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isScraping) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.5.dp
                        )
                    }
                    Text(
                        when {
                            !MetaTubeConfig.isConfigured() -> "未配置 MetaTube 服务"
                            isPending -> "排队中..."
                            isScraping -> "正在获取番号信息..."
                            isFailed -> "未获取到番号信息"
                            else -> "等待获取番号信息..."
                        },
                        color = MutedText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (!isDetailMode) {
                val titleText = buildString {
                    append(metadata?.code ?: item.value)
                    val rawTitle = metadata?.title.orEmpty()
                    if (rawTitle.isNotBlank()) {
                        append(' ')
                        append(rawTitle)
                    }
                }
                val imageModel = if (metadata != null) {
                    if (displayMode == CodeDisplayMode.BACKDROP) {
                        previewBackdropModel
                    } else {
                        MetaTubeImageCache.resolve(
                            MetaTubeApiClient.resolveDisplayCoverUrl(
                                server = MetaTubeConfig.getServer(),
                                provider = metadata.provider,
                                providerId = metadata.providerId,
                                coverUrl = metadata.coverUrl,
                                thumbUrl = metadata.thumbUrl
                            )
                        )
                    }
                } else {
                    ""
                }
                val previewImageModel = if (displayMode == CodeDisplayMode.BACKDROP) {
                    imageModel
                } else {
                    previewBackdropModel
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (displayMode == CodeDisplayMode.BACKDROP) {
                                    Modifier.aspectRatio(16f / 9f)
                                } else {
                                    Modifier.aspectRatio(0.7f)
                                }
                            )
                            .clickable(
                                enabled = !selectionMode && previewImageModel.isNotBlank() && onPreviewImage != null
                            ) {
                                onPreviewImage?.invoke(item)
                            }
                    ) {
                        CodeImagePlaceholder(modifier = Modifier.fillMaxSize())
                        if (imageModel.isNotBlank()) {
                            AsyncImage(
                                model = imageModel,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Text(
                        text = titleText,
                        color = TitleText,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 5.dp, bottom = 5.dp)
                    )
                }
            }
        }
    }
    if (isDetailMode && !selectionMode) {
        DropdownMenu(
            expanded = actionMenuExpanded,
            onDismissRequest = { actionMenuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("复制") },
                onClick = {
                    actionMenuExpanded = false
                    onCopy()
                }
            )
            DropdownMenuItem(
                text = { Text("分享") },
                onClick = {
                    actionMenuExpanded = false
                    onShare()
                }
            )
            DropdownMenuItem(
                text = { Text("删除") },
                onClick = {
                    actionMenuExpanded = false
                    onDelete()
                }
            )
        }
    }
}

@Composable
internal fun MetaTubeSettingsDialog(
    initialServer: String,
    initialToken: String,
    onDismiss: () -> Unit,
    onClearData: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var server by remember(initialServer) { mutableStateOf(initialServer) }
    var token by remember(initialToken) { mutableStateOf(initialToken) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("MetaTube 设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = server,
                    onValueChange = { server = it },
                    singleLine = true,
                    label = { Text("服务地址") },
                    placeholder = { Text("http://127.0.0.1:8080") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    singleLine = true,
                    label = { Text("Token（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = onClearData,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("清除刮削数据")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(server, token) },
                enabled = server.trim().isNotBlank()
            ) {
                Text("保存")
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
internal fun SelectableFavoriteText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    onFavorite: (String, String?) -> Unit,
    favoriteOnlyWhenRecognized: Boolean = false,
    onDetectedLinkAction: ((String, String?) -> Unit)? = null
) {
    val textSizeSp = style.fontSize.takeIf { it != androidx.compose.ui.unit.TextUnit.Unspecified } ?: 16.sp
    val detectedMatches = remember(text) { ThreadLinkRecognizer.extractDisplayLinkMatches(text) }
    val detectedMatchesState = rememberUpdatedState(detectedMatches)
    AndroidView(
        factory = {
            androidx.appcompat.widget.AppCompatTextView(it).apply {
                setTextColor(color.toArgb())
                textSize = textSizeSp.value
                setTextIsSelectable(true)
                customSelectionActionModeCallback = object : android.view.ActionMode.Callback {
                    private val favoriteId = 0x7f0f1234
                    private fun selectedText(): String {
                        val fullText = this@apply.text?.toString().orEmpty()
                        val start = selectionStart.coerceAtLeast(0)
                        val end = selectionEnd.coerceAtLeast(0)
                        val from = minOf(start, end)
                        val to = maxOf(start, end)
                        return if (to > from && to <= fullText.length) fullText.substring(from, to).trim() else ""
                    }

                    override fun onCreateActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean {
                        return true
                    }

                    override fun onPrepareActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean {
                        val selected = selectedText()
                        val detectedType = ThreadLinkRecognizer.detectTypeForSelection(selected)
                        if (favoriteOnlyWhenRecognized && detectedType == null) {
                            menu?.removeItem(favoriteId)
                            return true
                        }
                        val title = if (detectedType != null) "收藏$detectedType" else "收藏文本"
                        val item = menu?.findItem(favoriteId) ?: menu?.add(android.view.Menu.NONE, favoriteId, android.view.Menu.NONE, title)
                        item?.title = title
                        return true
                    }

                    override fun onActionItemClicked(mode: android.view.ActionMode?, item: android.view.MenuItem?): Boolean {
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

                    override fun onDestroyActionMode(mode: android.view.ActionMode?) = Unit
                }
                val gestureDetector = android.view.GestureDetector(
                    context,
                    object : android.view.GestureDetector.SimpleOnGestureListener() {
                        private fun detectLink(event: android.view.MotionEvent): ThreadLinkRecognizer.DisplayLinkMatch? {
                            val layout = layout ?: return null
                            val x = (event.x - totalPaddingLeft + scrollX).toInt()
                            val y = (event.y - totalPaddingTop + scrollY).toInt()
                            val line = layout.getLineForVertical(y)
                            val offset = layout.getOffsetForHorizontal(line, x.toFloat())
                            return detectedMatchesState.value
                                .firstOrNull { offset in it.range }
                        }

                        override fun onSingleTapUp(e: android.view.MotionEvent): Boolean {
                            val match = detectLink(e) ?: return false
                            onDetectedLinkAction?.invoke(match.value, match.type)
                            return true
                        }

                        override fun onLongPress(e: android.view.MotionEvent) {
                            val match = detectLink(e) ?: return
                            onDetectedLinkAction?.invoke(match.value, match.type)
                        }
                    }
                )
                setOnTouchListener { _, event ->
                    val handled = gestureDetector.onTouchEvent(event)
                    handled
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LocalFavoritesScreen(
    images: List<LocalFavoriteImage>,
    imageLoader: ImageLoader,
    refreshing: Boolean,
    padding: PaddingValues,
    modifier: Modifier = Modifier,
    drawBehindTopBar: Boolean = false,
    onRefresh: () -> Unit,
    onDeleteSelected: (Set<String>) -> Unit,
    onOpenImage: (Int, PreviewLaunchSource?) -> Unit
) {
    val pageSize = 120
    val preloadThreshold = 18
    val selectedIds = remember { mutableStateListOf<String>() }
    var confirmDelete by remember { mutableStateOf(false) }
    var loadedCount by remember(images) { mutableStateOf(images.size.coerceAtMost(pageSize)) }
    val visibleImages = remember(images, loadedCount) { images.take(loadedCount) }
    val latestTotalCount = rememberUpdatedState(images.size)
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
    LaunchedEffect(visibleImages) {
        adapter.submitItems(visibleImages)
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
    BackHandler(enabled = selectedIds.isNotEmpty()) {
        confirmDelete = false
        selectedIds.clear()
    }
    val recyclerTopPaddingPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        if (drawBehindTopBar) padding.calculateTopPadding().roundToPx() else 0
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = padding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                end = padding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                bottom = padding.calculateBottomPadding(),
                top = if (drawBehindTopBar) 0.dp else padding.calculateTopPadding()
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { androidContext ->
                    RecyclerView(androidContext).apply {
                        clipToPadding = false
                        itemAnimator = null
                        val spanCount = 2
                        val spacingPx = (3 * resources.displayMetrics.density).roundToInt()
                        val halfSpacingPx = spacingPx / 2
                        setPadding(
                            0,
                            recyclerTopPaddingPx,
                            0,
                            (11 * resources.displayMetrics.density).roundToInt()
                        )
                        layoutManager = object : StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL) {
                            override fun supportsPredictiveItemAnimations(): Boolean = false
                        }.apply {
                            gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
                        }
                        addItemDecoration(object : RecyclerView.ItemDecoration() {
                            override fun getItemOffsets(outRect: android.graphics.Rect, view: android.view.View, parent: RecyclerView, state: RecyclerView.State) {
                                val lp = view.layoutParams as? StaggeredGridLayoutManager.LayoutParams
                                val spanIndex = lp?.spanIndex ?: 0
                                if (spanIndex == 0) {
                                    outRect.set(0, 0, halfSpacingPx, spacingPx)
                                } else {
                                    outRect.set(halfSpacingPx, 0, 0, spacingPx)
                                }
                            }
                        })
                        this.adapter = adapter.also { favoritesAdapter ->
                            favoritesAdapter.recyclerView = this
                            favoritesAdapter.submitItems(images)
                            favoritesAdapter.updateSelection(selectedIds.toSet())
                            post { favoritesAdapter.rebindVisiblePlayers() }
                        }
                        val updateColumnWidth = {
                            val availableWidth = width - paddingLeft - paddingRight
                            val columnWidth = ((availableWidth - spacingPx) / spanCount).coerceAtLeast(0)
                            adapter.updateColumnWidth(columnWidth)
                        }
                        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                            updateColumnWidth()
                        }
                        post { updateColumnWidth() }
                        addOnScrollListener(object : RecyclerView.OnScrollListener() {
                            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                                adapter.setScrolling(newState != RecyclerView.SCROLL_STATE_IDLE)
                            }

                            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                                val lm = recyclerView.layoutManager as? StaggeredGridLayoutManager ?: return
                                val last = lm.findLastVisibleItemPositions(null).maxOrNull() ?: return
                                if (last >= adapter.itemCount - preloadThreshold && loadedCount < latestTotalCount.value) {
                                    loadedCount = (loadedCount + pageSize).coerceAtMost(latestTotalCount.value)
                                }
                            }
                        })
                    }
                },
                update = { recyclerView ->
                    adapter.recyclerView = recyclerView
                    recyclerView.setPadding(
                        recyclerView.paddingLeft,
                        recyclerTopPaddingPx,
                        recyclerView.paddingRight,
                        recyclerView.paddingBottom
                    )
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
                SmallFloatingActionButton(
                    onClick = { confirmDelete = true },
                    containerColor = AccentGreen.copy(alpha = 0.8f),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = FloatingButtonEdgePadding, bottom = FloatingButtonEdgePadding)
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
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
