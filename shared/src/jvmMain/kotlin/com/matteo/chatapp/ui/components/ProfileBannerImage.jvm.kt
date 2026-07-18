package com.matteo.chatapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.matteo.chatapp.network.BASE_URL
import com.matteo.chatapp.ui.theme.PrimarySoft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import java.net.URI

@Composable
actual fun ProfileBannerImage(
    bannerUrl: String?,
    modifier: Modifier
) {
    var bitmap by remember(bannerUrl) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(bannerUrl) {
        bitmap = bannerUrl
            ?.takeIf { it.isNotBlank() }
            ?.let { loadBannerBitmap(it) }
    }

    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = "profile banner",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(modifier = modifier.background(PrimarySoft))
    }
}

private suspend fun loadBannerBitmap(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
    try {
        val resolvedUrl = if (url.startsWith("/")) "$BASE_URL$url" else url
        val bytes = URI(resolvedUrl).toURL().readBytes()
        SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
    } catch (e: Exception) {
        null
    }
}
