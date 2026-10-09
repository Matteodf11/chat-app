package com.matteo.chatapp.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.matteo.chatapp.network.BASE_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import javax.imageio.ImageIO

internal actual suspend fun loadSharedThumbnail(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val connection = URI(if (url.startsWith("/")) "$BASE_URL$url" else url).toURL().openConnection()
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.getInputStream().use { input ->
            ImageIO.createImageInputStream(input).use { stream ->
                val reader = ImageIO.getImageReaders(stream).asSequence().firstOrNull() ?: return@withContext null
                try {
                    reader.input = stream
                    val sample = ((maxOf(reader.getWidth(0), reader.getHeight(0)) + 255) / 256).coerceAtLeast(1)
                    val parameters = reader.defaultReadParam.apply { setSourceSubsampling(sample, sample, 0, 0) }
                    reader.read(0, parameters).toComposeImageBitmap()
                } finally { reader.dispose() }
            }
        }
    }.getOrNull()
}
