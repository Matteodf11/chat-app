package com.matteo.chatapp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import com.matteo.chatapp.repository.AppVisibility
import com.matteo.chatapp.platform.DesktopMessageNotifications
import java.awt.event.WindowFocusListener
import java.awt.event.WindowEvent
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.util.prefs.Preferences
import kotlinx.coroutines.delay

fun main() = application {
    var windowVisible by remember { mutableStateOf(true) }
    var trayAvailable by remember { mutableStateOf(false) }
    val windowPreferences = remember { Preferences.userRoot().node("com/matteo/chatapp/window") }
    val screenBounds = remember { GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds }
    val minWidth = minOf(900, screenBounds.width)
    val minHeight = minOf(600, screenBounds.height)
    val desktopState = rememberWindowState(
        placement = if (windowPreferences.getBoolean("maximized", false)) WindowPlacement.Maximized else WindowPlacement.Floating,
        width = windowPreferences.getInt("width", 1280).coerceIn(minWidth, screenBounds.width).dp,
        height = windowPreferences.getInt("height", 800).coerceIn(minHeight, screenBounds.height).dp)
    LaunchedEffect(desktopState.size, desktopState.placement) {
        delay(500)
        if (desktopState.placement == WindowPlacement.Floating) {
            windowPreferences.putInt("width", desktopState.size.width.value.toInt())
            windowPreferences.putInt("height", desktopState.size.height.value.toInt())
        }
        windowPreferences.putBoolean("maximized", desktopState.placement == WindowPlacement.Maximized)
    }
    val appIcon = remember {
        val bytes = object {}.javaClass.getResourceAsStream("/chimera.png")!!.readBytes()
        BitmapPainter(Image.makeFromEncoded(bytes).toComposeImageBitmap())
    }
    Window(
        onCloseRequest = {
            if (trayAvailable) {
                windowVisible = false
                AppVisibility.setInForeground(false)
            } else exitApplication()
        },
        visible = windowVisible,
        title = "Chimera",
        icon = appIcon,
        state = desktopState,
    ) {
        DisposableEffect(window) {
            window.minimumSize = Dimension(minWidth, minHeight)
            val focusListener = object : WindowFocusListener {
                override fun windowGainedFocus(event: WindowEvent) { AppVisibility.setInForeground(true) }
                override fun windowLostFocus(event: WindowEvent) { AppVisibility.setInForeground(false) }
            }
            window.addWindowFocusListener(focusListener)
            trayAvailable = DesktopMessageNotifications.install(
                onOpen = {
                    windowVisible = true
                    window.isVisible = true
                    window.toFront()
                    window.requestFocus()
                },
                onExit = ::exitApplication
            )
            onDispose {
                window.removeWindowFocusListener(focusListener)
                DesktopMessageNotifications.close()
            }
        }
        App()
    }
}
