package com.matteo.chatapp.platform

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

expect suspend fun startVoiceRecording(): Boolean

expect suspend fun stopVoiceRecording(): PickedAttachmentFile?

expect fun cancelVoiceRecording()

interface VoicePlaybackController {
    val isPlaying: StateFlow<Boolean>
    val isLoading: StateFlow<Boolean>
    val error: StateFlow<String?>
    val progress: StateFlow<Float>
    val durationSeconds: StateFlow<Int>
    fun playOrPause()
    fun seekTo(progress: Float)
    fun release()
}

@Composable
expect fun rememberVoicePlaybackController(url: String): VoicePlaybackController
