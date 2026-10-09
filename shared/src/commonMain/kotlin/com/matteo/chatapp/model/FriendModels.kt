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
    val createdAt: String? = null,
    val unreadCount: Int = 0,
    val lastMessagePreview: String? = null,
    val lastMessageAt: String? = null,
    val lastActivityAt: String? = null,
    val pinned: Boolean = false,
    val muted: Boolean = false,
    val archived: Boolean = false,
    val canMessage: Boolean = true
)

@Serializable
data class DmPreferencesRequest(val pinned: Boolean? = null, val muted: Boolean? = null, val archived: Boolean? = null)

@Serializable
data class DmReadEvent(val type: String = "dm_read", val dmId: String, val readerId: String, val readAt: String)

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

@Serializable
data class BlockedUser(val id: String, val username: String, val avatarUrl: String? = null)
