package com.matteo.chatapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.matteo.chatapp.ui.theme.SurfaceVariant

@Composable
fun SharedMediaThumbnail(url: String?, description: String?, modifier: Modifier = Modifier) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url) { bitmap = url?.let { loadSharedThumbnail(it) } }
    bitmap?.let { Image(it, description, modifier, contentScale = ContentScale.Crop) }
        ?: Box(modifier.background(SurfaceVariant))
}

internal expect suspend fun loadSharedThumbnail(url: String): ImageBitmap?
