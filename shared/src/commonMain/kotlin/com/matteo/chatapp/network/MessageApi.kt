package com.matteo.chatapp.network

import com.matteo.chatapp.model.Message
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get

class MessageApi(private val client: HttpClient) {

    suspend fun getChannelMessages(channelId: String, token: String): List<Message> {
        return client.get("$BASE_URL/api/messages/channel/$channelId") {
            bearerAuth(token)
        }.body()
    }
}