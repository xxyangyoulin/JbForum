package com.xxyangyoulin.jbforum

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import coil.ImageLoader
import com.xxyangyoulin.jbforum.ui.theme.ForumTheme
import com.xxyangyoulin.jbforum.ui.theme.rememberForumImageLoader
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun LocalFavoritesActivityScreen(
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun LocalLinkFavoritesContent(
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
                    border = BorderStroke(
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
                                SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(item.savedAt)),
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
internal fun SelectableFavoriteText(
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
