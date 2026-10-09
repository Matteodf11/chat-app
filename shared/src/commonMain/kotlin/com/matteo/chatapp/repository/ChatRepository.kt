package com.matteo.chatapp.repository

import com.matteo.chatapp.model.Message
import com.matteo.chatapp.model.MessagePage
import com.matteo.chatapp.network.FriendApi
import com.matteo.chatapp.network.MessageApi
import com.matteo.chatapp.platform.PickedAttachmentFile

sealed interface ConversationTarget {
    val id: String

    data class Channel(override val id: String) : ConversationTarget

    data class DirectMessage(override val id: String) : ConversationTarget
}

data class MessageSearchQuery(
    val text: String,
    val sender: String,
    val from: String,
    val to: String,
    val attachmentsOnly: Boolean
)

interface ChatRepository {
    suspend fun loadMessages(
        target: ConversationTarget,
        before: String? = null
    ): MessagePage

    suspend fun sendMessage(
        target: ConversationTarget,
        content: String,
        replyToId: String?
    ): Message

    suspend fun sendAttachment(
        target: ConversationTarget,
        file: PickedAttachmentFile,
        content: String,
        replyToId: String?
    ): Message

    suspend fun searchMessages(
        target: ConversationTarget,
        query: MessageSearchQuery
    ): List<Message>

    suspend fun getMessageContext(messageId: String): MessagePage

    suspend fun editMessage(messageId: String, content: String): Message

    suspend fun deleteMessage(messageId: String)

    suspend fun markDmRead(dmId: String)
}

class NetworkChatRepository(
    private val messageApi: MessageApi,
    private val friendApi: FriendApi,
    private val accessTokenProvider: AccessTokenProvider
) : ChatRepository {
    override suspend fun loadMessages(
        target: ConversationTarget,
        before: String?
    ): MessagePage {
        val token = accessTokenProvider.requireToken()
        return when (target) {
            is ConversationTarget.Channel ->
                messageApi.getChannelMessages(target.id, token, before)
            is ConversationTarget.DirectMessage ->
                messageApi.getDmMessages(target.id, token, before)
        }
    }

    override suspend fun sendMessage(
        target: ConversationTarget,
        content: String,
        replyToId: String?
    ): Message {
        val token = accessTokenProvider.requireToken()
        return when (target) {
            is ConversationTarget.Channel ->
                messageApi.sendMessage(target.id, content, replyToId, token)
            is ConversationTarget.DirectMessage ->
                messageApi.sendDmMessage(target.id, content, replyToId, token)
        }
    }

    override suspend fun sendAttachment(
        target: ConversationTarget,
        file: PickedAttachmentFile,
        content: String,
        replyToId: String?
    ): Message {
        val token = accessTokenProvider.requireToken()
        return when (target) {
            is ConversationTarget.Channel ->
                messageApi.sendChannelAttachment(target.id, file, content, replyToId, token)
            is ConversationTarget.DirectMessage ->
                messageApi.sendDmAttachment(target.id, file, content, replyToId, token)
        }
    }

    override suspend fun searchMessages(
        target: ConversationTarget,
        query: MessageSearchQuery
    ): List<Message> {
        val token = accessTokenProvider.requireToken()
        return when (target) {
            is ConversationTarget.Channel -> messageApi.searchChannelMessages(
                target.id,
                query.text,
                query.sender,
                query.from,
                query.to,
                query.attachmentsOnly,
                token
            )
            is ConversationTarget.DirectMessage -> messageApi.searchDmMessages(
                target.id,
                query.text,
                query.sender,
                query.from,
                query.to,
                query.attachmentsOnly,
                token
            )
        }
    }

    override suspend fun getMessageContext(messageId: String): MessagePage =
        messageApi.getMessageContext(messageId, accessTokenProvider.requireToken())

    override suspend fun editMessage(messageId: String, content: String): Message =
        messageApi.editMessage(messageId, content, accessTokenProvider.requireToken())

    override suspend fun deleteMessage(messageId: String) {
        messageApi.deleteMessage(messageId, accessTokenProvider.requireToken())
    }

    override suspend fun markDmRead(dmId: String) {
        friendApi.markDmRead(dmId, accessTokenProvider.requireToken())
    }
}
