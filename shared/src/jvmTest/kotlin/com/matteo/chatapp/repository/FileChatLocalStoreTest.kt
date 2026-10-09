package com.matteo.chatapp.repository

import com.matteo.chatapp.platform.PickedAttachmentFile
import com.matteo.chatapp.viewmodel.*
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class FileChatLocalStoreTest {
    @Test
    fun restartRestoresTextFileAndVoiceWithOriginalIdentifiersAndAccountIsolation() = runTest {
        val root = Files.createTempDirectory("chat-outbox-test").toFile()
        try {
            val store = FileChatLocalStore(root, "one@example.invalid")
            val entries = listOf(null, PickedAttachmentFile("document.pdf", "application/pdf", byteArrayOf(1, 2, 3)),
                PickedAttachmentFile("voice.m4a", "audio/mp4", byteArrayOf(4, 5)))
            entries.forEachIndexed { index, file ->
                store.put(PendingMessage("00000000-0000-0000-0000-00000000000$index",
                    ConversationTarget.DirectMessage("dm-1"), "Mensaje $index", "reply", file))
            }
            val restarted = FileChatLocalStore(root, "one@example.invalid")
            val restored = restarted.pending()
            assertEquals(3, restored.size)
            assertTrue(restored.all { it.status == SendStatus.FAILED && it.replyToId == "reply" })
            assertContentEquals(byteArrayOf(1, 2, 3), restored[1].file?.bytes)
            assertContentEquals(byteArrayOf(4, 5), restored[2].file?.bytes)
            assertTrue(FileChatLocalStore(root, "two@example.invalid").pending().isEmpty())
            restarted.remove(restored[1].id)
            assertEquals(2, FileChatLocalStore(root, "one@example.invalid").pending().size)
        } finally { root.deleteRecursively() }
    }

    @Test
    fun photoBatchSurvivesRestartAndAcknowledgementsRemoveOnlyTheirPhoto() = runTest {
        val root = Files.createTempDirectory("chat-photo-batch-test").toFile()
        try {
            val store = FileChatLocalStore(root, "account")
            val photos = (0..2).map { index ->
                PendingMessage("00000000-0000-0000-0000-00000000000$index",
                    ConversationTarget.DirectMessage("dm"), if (index == 0) "Comentario" else "", null,
                    PickedAttachmentFile("$index.jpg", "image/jpeg", byteArrayOf(index.toByte())))
            }
            store.putBatch(photos)
            assertEquals(photos.map { it.id }, FileChatLocalStore(root, "account").pending().map { it.id })
            store.remove(photos[1].id)
            val restored = FileChatLocalStore(root, "account").pending()
            assertEquals(listOf(photos[0].id, photos[2].id), restored.map { it.id })
            assertEquals("Comentario", restored.first().content)
            store.remove(photos[0].id)
            store.remove(photos[2].id)
            assertTrue(FileChatLocalStore(root, "account").pending().isEmpty())
        } finally { root.deleteRecursively() }
    }

    @Test
    fun failureBeforeBatchCommitDoesNotRestorePartOfTheSelection() = runTest {
        val root = Files.createTempDirectory("chat-photo-failure-test").toFile()
        try {
            val store = FileChatLocalStore(root, "account")
            store.saveSummaries(emptyMap())
            val directory = root.listFiles()!!.single()
            val secondId = "00000000-0000-0000-0000-000000000002"
            java.io.File(directory, "$secondId.bin").mkdir()
            val photos = listOf("00000000-0000-0000-0000-000000000001", secondId).map {
                PendingMessage(it, ConversationTarget.DirectMessage("dm"), "", null,
                    PickedAttachmentFile("foto.jpg", "image/jpeg", byteArrayOf(1)))
            }
            assertFails { store.putBatch(photos) }
            assertTrue(FileChatLocalStore(root, "account").pending().isEmpty())
            assertFalse(java.io.File(directory, "${photos[0].id}.bin").exists())
        } finally { root.deleteRecursively() }
    }

    @Test
    fun atomicReplacementRetainsLatestSummaryAfterRestart() = runTest {
        val root = Files.createTempDirectory("chat-summary-test").toFile()
        try {
            val store = FileChatLocalStore(root, "account")
            store.saveSummaries(mapOf("dm" to DmSummary("Antes", "2026-10-09T10:00:00", 1)))
            val latest = mapOf("dm" to DmSummary("Después", "2026-10-09T11:00:00", 2))
            store.saveSummaries(latest)
            assertEquals(latest, FileChatLocalStore(root, "account").summaries())
        } finally { root.deleteRecursively() }
    }
}
