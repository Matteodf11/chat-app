package com.matteo.chatapp.viewmodel

import com.matteo.chatapp.model.Message
import com.matteo.chatapp.platform.PickedAttachmentFile
import kotlin.test.*

class ComposerDraftsTest {
    private fun file(size: Int) = PickedAttachmentFile("foto.jpg", "image/jpeg", ByteArray(size))
    @Test fun navigationPreservesReplyAndFilesIndependently() {
        val drafts = ComposerDrafts()
        val reply = Message("reply", "Texto", senderUsername = "Ana")
        val photo = file(8)
        val attachment = file(3)
        drafts.update("dm:ana") { it.copy(reply = reply, photos = listOf(photo), fromCamera = true) }
        drafts.update("channel:work") { it.copy(attachment = attachment) }
        assertEquals(reply, drafts.state.value["dm:ana"]?.reply)
        assertSame(photo, drafts.state.value["dm:ana"]?.photos?.single())
        assertSame(attachment, drafts.state.value["channel:work"]?.attachment)
        drafts.update("dm:ana") { ComposerDraft() }
        assertNull(drafts.state.value["dm:ana"])
        assertSame(attachment, drafts.state.value["channel:work"]?.attachment)
    }
    @Test fun exceedingBudgetNeverEvictsOtherDraftsAndReplacementReclaimsSpace() {
        val drafts = ComposerDrafts(10)
        val first = file(8)
        assertTrue(drafts.update("one") { it.copy(attachment = first) })
        assertFalse(drafts.update("two") { it.copy(photos = listOf(file(4))) })
        assertSame(first, drafts.state.value["one"]?.attachment)
        assertNull(drafts.state.value["two"])
        assertTrue(drafts.update("one") { it.copy(attachment = file(2)) })
        assertTrue(drafts.update("two") { it.copy(photos = listOf(file(4))) })
        drafts.clear()
        assertTrue(drafts.state.value.isEmpty())
    }
}
