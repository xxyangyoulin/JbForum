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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = AppColors.MutedText,
            style = MaterialTheme.typography.bodySmall
        )
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
    resizeWidthPx: Int? = null
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
                    resizeWidthPx?.takeIf { it > 0 }?.let { width ->
                        drawable.setBounds(0, 0, width, (width / drawable.intrinsicWidth.toFloat() * drawable.intrinsicHeight).roundToInt())
                    }
                    drawable.stop()
                    gifImageView.setImageDrawable(drawable)
                }.onFailure {
                    gifImageView.setImageDrawable(null)
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
    resizeWidthPx: Int? = null
) {
    val context = LocalContext.current
    val displayFile = displayRef?.let(::File)
    if (displayFile?.exists() == true) {
        if (isGifImage(displayFile.absolutePath)) {
            AutoPlayGifImage(
                imageRef = displayFile.absolutePath,
                modifier = modifier,
                contentScale = contentScale,
                resizeWidthPx = resizeWidthPx
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
                    val dataKey = displayFile?.absolutePath ?: ""
                    if (imageView.tag != dataKey) {
                        imageView.tag = dataKey
                        imageLoader.enqueue(
                            ImageRequest.Builder(context)
                                .data(displayFile)
                                .crossfade(150)
                                .allowHardware(false)
                                .bitmapConfig(Bitmap.Config.RGB_565)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
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
    showOriginalDirectly: Boolean = false
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
    val context = LocalContext.current
    var isVisible by remember(imageRef) { mutableStateOf(true) }
    val trackedModifier = modifier.onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInWindow()
        val windowHeight = (context as? Activity)?.window?.decorView?.height?.toFloat()
            ?.coerceAtLeast(1f)
            ?: 1f
        isVisible = bounds.bottom > 0f && bounds.top < windowHeight
    }
    LaunchedEffect(imageRef, isVisible, displayRef, phase) {
        if (isVisible && (phase != com.xxyangyoulin.jbforum.CachedImagePhase.Ready || displayRef == null)) {
            phase = ThreadImageCache.phase(imageRef)
            displayRef = runCatching {
                ThreadImageCache.ensureCached(imageDownloadClient, imageRef)
            }.getOrNull() ?: displayRef
            phase = if (displayRef != null) com.xxyangyoulin.jbforum.CachedImagePhase.Ready else ThreadImageCache.phase(imageRef)
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
