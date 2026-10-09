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
