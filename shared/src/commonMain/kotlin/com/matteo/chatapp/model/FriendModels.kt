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
    val createdAt: String? = null
)

@Serializable
data class DmConversation(
    val id: String,
    val otherUserId: String,
    val otherUsername: String,
    val otherAvatar: String? = null,
    val otherIsOnline: Boolean = false,
    val createdAt: String? = null
)