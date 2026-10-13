package com.matteo.chatapp.viewmodel

import com.matteo.chatapp.model.Message
import com.matteo.chatapp.repository.ConversationTarget
import kotlin.test.*

class ConversationHistoryCacheTest {
    private fun target(id: String) = ConversationTarget.DirectMessage(id)
    private fun message(id: String, content: String = id) = Message(id, content, senderUsername = "Ana", createdAt = id)

    @Test fun recentWindowsAreBoundedAndEvictionFollowsUse() {
        val cache = ConversationHistoryCache(maxConversations = 2, maxMessages = 2)
        cache.put(target("a"), listOf(message("1"), message("2"), message("3")), false)
        assertEquals(listOf("2", "3"), cache.get(target("a"))!!.messages.map { it.id })
        assertTrue(cache.get(target("a"))!!.hasOlder)
        cache.put(target("b"), listOf(message("b")), false)
        cache.get(target("a"))
        cache.put(target("c"), listOf(message("c")), false)
        assertNull(cache.get(target("b")))
        assertNotNull(cache.get(target("a")))
        cache.clear()
        assertNull(cache.get(target("a")))
    }

    @Test fun incomingEditsAndDeletionsUpdateTheCachedWindow() {
        val cache = ConversationHistoryCache()
        val chat = target("a")
        cache.put(chat, listOf(message("1"), message("2")), true)
        cache.update(chat, message("2", "Corregido").copy(edited = true))
        cache.update(chat, message("1").copy(deleted = true))
        cache.update(chat, message("3"))
        cache.update(chat, message("3"))
        assertEquals(listOf("2", "3"), cache.get(chat)!!.messages.map { it.id })
        assertEquals("Corregido", cache.get(chat)!!.messages.first().content)
        cache.update(chat, message("0", "Edición antigua").copy(edited = true))
        assertEquals(2, cache.get(chat)!!.messages.size)
    }

    @Test fun oversizedMessagesCannotGrowTheCacheBeyondItsBudget() {
        val cache = ConversationHistoryCache(maxBytes = 600)
        cache.put(target("a"), listOf(message("1", "x".repeat(1000)), message("2")), false)
        assertEquals(listOf("2"), cache.get(target("a"))!!.messages.map { it.id })
        assertTrue(cache.get(target("a"))!!.hasOlder)
        cache.put(target("b"), listOf(message("3", "y".repeat(100))), false)
        assertNull(cache.get(target("a")))
        assertNotNull(cache.get(target("b")))
    }
}
