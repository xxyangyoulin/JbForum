package com.xxyangyoulin.jbforum.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

val ForumShapes = Shapes(
    extraSmall = RoundedCornerShape(Dimens.cornerSmall),
    small = RoundedCornerShape(Dimens.cornerSmall),
    medium = RoundedCornerShape(Dimens.contentCardCorner),
    large = RoundedCornerShape(Dimens.cornerMedium),
    extraLarge = RoundedCornerShape(Dimens.cornerLarge)
)

val PillShape = RoundedCornerShape(Dimens.cornerPill)
