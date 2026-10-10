package com.matteo.chatapp.ui.components

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Forward
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
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
fun ImageViewerDialog(initialMessage: Message, refresh: suspend (Message) -> Message,
    onDownload: (Message) -> Unit, onShare: (Message) -> Unit, onDismiss: () -> Unit,
    isWorking: Boolean = false, notice: String? = null,
    images: List<Message> = listOf(initialMessage), hasMore: Boolean = false, loadingMore: Boolean = false,
    noticeMessageId: String? = initialMessage.id, galleryError: String? = null, onMore: () -> Unit = {}, onForward: ((Message) -> Unit)? = null) {
    val pager = rememberPagerState(initialPage = images.indexOfFirst { it.id == initialMessage.id }.coerceAtLeast(0)) { images.size }
    val scope = rememberCoroutineScope()
    val message = images.getOrElse(pager.currentPage) { initialMessage }
    var scale by remember(message.id) { mutableStateOf(1f) }
    var zoomReset by remember(message.id) { mutableStateOf(0) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(Color(0xFF101113))) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(message.attachmentName ?: "Imagen", color = Color.White, maxLines = 1,
                    modifier = Modifier.weight(1f), overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                if (isWorking) CircularProgressIndicator(Modifier.padding(12.dp).size(22.dp), strokeWidth = 2.dp)
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Cerrar visor", tint = Color.White) }
            }
            HorizontalPager(state = pager, key = { images[it].id }, userScrollEnabled = scale <= 1f && !isWorking,
                modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                val current = page == pager.currentPage
                ImageViewerPage(images[page], refresh, if (current) scale else 1f, zoomReset,
                    onScale = { if (current) scale = it })
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center) {
                IconButton(enabled = pager.currentPage > 0 && !isWorking, onClick = {
                    scope.launch { pager.animateScrollToPage((pager.currentPage - 1).coerceAtLeast(0)) }
                }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Foto anterior") }
                Text("${pager.currentPage + 1} / ${images.size}", color = Color.White)
                IconButton(enabled = pager.currentPage < images.lastIndex && !isWorking, onClick = {
                    scope.launch { pager.animateScrollToPage((pager.currentPage + 1).coerceAtMost(images.lastIndex)) }
                }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Foto siguiente") }
                if (loadingMore) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else if (hasMore || galleryError != null) TextButton(onClick = onMore) {
                    Text(if (galleryError != null) "Reintentar" else "Anteriores")
                }
                onForward?.let { forward ->
                    IconButton(enabled = !isWorking, onClick = { forward(message) }) {
                        Icon(Icons.AutoMirrored.Rounded.Forward, "Reenviar foto")
                    }
                }
            }
            galleryError?.let { Text(it, color = Color.White, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp)) }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(enabled = scale > 1f, onClick = { scale = (scale / 1.5f).coerceAtLeast(1f); zoomReset++ }) {
                    Icon(Icons.Rounded.ZoomOut, "Reducir imagen")
                }
                TextButton(onClick = { scale = 1f; zoomReset++ }) { Text("${(scale * 100).toInt()} %") }
                IconButton(enabled = scale < 5f, onClick = { scale = (scale * 1.5f).coerceAtMost(5f) }) {
                    Icon(Icons.Rounded.ZoomIn, "Ampliar imagen")
                }
            }
            notice?.takeIf { noticeMessageId == message.id }?.let { Text(it, color = Color.White, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(horizontal = 16.dp, vertical = 4.dp)) }
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                OutlinedButton(enabled = !isWorking, onClick = { onDownload(message) }) {
                    Icon(Icons.Rounded.Download, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp)); Text("Guardar")
                }
                OutlinedButton(enabled = !isWorking, onClick = { onShare(message) }) {
                    Icon(Icons.Rounded.Share, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp)); Text(com.matteo.chatapp.platform.imageShareActionLabel())
                }
            }
        }
    }
}


@Composable
private fun ImageViewerPage(message: Message, refresh: suspend (Message) -> Message, scale: Float, zoomReset: Int,
    onScale: (Float) -> Unit) {
    var bitmap by remember(message.id) { mutableStateOf<ImageBitmap?>(null) }
    var error by remember(message.id) { mutableStateOf<String?>(null) }
    var attempt by remember(message.id) { mutableStateOf(0) }
    var offset by remember(message.id, zoomReset) { mutableStateOf(Offset.Zero) }
    LaunchedEffect(scale) { if (scale <= 1f) offset = Offset.Zero }
    LaunchedEffect(message.id, attempt) {
        bitmap = null
        error = null
        try {
            val fresh = refresh(message)
            bitmap = fresh.attachmentUrl?.let { loadSharedThumbnail(it, 2048) }
            if (bitmap == null) error = "No se pudo cargar la imagen"
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "No se pudo cargar la imagen" }
    }
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds(), contentAlignment = Alignment.Center) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val transform = rememberTransformableState { zoom, pan, _ ->
            val nextScale = (scale * zoom).coerceIn(1f, 5f)
            onScale(nextScale)
            val maxX = widthPx * (nextScale - 1) / 2
            val maxY = heightPx * (nextScale - 1) / 2
            offset = if (nextScale == 1f) Offset.Zero else Offset(
                (offset.x + pan.x).coerceIn(-maxX, maxX), (offset.y + pan.y).coerceIn(-maxY, maxY))
        }
        bitmap?.let {
            Image(it, message.attachmentName, Modifier.fillMaxSize().transformable(state = transform, canPan = { scale > 1f })
                .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y },
                contentScale = ContentScale.Fit, filterQuality = androidx.compose.ui.graphics.FilterQuality.High)
        } ?: if (error == null) CircularProgressIndicator() else Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(error.orEmpty(), color = Color.White, modifier = Modifier.padding(24.dp))
            TextButton(onClick = { attempt++ }) { Text("Reintentar") }
        }
    }
}
