package com.matteo.chatapp.repository

import com.matteo.chatapp.model.ServerEvent
import com.matteo.chatapp.network.WebSocketClient
import kotlinx.coroutines.flow.Flow

interface ServerEventsSource {
    val events: Flow<ServerEvent>

    suspend fun connect(token: String)

    fun disconnect()
}

class WebSocketServerEventsSource(
    private val client: WebSocketClient
) : ServerEventsSource {
    override val events: Flow<ServerEvent> = client.serverEvents

    override suspend fun connect(token: String) {
        client.connectServerEvents(token)
    }

    override fun disconnect() {
        client.disconnect()
    }
}
