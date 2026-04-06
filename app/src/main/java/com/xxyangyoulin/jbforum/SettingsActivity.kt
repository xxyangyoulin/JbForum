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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                        Icon(Icons.AutoMirrored.Outlined.ArrowBackIos, contentDescription = null, tint = TitleText)
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
    var confirmLogout by remember { mutableStateOf(false) }
    var forumDomain by remember { mutableStateOf(ForumDomainConfig.getDomain()) }
    var openThreadInWeb by remember { mutableStateOf(ForumDomainConfig.openThreadInWebDefault()) }
    var showDomainDialog by remember { mutableStateOf(false) }
    var themeMode by remember { mutableStateOf(ThemeModePersistence.mode) }
    var showThemeModeDialog by remember { mutableStateOf(false) }

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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 通用设置
        SettingsSection(title = "通用设置") {
            SettingsItem(
                icon = Icons.Outlined.Settings,
                title = "主题模式",
                subtitle = themeMode.label,
                onClick = { showThemeModeDialog = true }
            )
            SettingsItem(
                icon = Icons.Outlined.Language,
                title = "论坛域名",
                subtitle = forumDomain.ifBlank { "未设置" },
                onClick = { showDomainDialog = true }
            )
            SettingsSwitchItem(
                icon = Icons.Outlined.Language,
                title = "默认网页浏览帖子",
                checked = openThreadInWeb,
                onCheckedChange = {
                    openThreadInWeb = it
                    ForumDomainConfig.setOpenThreadInWebDefault(it)
                }
            )
        }

        // 数据管理
        SettingsSection(title = "数据管理") {
            // 缓存信息
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("缓存占用", color = TitleText, fontSize = 16.sp)
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = AccentGreen,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            formatBytes(stats.totalBytes),
                            color = MutedText,
                            fontSize = 14.sp
                        )
                    }
                }
                if (!loading && stats.totalBytes > 0) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (stats.favoriteBytes.toFloat() / stats.totalBytes.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AccentGreen,
                        trackColor = Color(0xFFE8EBEF)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "收藏缓存：${formatBytes(stats.favoriteBytes)}",
                        color = MutedText,
                        fontSize = 12.sp
                    )
                }
            }

            SettingsDivider()

            SettingsItem(
                icon = Icons.Outlined.Delete,
                title = "清除缓存",
                titleColor = Color(0xFFD32F2F),
                onClick = { confirmClear = true }
            )
            SettingsDivider()
            SettingsItem(
                icon = Icons.Outlined.CloudUpload,
                title = "导出备份",
                onClick = {
                    backupProcessingText = "正在导出备份..."
                    backupProcessing = true
                    scope.launch {
                        runCatching { BackupManager.exportToDownloads(context) }
                            .onSuccess { Toast.makeText(context, "导出成功：$it", Toast.LENGTH_SHORT).show() }
                            .onFailure { Toast.makeText(context, it.message ?: "导出失败", Toast.LENGTH_SHORT).show() }
                        backupProcessing = false
                    }
                }
            )
            SettingsDivider()
            SettingsItem(
                icon = Icons.Outlined.CloudDownload,
                title = "导入备份",
                onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }
            )
        }

        // 关于
        SettingsSection(title = "关于") {
            SettingsItem(
                icon = Icons.Outlined.Info,
                title = "当前版本",
                subtitle = "v${currentVersion}",
                showArrow = false
            )
            SettingsDivider()
            SettingsItem(
                icon = Icons.Outlined.Refresh,
                title = "检查更新",
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
                }
            )
        }

        // 账号（仅登录时显示）
        if (loggedIn) {
            SettingsSection(title = "账号") {
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "退出登录",
                    titleColor = Color(0xFFD32F2F),
                    onClick = { confirmLogout = true }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    // 处理中对话框
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

    // 版本更新对话框
    if (updateInfo != null) {
        UpdateResultDialog(
            latest = updateInfo!!,
            currentVersion = currentVersion,
            hasNewVersion = hasNewVersion,
            onDismiss = { updateInfo = null }
        )
    }

    // 清除缓存确认
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
                    Text("确认", color = Color(0xFFD32F2F))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 域名设置对话框
    if (showDomainDialog) {
        AlertDialog(
            onDismissRequest = { showDomainDialog = false },
            title = { Text("论坛域名") },
            text = {
                OutlinedTextField(
                    value = forumDomain,
                    onValueChange = { forumDomain = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("例如 www.xxx.com") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        runCatching {
                            ForumDomainConfig.setDomain(forumDomain)
                            CookiePersistence.clear()
                        }.onSuccess {
                            Toast.makeText(context, "域名已保存", Toast.LENGTH_SHORT).show()
                        }.onFailure {
                            Toast.makeText(context, it.message ?: "域名保存失败", Toast.LENGTH_SHORT).show()
                        }
                        showDomainDialog = false
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDomainDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showThemeModeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeModeDialog = false },
            title = { Text("主题模式") },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    themeMode = mode
                                    ThemeModePersistence.updateMode(mode)
                                    showThemeModeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mode.label,
                                color = if (mode == themeMode) AccentGreen else TitleText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeModeDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 退出登录确认
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出当前账号吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmLogout = false
                        onLogout()
                        // 返回首页并触发刷新
                        context.startActivity(
                            android.content.Intent(context, MainActivity::class.java)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        )
                        (context as? android.app.Activity)?.finish()
                    }
                ) {
                    Text("确定", color = Color(0xFFD32F2F))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            color = AccentGreen,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.outlinedCardColors(containerColor = CardBackground),
            border = BorderStroke(0.dp, Color.Transparent)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    titleColor: Color = TitleText,
    showArrow: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable { onClick() }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                } else {
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = titleColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = titleColor,
                fontSize = 16.sp
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = MutedText,
                    fontSize = 13.sp
                )
            }
        }
        if (showArrow && onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MutedText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TitleText,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            color = TitleText,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(CardBorder)
    )
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
