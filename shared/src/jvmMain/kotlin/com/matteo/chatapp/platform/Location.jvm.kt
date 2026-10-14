package com.matteo.chatapp.platform

import java.awt.Desktop
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.CancellationException

actual fun supportsCurrentLocation(): Boolean = supportsLocationMap()

actual fun supportsLocationMap(): Boolean = Desktop.isDesktopSupported() &&
    Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)

actual fun openLocationSettings() {
    if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
        Desktop.getDesktop().browse(URI("ms-settings:privacy-location"))
    }
}

actual suspend fun currentLocation(): SharedLocation = openLocationMap(null, true)
    ?: throw CancellationException("Selección de ubicación cancelada")

actual suspend fun chooseLocationOnMap(initial: SharedLocation?): SharedLocation? = openLocationMap(initial, false)

private suspend fun openLocationMap(initial: SharedLocation?, locate: Boolean): SharedLocation? = withContext(Dispatchers.IO) {
    check(supportsLocationMap()) { "No se pudo abrir un navegador para seleccionar la ubicación" }
    DesktopLocationMap(initial, locate).use { session ->
        Desktop.getDesktop().browse(session.url)
        withTimeout(5 * 60_000L) { session.result.await() }
    }
}
