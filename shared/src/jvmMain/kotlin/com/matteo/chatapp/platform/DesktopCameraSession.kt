package com.matteo.chatapp.platform

import com.github.eduramiba.webcamcapture.drivers.NativeDriver
import com.github.sarxos.webcam.Webcam
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

internal interface DesktopCameraDevice {
    val name: String
    fun open()
    fun frame(): BufferedImage?
    fun close()
}

private val nativeDriver by lazy { Webcam.setDriver(NativeDriver()); true }

internal fun discoverDesktopCameras(): List<DesktopCameraDevice> {
    val devices = try {
        check(nativeDriver)
        Webcam.getWebcams(15, TimeUnit.SECONDS)
    } catch (e: LinkageError) { throw IllegalStateException("No se pudo cargar el controlador de cámara", e) }
    return devices.map { webcam -> object : DesktopCameraDevice {
        override val name: String get() = webcam.name
        override fun open() {
            val available = webcam.viewSizes.toList()
            val size = available.filter { it.width <= 1280 && it.height <= 720 }.maxByOrNull { it.width * it.height }
                ?: available.minByOrNull { it.width * it.height }
            if (size != null) webcam.viewSize = size
            check(webcam.open()) { "No se pudo abrir la cámara" }
        }
        override fun frame(): BufferedImage? = webcam.image
        override fun close() { webcam.close() }
    } }
}

/** Serializes native work and prevents late opens after cancellation or logout. */
internal class DesktopCameraSession(private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    private val mutex = Mutex()
    private var active: DesktopCameraDevice? = null
    private var activeOwner: Any? = null
    @Volatile private var disposed = false
    suspend fun open(device: DesktopCameraDevice, owner: Any = device) = withContext(dispatcher) {
        mutex.withLock {
            check(!disposed) { "La ventana de cámara ya está cerrada" }
            closeLocked()
            active = device
            activeOwner = owner
            try { device.open() }
            catch (e: Exception) { closeLocked(); throw e }
            catch (e: LinkageError) { closeLocked(); throw IllegalStateException("No se pudo cargar la cámara", e) }
        }
    }
    suspend fun frame(owner: Any? = null): BufferedImage? = withContext(dispatcher) {
        mutex.withLock {
            if (disposed || (owner != null && activeOwner !== owner)) return@withLock null
            active?.frame()?.let { image ->
                BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB).also { copy ->
                    val graphics = copy.createGraphics()
                    try { graphics.drawImage(image, 0, 0, null) } finally { graphics.dispose() }
                }
            }
        }
    }
    suspend fun close(owner: Any? = null) = withContext(NonCancellable + dispatcher) {
        mutex.withLock { if (owner == null || activeOwner === owner) closeLocked() }
    }
    fun dispose() { disposed = true; cleanupScope.launch { close() } }
    private fun closeLocked() {
        val device = active
        active = null
        activeOwner = null
        runCatching { device?.close() }
    }
    private companion object { val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO) }
}

internal fun cameraJpeg(image: BufferedImage): PickedAttachmentFile {
    val output = ByteArrayOutputStream()
    check(javax.imageio.ImageIO.write(image, "jpg", output)) { "No se pudo guardar la foto" }
    return PickedAttachmentFile("foto-${System.currentTimeMillis()}.jpg", "image/jpeg", output.toByteArray())
        .also { validatePhotoSelection(listOf(it)) }
}
