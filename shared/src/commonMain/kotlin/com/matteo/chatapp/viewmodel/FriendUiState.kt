package com.matteo.chatapp.viewmodel

import com.matteo.chatapp.model.DmConversation
import com.matteo.chatapp.model.FriendRequest
import com.matteo.chatapp.model.PresenceEvent
import com.matteo.chatapp.model.ProfileEvent
import com.matteo.chatapp.model.UserProfile

data class FriendUiState(
    val friends: List<FriendRequest> = emptyList(),
    val pendingRequests: List<FriendRequest> = emptyList(),
    val dms: List<DmConversation> = emptyList(),
    val selectedDm: DmConversation? = null,
    val profile: UserProfile? = null,
    val searchResult: UserProfile? = null,
    val viewedProfile: UserProfile? = null,
    val error: String? = null,
    val passwordError: String? = null,
    val changingPassword: Boolean = false,
    val savingProfile: Boolean = false,
    val isSearchingUser: Boolean = false
)

internal fun FriendUiState.withPresenceEvent(event: PresenceEvent): FriendUiState = copy(
    friends = friends.map { friend ->
        if (friend.friendId == event.userId) friend.copy(friendIsOnline = event.online) else friend
    },
    pendingRequests = pendingRequests.map { request ->
        if (request.friendId == event.userId) request.copy(friendIsOnline = event.online) else request
    },
    dms = dms.map { dm ->
        if (dm.otherUserId == event.userId) dm.copy(otherIsOnline = event.online) else dm
    },
    selectedDm = selectedDm?.let { dm ->
        if (dm.otherUserId == event.userId) dm.copy(otherIsOnline = event.online) else dm
    },
    profile = profile?.withOnlineStatus(event.userId, event.online),
    searchResult = searchResult?.withOnlineStatus(event.userId, event.online),
    viewedProfile = viewedProfile?.withOnlineStatus(event.userId, event.online)
)

internal fun FriendUiState.withProfileEvent(event: ProfileEvent): FriendUiState = copy(
    profile = profile?.withProfileEvent(event),
    searchResult = searchResult?.withProfileEvent(event),
    viewedProfile = viewedProfile?.withProfileEvent(event),
    friends = friends.map { friend ->
        if (friend.friendId == event.userId) {
            friend.copy(friendUsername = event.username, friendAvatar = event.avatarUrl)
        } else {
            friend
        }
    },
    pendingRequests = pendingRequests.map { request ->
        if (request.friendId == event.userId) {
            request.copy(friendUsername = event.username, friendAvatar = event.avatarUrl)
        } else {
            request
        }
    },
    dms = dms.map { dm ->
        if (dm.otherUserId == event.userId) {
            dm.copy(otherUsername = event.username, otherAvatar = event.avatarUrl)
        } else {
            dm
        }
    },
    selectedDm = selectedDm?.let { dm ->
        if (dm.otherUserId == event.userId) {
            dm.copy(otherUsername = event.username, otherAvatar = event.avatarUrl)
        } else {
            dm
        }
    }
)

internal fun List<FriendRequest>.upsertFriendship(friendship: FriendRequest): List<FriendRequest> {
    val existingIndex = indexOfFirst { it.id == friendship.id }
    return if (existingIndex < 0) {
        this + friendship
    } else {
        toMutableList().apply { this[existingIndex] = friendship }
    }
}

internal fun List<DmConversation>.upsertDm(dm: DmConversation): List<DmConversation> {
    val existingIndex = indexOfFirst { it.id == dm.id }
    return if (existingIndex < 0) {
        this + dm
    } else {
        toMutableList().apply { this[existingIndex] = dm }
    }
}

private fun UserProfile.withOnlineStatus(userId: String, online: Boolean): UserProfile =
    if (id == userId) copy(isOnline = online) else this

private fun UserProfile.withProfileEvent(event: ProfileEvent): UserProfile =
    if (id == event.userId) {
        copy(
            username = event.username,
            avatarUrl = event.avatarUrl,
            profileBannerUrl = event.profileBannerUrl,
            status = event.status,
            profileColor = event.profileColor,
            bio = event.bio
        )
    } else {
        this
    }
