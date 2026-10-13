package com.matteo.chatapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.matteo.chatapp.ui.theme.SurfaceVariant
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.withLock

@Composable
fun SharedMediaThumbnail(url: String?, description: String?, modifier: Modifier = Modifier, maxEdge: Int = 256,
    showFullImage: Boolean = false, cacheKey: String = url.orEmpty()) {
    val cached = remember(cacheKey, maxEdge) { thumbnailCache.peek("$cacheKey:$maxEdge") }
    var ratio by rememberSaveable(cacheKey) { mutableStateOf(cached?.let { it.width.toFloat() / it.height.coerceAtLeast(1) }) }
    var bitmap by remember(cacheKey, maxEdge) { mutableStateOf(cached) }
    LaunchedEffect(url, cacheKey, maxEdge) {
        bitmap = url?.let { loadSharedThumbnail(it, maxEdge, cacheKey) }
        bitmap?.let { ratio = it.width.toFloat() / it.height.coerceAtLeast(1) }
    }
    if (showFullImage) {
        BoxWithConstraints(modifier) {
            val availableWidth = maxWidth.coerceAtMost(360.dp)
            val image = bitmap
            val imageRatio = image?.let { it.width.toFloat() / it.height.coerceAtLeast(1) }
                ?: ratio ?: (availableWidth.value / 180f).coerceAtLeast(0.01f)
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
private val imageLocks = List(32) { kotlinx.coroutines.sync.Mutex() }
internal suspend fun loadSharedThumbnail(url: String, maxEdge: Int = 256, cacheKey: String = url): ImageBitmap? =
    loadCachedImage(url, maxEdge, "$cacheKey:$maxEdge", thumbnailCache)

internal suspend fun loadCachedImage(url: String, maxEdge: Int, key: String,
    cache: MediaMemoryCache<ImageBitmap>): ImageBitmap? {
    cache.get(key)?.let { return it }
    // Serialize identical requests and recheck after acquiring the lock.
    val lock = imageLocks[(key.hashCode() and Int.MAX_VALUE) % imageLocks.size]
    return lock.withLock {
        cache.get(key)?.let { return@withLock it }
        val generation = cache.generation()
        val image = mediaLoads.withPermit { loadRemoteImage(url, maxEdge) } ?: return@withLock null
        cache.put(key, image, image.width.toLong() * image.height * 4, generation)
        image
    }
}

internal expect suspend fun loadRemoteImage(url: String, maxEdge: Int): ImageBitmap?
