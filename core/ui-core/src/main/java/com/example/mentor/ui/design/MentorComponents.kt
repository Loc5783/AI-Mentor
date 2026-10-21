package com.example.mentor.ui.design

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

@Composable
fun motionEnabled(): Boolean {
    return LocalMentorMotion.current
}

@Composable
fun MentorButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val moving = motionEnabled()
    val scale by animateFloatAsState(if (pressed && moving) .98f else 1f,
        animationSpec = if (moving) spring() else snap(), label = "press")
    Button(onClick, modifier.scale(scale).heightIn(min = MentorTokens.TouchTarget),
        enabled = enabled, interactionSource = interactions) { Text(label) }
}

@Composable
fun MentorCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, tonalElevation = 1.dp,
        shadowElevation = MentorTokens.CardElevation) {
        Column(Modifier.padding(MentorTokens.SpaceMedium), verticalArrangement = Arrangement.spacedBy(MentorTokens.SpaceSmall), content = content)
    }
}

@Composable
fun MentorProgress(value: Float, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(value.coerceIn(0f, 1f),
        tween(if (motionEnabled()) 320 else 0), label = "progress")
    LinearProgressIndicator(progress = { progress }, modifier = modifier.fillMaxWidth().height(MentorTokens.SpaceSmall),
        color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
}

/** One draw-only animated brush; no spinner or changing layout during loading. */
@Composable
fun MentorSkeleton(description: String, modifier: Modifier = Modifier) {
    val moving = motionEnabled()
    val phase = if (moving) {
        val transition = rememberInfiniteTransition(label = "skeleton")
        transition.animateFloat(-1f, 2f,
            infiniteRepeatable(tween(1200), RepeatMode.Restart), label = "shimmer")
    } else remember { mutableFloatStateOf(.5f) }
    val base = MaterialTheme.colorScheme.outlineVariant
    Box(modifier.fillMaxWidth().height(72.dp).clip(MaterialTheme.shapes.medium)
        .drawWithContent {
            // Read animation state in drawing, avoiding composition/layout work every frame.
            val x = size.width * phase.value
            drawRect(Brush.linearGradient(listOf(base.copy(alpha = .5f), base.copy(alpha = .12f), base.copy(alpha = .5f)),
                start = Offset(x - size.width, 0f), end = Offset(x + size.width, size.height)))
        }
        .semantics { contentDescription = description; liveRegion = LiveRegionMode.Polite })
}
