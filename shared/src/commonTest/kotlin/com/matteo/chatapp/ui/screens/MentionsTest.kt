package com.matteo.chatapp.ui.screens

import kotlin.test.*

class MentionsTest {
    @Test fun emailAddressesAreNotMentionQueries() {
        assertNull(mentionQuery("hola ana@correo.es", 18))
        assertNull(mentionQuery("@@ana", 5))
        assertEquals("An", mentionQuery("Hola @An", 8)?.query)
        assertEquals("", mentionQuery("@", 1)?.query)
    }
    @Test fun insertionAtCursorPreservesRestOfMessageAndSupportsNamesWithSpaces() {
        val text = "Para @An, revisa esto"
        val query = mentionQuery(text, 8)!!
        val (updated, cursor) = insertMention(text, query, "Ana María")
        assertEquals("Para @Ana María , revisa esto", updated)
        assertEquals(16, cursor)
        assertNull(mentionQuery("@Ana\notra línea", 15))
    }
}
