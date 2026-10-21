package com.example.mentor.ui.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

/** Semantic tokens shared by every Compose screen. XML aliases use the same palette. */
object MentorTokens {
    val OnBrand = Color.White
    val Blue = Color(0xFF2563EB)
    val BlueDark = Color(0xFF1D4ED8)
    val BlueLight = Color(0xFF60A5FA)
    val Indigo = Color(0xFF4F46E5)
    val Amber = Color(0xFFF59E0B)
    val Ink = Color(0xFF0B1220)
    val LightBackground = Color(0xFFF8FAFC)
    val DarkSurface = Color(0xFF111B2E)
    val Success = Color(0xFF22C55E)
    val Error = Color(0xFFEF4444)
    val Locked = Color(0xFF94A3B8)
    val SpaceTiny = 4.dp
    val SpaceCompact = 12.dp
    val PagePadding = 20.dp
    val CardElevation = 2.dp
    val ActionElevation = 3.dp
    val ContentMaxWidth = 840.dp
    val SpaceSmall = 8.dp
    val SpaceMedium = 16.dp
    val SpaceLarge = 24.dp
    val CardRadius = 20.dp
    val TouchTarget = 48.dp
    val AvatarSize = 64.dp
    val ChartHeight = 144.dp
    const val MotionMillis = 220
}

private val LightColors = lightColorScheme(
    primary = MentorTokens.Blue, onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE), onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF92400E), secondaryContainer = MentorTokens.Amber,
    onSecondaryContainer = MentorTokens.Ink,
    background = MentorTokens.LightBackground, onBackground = MentorTokens.Ink,
    surface = Color.White, onSurface = MentorTokens.Ink,
    surfaceVariant = Color(0xFFEFF6FF), onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF64748B), outlineVariant = Color(0xFFCBD5E1),
    error = Color(0xFFB91C1C),
)
private val DarkColors = darkColorScheme(
    primary = MentorTokens.BlueLight, onPrimary = MentorTokens.Ink,
    primaryContainer = Color(0xFF1E3A8A), onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = MentorTokens.Amber, secondaryContainer = MentorTokens.Amber,
    onSecondaryContainer = MentorTokens.Ink,
    background = MentorTokens.Ink, onBackground = Color(0xFFE2E8F0),
    surface = MentorTokens.DarkSurface, onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E293B), onSurfaceVariant = Color(0xFFCBD5E1),
    outline = MentorTokens.Locked, outlineVariant = Color(0xFF334155),
    error = Color(0xFFFCA5A5),
)

@Composable
fun MentorTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMentorMotion provides rememberMotionPolicy()) {
      MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography(),
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(MentorTokens.CardRadius),
            large = RoundedCornerShape(28.dp),
        ), content = content,
      )
    }
}
