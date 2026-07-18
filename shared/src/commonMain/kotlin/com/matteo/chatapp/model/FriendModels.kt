package com.matteo.chatapp.model

import kotlinx.serialization.Serializable

@Serializable
data class FriendRequest(
    val id: String,
    val friendId: String,
    val friendUsername: String,
    val friendAvatar: String? = null,
    val friendIsOnline: Boolean = false,
    val status: String,
    val isIncoming: Boolean = false,
    val createdAt: String? = null
)

@Serializable
data class DmConversation(
    val id: String,
    val otherUserId: String,
    val otherUsername: String,
    val otherAvatar: String? = null,
    val otherIsOnline: Boolean = false,
    val hasUnread: Boolean = false,
    val createdAt: String? = null
)

@Serializable
data class FriendshipEvent(
    val type: String,
    val friendship: FriendRequest
)

@Serializable
data class PresenceEvent(
    val userId: String,
    val username: String,
    val online: Boolean
)
