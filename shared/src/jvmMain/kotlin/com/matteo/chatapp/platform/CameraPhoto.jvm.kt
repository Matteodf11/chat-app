package com.matteo.chatapp.platform

import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

private class CaptureRequest(val result: CompletableDeferred<PickedAttachmentFile?> = CompletableDeferred())
private val captureRequest = MutableStateFlow<CaptureRequest?>(null)
private val captureMutex = Mutex()

actual fun supportsPhotoCapture(): Boolean = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

actual suspend fun capturePhoto(): PickedAttachmentFile? = captureMutex.withLock {
    check(supportsPhotoCapture()) { "La captura de escritorio está disponible en Windows" }
    val request = CaptureRequest()
    captureRequest.value = request
    try { request.result.await() }
    finally { request.result.cancel(); captureRequest.compareAndSet(request, null) }
}

@Composable
actual fun PrepareCameraCapture() {
    val request by captureRequest.collectAsState()
    request?.let { current -> key(current) { DesktopCameraDialog { current.result.complete(it) } } }
}

@Composable
private fun DesktopCameraDialog(onFinish: (PickedAttachmentFile?) -> Unit) {
    val session = remember { DesktopCameraSession() }
    var cameras by remember { mutableStateOf<List<DesktopCameraDevice>>(emptyList()) }
    var selected by remember { mutableStateOf<DesktopCameraDevice?>(null) }
    var frame by remember { mutableStateOf<BufferedImage?>(null) }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var choosing by remember { mutableStateOf(false) }
    var finishing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun finish(photo: BufferedImage? = null) {
        if (finishing) return
        finishing = true
        scope.launch {
            try {
                session.close()
                val picked = photo?.let { withContext(Dispatchers.IO) { cameraJpeg(it) } }
                onFinish(picked)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = "No se pudo preparar la foto. Vuelve a abrir la cámara."; finishing = false }
        }
    }
    LaunchedEffect(retry) {
        loading = true; error = null; selected = null
        try {
            cameras = withContext(Dispatchers.IO) { discoverDesktopCameras() }
            selected = cameras.firstOrNull()
            if (cameras.isEmpty()) error = "No se ha encontrado ninguna cámara. Conecta una webcam y pulsa Reintentar."
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { error = "No se pudo detectar la cámara. Comprueba los permisos de cámara de Windows y vuelve a intentarlo." }
        finally { loading = false }
    }
    LaunchedEffect(selected, retry) {
        val camera = selected ?: return@LaunchedEffect
        val owner = Any()
        frame = null; preview = null; error = null; loading = true
        try {
            session.open(camera, owner)
            loading = false
            var missingFrames = 0
            while (isActive && !finishing) {
                val image = session.frame(owner)
                if (image != null) {
                    val bitmap = withContext(Dispatchers.IO) { image.toComposeImageBitmap() }
                    frame = image; preview = bitmap; missingFrames = 0
                } else if (++missingFrames >= 50) error("No se reciben imágenes")
                delay(100)
            }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            frame = null; preview = null
            error = "No se pudo abrir la cámara. Cierra otras aplicaciones que la estén usando y comprueba los permisos de cámara de Windows."
        } finally { loading = false; session.close(owner) }
    }
    DisposableEffect(session) { onDispose { session.dispose() } }
    Dialog(onDismissRequest = { finish() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 760.dp).fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.heightIn(max = 600.dp).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Cámara", style = MaterialTheme.typography.titleLarge)
                Text("Haz la foto y revísala antes de enviarla.", style = MaterialTheme.typography.bodyMedium)
                if (cameras.size > 1) Box {
                    OutlinedButton(enabled = !loading && !finishing, onClick = { choosing = true }) {
                        Text(selected?.name ?: "Elegir cámara", maxLines = 1)
                    }
                    DropdownMenu(choosing, { choosing = false }) {
                        cameras.forEach { camera -> DropdownMenuItem(text = { Text(camera.name) }, onClick = {
                            choosing = false
                            if (camera !== selected) { frame = null; preview = null; loading = true; selected = camera }
                        }) }
                    }
                }
                Box(Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 360.dp).aspectRatio(16f / 9)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    preview?.let { Image(it, "Vista previa de la webcam", Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
                    if (loading || (selected != null && preview == null && error == null)) CircularProgressIndicator()
                    else if (preview == null) Text("Cámara no disponible", Modifier.padding(16.dp))
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                    TextButton(enabled = !finishing, onClick = { frame = null; preview = null; loading = true; retry++ }) { Text("Reintentar") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(enabled = !finishing, onClick = { finish() }) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(enabled = frame != null && error == null && !loading && !finishing,
                        onClick = { finish(frame) }) { Text(if (finishing) "Preparando…" else "Hacer foto") }
                }
            }
        }
    }
}

actual suspend fun flipCameraPhoto(file: PickedAttachmentFile): PickedAttachmentFile = withContext(Dispatchers.IO) {
    require(file.contentType in setOf("image/jpeg", "image/jpg")) { "La captura debe ser una foto JPEG" }
    val original = ImageIO.read(ByteArrayInputStream(file.bytes)) ?: error("No se pudo leer la foto")
    val flipped = BufferedImage(original.width, original.height, BufferedImage.TYPE_INT_RGB)
    val graphics = flipped.createGraphics()
    try { graphics.drawImage(original, original.width, 0, -original.width, original.height, null) }
    finally { graphics.dispose() }
    cameraJpeg(flipped).copy(fileName = file.fileName)
}
