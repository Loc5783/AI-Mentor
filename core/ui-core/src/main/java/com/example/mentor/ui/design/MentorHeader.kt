package com.example.mentor.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import com.example.mentor.ui.R

/** A floating notebook cover; decoration never participates in layout or input. */
@Composable
fun Modifier.mentorHeader(illustration: MentorArtwork = MentorArtwork.LEARNING): Modifier {
    val colors = MaterialTheme.colorScheme
    val artwork = ImageBitmap.imageResource(when (illustration) {
        MentorArtwork.ASSISTANT -> R.drawable.illustration_mentor
        MentorArtwork.PLANNER -> R.drawable.illustration_planner
        MentorArtwork.GOALS -> R.drawable.illustration_goals
        MentorArtwork.IDEAS -> R.drawable.illustration_ideas
        MentorArtwork.LEARNING -> R.drawable.illustration_learning
    })
    val shape = RoundedCornerShape(topStart = 12.dp, topEnd = 28.dp, bottomEnd = 28.dp, bottomStart = 28.dp)
    return padding(horizontal = 16.dp, vertical = 14.dp)
        .shadow(8.dp, shape, ambientColor = colors.primary.copy(alpha = .18f), spotColor = Color.Black.copy(alpha = .18f))
        .clip(shape)
        .background(Brush.horizontalGradient(listOf(colors.surface, colors.primaryContainer)))
        .drawBehind {
            val scale = minOf(100.dp.toPx() / artwork.width, (size.height - 16.dp.toPx()).coerceAtLeast(1f) / artwork.height)
            val width = (artwork.width * scale).toInt().coerceAtLeast(1)
            val height = (artwork.height * scale).toInt().coerceAtLeast(1)
            drawImage(artwork, dstOffset = IntOffset((size.width - width - 8.dp.toPx()).toInt(),
                ((size.height - height) / 2).toInt()), dstSize = IntSize(width, height), alpha = .24f)
        }
}
