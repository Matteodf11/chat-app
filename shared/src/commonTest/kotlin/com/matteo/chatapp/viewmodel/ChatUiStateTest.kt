package com.matteo.chatapp.viewmodel

import com.matteo.chatapp.model.Message
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatUiStateTest {

    @Test
    fun appendsNewMessage() {
        val existing = message(id = "1", content = "Primero")
        val incoming = message(id = "2", content = "Segundo")

        assertEquals(listOf(existing, incoming), listOf(existing).withMessageChange(incoming))
    }

    @Test
    fun replacesEditedMessageWithoutChangingItsPosition() {
        val first = message(id = "1", content = "Original")
        val second = message(id = "2", content = "Segundo")
        val edited = first.copy(content = "Editado", edited = true)

        assertEquals(listOf(edited, second), listOf(first, second).withMessageChange(edited))
    }

    @Test
    fun removesDeletedMessage() {
        val first = message(id = "1", content = "Primero")
        val second = message(id = "2", content = "Segundo")

        assertEquals(
            listOf(second),
            listOf(first, second).withMessageChange(first.copy(deleted = true))
        )
    }

    @Test
    fun prependsOnlyMessagesThatAreNotAlreadyLoaded() {
        val oldest = message(id = "1", content = "Antiguo")
        val duplicate = message(id = "2", content = "Duplicado")
        val current = message(id = "2", content = "Actual")
        val newest = message(id = "3", content = "Reciente")

        assertEquals(
            listOf(oldest, current, newest),
            listOf(current, newest).prependNewMessages(listOf(oldest, duplicate))
        )
    }

    private fun message(id: String, content: String) = Message(
        id = id,
        content = content,
        senderUsername = "matteo"
    )
}
