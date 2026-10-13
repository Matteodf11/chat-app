package com.matteo.chatapp.ui.screens

import com.matteo.chatapp.model.Message
import kotlin.test.*

class ConversationArrivalsTest {
    private fun message(id: String, own: Boolean = false) = Message(id, id, if (own) "me" else "friend", "Autor")
    @Test fun initialLoadAndOlderPagesNeverCountAsArrivals() {
        val tracker = ConversationArrivals()
        tracker.update(listOf(message("2"), message("3")), "me", false)
        tracker.update(listOf(message("1"), message("2"), message("3")), "me", false)
        assertEquals(0, tracker.count)
    }
    @Test fun countsOnlyIncomingOnceAndRemovesDeletedMessages() {
        val tracker = ConversationArrivals()
        val baseline = listOf(message("1"))
        tracker.update(baseline, "me", true)
        val messages = baseline + listOf(message("2"), message("3", own = true), message("4"))
        tracker.update(messages, "me", false)
        tracker.update(messages.map { if (it.id == "2") it.copy(content = "editado") else it }, "me", false)
        assertEquals(2, tracker.count)
        tracker.update(messages.map { if (it.id == "2") it.copy(deleted = true) else it }, "me", false)
        assertEquals(1, tracker.count)
    }
    @Test fun returningToBottomClearsCountAndContinuesFollowing() {
        val tracker = ConversationArrivals()
        tracker.update(listOf(message("1")), "me", true)
        tracker.update(listOf(message("1"), message("2")), "me", false)
        assertEquals(1, tracker.count)
        tracker.clear()
        tracker.update(listOf(message("1"), message("2"), message("3")), "me", true)
        assertEquals(0, tracker.count)
    }
}
