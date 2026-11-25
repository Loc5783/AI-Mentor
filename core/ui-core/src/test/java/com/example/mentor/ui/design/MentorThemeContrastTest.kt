package com.example.mentor.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme
import org.junit.Assert.assertTrue
import org.junit.Test

class MentorThemeContrastTest {
    @Test
    fun lightThemeTextColorsMeetWcagAa() {
        assertTextPairsMeetWcagAa(LightColors)
    }

    @Test
    fun darkThemeTextColorsMeetWcagAa() {
        assertTextPairsMeetWcagAa(DarkColors)
    }

    private fun assertTextPairsMeetWcagAa(scheme: ColorScheme) {
        listOf(
            "primary" to (scheme.onPrimary to scheme.primary),
            "primaryContainer" to (scheme.onPrimaryContainer to scheme.primaryContainer),
            "secondary" to (scheme.onSecondary to scheme.secondary),
            "secondaryContainer" to (scheme.onSecondaryContainer to scheme.secondaryContainer),
            "tertiary" to (scheme.onTertiary to scheme.tertiary),
            "tertiaryContainer" to (scheme.onTertiaryContainer to scheme.tertiaryContainer),
            "background" to (scheme.onBackground to scheme.background),
            "surface" to (scheme.onSurface to scheme.surface),
            "surfaceVariant" to (scheme.onSurfaceVariant to scheme.surfaceVariant),
            "error" to (scheme.onError to scheme.error),
            "errorContainer" to (scheme.onErrorContainer to scheme.errorContainer),
        ).forEach { (name, pair) ->
            val contrast = contrastRatio(pair.first, pair.second)
            assertTrue("$name contrast was $contrast:1; expected at least 4.5:1", contrast >= 4.5)
        }
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val lighter = maxOf(relativeLuminance(first), relativeLuminance(second))
        val darker = minOf(relativeLuminance(first), relativeLuminance(second))
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        fun linear(channel: Float): Double {
            val value = channel.toDouble()
            return if (value <= 0.04045) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)
        }

        return 0.2126 * linear(color.red) +
            0.7152 * linear(color.green) +
            0.0722 * linear(color.blue)
    }
}
