package com.matteo.chatapp.platform

import kotlinx.coroutines.*
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.util.concurrent.TimeUnit

private fun locationExecutable() = File(System.getenv("SystemRoot") ?: "C:\\Windows",
    "System32/WindowsPowerShell/v1.0/powershell.exe")

actual fun supportsCurrentLocation(): Boolean = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
    && locationExecutable().isFile

actual fun openLocationSettings() { Desktop.getDesktop().browse(URI("ms-settings:privacy-location")) }

actual suspend fun currentLocation(): SharedLocation = withContext(Dispatchers.IO) {
    check(supportsCurrentLocation()) { "La ubicación automática no está disponible en este equipo. Puedes pegar unas coordenadas." }
    // Static packaged code only; no user text is passed to PowerShell. Never log its output.
    val script = checkNotNull(object {}.javaClass.getResourceAsStream("/location-windows.ps1"))
        .use { it.bufferedReader(Charsets.UTF_8).readText() }
    val process = ProcessBuilder(locationExecutable().absolutePath, "-NoLogo", "-NoProfile", "-NonInteractive",
        "-WindowStyle", "Hidden", "-Command", script).redirectErrorStream(true).start()
    awaitLocationProcess(object : LocationProcess {
        override val isAlive get() = process.isAlive
        override fun output() = process.inputStream.readNBytes(4096).toString(Charsets.UTF_8)
        override fun close() {
            if (process.isAlive) { process.destroyForcibly(); process.waitFor(2, TimeUnit.SECONDS) }
            process.inputStream.close(); process.outputStream.close(); process.errorStream.close()
        }
    })
}

internal interface LocationProcess {
    val isAlive: Boolean
    fun output(): String
    fun close()
}

internal suspend fun awaitLocationProcess(process: LocationProcess, timeoutMillis: Long = 30_000): SharedLocation {
    try {
        return withTimeout(timeoutMillis) {
            while (process.isAlive) delay(100)
            ensureActive()
            parseLocationResult(process.output())
        }
    } finally { runCatching { process.close() } }
}

internal fun parseLocationResult(output: String): SharedLocation {
    val result = output.trim()
    if (result == "DENIED") error("Windows no permite acceder a la ubicación. Revisa sus ajustes de ubicación o elige un lugar manualmente.")
    if (result == "UNAVAILABLE") error("Windows no ha podido localizar este equipo. Activa su servicio de ubicación o pega unas coordenadas.")
    val parts = result.split('|')
    check(parts.size == 3 && parts[0] == "OK") { "No se pudo obtener la ubicación de Windows. Puedes elegir el lugar manualmente." }
    val latitude = parts[1].toDoubleOrNull(); val longitude = parts[2].toDoubleOrNull()
    check(latitude != null && longitude != null) { "Windows devolvió una ubicación no válida" }
    return SharedLocation(latitude, longitude)
}
