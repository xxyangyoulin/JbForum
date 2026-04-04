package com.xxyangyoulin.jbforum

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import okhttp3.Request
import kotlin.math.roundToInt

@Parcelize
data class PreviewImageItem(
    val imageRef: String,
    val sourceThreadTitle: String = "",
    val sourceThreadUrl: String = "",
    val canFavorite: Boolean = true
) : Parcelable

@Parcelize
data class PreviewLaunchSource(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int
) : Parcelable

class ImagePreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        enableEdgeToEdge()
        val images = intent.getParcelableArrayListExtraCompat<PreviewImageItem>(EXTRA_IMAGES)
        val initialIndex = intent.getIntExtra(EXTRA_INITIAL_INDEX, 0)
        val launchSource = intent.getParcelableExtraCompat<PreviewLaunchSource>(EXTRA_LAUNCH_SOURCE)
        setContent {
            val imageLoader = remember(this) {
                ImageLoader.Builder(this)
                    .okHttpClient(ForumRepository().imageClient())
                    .memoryCache {
                        MemoryCache.Builder(this)
                            .maxSizePercent(0.1)
                            .build()
                    }
                    .diskCache {
                        DiskCache.Builder()
                            .directory(cacheDir.resolve("forum_image_cache"))
                            .maxSizePercent(0.03)
                            .build()
                    }
                    .respectCacheHeaders(false)
                    .components {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            add(ImageDecoderDecoder.Factory())
                        } else {
                            add(GifDecoder.Factory())
                        }
                    }
                    .build()
            }
            val imageDownloadClient = remember(this) { ForumRepository().imageClient() }
            MaterialTheme {
                ImagePreviewScreen(
                    images = images,
                    initialIndex = initialIndex,
                    launchSource = launchSource,
                    imageLoader = imageLoader,
                    imageDownloadClient = imageDownloadClient,
                    onDismiss = { finish() },
                    onRefreshFavorites = {
                        setResult(RESULT_OK, Intent().putExtra(EXTRA_REFRESH_FAVORITES, true))
                    },
                    onOpenSourceThread = { title, url ->
                        setResult(
                            RESULT_OK,
                            Intent()
                                .putExtra(EXTRA_OPEN_THREAD_URL, url)
                                .putExtra(EXTRA_OPEN_THREAD_TITLE, title)
                                .putExtra(EXTRA_REFRESH_FAVORITES, true)
                        )
                        finish()
                    }
                )
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        private const val EXTRA_IMAGES = "images"
        private const val EXTRA_INITIAL_INDEX = "initial_index"
        private const val EXTRA_LAUNCH_SOURCE = "launch_source"
        const val EXTRA_REFRESH_FAVORITES = "refresh_favorites"
        const val EXTRA_OPEN_THREAD_URL = "open_thread_url"
        const val EXTRA_OPEN_THREAD_TITLE = "open_thread_title"

        fun createIntent(
            context: Context,
            images: List<PreviewImageItem>,
            initialIndex: Int,
            launchSource: PreviewLaunchSource? = null
        ): Intent {
            return Intent(context, ImagePreviewActivity::class.java).apply {
                putParcelableArrayListExtra(EXTRA_IMAGES, ArrayList(images))
                putExtra(EXTRA_INITIAL_INDEX, initialIndex)
                putExtra(EXTRA_LAUNCH_SOURCE, launchSource)
            }
        }
    }
}

@Suppress("DEPRECATION")
private inline fun <reified T : Parcelable> Intent.getParcelableArrayListExtraCompat(name: String): ArrayList<T> {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayListExtra(name, T::class.java) ?: arrayListOf()
    } else {
        getParcelableArrayListExtra(name) ?: arrayListOf()
    }
}

@Suppress("DEPRECATION")
private inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(name: String): T? {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(name, T::class.java)
    } else {
        getParcelableExtra(name)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImagePreviewScreen(
    images: List<PreviewImageItem>,
    initialIndex: Int,
    launchSource: PreviewLaunchSource?,
    imageLoader: ImageLoader,
    imageDownloadClient: okhttp3.OkHttpClient,
    onDismiss: () -> Unit,
    onRefreshFavorites: () -> Unit,
    onOpenSourceThread: (String, String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val view = LocalView.current
    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, (images.size - 1).coerceAtLeast(0)), pageCount = { images.size })
    val pageScales = remember(images) { mutableStateListOf<Float>().apply { repeat(images.size) { add(1f) } } }
    val coroutineScope = rememberCoroutineScope()
    var actionTargetIndex by remember { mutableStateOf<Int?>(null) }
    val enterProgress = remember { Animatable(if (launchSource != null) 0f else 1f) }
    var dismissing by remember { mutableStateOf(false) }

    fun dismissWithAnimation() {
        if (dismissing) return
        dismissing = true
        coroutineScope.launch {
            if (launchSource != null && pagerState.currentPage == initialIndex) {
                enterProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                )
            }
            onDismiss()
        }
    }

    DisposableEffect(view) {
        val controller = WindowInsetsControllerCompat((context as ComponentActivity).window, view)
        controller.hide(WindowInsetsCompat.Type.statusBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {}
    }

    DisposableEffect(launchSource, initialIndex) {
        val job = coroutineScope.launch {
            if (launchSource != null) {
                enterProgress.snapTo(0f)
                enterProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                )
            } else {
                enterProgress.snapTo(1f)
            }
        }
        onDispose { job.cancel() }
    }

    BackHandler(onBack = ::dismissWithAnimation)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = enterProgress.value))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = pageScales.getOrElse(pagerState.currentPage) { 1f } <= 1.01f
        ) { page ->
            var scale by remember(page) { mutableStateOf(1f) }
            var offsetX by remember(page) { mutableStateOf(0f) }
            var offsetY by remember(page) { mutableStateOf(0f) }
            var containerSize by remember(page) { mutableStateOf(IntSize.Zero) }
            var imageSize by remember(page) { mutableStateOf(IntSize.Zero) }

            fun clampOffsets(targetScale: Float, targetOffsetX: Float, targetOffsetY: Float): Pair<Float, Float> {
                val containerWidth = containerSize.width.toFloat()
                val containerHeight = containerSize.height.toFloat()
                val imageWidth = imageSize.width.toFloat()
                val imageHeight = imageSize.height.toFloat()
                if (containerWidth <= 0f || containerHeight <= 0f) return 0f to 0f
                val (maxOffsetX, maxOffsetY) = if (imageWidth > 0f && imageHeight > 0f) {
                    val fitScale = minOf(containerWidth / imageWidth, containerHeight / imageHeight)
                    val displayedWidth = imageWidth * fitScale * targetScale
                    val displayedHeight = imageHeight * fitScale * targetScale
                    (((displayedWidth - containerWidth) / 2f).coerceAtLeast(0f)) to
                        (((displayedHeight - containerHeight) / 2f).coerceAtLeast(0f))
                } else {
                    (((containerWidth * targetScale) - containerWidth) / 2f).coerceAtLeast(0f) to
                        (((containerHeight * targetScale) - containerHeight) / 2f).coerceAtLeast(0f)
                }
                return targetOffsetX.coerceIn(-maxOffsetX, maxOffsetX) to
                    targetOffsetY.coerceIn(-maxOffsetY, maxOffsetY)
            }

            val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
                val updatedScale = (scale * zoomChange).coerceIn(1f, 5f)
                scale = updatedScale
                pageScales[page] = updatedScale
                if (updatedScale > 1.01f) {
                    val (clampedX, clampedY) = clampOffsets(updatedScale, offsetX + panChange.x, offsetY + panChange.y)
                    offsetX = clampedX
                    offsetY = clampedY
                } else {
                    offsetX = 0f
                    offsetY = 0f
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { containerSize = it }
                    .combinedClickable(
                        onClick = ::dismissWithAnimation,
                        onDoubleClick = {
                            if (scale > 1.01f) {
                                scale = 1f
                                pageScales[page] = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                scale = 2.5f
                                pageScales[page] = 2.5f
                            }
                        },
                        onLongClick = { actionTargetIndex = page }
                    )
            ) {
                val imageWidth = imageSize.width.toFloat()
                val imageHeight = imageSize.height.toFloat()
                val containerWidth = containerSize.width.toFloat()
                val containerHeight = containerSize.height.toFloat()
                val fitScale = if (imageWidth > 0f && imageHeight > 0f && containerWidth > 0f && containerHeight > 0f) {
                    minOf(containerWidth / imageWidth, containerHeight / imageHeight)
                } else {
                    1f
                }
                val displayedWidth = if (imageWidth > 0f) imageWidth * fitScale else containerWidth
                val displayedHeight = if (imageHeight > 0f) imageHeight * fitScale else containerHeight
                val displayedCenterX = containerWidth / 2f
                val displayedCenterY = containerHeight / 2f
                val launchScaleX = if (launchSource != null && displayedWidth > 0f && page == initialIndex) {
                    launchSource.width.toFloat() / displayedWidth
                } else {
                    1f
                }
                val launchScaleY = if (launchSource != null && displayedHeight > 0f && page == initialIndex) {
                    launchSource.height.toFloat() / displayedHeight
                } else {
                    1f
                }
                val launchTranslationX = if (launchSource != null && page == initialIndex) {
                    launchSource.left + launchSource.width / 2f - displayedCenterX
                } else {
                    0f
                }
                val launchTranslationY = if (launchSource != null && page == initialIndex) {
                    launchSource.top + launchSource.height / 2f - displayedCenterY
                } else {
                    0f
                }
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(images[page].imageRef)
                        .crossfade(true)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .networkCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    imageLoader = imageLoader,
                    onSuccess = { state ->
                        imageSize = IntSize(
                            state.result.drawable.intrinsicWidth.coerceAtLeast(1),
                            state.result.drawable.intrinsicHeight.coerceAtLeast(1)
                        )
                        val (clampedX, clampedY) = clampOffsets(scale, offsetX, offsetY)
                        offsetX = clampedX
                        offsetY = clampedY
                    },
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .transformable(state = transformableState, canPan = { scale > 1.01f })
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                        .graphicsLayer(
                            scaleX = scale * (launchScaleX + (1f - launchScaleX) * enterProgress.value),
                            scaleY = scale * (launchScaleY + (1f - launchScaleY) * enterProgress.value),
                            translationX = launchTranslationX * (1f - enterProgress.value),
                            translationY = launchTranslationY * (1f - enterProgress.value)
                        )
                )
            }
        }

        Text(
            text = "${pagerState.currentPage + 1} / ${images.size}",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 42.dp)
                .graphicsLayer(alpha = enterProgress.value)
        )
    }

    actionTargetIndex?.let { page ->
        val image = images[page]
        AlertDialog(
            onDismissRequest = { actionTargetIndex = null },
            title = { Text("图片操作") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            actionTargetIndex = null
                            coroutineScope.launch {
                                val message = runCatching {
                                    saveImageToGallery(context, imageDownloadClient, image.imageRef)
                                }.fold(
                                    onSuccess = { "图片已保存到相册" },
                                    onFailure = { it.message ?: "保存失败" }
                                )
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存图片")
                    }
                    if (image.canFavorite) {
                        OutlinedButton(
                            onClick = {
                                actionTargetIndex = null
                                coroutineScope.launch {
                                    val message = runCatching {
                                        LocalImageFavorites.add(
                                            context = context,
                                            client = imageDownloadClient,
                                            imageRef = image.imageRef,
                                            sourceThreadTitle = image.sourceThreadTitle,
                                            sourceThreadUrl = image.sourceThreadUrl
                                        )
                                    }.fold(
                                        onSuccess = {
                                            onRefreshFavorites()
                                            "图片已收藏到本地"
                                        },
                                        onFailure = { it.message ?: "收藏失败" }
                                    )
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("收藏图片")
                        }
                    }
                    if (!image.canFavorite && image.sourceThreadUrl.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                actionTargetIndex = null
                                onOpenSourceThread(image.sourceThreadTitle, image.sourceThreadUrl)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("查看原帖")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { actionTargetIndex = null }) {
                    Text("取消")
                }
            }
        )
    }
}

private suspend fun saveImageToGallery(
    context: Context,
    client: okhttp3.OkHttpClient,
    imageUrl: String
) {
    val bytes = withContext(Dispatchers.IO) {
        if (imageUrl.startsWith("http")) {
            client.newCall(
                Request.Builder()
                    .url(imageUrl)
                    .header("Referer", ForumDomainConfig.requireBaseUrl())
                    .build()
            ).execute().use { response ->
                if (!response.isSuccessful) error("下载图片失败: ${response.code}")
                response.body?.bytes() ?: error("图片内容为空")
            }
        } else {
            java.io.File(imageUrl).takeIf { it.exists() }?.readBytes() ?: error("图片内容为空")
        }
    }
    withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val filename = "jbforum_${System.currentTimeMillis()}.jpg"
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/JbForum")
                put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("无法创建相册文件")
        resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("无法写入相册文件")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            resolver.update(uri, android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
            }, null, null)
        }
    }
}
