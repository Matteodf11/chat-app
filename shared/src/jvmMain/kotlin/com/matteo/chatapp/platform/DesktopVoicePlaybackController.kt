package com.matteo.chatapp.platform

import java.net.HttpURLConnection
import java.net.URI
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

internal interface DesktopVoiceClip {
    val durationMicros: Long
    var positionMicros: Long
    val running: Boolean
    fun start()
    fun stop()
    fun close()
}

internal class DesktopVoicePlaybackController(
    private val url: String,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val loadClip: suspend (String) -> DesktopVoiceClip = ::loadDesktopVoiceClip
) : VoicePlaybackController {
    override val isPlaying = MutableStateFlow(false)
    override val isLoading = MutableStateFlow(false)
    override val error = MutableStateFlow<String?>(null)
    override val progress = MutableStateFlow(0f)
    override val durationSeconds = MutableStateFlow(0)
    private val lock = Any()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var clip: DesktopVoiceClip? = null
    private var released = false
    private var progressJob: Job? = null

    override fun playOrPause(): Unit = synchronized(lock) {
        if (released || isLoading.value) return@synchronized
        error.value = null
        val current = clip
        if (current == null) {
            isLoading.value = true
            scope.launch {
                try {
                    val loaded = loadClip(url)
                    synchronized(lock) {
                        if (released) {
                            runCatching { loaded.close() }
                        } else {
                            clip = loaded
                            durationSeconds.value = (loaded.durationMicros / 1_000_000).toInt()
                            isLoading.value = false
                            startPlayback(loaded)
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    synchronized(lock) {
                        if (!released) {
                            playbackFailed()
                        }
                    }
                }
            }
        } else if (isPlaying.value) {
            try {
                current.stop()
                isPlaying.value = false
                progressJob?.cancel()
            } catch (_: Exception) {
                playbackFailed()
            }
        } else {
            try {
                startPlayback(current)
            } catch (_: Exception) {
                playbackFailed()
            }
        }
    }

    private fun startPlayback(current: DesktopVoiceClip) {
        current.start()
        isPlaying.value = true
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                delay(200)
                synchronized(lock) {
                    if (released || clip !== current || !isPlaying.value) return@launch
                    try {
                        if (!current.running) {
                            current.positionMicros = 0
                            progress.value = 0f
                            isPlaying.value = false
                            return@launch
                        }
                        progress.value = if (current.durationMicros > 0) {
                            (current.positionMicros.toDouble() / current.durationMicros).toFloat().coerceIn(0f, 1f)
                        } else 0f
                    } catch (_: Exception) {
                        playbackFailed()
                        return@launch
                    }
                }
            }
        }
    }

    override fun seekTo(progress: Float): Unit = synchronized(lock) {
        if (released || !progress.isFinite()) return@synchronized
        clip?.let {
            try {
                it.positionMicros = (it.durationMicros * progress.coerceIn(0f, 1f)).toLong()
                this.progress.value = progress.coerceIn(0f, 1f)
            } catch (_: Exception) {
                playbackFailed()
            }
        }
    }

    private fun playbackFailed() {
        progressJob?.cancel()
        runCatching { clip?.close() }
        clip = null
        isLoading.value = false
        isPlaying.value = false
        progress.value = 0f
        durationSeconds.value = 0
        error.value = "No se pudo reproducir el audio. Pulsa para reintentar."
    }

    override fun release(): Unit = synchronized(lock) {
        if (released) return@synchronized
        released = true
        scope.cancel()
        runCatching { clip?.close() }
        clip = null
        isPlaying.value = false
        isLoading.value = false
        progress.value = 0f
    }
}

// Called only on the controller's IO dispatcher. A late result is closed by the
// controller if its conversation has already disappeared.
private suspend fun loadDesktopVoiceClip(url: String): DesktopVoiceClip {
    val connection = URI(url).toURL().openConnection().apply {
        connectTimeout = 10_000
        readTimeout = 15_000
    }
    var clip: Clip? = null
    try {
        connection.getInputStream().buffered().use { input ->
            AudioSystem.getAudioInputStream(input).use { audio ->
                val loaded = AudioSystem.getClip()
                clip = loaded
                loaded.open(audio)
                return object : DesktopVoiceClip {
                    override val durationMicros get() = loaded.microsecondLength
                    override var positionMicros: Long
                        get() = loaded.microsecondPosition
                        set(value) { loaded.microsecondPosition = value }
                    override val running get() = loaded.isRunning
                    override fun start() = loaded.start()
                    override fun stop() = loaded.stop()
                    override fun close() = loaded.close()
                }
            }
        }
    } catch (failure: Exception) {
        runCatching { clip?.close() }
        throw failure
    } finally {
        (connection as? HttpURLConnection)?.disconnect()
    }
}
