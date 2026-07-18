package com.matteo.chatapp.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.net.URI
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.DataLine
import javax.sound.sampled.LineEvent
import javax.sound.sampled.TargetDataLine

private object JvmVoiceRecorder {
    private const val SAMPLE_RATE = 16_000f
    private const val MAX_PCM_BYTES = 10 * 1024 * 1024
    private val format = AudioFormat(SAMPLE_RATE, 16, 1, true, false)
    private var line: TargetDataLine? = null
    private var output: ByteArrayOutputStream? = null
    private var thread: Thread? = null
    @Volatile private var recording = false

    fun start(): Boolean {
        if (recording) return false
        return runCatching {
            val info = DataLine.Info(TargetDataLine::class.java, format)
            if (!AudioSystem.isLineSupported(info)) return false
            val target = AudioSystem.getLine(info) as TargetDataLine
            target.open(format)
            val byteOutput = ByteArrayOutputStream()
            line = target
            output = byteOutput
            recording = true
            target.start()
            thread = Thread {
                val buffer = ByteArray(4096)
                while (recording && byteOutput.size() < MAX_PCM_BYTES) {
                    val read = target.read(buffer, 0, buffer.size)
                    if (read > 0) byteOutput.write(buffer, 0, read)
                }
                recording = false
            }.apply {
                name = "chimera-voice-recorder"
                start()
            }
            true
        }.getOrDefault(false)
    }

    fun stop(): PickedAttachmentFile? {
        val target = line ?: return null
        recording = false
        runCatching { target.stop() }
        runCatching { target.close() }
        thread?.join(1500)
        val pcm = output?.toByteArray() ?: ByteArray(0)
        clear()
        if (pcm.size < SAMPLE_RATE.toInt() / 2) return null
        return PickedAttachmentFile(
            fileName = "nota-de-voz-${System.currentTimeMillis()}.wav",
            contentType = "audio/wav",
            bytes = pcmToWav(pcm, SAMPLE_RATE.toInt())
        )
    }

    fun cancel() {
        recording = false
        runCatching { line?.stop() }
        runCatching { line?.close() }
        thread?.join(1000)
        clear()
    }

    private fun clear() {
        line = null
        output = null
        thread = null
    }
}

actual suspend fun startVoiceRecording(): Boolean = JvmVoiceRecorder.start()

actual suspend fun stopVoiceRecording(): PickedAttachmentFile? = JvmVoiceRecorder.stop()

actual fun cancelVoiceRecording() = JvmVoiceRecorder.cancel()

@Composable
actual fun rememberVoicePlaybackController(url: String): VoicePlaybackController {
    val controller = remember(url) { JvmVoicePlaybackController(url) }
    DisposableEffect(controller) { onDispose { controller.release() } }
    return controller
}

private class JvmVoicePlaybackController(private val url: String) : VoicePlaybackController {
    override val isPlaying = MutableStateFlow(false)
    override val progress = MutableStateFlow(0f)
    override val durationSeconds = MutableStateFlow(0)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var clip: Clip? = null
    private var playWhenReady = false
    private var progressJob: Job? = null

    init {
        scope.launch {
            runCatching {
                URI(url).toURL().openStream().buffered().use { input ->
                    AudioSystem.getAudioInputStream(input).use { audio ->
                        val loadedClip = AudioSystem.getClip()
                        loadedClip.open(audio)
                        loadedClip.addLineListener { event ->
                            if (event.type == LineEvent.Type.STOP && loadedClip.microsecondPosition >= loadedClip.microsecondLength - 20_000) {
                                isPlaying.value = false
                                progress.value = 0f
                                loadedClip.microsecondPosition = 0
                            }
                        }
                        clip = loadedClip
                        durationSeconds.value = (loadedClip.microsecondLength / 1_000_000L).toInt()
                        if (playWhenReady) startPlayback()
                    }
                }
            }
        }
    }

    override fun playOrPause() {
        val current = clip
        if (current == null) {
            playWhenReady = true
            return
        }
        if (current.isRunning) {
            current.stop()
            isPlaying.value = false
            progressJob?.cancel()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        val current = clip ?: return
        playWhenReady = false
        current.start()
        isPlaying.value = true
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && current.isRunning) {
                progress.value = if (current.microsecondLength > 0) {
                    current.microsecondPosition.toFloat() / current.microsecondLength
                } else 0f
                delay(200)
            }
        }
    }

    override fun seekTo(progress: Float) {
        clip?.let { it.microsecondPosition = (it.microsecondLength * progress.coerceIn(0f, 1f)).toLong() }
    }

    override fun release() {
        progressJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
        runCatching { clip?.close() }
    }
}

private fun pcmToWav(pcm: ByteArray, sampleRate: Int): ByteArray {
    val output = ByteArrayOutputStream(pcm.size + 44)
    fun writeAscii(value: String) = output.write(value.encodeToByteArray())
    fun writeInt(value: Int) {
        output.write(value and 0xFF); output.write(value shr 8 and 0xFF)
        output.write(value shr 16 and 0xFF); output.write(value shr 24 and 0xFF)
    }
    fun writeShort(value: Int) { output.write(value and 0xFF); output.write(value shr 8 and 0xFF) }
    writeAscii("RIFF"); writeInt(pcm.size + 36); writeAscii("WAVE")
    writeAscii("fmt "); writeInt(16); writeShort(1); writeShort(1)
    writeInt(sampleRate); writeInt(sampleRate * 2); writeShort(2); writeShort(16)
    writeAscii("data"); writeInt(pcm.size); output.write(pcm)
    return output.toByteArray()
}
