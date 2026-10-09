package com.matteo.chatapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.matteo.chatapp.ui.theme.SurfaceVariant
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@Composable
fun SharedMediaThumbnail(url: String?, description: String?, modifier: Modifier = Modifier, maxEdge: Int = 256,
    showFullImage: Boolean = false) {
    var bitmap by remember(url, maxEdge) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url, maxEdge) { bitmap = url?.let { loadSharedThumbnail(it, maxEdge) } }
    if (showFullImage) {
        BoxWithConstraints(modifier) {
            val availableWidth = maxWidth.coerceAtMost(360.dp)
            val image = bitmap
            if (image != null) {
                val ratio = image.width.toFloat() / image.height.coerceAtLeast(1)
                val height = (availableWidth / ratio).coerceAtMost(420.dp)
                Image(image, description, Modifier.size(height * ratio, height), contentScale = ContentScale.Fit,
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.High)
            } else Box(Modifier.size(availableWidth, 180.dp).background(SurfaceVariant))
        }
    } else {
        bitmap?.let { Image(it, description, modifier, contentScale = ContentScale.Crop, filterQuality = androidx.compose.ui.graphics.FilterQuality.High) }
            ?: Box(modifier.background(SurfaceVariant))
    }
}

private val mediaLoads = Semaphore(3)
internal suspend fun loadSharedThumbnail(url: String, maxEdge: Int = 256): ImageBitmap? =
    mediaLoads.withPermit { loadRemoteImage(url, maxEdge) }

internal expect suspend fun loadRemoteImage(url: String, maxEdge: Int): ImageBitmap?
