package com.matteo.chatapp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

fun main() = application {
    val appIcon = remember {
        val bytes = object {}.javaClass.getResourceAsStream("/chimera.png")!!.readBytes()
        BitmapPainter(Image.makeFromEncoded(bytes).toComposeImageBitmap())
    }
    Window(
        onCloseRequest = ::exitApplication,
        title = "Chimera",
        icon = appIcon,
        state = rememberWindowState(width = 1280.dp, height = 800.dp),
    ) {
        App()
    }
}
