package com.matteo.chatapp.ui.screens

import kotlin.test.*

class TextDraftsTest {
    private val stored = mutableMapOf<Pair<String, String>, String>()
    private fun createDrafts(account: String = "me") = TextDrafts(account,
        read = { owner, chat -> stored[owner to chat].orEmpty() },
        write = { owner, chat, text -> stored[owner to chat] = text })

    @Test fun acceptingSendClearsBothVisibleEditorAndPersistentDraft() {
        val drafts = createDrafts()
        val editor = drafts.state("dm:ana")
        drafts.update("dm:ana", "  Hola Ana\n")
        val queued = drafts.capture("dm:ana")
        assertTrue(drafts.clearIfUnchanged(queued))
        assertEquals("", editor.value)
        assertEquals("", stored["me" to "dm:ana"])
        assertEquals("", createDrafts().state("dm:ana").value)
    }

    @Test fun completingOldSendAfterNavigationLeavesOtherChatUntouched() {
        val drafts = createDrafts()
        drafts.update("dm:ana", "Primero")
        val queued = drafts.capture("dm:ana")
        val otherEditor = drafts.state("channel:work")
        drafts.update("channel:work", "Otro mensaje")
        assertTrue(drafts.clearIfUnchanged(queued))
        assertEquals("Otro mensaje", otherEditor.value)
        assertEquals("Otro mensaje", stored["me" to "channel:work"])
        assertEquals("", drafts.state("dm:ana").value)
    }

    @Test fun textTypedWhileQueueIsSavingMustSurviveTheCallback() {
        val drafts = createDrafts()
        drafts.update("dm:ana", "Mensaje enviado")
        val queued = drafts.capture("dm:ana")
        drafts.update("dm:ana", "Siguiente mensaje")
        assertFalse(drafts.clearIfUnchanged(queued))
        assertEquals("Siguiente mensaje", drafts.state("dm:ana").value)
        assertEquals("Siguiente mensaje", stored["me" to "dm:ana"])
    }

    @Test fun retypingIdenticalTextIsStillANewDraftAndOtherAccountsAreIsolated() {
        val drafts = createDrafts()
        drafts.update("dm:ana", "Hola")
        val queued = drafts.capture("dm:ana")
        drafts.update("dm:ana", "")
        drafts.update("dm:ana", "Hola")
        assertFalse(drafts.clearIfUnchanged(queued))
        assertEquals("Hola", drafts.state("dm:ana").value)
        assertEquals("", createDrafts("other").state("dm:ana").value)
    }
}
