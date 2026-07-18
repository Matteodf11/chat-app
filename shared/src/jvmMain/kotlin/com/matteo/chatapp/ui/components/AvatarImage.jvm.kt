package com.matteo.chatapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matteo.chatapp.network.BASE_URL
import com.matteo.chatapp.ui.theme.OnBackground
import com.matteo.chatapp.ui.theme.PrimarySoft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import java.net.URI

@Composable
actual fun AvatarImage(
    avatarUrl: String?,
    name: String,
    size: Int,
    modifier: Modifier
) {
    var bitmap by remember(avatarUrl) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(avatarUrl) {
        bitmap = avatarUrl
            ?.takeIf { it.isNotBlank() }
            ?.let { loadImageBitmap(it) }
    }

    val baseModifier = modifier.size(size.dp).clip(CircleShape)
    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = "$name avatar",
            modifier = baseModifier,
            contentScale = ContentScale.Crop
        )
    } else {
        AvatarFallback(name = name, size = size, modifier = baseModifier)
    }
}

private suspend fun loadImageBitmap(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
    try {
        val resolvedUrl = if (url.startsWith("/")) "$BASE_URL$url" else url
        val bytes = URI(resolvedUrl).toURL().readBytes()
        SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun AvatarFallback(name: String, size: Int, modifier: Modifier) {
    Box(
        modifier = modifier.background(PrimarySoft),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(1).uppercase(),
            color = OnBackground,
            fontSize = (size / 2.6).sp,
            fontWeight = FontWeight.Bold
        )
    }
}
