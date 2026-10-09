package com.matteo.chatapp.ui.components

import com.matteo.chatapp.platform.readAttachmentBytes
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.matteo.chatapp.network.BASE_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

internal actual suspend fun loadRemoteImage(url: String, maxEdge: Int): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val connection = URL(if (url.startsWith("/")) "$BASE_URL$url" else url).openConnection()
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        val bytes = connection.getInputStream().use { it.readAttachmentBytes() }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        require(options.outWidth > 0 && options.outHeight > 0)
        var sample = 1
        while (maxOf(options.outWidth, options.outHeight) / sample > maxEdge) sample *= 2
        options.inJustDecodeBounds = false
        options.inSampleSize = sample
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
    }.getOrNull()
}
