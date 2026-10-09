package com.matteo.chatapp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import com.matteo.chatapp.repository.AppVisibility
import com.matteo.chatapp.platform.DesktopMessageNotifications
import java.awt.event.WindowFocusListener
import java.awt.event.WindowEvent
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

fun main() = application {
    var windowVisible by remember { mutableStateOf(true) }
    var trayAvailable by remember { mutableStateOf(false) }
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
        state = rememberWindowState(width = 1280.dp, height = 800.dp),
    ) {
        DisposableEffect(window) {
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
