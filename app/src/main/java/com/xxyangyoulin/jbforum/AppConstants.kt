package com.xxyangyoulin.jbforum

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.xxyangyoulin.jbforum.ui.theme.ForumColors
import com.xxyangyoulin.jbforum.ui.theme.LightForumColors

/**
 * 应用全局常量
 */
object AppConstants {
    // SharedPreferences 名称
    const val PREFS_COOKIES = "forum_cookies"
    const val PREFS_LOGIN = "forum_login"
    const val PREFS_MESSAGE_STATUS = "forum_message_status"
    const val PREFS_SETTINGS = "forum_settings"
    const val PREFS_LOCAL_FAVORITES = "local_image_favorites"
    const val PREFS_LOCAL_LINK_FAVORITES = "local_link_favorites"
    const val PREFS_THREAD_HISTORY = "thread_browse_history"

    // 缓存目录名称
    const val CACHE_DIR_FAVORITE_IMAGES = "favorite_images"
    const val CACHE_DIR_FAVORITE_THUMBNAILS = "favorite_image_thumbnails"
    const val CACHE_DIR_COIL = "forum_image_cache"
    const val CACHE_DIR_THREAD_ORIGINALS = "thread_image_originals"
    const val CACHE_DIR_THREAD_THUMBNAILS = "thread_image_thumbnails"
    const val CACHE_DIR_THREAD_DETAILS = "thread_detail_cache"

    // 图片压缩阈值 (200KB)
    const val IMAGE_COMPRESS_THRESHOLD_BYTES = 200L * 1024L

    // 缩略图配置
    const val THUMBNAIL_SIZE_FAVORITES = 480
    const val THUMBNAIL_SIZE_THREAD = 720
    const val THUMBNAIL_JPEG_QUALITY_FAVORITES = 82
    const val THUMBNAIL_JPEG_QUALITY_THREAD = 84

    // Coil 缓存配置
    const val COIL_MEMORY_CACHE_PERCENT = 0.1
    const val COIL_DISK_CACHE_PERCENT = 0.03

    // OkHttp 图片请求配置
    const val OKHTTP_MAX_REQUESTS = 2
    const val OKHTTP_MAX_REQUESTS_PER_HOST = 1

    // HTML 解析配置
    const val PARSER_EMOTION_IMAGE_MAX_SIZE = 80
    const val PARSER_STATUS_ICON_MAX_SIZE = 40
    const val PARSER_THREAD_PREVIEW_MAX_IMAGES = 3
    const val PARSER_REDIRECT_MAX_DEPTH = 3

    // 登录 Cookie 有效期 (30天，秒)
    const val LOGIN_COOKIE_LIFETIME_SECONDS = 2592000

    // 日志标签
    const val LOG_TAG = "JbForum"
}

/**
 * UI 颜色常量
 */
object AppColors {
    private var palette by mutableStateOf<ForumColors>(LightForumColors)

    internal fun applyPalette(colors: ForumColors) {
        palette = colors
    }

    val SurfaceBright: Color
        get() = palette.surfaceBright
    val SurfaceContainer: Color
        get() = palette.surfaceContainer
    val SurfaceDim: Color
        get() = palette.surfaceDim
    val AppBackground: Color
        get() = palette.appBackground
    val CardBackground: Color
        get() = palette.cardBackground
    val CardBorder: Color
        get() = palette.cardBorder
    val MutedText: Color
        get() = palette.mutedText
    val TitleText: Color
        get() = palette.titleText
    val AccentGreen: Color
        get() = palette.accent
    val InputBackground: Color
        get() = palette.inputBackground
}

/**
 * UI 尺寸常量
 */
object AppDimensions {
    val FloatingButtonEdgePadding = 16
    val FloatingButtonStackSpacing = 66
    val AuthorAvatarSize = 52
    val SmallAvatarSize = 28
}

/**
 * 扩展函数：判断是否为 GIF 图片
 */
fun isGifImage(imageRef: String): Boolean {
    return imageRef.substringBefore('?').lowercase().endsWith(".gif")
}

/**
 * 格式化字节大小
 */
fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "${bytes} B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.2f GB", gb)
}
