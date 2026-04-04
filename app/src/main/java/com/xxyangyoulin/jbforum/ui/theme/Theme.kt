package com.xxyangyoulin.jbforum.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.xxyangyoulin.jbforum.AppConstants
import com.xxyangyoulin.jbforum.ForumRepository
import okhttp3.OkHttpClient
import android.os.Build

/**
 * 论坛应用主题
 */
@Composable
fun ForumTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = com.xxyangyoulin.jbforum.AppColors.AccentGreen,
            secondary = com.xxyangyoulin.jbforum.AppColors.InputBackground,
            surface = com.xxyangyoulin.jbforum.AppColors.CardBackground,
            background = com.xxyangyoulin.jbforum.AppColors.AppBackground
        ),
        content = content
    )
}

/**
 * 记住论坛图片加载器
 */
@Composable
fun rememberForumImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember(context) {
        ImageLoader.Builder(context)
            .okHttpClient(ForumRepository().imageClient())
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(AppConstants.COIL_MEMORY_CACHE_PERCENT)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve(AppConstants.CACHE_DIR_COIL))
                    .maxSizePercent(AppConstants.COIL_DISK_CACHE_PERCENT)
                    .build()
            }
            .respectCacheHeaders(false)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
}

/**
 * 记住论坛图片下载客户端
 */
@Composable
fun rememberForumImageDownloadClient(): OkHttpClient {
    val context = LocalContext.current
    return remember(context) { ForumRepository().imageClient() }
}
