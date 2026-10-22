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
    val Primary = Color(0xFFBE185D)
    val PrimaryDark = Color(0xFF9D174D)
    val PrimaryLight = Color(0xFFF9A8D4)
    val PrimaryContainer = Color(0xFFFFD9EC)
    val DarkPrimaryContainer = Color(0xFF5B2543)
    val HeroEnd = Color(0xFFD94D87)
    val DarkHeroEnd = Color(0xFF512147)
    val Peach = Color(0xFFFFD9BB)
    val WarmAccent = Color(0xFFFFC57E)
    val TextPrimary = Color(0xFF25152A)
    val TextSecondary = Color(0xFF6E6071)
    val LightBackground = Color(0xFFFFF7FB)
    val DarkBackground = Color(0xFF18111F)
    val DarkSurface = Color(0xFF241B2B)
    val Success = Color(0xFF19734F)
    val DarkSuccess = Color(0xFF84DEB2)
    val Warning = Color(0xFF9A5B00)
    val Error = Color(0xFFB42342)
    val Info = Color(0xFF2459A5)
    val Locked = Color(0xFF94A3B8)
    val Completed = Success
    val Current = Primary
    val Disabled = Color(0xFFB6A9B5)
    val Divider = Color(0xFFE6D9E2)
    val ProgressTrack = Color(0xFFF3DDE9)
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
    primary = MentorTokens.Primary, onPrimary = MentorTokens.OnBrand,
    primaryContainer = MentorTokens.PrimaryContainer, onPrimaryContainer = MentorTokens.PrimaryDark,
    secondary = MentorTokens.Warning, onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE8C7), onSecondaryContainer = Color(0xFF5B3A00),
    background = MentorTokens.LightBackground, onBackground = MentorTokens.TextPrimary,
    surface = Color.White, onSurface = MentorTokens.TextPrimary,
    surfaceVariant = Color(0xFFFFEAF3), onSurfaceVariant = MentorTokens.TextSecondary,
    outline = Color(0xFF8E687A), outlineVariant = MentorTokens.Divider,
    error = MentorTokens.Error,
)
private val DarkColors = darkColorScheme(
    primary = MentorTokens.PrimaryLight, onPrimary = MentorTokens.DarkBackground,
    primaryContainer = MentorTokens.DarkPrimaryContainer, onPrimaryContainer = MentorTokens.PrimaryContainer,
    secondary = MentorTokens.WarmAccent, onSecondary = Color(0xFF3B2500),
    secondaryContainer = Color(0xFF62431F), onSecondaryContainer = Color(0xFFFFE8C7),
    background = MentorTokens.DarkBackground, onBackground = Color(0xFFF8EEF7),
    surface = MentorTokens.DarkSurface, onSurface = Color(0xFFF8EEF7),
    surfaceVariant = Color(0xFF3B2C40), onSurfaceVariant = Color(0xFFE0C9D9),
    outline = Color(0xFFB69AAB), outlineVariant = Color(0xFF63485B),
    error = Color(0xFFFFB4C0),
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
