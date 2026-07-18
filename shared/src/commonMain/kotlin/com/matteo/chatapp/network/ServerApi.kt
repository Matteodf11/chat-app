package com.matteo.chatapp.network

import com.matteo.chatapp.model.Channel
import com.matteo.chatapp.model.CreateChannelRequest
import com.matteo.chatapp.model.CreateServerRequest
import com.matteo.chatapp.model.InviteServerMemberRequest
import com.matteo.chatapp.model.Server
import com.matteo.chatapp.model.ServerInvitation
import com.matteo.chatapp.model.ServerMember
import com.matteo.chatapp.model.UpdateServerRequest
import com.matteo.chatapp.model.UpdateServerMemberRoleRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

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

    suspend fun getMembers(serverId: String, token: String): List<ServerMember> {
        return client.get("$BASE_URL/api/servers/$serverId/members") {
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

    suspend fun updateServer(serverId: String, request: UpdateServerRequest, token: String): Server {
        return client.put("$BASE_URL/api/servers/$serverId") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun uploadServerIcon(serverId: String, contentType: String, bytes: ByteArray, token: String): Server {
        if (bytes.size > MAX_IMAGE_BYTES) {
            throw RuntimeException("La imagen no puede superar 2 MB")
        }
        val response = client.post("$BASE_URL/api/servers/$serverId/icon") {
            bearerAuth(token)
            contentType(ContentType.parse(contentType))
            header(HttpHeaders.ContentDisposition, "attachment; filename=\"server-icon\"")
            setBody(bytes)
        }
        if (!response.status.isSuccess()) {
            throw RuntimeException(response.bodyAsText())
        }
        return response.body()
    }

    suspend fun inviteMember(serverId: String, username: String, token: String): ServerInvitation {
        return client.post("$BASE_URL/api/servers/$serverId/members") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(InviteServerMemberRequest(username))
        }.body()
    }

    suspend fun updateMemberRole(serverId: String, userId: String, role: String, token: String): ServerMember {
        return client.put("$BASE_URL/api/servers/$serverId/members/$userId/role") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(UpdateServerMemberRoleRequest(role))
        }.body()
    }

    suspend fun removeMember(serverId: String, userId: String, token: String) {
        requireSuccess(client.delete("$BASE_URL/api/servers/$serverId/members/$userId") {
            bearerAuth(token)
        })
    }

    suspend fun getPendingInvitations(token: String): List<ServerInvitation> {
        return client.get("$BASE_URL/api/servers/invitations/pending") {
            bearerAuth(token)
        }.body()
    }

    suspend fun acceptInvitation(invitationId: String, token: String): Server {
        return client.post("$BASE_URL/api/servers/invitations/$invitationId/accept") {
            bearerAuth(token)
        }.body()
    }

    suspend fun rejectInvitation(invitationId: String, token: String) {
        requireSuccess(client.post("$BASE_URL/api/servers/invitations/$invitationId/reject") {
            bearerAuth(token)
        })
    }

    suspend fun leaveServer(serverId: String, token: String) {
        requireSuccess(client.delete("$BASE_URL/api/servers/$serverId/leave") {
            bearerAuth(token)
        })
    }

    suspend fun deleteServer(serverId: String, token: String) {
        requireSuccess(client.delete("$BASE_URL/api/servers/$serverId") {
            bearerAuth(token)
        })
    }

    private suspend fun requireSuccess(response: HttpResponse) {
        if (response.status.isSuccess()) return
        val raw = response.bodyAsText()
        val message = runCatching {
            Json.parseToJsonElement(raw).jsonObject["error"]?.jsonPrimitive?.contentOrNull
        }.getOrNull()
        throw RuntimeException(message ?: "Error del servidor (${response.status.value})")
    }

    private companion object {
        private const val MAX_IMAGE_BYTES = 2 * 1024 * 1024
    }
}
