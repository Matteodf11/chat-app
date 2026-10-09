package com.matteo.chatapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.matteo.chatapp.ui.theme.SurfaceVariant
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@Composable
fun SharedMediaThumbnail(url: String?, description: String?, modifier: Modifier = Modifier, maxEdge: Int = 256) {
    var bitmap by remember(url, maxEdge) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url, maxEdge) { bitmap = url?.let { loadSharedThumbnail(it, maxEdge) } }
    bitmap?.let { Image(it, description, modifier, contentScale = ContentScale.Crop, filterQuality = androidx.compose.ui.graphics.FilterQuality.High) }
        ?: Box(modifier.background(SurfaceVariant))
}

private val mediaLoads = Semaphore(3)
internal suspend fun loadSharedThumbnail(url: String, maxEdge: Int = 256): ImageBitmap? =
    mediaLoads.withPermit { loadRemoteImage(url, maxEdge) }

internal expect suspend fun loadRemoteImage(url: String, maxEdge: Int): ImageBitmap?
