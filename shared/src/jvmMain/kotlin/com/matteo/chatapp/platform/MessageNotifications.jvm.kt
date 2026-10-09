package com.matteo.chatapp.platform

import androidx.compose.runtime.Composable
import java.awt.EventQueue
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import javax.imageio.ImageIO

@Composable
actual fun PrepareMessageNotifications() = Unit

actual fun clearMessageNotifications() {
    EventQueue.invokeLater { DesktopMessageNotifications.lastDmId = null }
}

actual fun platformMessageNotificationSink() = MessageNotificationSink { message ->
    EventQueue.invokeLater {
        DesktopMessageNotifications.lastDmId = message.dmId
        DesktopMessageNotifications.trayIcon?.displayMessage(
            "Chimera · ${message.sender}", message.preview, TrayIcon.MessageType.NONE
        )
    }
}

object DesktopMessageNotifications {
    internal var trayIcon: TrayIcon? = null
    internal var lastDmId: String? = null

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
                    lastDmId?.let(NotificationNavigation::openDm)
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
        lastDmId = null
    }
}
