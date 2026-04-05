package com.xxyangyoulin.jbforum

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.xxyangyoulin.jbforum.ui.components.AuthorAvatar
import com.xxyangyoulin.jbforum.ui.components.RefreshContainer
import com.xxyangyoulin.jbforum.ui.components.ForumMessageAction
import com.xxyangyoulin.jbforum.ui.components.UserThreadCard
import com.xxyangyoulin.jbforum.ui.theme.Dimens
import com.xxyangyoulin.jbforum.ui.theme.ForumTheme
import com.xxyangyoulin.jbforum.util.openThreadByPreference
import com.xxyangyoulin.jbforum.ui.theme.rememberForumImageLoader

class UserCenterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookiePersistence.init(applicationContext)
        LoginPersistence.init(applicationContext)
        BoardDiskCache.init(applicationContext)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UserCenterActivityScreen(
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
            onRefresh = { viewModel.openUserCenter(state.userCenterUid.ifBlank { uid }, forceRefresh = true) },
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

@Composable
internal fun UserCenterScreen(
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
                contentPadding = PaddingValues(Dimens.contentCardPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.contentCardSpacing)
            ) {
            profile?.let {
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Dimens.contentCardCorner),
                        colors = CardDefaults.outlinedCardColors(containerColor = CardBackground),
                        border = BorderStroke(0.dp, Color.Transparent)
                    ) {
                        Column(modifier = Modifier.padding(Dimens.contentCardPadding)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AuthorAvatar(imageLoader = imageLoader, imageUrl = it.avatarUrl, name = it.username, size = 52.dp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(it.username, color = TitleText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                    Text("UID: ${it.uid}", color = MutedText, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Spacer(Modifier.height(Dimens.contentCardSpacing))
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

@Composable
internal fun UserProfileCard(
    title: String,
    items: List<Pair<String, String>>
) {
    if (items.isEmpty()) return
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.contentCardCorner),
        colors = CardDefaults.outlinedCardColors(containerColor = CardBackground),
        border = BorderStroke(0.dp, Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(Dimens.contentCardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.contentCardSpacing)
        ) {
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
