package com.matteo.chatapp.viewmodel

import com.matteo.chatapp.model.DmConversation
import com.matteo.chatapp.model.FriendRequest
import com.matteo.chatapp.model.PresenceEvent
import com.matteo.chatapp.model.ProfileEvent
import com.matteo.chatapp.model.UserProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FriendUiStateTest {

    @Test
    fun presenceEventUpdatesEveryRepresentationOfTheUser() {
        val profile = profile(id = "user-1", username = "Matteo")
        val friend = friendship(userId = profile.id, username = profile.username)
        val dm = dm(userId = profile.id, username = profile.username)
        val initial = FriendUiState(
            friends = listOf(friend),
            pendingRequests = listOf(friend),
            dms = listOf(dm),
            selectedDm = dm,
            searchResult = profile,
            viewedProfile = profile
        )

        val updated = initial.withPresenceEvent(
            PresenceEvent(userId = profile.id, username = profile.username, online = true)
        )

        assertTrue(updated.friends.single().friendIsOnline)
        assertTrue(updated.pendingRequests.single().friendIsOnline)
        assertTrue(updated.dms.single().otherIsOnline)
        assertTrue(updated.selectedDm?.otherIsOnline == true)
        assertTrue(updated.searchResult?.isOnline == true)
        assertTrue(updated.viewedProfile?.isOnline == true)
    }

    @Test
    fun profileEventUpdatesListsAndOpenConversationAtomically() {
        val friend = friendship(userId = "user-1", username = "Antes")
        val dm = dm(userId = "user-1", username = "Antes")
        val viewedProfile = profile(id = "user-1", username = "Antes")
        val initial = FriendUiState(
            friends = listOf(friend),
            dms = listOf(dm),
            selectedDm = dm,
            viewedProfile = viewedProfile
        )

        val updated = initial.withProfileEvent(
            ProfileEvent(userId = "user-1", username = "Despues", avatarUrl = "/avatar.png", bio = "Mi descripción")
        )

        assertEquals("Despues", updated.friends.single().friendUsername)
        assertEquals("Despues", updated.dms.single().otherUsername)
        assertEquals("Despues", updated.selectedDm?.otherUsername)
        assertEquals("Despues", updated.viewedProfile?.username)
        assertEquals("/avatar.png", updated.viewedProfile?.avatarUrl)
        assertEquals("Mi descripción", updated.viewedProfile?.bio)
    }

    @Test
    fun upsertFriendshipPreservesExistingOrder() {
        val first = friendship(userId = "user-1", username = "Antes", id = "friendship-1")
        val second = friendship(userId = "user-2", username = "Segundo", id = "friendship-2")
        val updatedFirst = first.copy(friendUsername = "Despues")

        assertEquals(
            listOf(updatedFirst, second),
            listOf(first, second).upsertFriendship(updatedFirst)
        )
    }

    private fun profile(id: String, username: String) = UserProfile(
        id = id,
        username = username,
        email = "$username@example.com"
    )

    private fun friendship(
        userId: String,
        username: String,
        id: String = "friendship-1"
    ) = FriendRequest(
        id = id,
        friendId = userId,
        friendUsername = username,
        status = "accepted"
    )

    private fun dm(userId: String, username: String) = DmConversation(
        id = "dm-1",
        otherUserId = userId,
        otherUsername = username
    )
}
