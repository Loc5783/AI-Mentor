package com.example.asm_final.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/** A readable error surface in both the app's light and dark palettes. */
@Composable
internal fun MentorErrorNotice(message: String, modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val background = if (dark) Color(0xFF5B1F25) else Color(0xFFFFE6E6)
    val foreground = if (dark) Color.White else Color(0xFF842121)
    val outline = if (dark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = background,
        border = BorderStroke(1.dp, outline)
    ) {
        Text(message, Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            color = foreground, style = MaterialTheme.typography.bodyMedium)
    }
}
