package com.xxyangyoulin.jbforum.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.xxyangyoulin.jbforum.AppColors
import com.xxyangyoulin.jbforum.AppDimensions
import com.xxyangyoulin.jbforum.PostRemark
import com.xxyangyoulin.jbforum.ThreadSummary
import com.xxyangyoulin.jbforum.UserThreadItem
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp

/**
 * 作者头像
 */
@Composable
fun AuthorAvatar(
    imageLoader: ImageLoader,
    imageUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp
) {
    val context = LocalContext.current
    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .build(),
            imageLoader = imageLoader,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFF8A2E2B)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1).ifBlank { "匿" },
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/**
 * 用户身份行（头像 + 名称）
 */
@Composable
fun UserIdentity(
    imageLoader: ImageLoader,
    imageUrl: String?,
    name: String,
    uid: String,
    avatarSize: Dp = AppDimensions.AuthorAvatarSize.dp,
    nameTextStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    metaText: String = "",
    onOpenUserCenter: (String) -> Unit
) {
    val clickable = uid.isNotBlank()
    Row(verticalAlignment = Alignment.CenterVertically) {
        AuthorAvatar(
            imageLoader = imageLoader,
            imageUrl = imageUrl,
            name = name,
            modifier = if (clickable) Modifier.clickable { onOpenUserCenter(uid) } else Modifier,
            size = avatarSize
        )
        Spacer(Modifier.width(10.dp))
        Column {
            ClickableName(
                name = name,
                uid = uid,
                color = AppColors.TitleText,
                style = nameTextStyle,
                onOpenUserCenter = onOpenUserCenter
            )
            if (metaText.isNotBlank()) {
                Text(metaText, style = MaterialTheme.typography.bodySmall, color = AppColors.MutedText)
            }
        }
    }
}

/**
 * 可点击的用户名
 */
@Composable
fun ClickableName(
    name: String,
    uid: String,
    color: androidx.compose.ui.graphics.Color = AppColors.TitleText,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    suffix: String = "",
    onOpenUserCenter: (String) -> Unit
) {
    Text(
        text = name + suffix,
        color = color,
        style = style,
        modifier = if (uid.isNotBlank()) Modifier.clickable { onOpenUserCenter(uid) } else Modifier
    )
}

/**
 * 评论/备注卡片
 */
@Composable
fun RemarkCard(
    remark: PostRemark,
    imageLoader: ImageLoader,
    onOpenUserCenter: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.InputBackground)
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        AuthorAvatar(
            imageLoader = imageLoader,
            imageUrl = remark.authorAvatarUrl,
            name = remark.author,
            modifier = if (remark.authorUid.isNotBlank()) Modifier.clickable { onOpenUserCenter(remark.authorUid) } else Modifier,
            size = AppDimensions.SmallAvatarSize.dp
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            ClickableName(
                name = remark.author.ifBlank { "匿名" },
                uid = remark.authorUid,
                color = AppColors.MutedText,
                style = MaterialTheme.typography.labelSmall,
                suffix = remark.time.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty(),
                onOpenUserCenter = onOpenUserCenter
            )
            if (remark.content.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(
                        text = remark.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TitleText
                    )
                }
            }
        }
    }
}

/**
 * 用户帖子卡片
 */
@Composable
fun UserThreadCard(
    item: UserThreadItem,
    onOpenThread: (com.xxyangyoulin.jbforum.UserThreadItem) -> Unit,
    showDeleteAction: Boolean = false,
    onDelete: ((com.xxyangyoulin.jbforum.UserThreadItem) -> Unit)? = null
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = { onOpenThread(item) }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = item.title,
                color = AppColors.TitleText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (showDeleteAction && onDelete != null && item.deleteActionUrl != null) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "删除收藏",
                        color = AppColors.MutedText,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.clickable { onDelete(item) }
                    )
                }
            }
            if (item.summary.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.summary,
                    color = AppColors.MutedText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (item.board.isNotBlank()) {
                    Text(
                        text = item.board,
                        color = AppColors.MutedText,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (item.time.isNotBlank()) {
                    Text(
                        text = item.time,
                        color = AppColors.MutedText,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

/**
 * 用户资料卡片
 */
@Composable
fun UserProfileCard(
    username: String,
    avatarUrl: String?,
    imageLoader: ImageLoader,
    onOpenUserCenter: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuthorAvatar(
                imageLoader = imageLoader,
                imageUrl = avatarUrl,
                name = username,
                size = 80.dp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = username,
                color = AppColors.TitleText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
