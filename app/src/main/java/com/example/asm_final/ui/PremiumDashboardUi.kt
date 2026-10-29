package com.example.asm_final.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.mentor.ui.design.motionEnabled
import com.example.mentor.ui.design.MentorTokens

/** Shared palette for the Home and Competition surfaces. */
internal object PremiumDashboardColors {
    val Navy = MentorTokens.NavyBackground
    val NavyBottom = Color(0xFF1B1A3E)
    val Surface = MentorTokens.NavySurface
    val Elevated = MentorTokens.NavyCard
    val Royal = MentorTokens.DarkPrimaryContainer
    val RoyalDeep = Color(0xFF312D59)
    val Sky = Color(0xFF8EBFFF)
    val Action = Color(0xFF79B3FF)
    val ActionEnd = Color(0xFF5E9DF2)
    val Text = MentorTokens.NavyOnSurface
    val Secondary = MentorTokens.NavyOnSurfaceVariant
    val Muted = Color(0xFF8497B3)
    val Streak = MentorTokens.Gold
    val Experience = MentorTokens.Gold
}

internal data class PremiumDashboardPalette(
    val background: Color,
    val card: Color,
    val elevated: Color,
    val border: Color,
    val title: Color,
    val secondary: Color,
    val muted: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val onHero: Color,
    val actionStart: Color,
    val actionEnd: Color,
    val onAction: Color,
    val iconSurface: Color,
    val accent: Color,
    val streak: Color,
    val experience: Color,
)

@Composable
internal fun premiumDashboardPalette(dark: Boolean): PremiumDashboardPalette {
    val theme = MaterialTheme.colorScheme
    return if (dark) PremiumDashboardPalette(
        background = PremiumDashboardColors.Navy,
        card = PremiumDashboardColors.Surface,
        elevated = PremiumDashboardColors.Elevated,
        border = PremiumDashboardColors.Sky.copy(alpha = .22f),
        title = PremiumDashboardColors.Text,
        secondary = PremiumDashboardColors.Secondary,
        muted = PremiumDashboardColors.Muted,
        heroStart = PremiumDashboardColors.Royal,
        heroEnd = PremiumDashboardColors.RoyalDeep,
        onHero = PremiumDashboardColors.Text,
        actionStart = PremiumDashboardColors.Action,
        actionEnd = PremiumDashboardColors.ActionEnd,
        onAction = PremiumDashboardColors.Navy,
        iconSurface = PremiumDashboardColors.Sky.copy(alpha = .16f),
        accent = PremiumDashboardColors.Sky,
        streak = PremiumDashboardColors.Streak,
        experience = PremiumDashboardColors.Experience,
    ) else PremiumDashboardPalette(
        background = theme.background,
        card = theme.surface,
        elevated = Color(0xFFE8EEFA),
        border = theme.primary.copy(alpha = .24f),
        title = theme.onSurface,
        secondary = theme.onSurfaceVariant,
        muted = theme.onSurfaceVariant.copy(alpha = .78f),
        heroStart = theme.primaryContainer,
        heroEnd = theme.primaryContainer.copy(alpha = .72f),
        onHero = theme.onPrimaryContainer,
        actionStart = theme.primary,
        actionEnd = theme.primary.copy(alpha = .82f),
        onAction = theme.onPrimary,
        iconSurface = theme.primary.copy(alpha = .13f),
        accent = theme.primary,
        streak = Color(0xFFB86A16),
        experience = Color(0xFF9A6A0A),
    )
}

@Composable
internal fun PremiumDashboardHeader(
    palette: PremiumDashboardPalette,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).clip(RoundedCornerShape(24.dp)).background(Brush.horizontalGradient(listOf(palette.heroStart, palette.heroEnd))).drawBehind {
            val x = size.width - 24.dp.toPx()
            drawCircle(palette.streak.copy(alpha = .5f), size.height * .42f, androidx.compose.ui.geometry.Offset(x, size.height * .5f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
            drawLine(palette.accent.copy(alpha = .35f), androidx.compose.ui.geometry.Offset(x - 44.dp.toPx(), size.height), androidx.compose.ui.geometry.Offset(x + 12.dp.toPx(), 0f), 2.dp.toPx())
        }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
internal fun PremiumDashboardCard(
    palette: PremiumDashboardPalette,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (highlighted) 24.dp else 20.dp),
        color = if (highlighted) palette.elevated else palette.card,
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(
            if (palette.background == PremiumDashboardColors.Navy) palette.streak.copy(alpha = .5f) else palette.border,
            palette.border,
        ))),
        shadowElevation = if (highlighted) 2.dp else 0.dp,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
internal fun PremiumDashboardAction(
    label: String,
    palette: PremiumDashboardPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val moving = motionEnabled()
    val scale by animateFloatAsState(if (pressed && moving) .98f else 1f,
        animationSpec = tween(if (moving) 160 else 0), label = "dashboard press")
    Button(
        onClick = onClick,
        modifier = modifier.scale(scale).heightIn(min = 48.dp),
        interactionSource = interaction,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = palette.actionEnd, contentColor = palette.onAction),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    ) { Text(label, maxLines = 2) }
}

@Composable
internal fun PremiumDashboardSegment(
    label: String,
    selected: Boolean,
    palette: PremiumDashboardPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier.heightIn(min = 48.dp).clip(shape)
            .background(if (selected) palette.heroStart else palette.card)
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            color = if (selected) palette.onHero else palette.secondary,
            style = MaterialTheme.typography.labelLarge, maxLines = 2)
    }
}
