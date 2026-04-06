package com.xxyangyoulin.jbforum

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import okhttp3.Request
import kotlin.math.abs
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
                        add(ImageDecoderDecoder.Factory())
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
    val pageImageSizes = remember(images) { mutableStateListOf<IntSize?>().apply { repeat(images.size) { add(null) } } }
    val coroutineScope = rememberCoroutineScope()
    var actionTargetIndex by remember { mutableStateOf<Int?>(null) }
    var detailTargetIndex by remember { mutableStateOf<Int?>(null) }
    var overlayVisible by remember { mutableStateOf(true) }
    var overlayInteractionNonce by remember { mutableStateOf(0) }
    var enterTarget by remember { mutableStateOf(if (launchSource != null) 0f else 1f) }
    var dismissDragProgress by remember { mutableStateOf(0f) }
    val enterProgress by animateFloatAsState(
        targetValue = enterTarget,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        finishedListener = { if (it == 0f) onDismiss() }
    )
    var dismissing by remember { mutableStateOf(false) }
    val hasLaunchSource = launchSource != null

    fun dismissWithAnimation() {
        if (dismissing) return
        dismissing = true
        if (hasLaunchSource && pagerState.currentPage == initialIndex) {
            enterTarget = 0f
        } else {
            onDismiss()
        }
    }

    fun showOverlay() {
        overlayVisible = true
        overlayInteractionNonce += 1
    }

    fun toggleOverlay() {
        overlayVisible = !overlayVisible
        if (overlayVisible) overlayInteractionNonce += 1
    }

    // 启动进入动画
    LaunchedEffect(Unit) {
        if (launchSource != null) {
            enterTarget = 1f
        }
    }

    LaunchedEffect(pagerState.currentPage, images) {
        showOverlay()
        val preloadIndexes = listOf(pagerState.currentPage - 1, pagerState.currentPage + 1)
            .filter { it in images.indices }
        preloadIndexes.forEach { index ->
            imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(images[index].imageRef)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .build()
            )
        }
    }

    LaunchedEffect(overlayVisible, overlayInteractionNonce, actionTargetIndex, detailTargetIndex) {
        if (!overlayVisible || actionTargetIndex != null || detailTargetIndex != null) return@LaunchedEffect
        delay(2500)
        overlayVisible = false
    }

    DisposableEffect(view) {
        val controller = WindowInsetsControllerCompat((context as ComponentActivity).window, view)
        controller.hide(WindowInsetsCompat.Type.statusBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {}
    }

    BackHandler(onBack = ::dismissWithAnimation)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = enterProgress * (1f - dismissDragProgress.coerceIn(0f, 0.85f))))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = pageScales.getOrElse(pagerState.currentPage) { 1f } <= 1.01f
        ) { page ->
            var scale by remember(page) { mutableStateOf(1f) }
            var offsetX by remember(page) { mutableStateOf(0f) }
            var offsetY by remember(page) { mutableStateOf(0f) }
            var dragOffsetY by remember(page) { mutableStateOf(0f) }
            var containerSize by remember(page) { mutableStateOf(IntSize.Zero) }
            var imageSize by remember(page) { mutableStateOf(IntSize.Zero) }
            var loadFailed by remember(page) { mutableStateOf(false) }
            var retryNonce by remember(page) { mutableStateOf(0) }
            var zoomAnimationJob by remember(page) { mutableStateOf<Job?>(null) }
            var dismissAnimationJob by remember(page) { mutableStateOf<Job?>(null) }

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

            fun fitScale(): Float {
                val containerWidth = containerSize.width.toFloat()
                val containerHeight = containerSize.height.toFloat()
                val imageWidth = imageSize.width.toFloat()
                val imageHeight = imageSize.height.toFloat()
                return if (imageWidth > 0f && imageHeight > 0f && containerWidth > 0f && containerHeight > 0f) {
                    minOf(containerWidth / imageWidth, containerHeight / imageHeight)
                } else {
                    1f
                }
            }

            fun lerp(start: Float, end: Float, progress: Float): Float {
                return start + (end - start) * progress
            }

            fun animateZoom(targetScale: Float, targetOffsetX: Float, targetOffsetY: Float) {
                zoomAnimationJob?.cancel()
                val startScale = scale
                val startOffsetX = offsetX
                val startOffsetY = offsetY
                zoomAnimationJob = coroutineScope.launch {
                    animate(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                    ) { progress, _ ->
                        scale = lerp(startScale, targetScale, progress)
                        pageScales[page] = scale
                        offsetX = lerp(startOffsetX, targetOffsetX, progress)
                        offsetY = lerp(startOffsetY, targetOffsetY, progress)
                    }
                }
            }

            fun animateDragOffset(targetOffsetY: Float, onFinished: (() -> Unit)? = null) {
                dismissAnimationJob?.cancel()
                val startOffsetY = dragOffsetY
                dismissAnimationJob = coroutineScope.launch {
                    animate(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                    ) { progress, _ ->
                        dragOffsetY = lerp(startOffsetY, targetOffsetY, progress)
                        if (pagerState.currentPage == page && containerSize.height > 0) {
                            dismissDragProgress = (abs(dragOffsetY) / (containerSize.height * 0.35f)).coerceIn(0f, 1f)
                        }
                    }
                    onFinished?.invoke()
                }
            }

            fun toggleZoom(tapOffset: Offset) {
                val nextScale = if (scale > 1.01f) {
                    1f
                } else {
                    val imageWidth = imageSize.width.toFloat()
                    val imageHeight = imageSize.height.toFloat()
                    val containerWidth = containerSize.width.toFloat()
                    val containerHeight = containerSize.height.toFloat()
                    if (imageWidth <= 0f || imageHeight <= 0f || containerWidth <= 0f || containerHeight <= 0f) {
                        2.5f
                    } else {
                        val baseFitScale = fitScale()
                        val widthFillScale = (containerWidth / (imageWidth * baseFitScale)).coerceAtLeast(1f)
                        val heightFillScale = (containerHeight / (imageHeight * baseFitScale)).coerceAtLeast(1f)
                        maxOf(widthFillScale, heightFillScale, 2f).coerceIn(1f, 5f)
                    }
                }
                if (nextScale <= 1.01f) {
                    animateZoom(1f, 0f, 0f)
                    return
                }
                val containerCenter = Offset(containerSize.width / 2f, containerSize.height / 2f)
                val scaleRatio = nextScale / scale
                val rawOffsetX = (offsetX + containerCenter.x - tapOffset.x) * scaleRatio - (containerCenter.x - tapOffset.x)
                val rawOffsetY = (offsetY + containerCenter.y - tapOffset.y) * scaleRatio - (containerCenter.y - tapOffset.y)
                val (clampedX, clampedY) = clampOffsets(nextScale, rawOffsetX, rawOffsetY)
                animateZoom(nextScale, clampedX, clampedY)
            }

            val transformableState = rememberTransformableState { _, zoomChange, panChange, _ ->
                zoomAnimationJob?.cancel()
                dismissAnimationJob?.cancel()
                showOverlay()
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
                    .draggable(
                        orientation = androidx.compose.foundation.gestures.Orientation.Vertical,
                        enabled = scale <= 1.01f,
                        state = rememberDraggableState { delta ->
                            dismissAnimationJob?.cancel()
                            showOverlay()
                            dragOffsetY += delta
                            if (pagerState.currentPage == page && containerSize.height > 0) {
                                dismissDragProgress = (abs(dragOffsetY) / (containerSize.height * 0.35f)).coerceIn(0f, 1f)
                            }
                        },
                        onDragStopped = {
                            val threshold = containerSize.height * 0.05f
                            if (scale <= 1.01f && abs(dragOffsetY) >= threshold) {
                                val target = if (dragOffsetY >= 0f) containerSize.height.toFloat() else -containerSize.height.toFloat()
                                animateDragOffset(target) { onDismiss() }
                            } else {
                                animateDragOffset(0f)
                            }
                        }
                    )
                    .pointerInput(page, scale, containerSize, imageSize) {
                        detectTapGestures(
                            onTap = { toggleOverlay() },
                            onLongPress = {
                                showOverlay()
                                actionTargetIndex = page
                            },
                            onDoubleTap = { tapOffset ->
                                showOverlay()
                                toggleZoom(tapOffset)
                            }
                        )
                    }
            ) {
                val imageWidth = imageSize.width.toFloat()
                val imageHeight = imageSize.height.toFloat()
                val containerWidth = containerSize.width.toFloat()
                val containerHeight = containerSize.height.toFloat()
                val fitScale = fitScale()
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
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = remember(page, retryNonce, images[page].imageRef, launchSource, initialIndex) {
                            ImageRequest.Builder(context)
                                .data(images[page].imageRef)
                                .crossfade(if (launchSource != null && page == initialIndex) 0 else 150)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .networkCachePolicy(CachePolicy.ENABLED)
                                .build()
                        },
                        imageLoader = imageLoader,
                        onSuccess = { state ->
                            loadFailed = false
                            imageSize = IntSize(
                                state.result.drawable.intrinsicWidth.coerceAtLeast(1),
                                state.result.drawable.intrinsicHeight.coerceAtLeast(1)
                            )
                            pageImageSizes[page] = imageSize
                            val (clampedX, clampedY) = clampOffsets(scale, offsetX, offsetY)
                            offsetX = clampedX
                            offsetY = clampedY
                        },
                        onError = {
                            loadFailed = true
                        },
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .transformable(state = transformableState, canPan = { scale > 1.01f })
                            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                            .graphicsLayer(
                                scaleX = scale * (launchScaleX + (1f - launchScaleX) * enterProgress),
                                scaleY = scale * (launchScaleY + (1f - launchScaleY) * enterProgress),
                                translationX = launchTranslationX * (1f - enterProgress),
                                translationY = launchTranslationY * (1f - enterProgress) + dragOffsetY
                            )
                    )
                    if (loadFailed) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        )
                        {
                            Text("图片加载失败", color = Color.White)
                            OutlinedButton(
                                onClick = {
                                    loadFailed = false
                                    retryNonce += 1
                                },
                                modifier = Modifier.height(44.dp)
                            ) {
                                Text("重试")
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "${pagerState.currentPage + 1} / ${images.size}",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 42.dp)
                .graphicsLayer(alpha = enterProgress * if (overlayVisible) 1f else 0f)
        )
    }

    actionTargetIndex?.let { page ->
        val image = images[page]
        AlertDialog(
            onDismissRequest = { actionTargetIndex = null },
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
            confirmButton = {
                TextButton(onClick = { actionTargetIndex = null }) {
                    Text("取消")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        actionTargetIndex = null
                        detailTargetIndex = page
                    }
                ) {
                    Text("图片详情", color = Color.Gray)
                }
            }
        )
    }

    detailTargetIndex?.let { page ->
        val image = images[page]
        val details = remember(page, image.imageRef, pageImageSizes[page]) {
            buildImageDetails(
                imageRef = image.imageRef,
                size = pageImageSizes[page]
            )
        }
        AlertDialog(
            onDismissRequest = { detailTargetIndex = null },
            title = { Text("图片详情") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("链接: ${details.link}")
                    Text("格式: ${details.format}")
                    Text("尺寸: ${details.dimensionText}")
                    details.fileSizeText?.let { Text("大小: $it") }
                }
            },
            confirmButton = {
                TextButton(onClick = { detailTargetIndex = null }) {
                    Text("关闭")
                }
            }
        )
    }
}

private data class GalleryImagePayload(
    val bytes: ByteArray,
    val extension: String,
    val mimeType: String
)

private suspend fun saveImageToGallery(
    context: Context,
    client: okhttp3.OkHttpClient,
    imageUrl: String
) {
    val payload = withContext(Dispatchers.IO) {
        if (imageUrl.startsWith("http")) {
            client.newCall(
                Request.Builder()
                    .url(imageUrl)
                    .header("Referer", ForumDomainConfig.requireBaseUrl())
                    .build()
            ).execute().use { response ->
                if (!response.isSuccessful) error("下载图片失败: ${response.code}")
                val bytes = response.body?.bytes() ?: error("图片内容为空")
                val fallback = imageFormatFor(imageUrl)
                val mimeType = response.body?.contentType()?.toString()?.substringBefore(';')
                    ?.takeIf { it.startsWith("image/") }
                    ?: fallback.mimeType
                val extension = extensionForMimeType(mimeType) ?: fallback.extension
                GalleryImagePayload(bytes, extension, mimeType)
            }
        } else {
            val file = java.io.File(imageUrl).takeIf { it.exists() } ?: error("图片内容为空")
            val format = imageFormatFor(imageUrl)
            GalleryImagePayload(file.readBytes(), format.extension, format.mimeType)
        }
    }
    withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val filename = "jbforum_${System.currentTimeMillis()}.${payload.extension}"
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, payload.mimeType)
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/JbForum")
            put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("无法创建相册文件")
        resolver.openOutputStream(uri)?.use { it.write(payload.bytes) } ?: error("无法写入相册文件")
        resolver.update(uri, android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
        }, null, null)
    }
}

private fun imageFormatFor(imageRef: String): GalleryImagePayload {
    val normalized = imageRef.substringBefore('?').lowercase()
    return when {
        normalized.endsWith(".gif") -> GalleryImagePayload(ByteArray(0), "gif", "image/gif")
        normalized.endsWith(".png") -> GalleryImagePayload(ByteArray(0), "png", "image/png")
        normalized.endsWith(".webp") -> GalleryImagePayload(ByteArray(0), "webp", "image/webp")
        normalized.endsWith(".jpeg") -> GalleryImagePayload(ByteArray(0), "jpeg", "image/jpeg")
        else -> GalleryImagePayload(ByteArray(0), "jpg", "image/jpeg")
    }
}

private fun extensionForMimeType(mimeType: String): String? {
    return when (mimeType.lowercase()) {
        "image/gif" -> "gif"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/jpeg" -> "jpg"
        else -> null
    }
}

private data class PreviewImageDetails(
    val link: String,
    val format: String,
    val dimensionText: String,
    val fileSizeText: String?
)

private fun buildImageDetails(imageRef: String, size: IntSize?): PreviewImageDetails {
    val normalized = imageRef.substringBefore('?')
    val cachedPreviewPath = ThreadImageCache.previewRef(imageRef).takeIf { it != imageRef }
    val localFile = cachedPreviewPath?.let { path -> java.io.File(path) }?.takeIf { file -> file.exists() }
        ?: java.io.File(imageRef).takeIf { file -> file.exists() }
    val fileSizeText = localFile?.length()?.takeIf { it > 0 }?.let(::formatPreviewBytes)
    val actualSize = size ?: localFile?.let { file ->
        BitmapFactory.Options().apply { inJustDecodeBounds = true }.also {
            BitmapFactory.decodeFile(file.absolutePath, it)
        }.let { bounds ->
            if (bounds.outWidth > 0 && bounds.outHeight > 0) IntSize(bounds.outWidth, bounds.outHeight) else null
        }
    }
    val format = normalized.substringAfterLast('.', "").ifBlank { "unknown" }.uppercase()
    val dimensionText = actualSize?.let { "${it.width} x ${it.height}" } ?: "未知"
    return PreviewImageDetails(
        link = imageRef,
        format = format,
        dimensionText = dimensionText,
        fileSizeText = fileSizeText
    )
}

private fun formatPreviewBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / 1024f / 1024f)
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024f)
        else -> "$bytes B"
    }
}
