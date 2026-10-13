package com.matteo.chatapp.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
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
    val controller = remember(url) { DesktopVoicePlaybackController(url) }
    DisposableEffect(controller) { onDispose { controller.release() } }
    return controller
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
