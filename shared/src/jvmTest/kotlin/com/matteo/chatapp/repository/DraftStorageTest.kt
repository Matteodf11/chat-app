package com.matteo.chatapp.repository

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class DraftStorageTest {
    @Test
    fun draftsAreIsolatedByAccountAndConversationAndCanBeCleared() {
        val account = "draft-test-${UUID.randomUUID()}@example.invalid"
        val otherAccount = "other-$account"
        try {
            DraftStorage.save(account, "dm:one", "Borrador con acentos y emoji 🎉")
            DraftStorage.save(account, "channel:one", "Otro borrador")
            assertEquals("Borrador con acentos y emoji 🎉", DraftStorage.load(account, "dm:one"))
            assertEquals("Otro borrador", DraftStorage.load(account, "channel:one"))
            assertEquals("", DraftStorage.load(otherAccount, "dm:one"))
            DraftStorage.save(account, "dm:one", "")
            assertEquals("", DraftStorage.load(account, "dm:one"))
            assertEquals("Otro borrador", DraftStorage.load(account, "channel:one"))
        } finally {
            DraftStorage.save(account, "dm:one", "")
            DraftStorage.save(account, "channel:one", "")
        }
    }
}
