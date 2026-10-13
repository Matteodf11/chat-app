package com.matteo.chatapp.platform

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopVoicePlaybackControllerTest {
    @Test fun historyDoesNotDownloadOrOpenAudioUntilPlay() = runTest {
        var loads = 0
        val clip = FakeClip()
        val controller = DesktopVoicePlaybackController("audio", StandardTestDispatcher(testScheduler)) {
            loads++; clip
        }
        runCurrent()
        assertEquals(0, loads)
        assertFalse(controller.isLoading.value)
        controller.playOrPause()
        assertTrue(controller.isLoading.value)
        runCurrent()
        assertEquals(1, loads)
        assertEquals(1, clip.starts)
        assertTrue(controller.isPlaying.value)
        assertFalse(controller.isLoading.value)
        assertEquals(10, controller.durationSeconds.value)
        controller.release()
    }

    @Test fun repeatedClicksDuringLoadDoNotStartMultipleDownloads() = runTest {
        val ready = CompletableDeferred<Unit>()
        val clip = FakeClip()
        var loads = 0
        val controller = DesktopVoicePlaybackController("audio", StandardTestDispatcher(testScheduler)) {
            loads++; ready.await(); clip
        }
        controller.playOrPause(); runCurrent()
        repeat(5) { controller.playOrPause() }
        ready.complete(Unit); runCurrent()
        assertEquals(1, loads)
        assertEquals(1, clip.starts)
        controller.release()
    }

    @Test fun leavingChatWhileLoadingClosesLateAudioWithoutPlayingIt() = runTest {
        val ready = CompletableDeferred<Unit>()
        val clip = FakeClip()
        val controller = DesktopVoicePlaybackController("audio", StandardTestDispatcher(testScheduler)) {
            // Simulates a blocking provider that returns after cancellation.
            withContext(NonCancellable) { ready.await() }
            clip
        }
        controller.playOrPause(); runCurrent()
        controller.release()
        ready.complete(Unit); runCurrent()
        assertEquals(1, clip.closes)
        assertEquals(0, clip.starts)
        assertFalse(controller.isPlaying.value)
        assertFalse(controller.isLoading.value)
    }

    @Test fun failedDownloadCanBeRetriedWithoutRecreatingConversation() = runTest {
        var attempts = 0
        val clip = FakeClip()
        val controller = DesktopVoicePlaybackController("audio", StandardTestDispatcher(testScheduler)) {
            attempts++
            if (attempts == 1) error("offline")
            clip
        }
        controller.playOrPause(); runCurrent()
        assertNotNull(controller.error.value)
        assertFalse(controller.isLoading.value)
        assertFalse(controller.isPlaying.value)
        controller.playOrPause(); runCurrent()
        assertEquals(2, attempts)
        assertNull(controller.error.value)
        assertTrue(controller.isPlaying.value)
        controller.release()
    }

    @Test fun pauseResumeAndSeekReuseTheLoadedClipAndCompletionRewinds() = runTest {
        val clip = FakeClip()
        var loads = 0
        val controller = DesktopVoicePlaybackController("audio", StandardTestDispatcher(testScheduler)) {
            loads++; clip
        }
        controller.playOrPause(); runCurrent()
        controller.seekTo(.5f)
        assertEquals(5_000_000L, clip.positionMicros)
        assertEquals(.5f, controller.progress.value)
        controller.playOrPause()
        assertFalse(controller.isPlaying.value)
        controller.playOrPause(); runCurrent()
        assertTrue(controller.isPlaying.value)
        assertEquals(1, loads)
        clip.running = false
        advanceTimeBy(201); runCurrent()
        assertFalse(controller.isPlaying.value)
        assertEquals(0L, clip.positionMicros)
        assertEquals(0f, controller.progress.value)
        controller.release()
    }

    @Test fun releaseIsIdempotentAndPreventsPlaybackOrSeekAfterDisposal() = runTest {
        val clip = FakeClip()
        val controller = DesktopVoicePlaybackController("audio", StandardTestDispatcher(testScheduler)) { clip }
        controller.playOrPause(); runCurrent()
        controller.seekTo(Float.NaN)
        assertEquals(0L, clip.positionMicros)
        controller.release(); controller.release()
        controller.playOrPause(); controller.seekTo(.8f); runCurrent()
        assertEquals(1, clip.closes)
        assertEquals(1, clip.starts)
        assertEquals(0L, clip.positionMicros)
    }

    @Test fun failedStartClosesLoadedDeviceAndAllowsANewAttempt() = runTest {
        val broken = FakeClip().apply { failStart = true }
        val working = FakeClip()
        var attempts = 0
        val controller = DesktopVoicePlaybackController("audio", StandardTestDispatcher(testScheduler)) {
            if (attempts++ == 0) broken else working
        }
        controller.playOrPause(); runCurrent()
        assertEquals(1, broken.closes)
        assertNotNull(controller.error.value)
        controller.playOrPause(); runCurrent()
        assertEquals(1, working.starts)
        controller.release()
    }

    @Test fun deviceFailureWhenResumingClosesTheClipAndOffersRetry() = runTest {
        val clip = FakeClip()
        val controller = DesktopVoicePlaybackController("audio", StandardTestDispatcher(testScheduler)) { clip }
        controller.playOrPause(); runCurrent()
        controller.playOrPause()
        clip.failStart = true
        controller.playOrPause()
        assertEquals(1, clip.closes)
        assertFalse(controller.isPlaying.value)
        assertNotNull(controller.error.value)
        controller.release()
    }

    private class FakeClip : DesktopVoiceClip {
        override val durationMicros = 10_000_000L
        override var positionMicros = 0L
        override var running = false
        var starts = 0
        var closes = 0
        var failStart = false
        override fun start() { if (failStart) error("unavailable"); starts++; running = true }
        override fun stop() { running = false }
        override fun close() { closes++; running = false }
    }
}
