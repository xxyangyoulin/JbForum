package com.xxyangyoulin.jbforum.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.xxyangyoulin.jbforum.AppConstants
import com.xxyangyoulin.jbforum.AppColors
import com.xxyangyoulin.jbforum.ForumRepository
import com.xxyangyoulin.jbforum.ThemeMode
import com.xxyangyoulin.jbforum.ThemeModePersistence
import okhttp3.OkHttpClient

/**
 * 论坛应用主题
 */
@Composable
fun ForumTheme(
    content: @Composable () -> Unit
) {
    val darkTheme = when (ThemeModePersistence.mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val forumColors = if (darkTheme) DarkForumColors else LightForumColors
    SideEffect {
        AppColors.applyPalette(forumColors)
    }
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = forumColors.accent,
            onPrimary = Color(0xFF3A0A0A),
            secondary = forumColors.inputBackground,
            background = forumColors.appBackground,
            onBackground = forumColors.titleText,
            surface = forumColors.cardBackground,
            onSurface = forumColors.titleText,
            surfaceBright = forumColors.surfaceBright,
            surfaceContainer = forumColors.surfaceContainer,
            surfaceDim = forumColors.surfaceDim,
            outline = forumColors.cardBorder
        )
    } else {
        lightColorScheme(
            primary = forumColors.accent,
            onPrimary = Color.White,
            secondary = forumColors.inputBackground,
            background = forumColors.appBackground,
            onBackground = forumColors.titleText,
            surface = forumColors.cardBackground,
            onSurface = forumColors.titleText,
            surfaceBright = forumColors.surfaceBright,
            surfaceContainer = forumColors.surfaceContainer,
            surfaceDim = forumColors.surfaceDim,
            outline = forumColors.cardBorder
        )
    }
    CompositionLocalProvider(LocalForumColors provides forumColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ForumTypography,
            shapes = ForumShapes,
            content = content
        )
    }
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
                add(ImageDecoderDecoder.Factory())
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
