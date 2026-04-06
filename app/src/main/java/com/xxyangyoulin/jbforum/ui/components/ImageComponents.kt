package com.xxyangyoulin.jbforum.ui.components

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.target.Target
import com.xxyangyoulin.jbforum.AppColors
import com.xxyangyoulin.jbforum.LocalImageFavorites
import com.xxyangyoulin.jbforum.ThreadImageCache
import com.xxyangyoulin.jbforum.isGifImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView
import java.io.File
import kotlin.math.roundToInt
import kotlin.coroutines.cancellation.CancellationException

/**
 * 缩略图生成中的占位符
 */
@Composable
fun ThumbnailGeneratingPlaceholder(
    text: String = "缩略图生成中...",
    modifier: Modifier = Modifier
) {
    val shimmerAlpha by rememberInfiniteTransition(label = "image_placeholder")
        .animateFloat(
            initialValue = 0.45f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900),
                repeatMode = RepeatMode.Reverse
            ),
            label = "image_placeholder_alpha"
        )
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                tint = AppColors.MutedText.copy(alpha = shimmerAlpha),
                modifier = Modifier.height(28.dp)
            )
            Text(
                text = text,
                color = AppColors.MutedText.copy(alpha = shimmerAlpha),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun LoadFailedPlaceholder(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit
) {
    Box(
        modifier = modifier.clickable(onClick = onRetry),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.BrokenImage,
                contentDescription = null,
                tint = AppColors.MutedText.copy(alpha = 0.85f),
                modifier = Modifier.height(28.dp)
            )
            Text(text = "点击重试", color = AppColors.MutedText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * 自动播放的 GIF 图片
 */
@Composable
fun AutoPlayGifImage(
    imageRef: String,
    modifier: Modifier = Modifier,
    contentScale: androidx.compose.ui.layout.ContentScale = androidx.compose.ui.layout.ContentScale.Fit,
    shouldAnimate: Boolean = true,
    resizeWidthPx: Int? = null,
    onImageReady: ((Int, Int) -> Unit)? = null,
    onLoadError: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isVisible by remember(imageRef) { mutableStateOf(false) }

    AndroidView(
        factory = {
            GifImageView(it).apply {
                adjustViewBounds = true
                scaleType = when (contentScale) {
                    androidx.compose.ui.layout.ContentScale.Crop -> ImageView.ScaleType.CENTER_CROP
                    else -> ImageView.ScaleType.FIT_CENTER
                }
            }
        },
        modifier = modifier.onGloballyPositioned { coordinates ->
            val bounds = coordinates.boundsInWindow()
            val decorView = (context as? Activity)?.window?.decorView
            val windowWidth = decorView?.width?.toFloat()?.coerceAtLeast(1f) ?: 1f
            val windowHeight = decorView?.height?.toFloat()?.coerceAtLeast(1f) ?: 1f
            val visibleWidth = (minOf(bounds.right, windowWidth) - maxOf(bounds.left, 0f)).coerceAtLeast(0f)
            val visibleHeight = (minOf(bounds.bottom, windowHeight) - maxOf(bounds.top, 0f)).coerceAtLeast(0f)
            val totalArea = bounds.width * bounds.height
            val visibleArea = visibleWidth * visibleHeight
            isVisible = bounds.bottom > 0f &&
                bounds.top < windowHeight &&
                totalArea > 0f &&
                (visibleArea / totalArea) >= 0.3f
        },
        update = { gifImageView ->
            gifImageView.scaleType = when (contentScale) {
                androidx.compose.ui.layout.ContentScale.Crop -> ImageView.ScaleType.CENTER_CROP
                else -> ImageView.ScaleType.FIT_CENTER
            }
            if (gifImageView.tag != imageRef) {
                gifImageView.tag = imageRef
                runCatching {
                    val drawable = GifDrawable(File(imageRef))
                    onImageReady?.invoke(drawable.intrinsicWidth, drawable.intrinsicHeight)
                    resizeWidthPx?.takeIf { it > 0 }?.let { width ->
                        drawable.setBounds(0, 0, width, (width / drawable.intrinsicWidth.toFloat() * drawable.intrinsicHeight).roundToInt())
                    }
                    drawable.stop()
                    gifImageView.setImageDrawable(drawable)
                }.onFailure {
                    gifImageView.setImageDrawable(null)
                    onLoadError?.invoke()
                }
            }
            (gifImageView.drawable as? GifDrawable)?.let { drawable ->
                if (isVisible && shouldAnimate) drawable.start() else drawable.stop()
            }
        }
    )
}

/**
 * 准备好的图片显示
 */
@Composable
fun PreparedDisplayImage(
    displayRef: String?,
    phase: com.xxyangyoulin.jbforum.CachedImagePhase,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    contentScale: androidx.compose.ui.layout.ContentScale = androidx.compose.ui.layout.ContentScale.Fit,
    resizeWidthPx: Int? = null,
    onLoadError: (() -> Unit)? = null,
    onLoadSuccess: ((Int, Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val displayFile = displayRef?.let(::File)
    if (displayFile?.exists() == true) {
        if (isGifImage(displayFile.absolutePath)) {
            AutoPlayGifImage(
                imageRef = displayFile.absolutePath,
                modifier = modifier,
                contentScale = contentScale,
                resizeWidthPx = resizeWidthPx,
                onImageReady = onLoadSuccess,
                onLoadError = onLoadError
            )
        } else {
            AndroidView(
                factory = {
                    AppCompatImageView(it).apply {
                        adjustViewBounds = true
                        scaleType = when (contentScale) {
                            androidx.compose.ui.layout.ContentScale.Crop -> ImageView.ScaleType.CENTER_CROP
                            else -> ImageView.ScaleType.FIT_CENTER
                        }
                    }
                },
                modifier = modifier,
                update = { imageView ->
                    imageView.scaleType = when (contentScale) {
                        androidx.compose.ui.layout.ContentScale.Crop -> ImageView.ScaleType.CENTER_CROP
                        else -> ImageView.ScaleType.FIT_CENTER
                    }
                    val dataKey = displayFile.absolutePath
                    if (imageView.tag != dataKey) {
                        imageView.tag = dataKey
                        imageLoader.enqueue(
                            ImageRequest.Builder(context)
                                .data(displayFile)
                                .allowHardware(false)
                                .bitmapConfig(Bitmap.Config.RGB_565)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .listener(
                                    onSuccess = { _, result ->
                                        val width = result.drawable.intrinsicWidth
                                        val height = result.drawable.intrinsicHeight
                                        if (width > 0 && height > 0) {
                                            onLoadSuccess?.invoke(width, height)
                                        }
                                    },
                                    onError = { _, _ -> onLoadError?.invoke() }
                                )
                                .target(imageView)
                                .build()
                        )
                    }
                }
            )
        }
    } else {
        ThumbnailGeneratingPlaceholder(
            text = when (phase) {
                com.xxyangyoulin.jbforum.CachedImagePhase.Loading -> "加载中..."
                com.xxyangyoulin.jbforum.CachedImagePhase.Compressing -> "压缩中..."
                com.xxyangyoulin.jbforum.CachedImagePhase.Ready -> "加载中..."
            },
            modifier = modifier
        )
    }
}
/**
 * 远程缓存图片显示
 */
@Composable
fun CachedRemoteDisplayImage(
    imageRef: String,
    imageLoader: ImageLoader,
    imageDownloadClient: okhttp3.OkHttpClient,
    modifier: Modifier = Modifier,
    contentScale: androidx.compose.ui.layout.ContentScale = androidx.compose.ui.layout.ContentScale.Fit,
    resizeWidthPx: Int? = null,
    showOriginalDirectly: Boolean = false,
    placeholderMinHeight: androidx.compose.ui.unit.Dp = 160.dp,
    trackVisibility: Boolean = true,
    onImageReady: ((Int, Int) -> Unit)? = null,
    onFailedStateChanged: ((Boolean) -> Unit)? = null
) {
    if (showOriginalDirectly) {
        val context = LocalContext.current
        AndroidView(
            factory = {
                AppCompatImageView(it).apply {
                    adjustViewBounds = true
                    scaleType = when (contentScale) {
                        androidx.compose.ui.layout.ContentScale.Crop -> ImageView.ScaleType.CENTER_CROP
                        else -> ImageView.ScaleType.FIT_CENTER
                    }
                }
            },
            modifier = modifier,
            update = { imageView ->
                imageView.scaleType = when (contentScale) {
                    androidx.compose.ui.layout.ContentScale.Crop -> ImageView.ScaleType.CENTER_CROP
                    else -> ImageView.ScaleType.FIT_CENTER
                }
                if (imageView.tag != imageRef) {
                    imageView.tag = imageRef
                    imageLoader.enqueue(
                        ImageRequest.Builder(context)
                            .data(imageRef)
                            .crossfade(true)
                            .allowHardware(false)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .target(imageView)
                            .build()
                    )
                }
            }
        )
        return
    }
    var displayRef by remember(imageRef) { mutableStateOf(ThreadImageCache.thumbnailRef(imageRef)) }
    var phase by remember(imageRef) {
        mutableStateOf(if (displayRef != null) com.xxyangyoulin.jbforum.CachedImagePhase.Ready else ThreadImageCache.phase(imageRef))
    }
    var failed by remember(imageRef) { mutableStateOf(ThreadImageCache.isFailed(imageRef)) }
    var retryNonce by remember(imageRef) { mutableStateOf(0) }
    val context = LocalContext.current
    var isVisible by remember(imageRef, trackVisibility) { mutableStateOf(!trackVisibility) }
    val trackedModifier = if (trackVisibility) {
        modifier.onGloballyPositioned { coordinates ->
            val bounds = coordinates.boundsInWindow()
            val windowHeight = (context as? Activity)?.window?.decorView?.height?.toFloat()
                ?.coerceAtLeast(1f)
                ?: 1f
            isVisible = bounds.bottom > 0f && bounds.top < windowHeight
        }
    } else {
        modifier
    }
    val placeholderModifier = trackedModifier.heightIn(min = placeholderMinHeight)
    LaunchedEffect(failed) {
        onFailedStateChanged?.invoke(failed)
    }
    LaunchedEffect(imageRef, isVisible, displayRef, phase, retryNonce) {
        if (!failed && isVisible && (phase != com.xxyangyoulin.jbforum.CachedImagePhase.Ready || displayRef == null)) {
            phase = ThreadImageCache.phase(imageRef)
            try {
                val resolved = ThreadImageCache.ensureCached(imageDownloadClient, imageRef)
                if (resolved != null) {
                    displayRef = resolved
                    failed = false
                    ThreadImageCache.clearFailed(imageRef)
                    phase = com.xxyangyoulin.jbforum.CachedImagePhase.Ready
                } else {
                    phase = ThreadImageCache.phase(imageRef)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                displayRef = null
                failed = true
                ThreadImageCache.markFailed(imageRef)
                phase = com.xxyangyoulin.jbforum.CachedImagePhase.Loading
            }
        }
    }
    if (failed && displayRef == null) {
        LoadFailedPlaceholder(
            modifier = placeholderModifier,
            onRetry = {
                failed = false
                ThreadImageCache.clearFailed(imageRef)
                phase = com.xxyangyoulin.jbforum.CachedImagePhase.Loading
                retryNonce += 1
            }
        )
    } else if (displayRef == null) {
        ThumbnailGeneratingPlaceholder(
            text = when (phase) {
                com.xxyangyoulin.jbforum.CachedImagePhase.Loading -> "加载中..."
                com.xxyangyoulin.jbforum.CachedImagePhase.Compressing -> "压缩中..."
                com.xxyangyoulin.jbforum.CachedImagePhase.Ready -> "加载中..."
            },
            modifier = placeholderModifier
        )
    } else {
        PreparedDisplayImage(
            displayRef = displayRef,
            phase = phase,
            imageLoader = imageLoader,
            modifier = trackedModifier,
            contentScale = contentScale,
            resizeWidthPx = resizeWidthPx,
            onLoadSuccess = onImageReady,
            onLoadError = {
                ThreadImageCache.clearCached(imageRef)
                displayRef = null
                failed = true
                ThreadImageCache.markFailed(imageRef)
            }
        )
    }
}

/**
 * 本地收藏图片显示
 */
@Composable
fun CachedLocalDisplayImage(
    item: com.xxyangyoulin.jbforum.LocalFavoriteImage,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    contentScale: androidx.compose.ui.layout.ContentScale = androidx.compose.ui.layout.ContentScale.Crop,
    resizeWidthPx: Int? = null
) {
    var displayRef by remember(item.id, item.thumbnailPath, item.filePath) { mutableStateOf(LocalImageFavorites.thumbnailOrOriginal(item)) }
    var phase by remember(item.id, item.thumbnailPath, item.filePath) {
        mutableStateOf(if (displayRef != null) com.xxyangyoulin.jbforum.CachedImagePhase.Ready else LocalImageFavorites.phase(item))
    }
    val context = LocalContext.current
    var isVisible by remember(item.id) { mutableStateOf(true) }
    val trackedModifier = modifier.onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInWindow()
        val windowHeight = (context as? Activity)?.window?.decorView?.height?.toFloat()
            ?.coerceAtLeast(1f)
            ?: 1f
        isVisible = bounds.bottom > 0f && bounds.top < windowHeight
    }
    LaunchedEffect(item.id, item.thumbnailPath, isVisible, displayRef, phase) {
        if (isVisible && (phase != com.xxyangyoulin.jbforum.CachedImagePhase.Ready || displayRef == null)) {
            phase = LocalImageFavorites.phase(item)
            displayRef = runCatching {
                LocalImageFavorites.ensureThumbnail(item)
            }.getOrNull()
            phase = if (displayRef != null) com.xxyangyoulin.jbforum.CachedImagePhase.Ready else LocalImageFavorites.phase(item)
        }
    }
    PreparedDisplayImage(
        displayRef = displayRef,
        phase = phase,
        imageLoader = imageLoader,
        modifier = trackedModifier,
        contentScale = contentScale,
        resizeWidthPx = resizeWidthPx
    )
}
