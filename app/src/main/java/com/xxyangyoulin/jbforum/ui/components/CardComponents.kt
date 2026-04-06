package com.xxyangyoulin.jbforum.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xxyangyoulin.jbforum.AppColors
import com.xxyangyoulin.jbforum.ui.theme.Dimens

/**
 * Hero 卡片
 */
@Composable
fun HeroCard(
    title: String,
    subtitle: String,
    highlightedSubtitle: String = ""
) {
    OutlinedCard(
        shape = RoundedCornerShape(Dimens.contentCardCorner),
        colors = CardDefaults.outlinedCardColors(containerColor = AppColors.CardBackground),
        border = androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.contentCardPadding)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                color = AppColors.TitleText,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            if (highlightedSubtitle.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = highlightedSubtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.TitleText,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.MutedText
            )
        }
    }
}

/**
 * Header 药丸标签
 */
@Composable
fun HeaderPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AppColors.InputBackground)
            .border(1.dp, AppColors.CardBorder, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = AppColors.MutedText,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
