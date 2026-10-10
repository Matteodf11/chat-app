package com.matteo.chatapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.matteo.chatapp.ui.theme.SurfaceVariant
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@Composable
fun SharedMediaThumbnail(url: String?, description: String?, modifier: Modifier = Modifier, maxEdge: Int = 256,
    showFullImage: Boolean = false, cacheKey: String = url.orEmpty()) {
    var ratio by rememberSaveable(cacheKey) { mutableStateOf<Float?>(null) }
    var bitmap by remember(cacheKey, maxEdge) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url, cacheKey, maxEdge) {
        bitmap = url?.let { loadSharedThumbnail(it, maxEdge, cacheKey) }
        bitmap?.let { ratio = it.width.toFloat() / it.height.coerceAtLeast(1) }
    }
    if (showFullImage) {
        BoxWithConstraints(modifier) {
            val availableWidth = maxWidth.coerceAtMost(360.dp)
            val image = bitmap
            val imageRatio = ratio ?: (availableWidth.value / 180f).coerceAtLeast(0.01f)
            val height = (availableWidth / imageRatio).coerceAtMost(420.dp)
            val frame = Modifier.size(height * imageRatio, height)
            if (image != null) {
                Image(image, description, frame, contentScale = ContentScale.Fit,
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.High)
            } else Box(frame.background(SurfaceVariant))
        }
    } else {
        bitmap?.let { Image(it, description, modifier, contentScale = ContentScale.Crop, filterQuality = androidx.compose.ui.graphics.FilterQuality.High) }
            ?: Box(modifier.background(SurfaceVariant))
    }
}

private val mediaLoads = Semaphore(3)
private val mediaCacheMutex = Mutex()
private val mediaCache = LinkedHashMap<String, ImageBitmap>()
private var mediaCacheBytes = 0L
private const val MAX_MEDIA_CACHE_BYTES = 24L * 1024 * 1024

internal suspend fun loadSharedThumbnail(url: String, maxEdge: Int = 256, cacheKey: String = url): ImageBitmap? {
    val key = "$cacheKey:$maxEdge"
    mediaCacheMutex.withLock {
        mediaCache.remove(key)?.let { cached -> mediaCache[key] = cached; return cached }
    }
    val image = mediaLoads.withPermit { loadRemoteImage(url, maxEdge) } ?: return null
    val bytes = image.width.toLong() * image.height * 4
    if (bytes <= MAX_MEDIA_CACHE_BYTES) mediaCacheMutex.withLock {
        mediaCache.remove(key)?.let { mediaCacheBytes -= it.width.toLong() * it.height * 4 }
        while (mediaCache.isNotEmpty() && mediaCacheBytes + bytes > MAX_MEDIA_CACHE_BYTES) {
            val oldest = mediaCache.entries.first()
            mediaCacheBytes -= oldest.value.width.toLong() * oldest.value.height * 4
            mediaCache.remove(oldest.key)
        }
        mediaCache[key] = image
        mediaCacheBytes += bytes
    }
    return image
}

internal expect suspend fun loadRemoteImage(url: String, maxEdge: Int): ImageBitmap?
