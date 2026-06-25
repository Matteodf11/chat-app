package com.matteo.chatapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Background = Color(0xFF1E1F22)
val Surface = Color(0xFF2B2D31)
val SurfaceVariant = Color(0xFF383A40)
val Primary = Color(0xFF5865F2)
val OnPrimary = Color.White
val OnBackground = Color(0xFFDBDEE1)
val OnSurface = Color(0xFFB5BAC1)
val Error = Color(0xFFED4245)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    error = Error
)

@Composable
fun ChatAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}