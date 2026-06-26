package com.matteo.chatapp.network

import com.matteo.chatapp.model.Message
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json

class WebSocketClient(private val client: HttpClient) {

    private val _incomingMessages = MutableSharedFlow<Message>()
    val incomingMessages: SharedFlow<Message> = _incomingMessages

    private var session: WebSocketSession? = null

    suspend fun connect(token: String, channelId: String) {
        val wsUrl = BASE_URL
            .replace("https://", "wss://")
            .replace("http://", "ws://")

        client.webSocket(
            urlString = "$wsUrl/ws/websocket?token=$token"
        ) {
            session = this
            subscribe(channelId)
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    try {
                        val message = Json.decodeFromString<Message>(frame.readText())
                        _incomingMessages.emit(message)
                    } catch (e: Exception) {
                        // ignorar frames no parseables
                    }
                }
            }
        }
    }

    private suspend fun subscribe(channelId: String) {
        session?.send(
            Frame.Text("""SUBSCRIBE\ndestination:/topic/channel.$channelId\n\n\u0000""")
        )
    }

    suspend fun sendMessage(content: String, channelId: String) {
        session?.send(
            Frame.Text("""SEND\ndestination:/app/chat.sendMessage\n\n{"content":"$content","channelId":"$channelId"}\u0000""")
        )
    }

    fun disconnect() {
        session = null
    }
}