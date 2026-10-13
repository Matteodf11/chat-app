package com.matteo.chatapp.ui.components

import androidx.compose.ui.graphics.ImageBitmap

internal suspend fun loadAndroidImage(url: String): ImageBitmap? =
    loadSharedThumbnail(url, 1024, "banner:$url")
