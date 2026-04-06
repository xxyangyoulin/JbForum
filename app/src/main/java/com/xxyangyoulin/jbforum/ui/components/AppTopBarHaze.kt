package com.xxyangyoulin.jbforum.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

@OptIn(ExperimentalHazeApi::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun Modifier.appTopBarHaze(hazeState: HazeState): Modifier {
    return hazeEffect(
        state = hazeState,
        style = HazeMaterials.thin()
    ) {
        noiseFactor = 0f
        inputScale = HazeInputScale.Fixed(1f)
    }
}
