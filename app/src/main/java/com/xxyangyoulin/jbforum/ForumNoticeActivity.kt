package com.xxyangyoulin.jbforum

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xxyangyoulin.jbforum.ui.components.AuthorAvatar
import com.xxyangyoulin.jbforum.ui.components.RefreshContainer
import com.xxyangyoulin.jbforum.ui.components.SelectablePill
import com.xxyangyoulin.jbforum.ui.theme.ForumTheme
import com.xxyangyoulin.jbforum.ui.theme.rememberForumImageLoader
import com.xxyangyoulin.jbforum.util.openThreadByPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class NoticeTab(val label: String, val type: String) {
    Post("帖子", "post"),
    Comment("点评", "pcomment")
}

class ForumNoticeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookiePersistence.init(applicationContext)
        LoginPersistence.init(applicationContext)
        enableEdgeToEdge()
        val noticeUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        setContent {
            ForumTheme {
                ForumNoticeScreen(
                    noticeUrl = noticeUrl,
                    onBack = { finish() }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_URL = "url"

        fun createIntent(context: android.content.Context, url: String): android.content.Intent {
            return android.content.Intent(context, ForumNoticeActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForumNoticeScreen(
    noticeUrl: String,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val imageLoader = rememberForumImageLoader()
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(NoticeTab.Post) }
    var loading by remember { mutableStateOf(true) }
    var items by remember { mutableStateOf<List<ForumNoticeItem>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }

    fun currentNoticeUrl(): String {
        if (noticeUrl.isBlank()) return ""
        val base = noticeUrl.substringBefore('#')
        val withoutType = base.replace(Regex("&type=[^&]+"), "")
        return if (withoutType.contains("type=")) {
            withoutType
        } else {
            "$withoutType&type=${selectedTab.type}"
        }
    }

    suspend fun load() {
        val targetUrl = currentNoticeUrl()
        if (targetUrl.isBlank()) {
            loading = false
            message = "提醒地址为空"
            return
        }
        loading = true
        message = null
        runCatching {
            withContext(Dispatchers.IO) {
                ForumRepository().loadNoticeItems(targetUrl)
            }
        }.onSuccess {
            items = it
            loading = false
        }.onFailure {
            message = it.message ?: "加载提醒失败"
            loading = false
        }
    }

    LaunchedEffect(noticeUrl, selectedTab) {
        load()
    }

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            message = null
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
                    Text(
                        text = "消息提醒",
                        color = TitleText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBackIos, contentDescription = null, tint = TitleText)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val targetUrl = currentNoticeUrl()
                            if (targetUrl.isNotBlank()) {
                                context.startActivity(
                                    ThreadWebViewActivity.createIntent(
                                        context = context,
                                        url = targetUrl,
                                        title = "消息提醒"
                                    )
                                )
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.OpenInBrowser, contentDescription = null, tint = TitleText)
                    }
                }
            )
        }
    ) { padding ->
        RefreshContainer(
            refreshing = loading,
            onRefresh = { scope.launch { load() } },
            contentVersion = "${selectedTab.name}-${items.size}-${items.firstOrNull()?.id.orEmpty()}",
            modifier = Modifier.padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, top = 10.dp, end = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NoticeTab.entries.forEach { tab ->
                        SelectablePill(
                            text = tab.label,
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab }
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (items.isEmpty() && !loading) {
                        item {
                            Text(
                                text = "暂无提醒",
                                color = MutedText,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    items(items, key = { it.id }) { item ->
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (item.targetUrl.contains("viewthread") || item.targetUrl.contains("findpost")) {
                                        openThreadByPreference(
                                            context,
                                            ThreadSummary(
                                                id = item.targetUrl.substringAfter("tid=", "").substringBefore('&'),
                                                title = item.content,
                                                author = item.author,
                                                authorUid = item.authorUid,
                                                authorAvatarUrl = item.authorAvatarUrl,
                                                publishedAt = item.time,
                                                url = item.targetUrl
                                            )
                                        )
                                    } else {
                                        context.startActivity(
                                            ThreadWebViewActivity.createIntent(
                                                context = context,
                                                url = item.targetUrl,
                                                title = "消息提醒"
                                            )
                                        )
                                    }
                                },
                            colors = CardDefaults.outlinedCardColors(containerColor = CardBackground),
                            border = androidx.compose.foundation.BorderStroke(0.dp, androidx.compose.ui.graphics.Color.Transparent)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                AuthorAvatar(
                                    imageLoader = imageLoader,
                                    imageUrl = item.authorAvatarUrl,
                                    name = item.author,
                                    modifier = Modifier,
                                    size = 32.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.author.ifBlank { "论坛提醒" },
                                        color = TitleText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.time,
                                        color = MutedText,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = remember(item.content, item.threadTitle) {
                                            buildAnnotatedString {
                                                val title = item.threadTitle
                                                val content = item.content
                                                val start = if (title.isNotBlank()) content.indexOf(title) else -1
                                                if (start >= 0) {
                                                    append(content.substring(0, start))
                                                    pushStyle(SpanStyle(color = AccentGreen))
                                                    append(title)
                                                    pop()
                                                    append(content.substring(start + title.length))
                                                } else {
                                                    append(content)
                                                }
                                            }
                                        },
                                        color = TitleText,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
