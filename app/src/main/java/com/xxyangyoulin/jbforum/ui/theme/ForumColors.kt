package com.xxyangyoulin.jbforum.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ForumColors(
    val appBackground: Color,
    val cardBackground: Color,
    val inputBackground: Color,
    val surfaceBright: Color,
    val surfaceContainer: Color,
    val surfaceDim: Color,
    val cardBorder: Color,
    val titleText: Color,
    val mutedText: Color,
    val accent: Color
)

val LightForumColors = ForumColors(
    appBackground = Color(0xFFEEF3F8),
    cardBackground = Color(0xFFFFFFFF),
    inputBackground = Color(0xFFE8EDF3),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF4F7FA),
    surfaceDim = Color(0xFFE8EDF3),
    cardBorder = Color(0xFFDCE3EC),
    titleText = Color(0xFF1F2937),
    mutedText = Color(0xFF8B94A1),
    accent = Color(0xFFCB0000)
)

val DarkForumColors = ForumColors(
    appBackground = Color(0xFF0F1318),
    cardBackground = Color(0xFF151B22),
    inputBackground = Color(0xFF1D2630),
    surfaceBright = Color(0xFF1D2630),
    surfaceContainer = Color(0xFF151B22),
    surfaceDim = Color(0xFF0B0F13),
    cardBorder = Color(0xFF2A3340),
    titleText = Color(0xFFD4DEE8),
    mutedText = Color(0xFF8A98AA),
    accent = Color(0xFFFF5A5A)
)

val LocalForumColors = staticCompositionLocalOf { LightForumColors }
