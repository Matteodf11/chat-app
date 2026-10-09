package com.matteo.chatapp.repository

import com.matteo.chatapp.model.FriendshipEvent
import com.matteo.chatapp.model.PresenceEvent
import com.matteo.chatapp.model.ProfileEvent
import com.matteo.chatapp.network.WebSocketClient
import kotlinx.coroutines.flow.Flow

interface FriendshipEventsSource {
    val events: Flow<FriendshipEvent>

    suspend fun connect(token: String, userId: String)

    fun disconnect()
}

interface PresenceEventsSource {
    val events: Flow<PresenceEvent>

    suspend fun connect(token: String)

    fun disconnect()
}

interface ProfileEventsSource {
    val events: Flow<ProfileEvent>

    suspend fun connect(token: String)

    fun disconnect()
}

class WebSocketFriendshipEventsSource(
    private val client: WebSocketClient
) : FriendshipEventsSource {
    override val events: Flow<FriendshipEvent> = client.friendshipEvents

    override suspend fun connect(token: String, userId: String) {
        client.connectFriendEvents(token, userId)
    }

    override fun disconnect() {
        client.disconnect()
    }
}

class WebSocketPresenceEventsSource(
    private val client: WebSocketClient
) : PresenceEventsSource {
    override val events: Flow<PresenceEvent> = client.presenceEvents

    override suspend fun connect(token: String) {
        client.connectPresenceEvents(token)
    }

    override fun disconnect() {
        client.disconnect()
    }
}

class WebSocketProfileEventsSource(
    private val client: WebSocketClient
) : ProfileEventsSource {
    override val events: Flow<ProfileEvent> = client.profileEvents

    override suspend fun connect(token: String) {
        client.connectProfileEvents(token)
    }

    override fun disconnect() {
        client.disconnect()
    }
}
