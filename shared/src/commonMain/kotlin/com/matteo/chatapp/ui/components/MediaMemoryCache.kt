package com.matteo.chatapp.ui.components

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class MediaMemoryCache<T>(private val maxBytes: Long) {
    private val mutex = Mutex()
    private val entries = linkedMapOf<String, Pair<T, Long>>()
    private val _bytes = MutableStateFlow(0L)
    val bytes = _bytes.asStateFlow()
    private var generation = 0L
    private val snapshot = MutableStateFlow<Map<String, T>>(emptyMap())
    fun peek(key: String): T? = snapshot.value[key]
    suspend fun get(key: String): T? = mutex.withLock {
        entries.remove(key)?.also { entries[key] = it }?.first
    }
    suspend fun generation(): Long = mutex.withLock { generation }
    suspend fun put(key: String, value: T, size: Long, startedAt: Long) = mutex.withLock {
        if (startedAt != generation || size !in 1..maxBytes) return@withLock
        entries.remove(key)?.let { _bytes.value -= it.second }
        while (entries.isNotEmpty() && _bytes.value + size > maxBytes) {
            val oldest = entries.entries.first()
            _bytes.value -= oldest.value.second
            entries.remove(oldest.key)
        }
        entries[key] = value to size
        _bytes.value += size
        snapshot.value = entries.mapValues { it.value.first }
    }
    suspend fun clear() = mutex.withLock { generation++; entries.clear(); snapshot.value = emptyMap(); _bytes.value = 0 }
}

internal val thumbnailCache = MediaMemoryCache<androidx.compose.ui.graphics.ImageBitmap>(24L * 1024 * 1024)

internal val avatarCache = MediaMemoryCache<androidx.compose.ui.graphics.ImageBitmap>(8L * 1024 * 1024)
