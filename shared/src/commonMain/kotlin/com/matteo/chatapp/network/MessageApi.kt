package com.matteo.chatapp.network

import com.matteo.chatapp.model.Message
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class MessageApi(private val client: HttpClient) {

    suspend fun getChannelMessages(channelId: String, token: String): List<Message> {
        return client.get("$BASE_URL/api/messages/channel/$channelId") {
            bearerAuth(token)
        }.body()
    }

    suspend fun sendMessage(channelId: String, content: String, token: String): Message {
        return client.post("$BASE_URL/api/messages/channel/$channelId") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(mapOf("content" to content, "channelId" to channelId))
        }.body()
    }

    suspend fun getDmMessages(dmId: String, token: String): List<Message> {
        return client.get("$BASE_URL/api/messages/dm/$dmId") {
            bearerAuth(token)
        }.body()
    }

    suspend fun sendDmMessage(dmId: String, content: String, token: String): Message {
        return client.post("$BASE_URL/api/messages/dm/$dmId") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(mapOf("content" to content, "dmId" to dmId))
        }.body()
    }
}