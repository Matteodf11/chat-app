package com.matteo.chatapp.platform

import androidx.compose.runtime.Composable
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import javax.imageio.ImageIO

@Composable
actual fun PrepareMessageNotifications() = Unit

actual fun clearMessageNotifications() = Unit

// Desktop keeps unread indicators inside Chimera without Windows notifications.
actual fun platformMessageNotificationSink() = MessageNotificationSink { }

object DesktopMessageNotifications {
    private var trayIcon: TrayIcon? = null

    fun install(onOpen: () -> Unit, onExit: () -> Unit): Boolean {
        if (!SystemTray.isSupported()) return false
        return runCatching {
            val image = javaClass.getResourceAsStream("/chimera.png")!!.use(ImageIO::read)
            val menu = PopupMenu().apply {
                add(MenuItem("Abrir Chimera").apply { addActionListener { onOpen() } })
                addSeparator()
                add(MenuItem("Salir").apply { addActionListener { onExit() } })
            }
            val icon = TrayIcon(image, "Chimera", menu).apply {
                isImageAutoSize = true
                addActionListener {
                    onOpen()
                }
            }
            SystemTray.getSystemTray().add(icon)
            trayIcon = icon
            true
        }.getOrDefault(false)
    }

    fun close() {
        trayIcon?.let { SystemTray.getSystemTray().remove(it) }
        trayIcon = null
    }
}
