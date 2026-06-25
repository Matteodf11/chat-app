package com.matteo.chatapp.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    val content: String,
    val senderUsername: String,
    val senderAvatar: String? = null,
    val channelId: String? = null,
    val dmId: String? = null,
    val edited: Boolean = false,
    val createdAt: String? = null
)