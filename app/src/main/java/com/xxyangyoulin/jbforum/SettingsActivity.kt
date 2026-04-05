package com.xxyangyoulin.jbforum

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xxyangyoulin.jbforum.ui.theme.ForumTheme
import com.xxyangyoulin.jbforum.util.readAppVersionName
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookiePersistence.init(applicationContext)
        LoginPersistence.init(applicationContext)
        BoardDiskCache.init(applicationContext)
        LocalImageFavorites.init(applicationContext)
        LocalLinkFavorites.init(applicationContext)
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
private fun SettingsScreen(
    loggedIn: Boolean,
    padding: PaddingValues,
    onCacheCleared: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var backupProcessing by remember { mutableStateOf(false) }
    var backupProcessingText by remember { mutableStateOf("处理中...") }
    var updateInfo by remember { mutableStateOf<GitHubReleaseInfo?>(null) }
    var hasNewVersion by remember { mutableStateOf(false) }
    var currentVersion by remember { mutableStateOf(readAppVersionName(context)) }
    var stats by remember { mutableStateOf(CacheStats(0L, 0L)) }
    var loading by remember { mutableStateOf(true) }
    var confirmClear by remember { mutableStateOf(false) }
    var forumDomain by remember { mutableStateOf(ForumDomainConfig.getDomain()) }
    var openThreadInWeb by remember { mutableStateOf(ForumDomainConfig.openThreadInWebDefault()) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        backupProcessingText = "正在导入备份..."
        backupProcessing = true
        scope.launch {
            runCatching { BackupManager.importFromUri(context, uri) }
                .onSuccess {
                    forumDomain = ForumDomainConfig.getDomain()
                    openThreadInWeb = ForumDomainConfig.openThreadInWebDefault()
                    onCacheCleared()
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    Toast.makeText(context, it.message ?: "导入失败", Toast.LENGTH_SHORT).show()
                }
            backupProcessing = false
        }
    }

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
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, CardBorder)
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
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !backupProcessing
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
                        },
                        enabled = !backupProcessing
                    )
                }
            }
        }
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, CardBorder)
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
            modifier = Modifier.fillMaxWidth(),
            enabled = !backupProcessing
        ) {
            Text("清除缓存", color = TitleText)
        }
        OutlinedButton(
            onClick = {
                backupProcessingText = "正在导出备份..."
                backupProcessing = true
                scope.launch {
                    runCatching { BackupManager.exportToDownloads(context) }
                        .onSuccess { Toast.makeText(context, "导出成功：$it", Toast.LENGTH_SHORT).show() }
                        .onFailure { Toast.makeText(context, it.message ?: "导出失败", Toast.LENGTH_SHORT).show() }
                    backupProcessing = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !backupProcessing
        ) {
            Text("导出备份", color = TitleText)
        }
        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !backupProcessing
        ) {
            Text("导入备份", color = TitleText)
        }
        OutlinedButton(
            onClick = {
                backupProcessingText = "正在检查更新..."
                backupProcessing = true
                scope.launch {
                    runCatching { GitHubUpdateChecker.latestRelease() }
                        .onSuccess { latest ->
                            currentVersion = readAppVersionName(context)
                            hasNewVersion = GitHubUpdateChecker.hasNewVersion(currentVersion, latest.tagName)
                            updateInfo = latest
                        }
                        .onFailure {
                            Toast.makeText(context, it.message ?: "检查更新失败", Toast.LENGTH_SHORT).show()
                        }
                    backupProcessing = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !backupProcessing
        ) {
            Text("检查更新", color = TitleText)
        }
        if (loggedIn) {
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                enabled = !backupProcessing
            ) {
                Text("注销", color = TitleText)
            }
        }
    }

    if (backupProcessing) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("请稍候") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = AccentGreen, strokeWidth = 2.dp)
                    Text(backupProcessingText, color = TitleText)
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    if (updateInfo != null) {
        UpdateResultDialog(
            latest = updateInfo!!,
            currentVersion = currentVersion,
            hasNewVersion = hasNewVersion,
            onDismiss = { updateInfo = null }
        )
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
internal fun UpdateResultDialog(
    latest: GitHubReleaseInfo,
    currentVersion: String,
    hasNewVersion: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentDisplayVersion = "v" + currentVersion.trim().removePrefix("v").removePrefix("V").ifBlank { "0.0.0" }
    val latestDisplayVersion = "v" + latest.tagName.trim().removePrefix("v").removePrefix("V").ifBlank { latest.tagName }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("版本检查") },
        text = {
            Text(
                if (hasNewVersion) {
                    "发现新版本：$latestDisplayVersion\n当前版本：$currentDisplayVersion"
                } else {
                    "当前已是最新版本\n当前版本：$currentDisplayVersion\n最新版本：$latestDisplayVersion"
                }
            )
        },
        confirmButton = {
            if (hasNewVersion) {
                TextButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(latest.htmlUrl)))
                        onDismiss()
                    }
                ) {
                    Text("前往下载")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("知道了")
                }
            }
        },
        dismissButton = {
            if (hasNewVersion) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}

