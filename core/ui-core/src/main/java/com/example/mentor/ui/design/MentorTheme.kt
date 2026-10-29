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
    val OnBrand = Color(0xFF0B2545)
    
    // Primary Blue Palette
    val Blue50 = Color(0xFFF2F7FE)
    val Blue100 = Color(0xFFE3EDFD)
    val Blue200 = Color(0xFFD6E4FB)
    val Blue300 = Color(0xFF8DB6F7)
    val Blue400 = Color(0xFF6A9BEF)
    val Blue500 = Color(0xFF4A80E0)
    val Blue600 = Color(0xFF2F5FB3)
    val Blue700 = Color(0xFF3A5A94)
    val Blue800 = Color(0xFF2E3D61)
    val Blue900 = Color(0xFF1F2A44)

    // Pink Palette (Light background & Tertiary)
    val Pink50 = Color(0xFFFFF7FA)
    val Pink100 = Color(0xFFFCEFF5)
    val Pink200 = Color(0xFFFBDCE8)
    val Pink300 = Color(0xFFF4A9C4)
    val Pink700 = Color(0xFF6B1F3D)
    val PinkOutline = Color(0xFFEBD5E0)

    // Dark Surface & Navy Palette
    val NavyBackground = Color(0xFF0F172A)
    val NavySurface = Color(0xFF1E293B)
    val NavyCard = Color(0xFF24334C)
    val NavyElevated = Color(0xFF30415F)
    val NavyOutline = Color(0xFF475A79)
    val NavyOnSurface = Color(0xFFF4F7FC)
    val NavyOnSurfaceVariant = Color(0xFFBBC9DF)
    val LightCanvas = Color(0xFFF7F8FE)
    val LightSoft = Color(0xFFEDF1FB)
    val LightOutline = Color(0xFFD9E1F0)
    val Gold = Color(0xFFFFD166)
    val Purple = Color(0xFF8B5CF6)

    // Yellow-Orange Palette
    val Orange50 = Color(0xFFFFF8EC)
    val Orange100 = Color(0xFFFDEFD6)
    val Orange200 = Color(0xFFFAE1B4)
    val Orange300 = Color(0xFFF7CE8D)
    val Orange400 = Color(0xFFF0B964)
    val Orange500 = Color(0xFFE8A33D)
    val Orange600 = Color(0xFFC98420)
    val Orange700 = Color(0xFFA0661A)
    val Orange800 = Color(0xFF764B15)
    val Orange900 = Color(0xFF4D3110)

    // Status colors - Light
    val SuccessLightText = Color(0xFF1E7F5C)
    val SuccessLightBg = Color(0xFFDDF5EA)
    val WarningLightText = Color(0xFFB3433A)
    val WarningLightBg = Color(0xFFFDE4E0)
    val ErrorLightText = Color(0xFFC62828)
    val ErrorLightBg = Color(0xFFFDE7E7)

    // Status colors - Dark
    val SuccessDarkText = Color(0xFF8EE0BC)
    val SuccessDarkBg = Color(0xFF1F4A3C)
    val WarningDarkText = Color(0xFFF7A99E)
    val WarningDarkBg = Color(0xFF55302F)
    val ErrorDarkText = Color(0xFFFF9AA0)
    val ErrorDarkBg = Color(0xFF54272D)

    val Primary = Blue300
    val PrimaryDark = Blue700
    val PrimaryLight = Blue200
    val PrimaryContainer = Blue200
    val DarkPrimaryContainer = Color(0xFF263D68)
    val LightBackground = LightCanvas
    val DarkBackground = NavyBackground
    val DarkSurface = NavySurface
    val WarmAccent = Gold
    val Success = SuccessLightText
    val DarkSuccess = SuccessDarkText
    val Warning = WarningLightText
    val Error = ErrorLightText
    val Info = Blue600
    val Locked = Color(0xFF94A3B8)
    val Completed = Success
    val Current = Primary
    val Disabled = Color(0xFFC3CCE0)
    val Divider = PinkOutline
    val ProgressTrack = Blue100

    val RewardStreakLight = Orange500
    val RewardStreakDark = Orange400
    val RewardXpBar = Gold

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

object SubjectPalette {
    val Math = Color(0xFF8DB6F7)
    val Physics = Color(0xFFB6A6F2)
    val Chemistry = Color(0xFF7FD8B0)
    val Biology = Color(0xFFA9D98B)
    val History = Color(0xFFF7CE8D)
    val Geography = Color(0xFF6FCFC4)
    val Literature = Color(0xFFF7998D)
    val English = Color(0xFFF2A6C8)
    val Programming = Color(0xFFA3B4CC)
    val Economics = Color(0xFFE8B08A)

    fun getAccent(subjectId: String): Color {
        return when (subjectId.lowercase()) {
            "math" -> Math
            "physics" -> Physics
            "chemistry" -> Chemistry
            "biology" -> Biology
            "history" -> History
            "geography" -> Geography
            "literature" -> Literature
            "english" -> English
            "programming" -> Programming
            "economics_law", "economics" -> Economics
            else -> MentorTokens.Blue300
        }
    }

    fun getContainer(subjectId: String, isDark: Boolean): Color {
        val accent = getAccent(subjectId)
        val bg = if (isDark) MentorTokens.NavyCard else Color.White
        val alpha = if (isDark) 0.24f else 0.18f
        return blendColor(accent, bg, alpha)
    }

    fun getOnContainer(isDark: Boolean): Color {
        return if (isDark) Color(0xFFF1F4FB) else Color(0xFF24203A)
    }

    private fun blendColor(fg: Color, bg: Color, alpha: Float): Color {
        val r = fg.red * alpha + bg.red * (1 - alpha)
        val g = fg.green * alpha + bg.green * (1 - alpha)
        val b = fg.blue * alpha + bg.blue * (1 - alpha)
        return Color(r, g, b, 1f)
    }
}

private val LightColors = lightColorScheme(
    primary = MentorTokens.Blue300, 
    onPrimary = MentorTokens.OnBrand,
    primaryContainer = MentorTokens.Blue200, 
    onPrimaryContainer = MentorTokens.OnBrand,
    secondary = MentorTokens.Gold,
    onSecondary = MentorTokens.Orange900,
    secondaryContainer = MentorTokens.Orange100, 
    onSecondaryContainer = MentorTokens.Orange800,
    tertiary = MentorTokens.Purple,
    onTertiary = Color(0xFF170E2D),
    tertiaryContainer = Color(0xFFEDE7FF),
    onTertiaryContainer = Color(0xFF36205E),
    background = MentorTokens.LightCanvas,
    onBackground = Color(0xFF18243A),
    surface = Color.White, 
    onSurface = Color(0xFF18243A),
    surfaceVariant = MentorTokens.LightSoft,
    onSurfaceVariant = Color(0xFF4A5A74),
    outline = MentorTokens.LightOutline,
    outlineVariant = MentorTokens.LightOutline.copy(alpha = 0.7f),
    error = MentorTokens.ErrorLightBg,
    onError = MentorTokens.ErrorLightText
)

private val DarkColors = darkColorScheme(
    primary = MentorTokens.Blue300, 
    onPrimary = MentorTokens.OnBrand,
    primaryContainer = MentorTokens.Blue700, 
    onPrimaryContainer = Color(0xFFEAF2FF),
    secondary = MentorTokens.Gold,
    onSecondary = MentorTokens.Orange900,
    secondaryContainer = Color(0xFF5A4220), 
    onSecondaryContainer = MentorTokens.Orange100,
    tertiary = Color(0xFFB7A0FF),
    onTertiary = Color(0xFF170E2D),
    tertiaryContainer = Color(0xFF3B315B),
    onTertiaryContainer = Color(0xFFEDE7FF),
    background = MentorTokens.NavyBackground, 
    onBackground = MentorTokens.NavyOnSurface,
    surface = MentorTokens.NavySurface, 
    onSurface = MentorTokens.NavyOnSurface,
    surfaceVariant = MentorTokens.NavyCard, // Card/SurfaceContainer
    onSurfaceVariant = MentorTokens.NavyOnSurfaceVariant,
    outline = MentorTokens.NavyOutline, 
    outlineVariant = MentorTokens.NavyOutline.copy(alpha = 0.5f),
    error = MentorTokens.ErrorDarkBg,
    onError = MentorTokens.ErrorDarkText
)

@Composable
fun MentorTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMentorMotion provides rememberMotionPolicy()) {
      MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography(),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(20.dp),
            small = RoundedCornerShape(16.dp),
            medium = RoundedCornerShape(MentorTokens.CardRadius),
            large = RoundedCornerShape(28.dp),
        ), content = content,
      )
    }
}
