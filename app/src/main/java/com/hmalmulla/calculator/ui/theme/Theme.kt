package com.hmalmulla.calculator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hmalmulla.calculator.data.Palette
import com.hmalmulla.calculator.data.Settings
import com.hmalmulla.calculator.data.ThemeMode

/** Settings that UI elements below the theme need (sizes, feedback). */
val LocalSettings = compositionLocalOf { Settings() }

private fun onColor(c: Color) = if (c.luminance() > 0.5f) Color(0xFF111318) else Color.White

private fun scheme(dark: Boolean, primary: Color, secondary: Color, amoled: Boolean, navyBase: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val bg = when {
        dark && amoled -> Color.Black
        dark && navyBase -> Color(0xFF0B1630)
        dark -> Color(0xFF111318)
        navyBase -> Color(0xFFF2F5FB)
        else -> Color(0xFFF7F7FA)
    }
    val surfaceVariant = when {
        dark && amoled -> Color(0xFF1A1A1A)
        dark && navyBase -> Color(0xFF16264A)
        dark -> Color(0xFF23262E)
        navyBase -> Color(0xFFE1E8F5)
        else -> Color(0xFFE9EAF0)
    }
    val primaryContainer = if (dark) primary.copy(alpha = 0.28f).compositeOver(bg) else primary.copy(alpha = 0.16f).compositeOver(bg)
    val secondaryContainer = if (dark) secondary.copy(alpha = 0.22f).compositeOver(bg) else secondary.copy(alpha = 0.14f).compositeOver(bg)
    return base.copy(
        primary = primary,
        onPrimary = onColor(primary),
        primaryContainer = primaryContainer,
        onPrimaryContainer = if (dark) primary.copy(alpha = 1f).let { if (it.luminance() < 0.3f) Color.White else it } else primary,
        secondary = secondary,
        onSecondary = onColor(secondary),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = if (dark) Color(0xFFE6E8EF) else Color(0xFF1B2333),
        background = bg,
        surface = bg,
        surfaceVariant = surfaceVariant,
        surfaceContainer = surfaceVariant,
        surfaceContainerLow = if (dark) surfaceVariant.copy(alpha = 0.6f).compositeOver(bg) else Color.White,
        surfaceContainerHigh = surfaceVariant,
        onBackground = if (dark) Color(0xFFF1F2F6) else Color(0xFF14171F),
        onSurface = if (dark) Color(0xFFF1F2F6) else Color(0xFF14171F),
        onSurfaceVariant = if (dark) Color(0xFFB9BECB) else Color(0xFF4A5060),
    )
}

@Composable
fun CalculatorTheme(settings: Settings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = when (settings.palette) {
        Palette.CLASSIC -> scheme(dark, if (dark) Color(0xFF8AB4F8) else Color(0xFF1A73E8), Color(0xFF5F6B85), settings.amoled, false)
        Palette.NAVY -> scheme(dark, if (dark) Color(0xFF6EA8FF) else Color(0xFF1E3A8A), Color(0xFF3B82F6), settings.amoled, true)
        Palette.GOLD -> scheme(dark, Color(0xFFE3B341), Color(0xFFB8892E), settings.amoled, false)
        Palette.CUSTOM -> scheme(dark, Color(settings.customAccent), Color(settings.customAccent).copy(alpha = 0.8f).compositeOver(Color.Gray), settings.amoled, false)
    }
    val f = settings.fontSize.scale
    val typography = Typography(
        displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light, fontSize = (56 * f).sp, lineHeight = (62 * f).sp),
        displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = (30 * f).sp, lineHeight = (36 * f).sp),
        titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = (22 * f).sp),
        titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = (17 * f).sp),
        bodyLarge = TextStyle(fontSize = (16 * f).sp, lineHeight = (22 * f).sp),
        bodyMedium = TextStyle(fontSize = (14 * f).sp, lineHeight = (20 * f).sp),
        labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = (15 * f).sp),
    )
    androidx.compose.runtime.CompositionLocalProvider(LocalSettings provides settings) {
        MaterialTheme(colorScheme = colors, typography = typography, content = content)
    }
}

/** Accent choices offered for the custom theme. */
val AccentChoices = listOf(
    0xFF7C4DFF, 0xFF00897B, 0xFFE53935, 0xFFFF6F00, 0xFF43A047, 0xFFD81B60, 0xFF00ACC1, 0xFF6D4C41,
).map { it.toInt() }
