package com.example.mentor.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Draw-only ornament: never changes header measurement or intercepts input. */
@Composable
fun Modifier.mentorHeader(): Modifier {
    val colors = MaterialTheme.colorScheme
    return padding(horizontal = 16.dp, vertical = 12.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(Brush.horizontalGradient(listOf(colors.primaryContainer, colors.tertiaryContainer)))
        .drawBehind {
            val x = size.width - 22.dp.toPx()
            drawCircle(colors.secondary.copy(alpha = .48f), size.height * .4f,
                Offset(x, size.height * .5f), style = Stroke(1.dp.toPx()))
            drawLine(colors.primary.copy(alpha = .24f),
                Offset(x - 46.dp.toPx(), size.height), Offset(x + 10.dp.toPx(), 0f), 2.dp.toPx())
            drawCircle(colors.secondary.copy(alpha = .65f), 3.dp.toPx(), Offset(x - 28.dp.toPx(), 16.dp.toPx()))
        }
}
