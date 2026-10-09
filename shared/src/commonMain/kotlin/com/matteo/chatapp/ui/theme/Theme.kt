package com.matteo.chatapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ThemeMode { DARK, LIGHT }

enum class ThemeAccent(val label: String, val color: Color, val darkSoft: Color, val lightSoft: Color) {
    CORAL("Coral", Color(0xFFE36F52), Color(0xFF4B2B26), Color(0xFFF8DED7)),
    BLUE("Azul", Color(0xFF4F7DF3), Color(0xFF26365F), Color(0xFFDDE7FF)),
    GREEN("Verde", Color(0xFF2FA780), Color(0xFF1E4A3F), Color(0xFFD8F1E8)),
    PURPLE("Violeta", Color(0xFF8B6FE8), Color(0xFF3A305C), Color(0xFFE8E1FB)),
    PINK("Rosa", Color(0xFFE46291), Color(0xFF57293A), Color(0xFFF9DDE7))
}

@Immutable
data class AppPalette(
    val background: Color,
    val navigation: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
    val primary: Color,
    val primaryHover: Color,
    val primarySoft: Color,
    val accent: Color,
    val warning: Color,
    val onPrimary: Color,
    val onBackground: Color,
    val onSurface: Color,
    val muted: Color,
    val outline: Color,
    val error: Color
)

// Neutral surfaces let each user-selected accent define the visual identity.
private val DarkBase = AppPalette(
    background = Color(0xFF111111),
    navigation = Color(0xFF0A0A0A),
    surface = Color(0xFF191919),
    surfaceVariant = Color(0xFF252525),
    surfaceElevated = Color(0xFF303030),
    primary = ThemeAccent.CORAL.color,
    primaryHover = Color(0xFFF07D60),
    primarySoft = ThemeAccent.CORAL.darkSoft,
    accent = Color(0xFF53C7A5),
    warning = Color(0xFFF0B45B),
    onPrimary = Color.White,
    onBackground = Color(0xFFF4F4F4),
    onSurface = Color(0xFFCCCCCC),
    muted = Color(0xFF999999),
    outline = Color(0xFF303030),
    error = Color(0xFFFF747A)
)

private val LightBase = AppPalette(
    background = Color(0xFFFAFAFA),
    navigation = Color(0xFFEBEBEB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0F0F0),
    surfaceElevated = Color(0xFFE3E3E3),
    primary = ThemeAccent.CORAL.color,
    primaryHover = Color(0xFFC95D44),
    primarySoft = ThemeAccent.CORAL.lightSoft,
    accent = Color(0xFF168666),
    warning = Color(0xFFB77516),
    onPrimary = Color.White,
    onBackground = Color(0xFF1B1B1B),
    onSurface = Color(0xFF515151),
    muted = Color(0xFF686868),
    outline = Color(0xFFD8D8D8),
    error = Color(0xFFC93E49)
)

private val LocalAppPalette = staticCompositionLocalOf { DarkBase }

val Background: Color @Composable get() = LocalAppPalette.current.background
val Navigation: Color @Composable get() = LocalAppPalette.current.navigation
val Surface: Color @Composable get() = LocalAppPalette.current.surface
val SurfaceVariant: Color @Composable get() = LocalAppPalette.current.surfaceVariant
val SurfaceElevated: Color @Composable get() = LocalAppPalette.current.surfaceElevated
val Primary: Color @Composable get() = LocalAppPalette.current.primary
val PrimaryHover: Color @Composable get() = LocalAppPalette.current.primaryHover
val PrimarySoft: Color @Composable get() = LocalAppPalette.current.primarySoft
val Accent: Color @Composable get() = LocalAppPalette.current.accent
val Warning: Color @Composable get() = LocalAppPalette.current.warning
val OnPrimary: Color @Composable get() = LocalAppPalette.current.onPrimary
val OnBackground: Color @Composable get() = LocalAppPalette.current.onBackground
val OnSurface: Color @Composable get() = LocalAppPalette.current.onSurface
val Muted: Color @Composable get() = LocalAppPalette.current.muted
val Outline: Color @Composable get() = LocalAppPalette.current.outline
val Error: Color @Composable get() = LocalAppPalette.current.error

private val AppTypography = Typography(
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun ChatAppTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    themeAccent: ThemeAccent = ThemeAccent.CORAL,
    content: @Composable () -> Unit
) {
    val base = if (themeMode == ThemeMode.DARK) DarkBase else LightBase
    val palette = base.copy(
        primary = themeAccent.color,
        primaryHover = themeAccent.color,
        primarySoft = if (themeMode == ThemeMode.DARK) themeAccent.darkSoft else themeAccent.lightSoft
    )
    val colorScheme = if (themeMode == ThemeMode.DARK) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            background = palette.background,
            onBackground = palette.onBackground,
            surface = palette.surface,
            onSurface = palette.onSurface,
            surfaceVariant = palette.surfaceVariant,
            outline = palette.outline,
            error = palette.error
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            background = palette.background,
            onBackground = palette.onBackground,
            surface = palette.surface,
            onSurface = palette.onSurface,
            surfaceVariant = palette.surfaceVariant,
            outline = palette.outline,
            error = palette.error
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
