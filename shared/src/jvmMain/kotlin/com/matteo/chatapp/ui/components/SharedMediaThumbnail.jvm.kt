package com.matteo.chatapp.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.matteo.chatapp.network.BASE_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.matteo.chatapp.platform.readAttachmentBytes
import java.io.ByteArrayInputStream
import java.net.URI
import javax.imageio.ImageIO

internal actual suspend fun loadRemoteImage(url: String, maxEdge: Int): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val connection = URI(if (url.startsWith("/")) "$BASE_URL$url" else url).toURL().openConnection()
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        val bytes = connection.getInputStream().use { it.readAttachmentBytes() }
        decodePickedImage(bytes, maxEdge)
    }.getOrNull()
}

internal actual suspend fun decodePickedImage(bytes: ByteArray, maxEdge: Int): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        ByteArrayInputStream(bytes).use { input ->
            ImageIO.createImageInputStream(input).use { stream ->
                val reader = ImageIO.getImageReaders(stream).asSequence().firstOrNull() ?: return@withContext decodeSkiaPreview(bytes, maxEdge)
                try {
                    reader.input = stream
                    val sample = ((maxOf(reader.getWidth(0), reader.getHeight(0)) + maxEdge - 1) / maxEdge).coerceAtLeast(1)
                    val parameters = reader.defaultReadParam.apply { setSourceSubsampling(sample, sample, 0, 0) }
                    orientPickedImage(reader.read(0, parameters).toComposeImageBitmap(), bytes)
                } finally { reader.dispose() }
            }
        }
    }.getOrNull()
}

// ImageIO does not provide a WebP reader by default. Keep that preview bounded too.
private fun decodeSkiaPreview(bytes: ByteArray, maxEdge: Int): ImageBitmap =
    org.jetbrains.skia.Image.makeFromEncoded(bytes).use { source ->
        val scale = minOf(1.0, maxEdge.toDouble() / maxOf(source.width, source.height))
        val size = androidx.compose.ui.unit.IntSize((source.width * scale).toInt().coerceAtLeast(1),
            (source.height * scale).toInt().coerceAtLeast(1))
        val result = ImageBitmap(size.width, size.height)
        androidx.compose.ui.graphics.Canvas(result).drawImageRect(source.toComposeImageBitmap(), dstSize = size,
            paint = androidx.compose.ui.graphics.Paint().apply { filterQuality = androidx.compose.ui.graphics.FilterQuality.High })
        result
    }
