package com.xxyangyoulin.jbforum

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
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
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import coil.ImageLoader
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.xxyangyoulin.jbforum.ui.theme.ForumTheme
import com.xxyangyoulin.jbforum.ui.theme.rememberForumImageLoader
import com.xxyangyoulin.jbforum.ui.components.EndOfListIndicator
import com.xxyangyoulin.jbforum.ui.components.appTopBarHaze
import com.xxyangyoulin.jbforum.util.buildHighlightedText
import com.xxyangyoulin.jbforum.util.openThreadByPreference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class LocalFavoritesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookiePersistence.init(applicationContext)
        LoginPersistence.init(applicationContext)
        BoardDiskCache.init(applicationContext)
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
    var tab by rememberSaveable { mutableStateOf(if (initialTab == LocalFavoritesActivity.TAB_LINK) LocalFavoritesActivity.TAB_LINK else LocalFavoritesActivity.TAB_IMAGE) }
    val hazeState = remember { HazeState() }
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
            TopAppBar(
                windowInsets = WindowInsets.statusBarsIgnoringVisibility,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                modifier = Modifier.appTopBarHaze(hazeState),
                title = { Text("本地收藏", color = TitleText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBackIos, contentDescription = null, tint = TitleText)
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
            LocalLinkFavoritesContent(
                items = linkItems,
                padding = padding,
                modifier = Modifier.hazeSource(state = hazeState),
                drawBehindTopBar = true,
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun LocalLinkFavoritesContent(
    items: List<LocalFavoriteLink>,
    padding: PaddingValues,
    modifier: Modifier = Modifier,
    drawBehindTopBar: Boolean = false,
    onLinksChanged: () -> Unit,
    onOpenThread: (LocalFavoriteLink) -> Unit
) {
    val context = LocalContext.current
    val pageSize = 80
    val preloadThreshold = 16
    var selectedTab by rememberSaveable { mutableStateOf(LinkCategory.ALL) }
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val filteredItems = remember(items, selectedTab) {
        items.filter { LinkCategory.matches(selectedTab, it.type) }
    }
    var loadedCount by remember(filteredItems) { mutableStateOf(filteredItems.size.coerceAtMost(pageSize)) }
    val visibleItems = remember(filteredItems, loadedCount) { filteredItems.take(loadedCount) }
    val listState = rememberLazyListState()
    val selectedItems = remember(filteredItems, selectedIds) {
        filteredItems.filter { selectedIds.contains(it.id) }
    }

    BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedIds = emptySet()
    }

    LaunchedEffect(filteredItems, loadedCount) {
        if (loadedCount >= filteredItems.size) return@LaunchedEffect
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@LaunchedEffect
        if (lastVisibleIndex >= visibleItems.size - preloadThreshold) {
            loadedCount = (loadedCount + pageSize).coerceAtMost(filteredItems.size)
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
        selectionMode = false
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
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = padding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                end = padding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                bottom = padding.calculateBottomPadding(),
                top = if (drawBehindTopBar) 0.dp else padding.calculateTopPadding()
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = 12.dp + if (drawBehindTopBar) padding.calculateTopPadding() else 0.dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .weight(1f)
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
                Row(
                    modifier = Modifier.heightIn(min = 30.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                state = listState,
                contentPadding = PaddingValues(
                    start = 12.dp,
                    top = 8.dp,
                    end = 12.dp,
                    bottom = if (selectionMode) 84.dp else 12.dp
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
        }

        if (selectionMode) {
            Surface(
                shape = RoundedCornerShape(999.dp),
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

@Composable
internal fun SelectableFavoriteText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    onFavorite: (String, String?) -> Unit,
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
                            if (drawBehindTopBar) androidContext.resources.displayMetrics.density.times(padding.calculateTopPadding().value).roundToInt() else 0,
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
