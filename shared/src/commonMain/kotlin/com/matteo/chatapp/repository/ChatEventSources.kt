package com.matteo.chatapp.repository

import com.matteo.chatapp.model.Message
import com.matteo.chatapp.model.TypingEvent
import com.matteo.chatapp.network.WebSocketClient
import kotlinx.coroutines.flow.Flow

interface ConversationEventsSource {
    val messages: Flow<Message>
    val typingEvents: Flow<TypingEvent>

    suspend fun connect(token: String, target: ConversationTarget)

    suspend fun sendTyping(
        channelId: String?,
        dmId: String?,
        typing: Boolean
    )

    fun disconnect()
}

interface PersonalMessagesSource {
    val messages: Flow<Message>

    suspend fun connect(token: String)

    fun disconnect()
}

interface DmInboxEventsSource {
    val messages: Flow<Message>

    suspend fun connect(token: String, dmIds: List<String>)

    fun disconnect()
}

class WebSocketConversationEventsSource(
    private val client: WebSocketClient
) : ConversationEventsSource {
    override val messages: Flow<Message> = client.incomingMessages
    override val typingEvents: Flow<TypingEvent> = client.typingEvents

    override suspend fun connect(token: String, target: ConversationTarget) {
        when (target) {
            is ConversationTarget.Channel -> client.connect(token, target.id)
            is ConversationTarget.DirectMessage -> client.connectDmMessages(token, target.id)
        }
    }

    override suspend fun sendTyping(
        channelId: String?,
        dmId: String?,
        typing: Boolean
    ) {
        client.sendTyping(channelId = channelId, dmId = dmId, typing = typing)
    }

    override fun disconnect() {
        client.disconnect()
    }
}

class WebSocketPersonalMessagesSource(
    private val client: WebSocketClient
) : PersonalMessagesSource {
    override val messages: Flow<Message> = client.personalMessages

    override suspend fun connect(token: String) {
        client.connectPersonalMessages(token)
    }

    override fun disconnect() {
        client.disconnect()
    }
}

class WebSocketDmInboxEventsSource(
    private val client: WebSocketClient
) : DmInboxEventsSource {
    override val messages: Flow<Message> = client.personalMessages

    override suspend fun connect(token: String, dmIds: List<String>) {
        client.connectDmInbox(token, dmIds)
    }

    override fun disconnect() {
        client.disconnect()
    }
}
