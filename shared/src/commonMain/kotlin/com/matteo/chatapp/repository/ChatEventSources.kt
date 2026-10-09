package com.matteo.chatapp.repository

import com.matteo.chatapp.model.Message
import com.matteo.chatapp.model.TypingEvent
import com.matteo.chatapp.network.WebSocketClient
import kotlinx.coroutines.flow.Flow

interface ConversationEventsSource {
    val readEvents: Flow<com.matteo.chatapp.model.DmReadEvent> get() = kotlinx.coroutines.flow.emptyFlow()
    val connected: Flow<Boolean> get() = kotlinx.coroutines.flow.flowOf(false)
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
    val readEvents: Flow<com.matteo.chatapp.model.DmReadEvent> get() = kotlinx.coroutines.flow.emptyFlow()
    val settings: Flow<com.matteo.chatapp.model.DmConversation> get() = kotlinx.coroutines.flow.emptyFlow()
    val connected: Flow<Boolean> get() = kotlinx.coroutines.flow.flowOf(false)
    val messages: Flow<Message>

    suspend fun connect(token: String)

    fun disconnect()
}

interface DmInboxEventsSource {
    val readEvents: Flow<com.matteo.chatapp.model.DmReadEvent> get() = kotlinx.coroutines.flow.emptyFlow()
    val messages: Flow<Message>

    suspend fun connect(token: String, dmIds: List<String>)

    fun disconnect()
}

class WebSocketConversationEventsSource(
    private val client: WebSocketClient
) : ConversationEventsSource {
    override val readEvents = client.readEvents
    override val connected: Flow<Boolean> = client.connected
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
    override val readEvents = client.readEvents
    override val settings = client.dmSettings
    override val connected: Flow<Boolean> = client.connected
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
    override val readEvents = client.readEvents
    override val messages: Flow<Message> = client.personalMessages

    override suspend fun connect(token: String, dmIds: List<String>) {
        client.connectDmInbox(token, dmIds)
    }

    override fun disconnect() {
        client.disconnect()
    }
}
