package com.matteo.chatapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.matteo.chatapp.model.Message
import kotlinx.coroutines.CancellationException

@Composable
fun ImageViewerDialog(message: Message, refresh: suspend (Message) -> Message,
    onDownload: (Message) -> Unit, onDismiss: () -> Unit) {
    var bitmap by remember(message.id) { mutableStateOf<ImageBitmap?>(null) }
    var error by remember(message.id) { mutableStateOf<String?>(null) }
    var attempt by remember(message.id) { mutableStateOf(0) }
    var scale by remember(message.id) { mutableStateOf(1f) }
    var offset by remember(message.id) { mutableStateOf(Offset.Zero) }
    LaunchedEffect(message.id, attempt) {
        error = null
        try {
            val fresh = refresh(message)
            bitmap = fresh.attachmentUrl?.let { loadSharedThumbnail(it, 4096) }
            if (bitmap == null) error = "No se pudo cargar la imagen"
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "No se pudo cargar la imagen" }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(Color(0xFF101113))) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(message.attachmentName ?: "Imagen", color = Color.White, maxLines = 1,
                    modifier = Modifier.weight(1f), overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                TextButton(onClick = { onDownload(message) }) { Text("Guardar") }
                TextButton(onClick = onDismiss) { Text("Cerrar") }
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clipToBounds(), contentAlignment = Alignment.Center) {
                val density = androidx.compose.ui.platform.LocalDensity.current
                val maxX = with(density) { maxWidth.toPx() } * (scale - 1) / 2
                val maxY = with(density) { maxHeight.toPx() } * (scale - 1) / 2
                val transform = rememberTransformableState { zoom, pan, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    offset = if (scale == 1f) Offset.Zero else Offset(
                        (offset.x + pan.x).coerceIn(-maxX, maxX), (offset.y + pan.y).coerceIn(-maxY, maxY))
                }
                bitmap?.let {
                    Image(it, message.attachmentName, Modifier.fillMaxSize().transformable(transform)
                        .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y },
                        contentScale = ContentScale.Fit, filterQuality = androidx.compose.ui.graphics.FilterQuality.High)
                } ?: if (error == null) CircularProgressIndicator() else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error.orEmpty(), color = Color.White, modifier = Modifier.padding(24.dp))
                    TextButton(onClick = { attempt++ }) { Text("Reintentar") }
                }
            }
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(8.dp), horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                TextButton(enabled = scale > 1f, onClick = { scale = (scale / 1.5f).coerceAtLeast(1f); offset = Offset.Zero }) { Text("−") }
                TextButton(onClick = { scale = 1f; offset = Offset.Zero }) { Text("${(scale * 100).toInt()} %") }
                TextButton(enabled = scale < 5f, onClick = { scale = (scale * 1.5f).coerceAtMost(5f) }) { Text("+") }
            }
        }
    }
}
