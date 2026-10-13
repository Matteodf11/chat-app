package com.matteo.chatapp.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    val content: String,
    val senderId: String? = null,
    val senderUsername: String,
    val senderAvatar: String? = null,
    val channelId: String? = null,
    val dmId: String? = null,
    val edited: Boolean = false,
    val deleted: Boolean = false,
    val replyToId: String? = null,
    val replyToSenderUsername: String? = null,
    val replyToContent: String? = null,
    val attachmentName: String? = null,
    val attachmentContentType: String? = null,
    val attachmentSize: Long? = null,
    val attachmentUrl: String? = null,
    val createdAt: String? = null,
    val clientMessageId: String? = null,
    val readAt: String? = null,
    val mentionedUserIds: List<String> = emptyList()
)

@Serializable
data class MessagePage(
    val messages: List<Message>,
    val hasMore: Boolean
)

@Serializable
data class SharedItemsPage(val messages: List<Message>, val hasMore: Boolean, val nextBefore: String? = null)

@Serializable
data class TypingEvent(
    val senderId: String,
    val senderUsername: String,
    val channelId: String? = null,
    val dmId: String? = null,
    val typing: Boolean = false
)
