package com.xxyangyoulin.jbforum.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.pager.rememberPagerState
import com.xxyangyoulin.jbforum.AppColors
import com.xxyangyoulin.jbforum.AppDimensions

/**
 * 下拉刷新容器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshContainer(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val pullState = rememberPullToRefreshState()
    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = modifier,
        indicator = {
            PullToRefreshDefaults.Indicator(
                modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter),
                isRefreshing = refreshing,
                state = pullState,
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
        shape = MaterialTheme.shapes.small,
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
