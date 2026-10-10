package com.matteo.chatapp.ui.screens

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConversationScrollTest {
    @Test fun readingTopOfLastPhotoDoesNotFollowIncomingMessages() {
        assertFalse(isNearConversationEnd(9, 10, 950, 600, 80f))
    }

    @Test fun readingOlderMessagesDoesNotFollowEvenIfTheirBottomIsVisible() {
        assertFalse(isNearConversationEnd(7, 10, 580, 600, 80f))
    }

    @Test fun lastMessageWithinBottomPaddingStillFollows() {
        assertTrue(isNearConversationEnd(9, 10, 616, 600, 80f))
        assertTrue(isNearConversationEnd(null, 0, 0, 600, 80f))
    }
}
