package com.matteo.chatapp.platform

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class DesktopProfileImagePickerTest {
    @Test fun nativePickerDoesNotBlockOrReenterTheCallingUiThread(): Unit = runBlocking {
        val directory = Files.createTempDirectory("chimera-profile-test")
        val file = directory.resolve("avatar.PNG").toFile().apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val entered = CompletableDeferred<Unit>()
        val dismiss = CountDownLatch(1)
        Executors.newSingleThreadExecutor { Thread(it, "profile-test-ui") }.asCoroutineDispatcher().use { ui ->
            val picking = async(ui) {
                pickDesktopProfileImage("Seleccionar foto", chooseFile = { title ->
                    assertEquals("Seleccionar foto", title)
                    assertNotEquals("profile-test-ui", Thread.currentThread().name)
                    entered.complete(Unit)
                    check(dismiss.await(5, TimeUnit.SECONDS)) { "El selector bloqueó la interfaz" }
                    file
                })
            }
            try {
                withTimeout(5_000) {
                    entered.await()
                    // This must run while the native picker is still open.
                    withContext(ui) { assertFalse(picking.isCompleted) }
                    dismiss.countDown()
                    val result = picking.await()!!
                    assertEquals("image/png", result.contentType)
                    assertContentEquals(byteArrayOf(1, 2, 3), result.bytes)
                }
            } finally {
                dismiss.countDown()
                picking.cancelAndJoin()
                file.delete(); directory.toFile().delete()
            }
        }
    }

    @Test fun cancellingTheNativePickerReturnsNoImage() = runTest {
        assertNull(pickDesktopProfileImage("Portada", chooseFile = { null }))
    }

    @Test fun leavingWhilePickerIsOpenDoesNotReadItsLateSelection(): Unit = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val dismiss = CountDownLatch(1)
        val picking = async {
            pickDesktopProfileImage("Foto", chooseFile = {
                entered.complete(Unit)
                check(dismiss.await(5, TimeUnit.SECONDS))
                // Cancellation must be observed before validating or reading this path.
                java.io.File("missing-profile-image.png")
            })
        }
        try {
            withTimeout(5_000) {
                entered.await()
                picking.cancel()
                dismiss.countDown()
                assertFailsWith<CancellationException> { picking.await() }
            }
        } finally {
            dismiss.countDown()
            picking.cancelAndJoin()
        }
    }

    @Test fun oversizedAndEmptyImagesAreRejectedBeforeLoading() = runTest {
        val directory = Files.createTempDirectory("chimera-profile-size")
        val file = directory.resolve("avatar.jpg").toFile()
        try {
            file.writeBytes(ByteArray(MAX_PROFILE_IMAGE_BYTES + 1))
            assertFailsWith<IllegalArgumentException> { pickDesktopProfileImage("Foto", { file }) }
            file.writeBytes(byteArrayOf())
            assertFailsWith<IllegalArgumentException> { pickDesktopProfileImage("Foto", { file }) }
        } finally { file.delete(); directory.toFile().delete() }
    }

    @Test fun unsupportedFormatProducesAnActionableError() = runTest {
        val file = Files.createTempFile("chimera-profile-format", ".txt").toFile()
        try {
            file.writeText("not an image")
            val error = assertFailsWith<IllegalArgumentException> { pickDesktopProfileImage("Foto", { file }) }
            assertTrue(error.message!!.contains("JPG, PNG o WEBP"))
        } finally { file.delete() }
    }

    @Test fun readLimitAlsoProtectsAgainstAFileGrowingAfterSelection() = runTest {
        val input = object : InputStream() {
            var remaining = MAX_PROFILE_IMAGE_BYTES + 1
            override fun read(): Int = if (remaining-- > 0) 1 else -1
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (remaining <= 0) return -1
                val count = minOf(length, remaining)
                remaining -= count
                return count
            }
        }
        assertFailsWith<IllegalArgumentException> { input.readProfileImageBytes() }
        val bytes = ByteArray(MAX_PROFILE_IMAGE_BYTES)
        assertContentEquals(bytes, ByteArrayInputStream(bytes).readProfileImageBytes())
    }
}
