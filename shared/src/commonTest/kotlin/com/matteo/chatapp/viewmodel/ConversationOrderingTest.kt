package com.matteo.chatapp.viewmodel

import com.matteo.chatapp.model.DmConversation
import kotlin.test.*

class ConversationOrderingTest {
    @Test
    fun olderConversationMovesUpAfterTalkingAndStaleReloadCannotMoveItBack() {
        val old = DmConversation("old", "one", "One", createdAt = "2026-10-01T10:00:00")
        val recent = DmConversation("recent", "two", "Two", createdAt = "2026-10-08T10:00:00",
            lastMessageAt = "2026-10-09T11:00:00")
        assertEquals("recent", orderedConversations(listOf(old, recent), emptyMap(), emptyMap()).first().id)
        val cache = mapOf("old" to DmSummary("Último", "2026-10-09T12:00:00", 0))
        assertEquals(listOf("old", "recent"), orderedConversations(listOf(recent, old), cache, emptyMap()).map { it.id })
        val pinned = recent.copy(pinned = true)
        assertEquals("recent", orderedConversations(listOf(old, recent), cache, mapOf(recent.id to pinned)).first().id)
    }

    @Test
    fun deletingLastMessageKeepsRecordedActivityAndSettings() {
        val dm = DmConversation("dm", "one", "One", lastActivityAt = "2026-10-09T12:00:00",
            lastMessageAt = "2026-10-09T10:00:00")
        val other = DmConversation("other", "two", "Two", lastMessageAt = "2026-10-09T11:00:00")
        val ordered = orderedConversations(listOf(other, dm), emptyMap(), mapOf(dm.id to dm.copy(archived = true, muted = true)))
        assertEquals(dm.id, ordered.first().id)
        assertTrue(ordered.first().archived && ordered.first().muted)
    }
}
