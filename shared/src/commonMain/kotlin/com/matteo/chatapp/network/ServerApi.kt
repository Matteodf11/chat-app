package com.matteo.chatapp.network

import com.matteo.chatapp.model.Channel
import com.matteo.chatapp.model.CreateChannelRequest
import com.matteo.chatapp.model.CreateServerRequest
import com.matteo.chatapp.model.Server
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ServerApi(private val client: HttpClient) {

    suspend fun getMyServers(token: String): List<Server> {
        return client.get("$BASE_URL/api/servers/me") {
            bearerAuth(token)
        }.body()
    }

    suspend fun createServer(request: CreateServerRequest, token: String): Server {
        return client.post("$BASE_URL/api/servers") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getChannels(serverId: String, token: String): List<Channel> {
        return client.get("$BASE_URL/api/servers/$serverId/channels") {
            bearerAuth(token)
        }.body()
    }

    suspend fun createChannel(serverId: String, request: CreateChannelRequest, token: String): Channel {
        return client.post("$BASE_URL/api/servers/$serverId/channels") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}