package com.xxyangyoulin.jbforum.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.xxyangyoulin.jbforum.AppColors
import com.xxyangyoulin.jbforum.ForumMessageStatus
import com.xxyangyoulin.jbforum.ui.theme.PillShape

/**
 * 下拉刷新容器
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RefreshContainer(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    contentVersion: Any? = Unit,
    indicatorTopPadding: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    val pullState = rememberPullToRefreshState()

    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = modifier,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullState,
                isRefreshing = refreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = indicatorTopPadding),
                containerColor = AppColors.AccentGreen.copy(alpha = 0.18f),
                color = AppColors.AccentGreen
            )
        }
    ) {
        content()
    }
}

/**
 * 加载更多按钮
 */
@Composable
fun LoadMoreButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text("加载更多")
    }
}

/**
 * 可选择的药丸形状按钮
 */
@Composable
fun SelectablePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) {
        AppColors.AccentGreen
    } else {
        AppColors.CardBackground
    }
    val contentColor = if (selected) {
        androidx.compose.ui.graphics.Color.White
    } else {
        AppColors.TitleText
    }

    Surface(
        onClick = onClick,
        modifier = modifier,
        color = containerColor,
        shape = PillShape,
        border = if (!selected) {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                AppColors.CardBorder
            )
        } else null
    ) {
        Text(
            text = text,
            color = contentColor,
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 6.dp
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun ForumMessageAction(
    status: ForumMessageStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        enabled = status.loggedIn,
        modifier = modifier
    ) {
        BadgedBox(
            badge = {
                if (status.hasUnread) {
                    Badge {
                        if (status.unreadCount > 0) {
                            Text(if (status.unreadCount > 99) "99+" else status.unreadCount.toString())
                        }
                    }
                }
            }
        ) {
            Icon(
                Icons.Outlined.MailOutline,
                contentDescription = null,
                tint = if (status.loggedIn) AppColors.TitleText else AppColors.MutedText
            )
        }
    }
}

@Composable
fun StyledDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        containerColor = AppColors.CardBackground,
        tonalElevation = 0.dp,
        shadowElevation = 6.dp,
        content = content
    )
}

@Composable
fun EndOfListIndicator(
    modifier: Modifier = Modifier
) {
    Text(
        text = "到底了",
        modifier = modifier.fillMaxWidth(),
        color = AppColors.MutedText,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun PagingFooterStatus(
    loading: Boolean,
    error: String?,
    hasMore: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        hasMore && loading -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ContainedLoadingIndicator(
                    containerColor = AppColors.AccentGreen.copy(alpha = 0.18f),
                    indicatorColor = AppColors.AccentGreen,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("加载中", color = AppColors.MutedText, style = MaterialTheme.typography.bodySmall)
            }
        }
        hasMore && !error.isNullOrBlank() -> {
            Text(
                text = "加载失败，点击重试",
                modifier = modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRetry),
                color = AppColors.MutedText,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
        hasMore -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ContainedLoadingIndicator(
                    containerColor = AppColors.AccentGreen.copy(alpha = 0.18f),
                    indicatorColor = AppColors.AccentGreen,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("加载中", color = AppColors.MutedText, style = MaterialTheme.typography.bodySmall)
            }
        }
        else -> {
            EndOfListIndicator(modifier = modifier)
        }
    }
}

@Composable
fun ForumLinkActionDialog(
    link: String,
    type: String? = null,
    message: String,
    onDismiss: () -> Unit,
    onOpen: (() -> Unit)? = null,
    openText: String = "打开",
    onExternalOpen: ((String) -> Unit)? = null,
    onFavorite: (() -> Unit)? = null,
    onOpenCode: (() -> Unit)? = null,
    onCopy: () -> Unit,
    showOpenOverride: Boolean? = null,
    showExternalOpenOverride: Boolean? = null,
    showFavoriteOverride: Boolean? = null
) {
    val externalTarget = normalizeExternalDetectedLink(link)
    val showOpen = showOpenOverride ?: (onOpen != null && shouldShowInternalOpen(link, type))
    val showExternalOpen = showExternalOpenOverride ?: (onExternalOpen != null && shouldShowExternalOpen(link, type))
    val showFavorite = showFavoriteOverride ?: (onFavorite != null)
    val openCodeAction = onOpenCode
    val showOpenCode = type == "番号" && openCodeAction != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("链接操作") },
        text = { Text(message) },
        confirmButton = {
            if (showOpen) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedButton(
                        onClick = { onOpen?.invoke() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(openText)
                    }
                }
            }
        },
        dismissButton = {
            Row {
                if (showExternalOpen) {
                    TextButton(onClick = { onExternalOpen?.invoke(externalTarget) }) { Text("外部打开") }
                }
                if (showOpenCode) {
                    TextButton(onClick = { openCodeAction.invoke() }) { Text("打开番号") }
                }
                TextButton(onClick = onCopy) { Text("复制") }
                if (showFavorite) {
                    TextButton(onClick = { onFavorite?.invoke() }) { Text("收藏") }
                }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}

private fun shouldShowInternalOpen(link: String, type: String?): Boolean {
    if (type == "番号" || type == "磁力") return false
    return link.startsWith("http://", ignoreCase = true) ||
        link.startsWith("https://", ignoreCase = true) ||
        link.startsWith("ed2k://", ignoreCase = true) ||
        isCloudShareLink(link)
}

private fun shouldShowExternalOpen(link: String, type: String?): Boolean {
    if (type == "番号") return false
    return link.startsWith("http://", ignoreCase = true) ||
        link.startsWith("https://", ignoreCase = true) ||
        link.startsWith("magnet:", ignoreCase = true) ||
        link.startsWith("ed2k://", ignoreCase = true) ||
        isCloudShareLink(link)
}

private fun normalizeExternalDetectedLink(link: String): String {
    return if (isCloudShareLink(link) && !link.startsWith("http://", ignoreCase = true) && !link.startsWith("https://", ignoreCase = true)) {
        "https://$link"
    } else {
        link
    }
}

private fun isCloudShareLink(link: String): Boolean {
    val normalized = link.lowercase()
    return normalized.startsWith("pan.baidu.com/") ||
        normalized.startsWith("pan.quark.cn/") ||
        normalized.startsWith("aliyundrive.com/") ||
        normalized.startsWith("www.aliyundrive.com/") ||
        normalized.startsWith("alipan.com/") ||
        normalized.startsWith("www.alipan.com/") ||
        normalized.startsWith("drive.uc.cn/") ||
        normalized.startsWith("115.com/") ||
        normalized.startsWith("pan.xunlei.com/") ||
        normalized.startsWith("123pan.com/") ||
        normalized.startsWith("www.123pan.com/") ||
        normalized.startsWith("share.weiyun.com/") ||
        normalized.startsWith("lanzou") && normalized.contains(".com/")
}
