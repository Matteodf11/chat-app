package com.matteo.chatapp.platform

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume

internal object AndroidVoiceRecorder {
    private const val SAMPLE_RATE = 16_000
    private const val MAX_PCM_BYTES = 10 * 1024 * 1024
    private var permissionLauncher: ActivityResultLauncher<String>? = null
    private var permissionResult: ((Boolean) -> Unit)? = null
    private var audioRecord: AudioRecord? = null
    private var output: ByteArrayOutputStream? = null
    private var recordingThread: Thread? = null
    @Volatile private var recording = false

    fun initialize(activity: ComponentActivity) {
        permissionLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            permissionResult?.invoke(granted)
            permissionResult = null
        }
    }

    suspend fun start(): Boolean {
        if (recording) return false
        if (!ensurePermission()) return false
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) return false

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuffer * 2
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return false
        }

        val byteOutput = ByteArrayOutputStream()
        audioRecord = recorder
        output = byteOutput
        recording = true
        recorder.startRecording()
        recordingThread = Thread {
            val buffer = ByteArray(minBuffer)
            while (recording && byteOutput.size() < MAX_PCM_BYTES) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) byteOutput.write(buffer, 0, read)
            }
            recording = false
        }.apply {
            name = "chimera-voice-recorder"
            start()
        }
        return true
    }

    fun stop(): PickedAttachmentFile? {
        val recorder = audioRecord ?: return null
        recording = false
        runCatching { recorder.stop() }
        recordingThread?.join(1500)
        recorder.release()
        val pcm = output?.toByteArray() ?: ByteArray(0)
        clear()
        if (pcm.size < SAMPLE_RATE / 2) return null
        return PickedAttachmentFile(
            fileName = "nota-de-voz-${System.currentTimeMillis()}.wav",
            contentType = "audio/wav",
            bytes = pcmToWav(pcm, SAMPLE_RATE)
        )
    }

    fun cancel() {
        recording = false
        runCatching { audioRecord?.stop() }
        recordingThread?.join(1000)
        audioRecord?.release()
        clear()
    }

    private suspend fun ensurePermission(): Boolean {
        if (ContextCompat.checkSelfPermission(AndroidPlatform.context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        return suspendCancellableCoroutine { continuation ->
            val launcher = permissionLauncher
            if (launcher == null || permissionResult != null) {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }
            permissionResult = { granted -> if (continuation.isActive) continuation.resume(granted) }
            continuation.invokeOnCancellation { permissionResult = null }
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun clear() {
        audioRecord = null
        output = null
        recordingThread = null
    }
}

actual suspend fun startVoiceRecording(): Boolean = AndroidVoiceRecorder.start()

actual suspend fun stopVoiceRecording(): PickedAttachmentFile? = AndroidVoiceRecorder.stop()

actual fun cancelVoiceRecording() = AndroidVoiceRecorder.cancel()

@Composable
actual fun rememberVoicePlaybackController(url: String): VoicePlaybackController {
    val controller = remember(url) { AndroidVoicePlaybackController(url) }
    DisposableEffect(controller) { onDispose { controller.release() } }
    return controller
}

private class AndroidVoicePlaybackController(url: String) : VoicePlaybackController {
    override val isPlaying = MutableStateFlow(false)
    override val progress = MutableStateFlow(0f)
    override val durationSeconds = MutableStateFlow(0)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val player = MediaPlayer()
    private var prepared = false
    private var playWhenReady = false
    private var progressJob: Job? = null

    init {
        player.setDataSource(url)
        player.setOnPreparedListener {
            prepared = true
            durationSeconds.value = (player.duration / 1000).coerceAtLeast(0)
            if (playWhenReady) startPlayback()
        }
        player.setOnCompletionListener {
            isPlaying.value = false
            progress.value = 0f
            player.seekTo(0)
            progressJob?.cancel()
        }
        player.prepareAsync()
    }

    override fun playOrPause() {
        if (!prepared) {
            playWhenReady = true
            return
        }
        if (player.isPlaying) {
            player.pause()
            isPlaying.value = false
            progressJob?.cancel()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        playWhenReady = false
        player.start()
        isPlaying.value = true
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && player.isPlaying) {
                progress.value = if (player.duration > 0) player.currentPosition.toFloat() / player.duration else 0f
                delay(200)
            }
        }
    }

    override fun seekTo(progress: Float) {
        if (prepared && player.duration > 0) player.seekTo((player.duration * progress.coerceIn(0f, 1f)).toInt())
    }

    override fun release() {
        progressJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
        runCatching { player.release() }
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
