package com.matteo.chatapp.repository

import com.matteo.chatapp.model.Channel
import com.matteo.chatapp.model.CreateChannelRequest
import com.matteo.chatapp.model.CreateServerRequest
import com.matteo.chatapp.model.Server
import com.matteo.chatapp.model.ServerInvitation
import com.matteo.chatapp.model.ServerMember
import com.matteo.chatapp.model.UpdateServerRequest
import com.matteo.chatapp.network.ServerApi
import com.matteo.chatapp.platform.PickedAvatarFile
import kotlinx.coroutines.CancellationException

interface ServerRepository {
    suspend fun getServers(): List<Server>

    suspend fun getChannels(serverId: String): List<Channel>

    suspend fun getMembers(serverId: String): List<ServerMember>

    suspend fun getPendingInvitations(): List<ServerInvitation>

    suspend fun getAllChannels(serverIds: List<String>): List<Channel>

    suspend fun createServer(
        name: String,
        description: String,
        iconFile: PickedAvatarFile?
    ): Server

    suspend fun updateServer(
        serverId: String,
        name: String,
        description: String,
        iconFile: PickedAvatarFile?
    ): Server

    suspend fun inviteMember(serverId: String, username: String)

    suspend fun updateMemberRole(serverId: String, userId: String, role: String): ServerMember

    suspend fun removeMember(serverId: String, userId: String)

    suspend fun acceptInvitation(invitationId: String): Server

    suspend fun rejectInvitation(invitationId: String)

    suspend fun leaveServer(serverId: String)

    suspend fun deleteServer(serverId: String)

    suspend fun createChannel(serverId: String, name: String): Channel
}

class NetworkServerRepository(
    private val serverApi: ServerApi,
    private val accessTokenProvider: AccessTokenProvider
) : ServerRepository {
    override suspend fun getServers(): List<Server> =
        serverApi.getMyServers(accessTokenProvider.requireToken())

    override suspend fun getChannels(serverId: String): List<Channel> =
        serverApi.getChannels(serverId, accessTokenProvider.requireToken())

    override suspend fun getMembers(serverId: String): List<ServerMember> =
        serverApi.getMembers(serverId, accessTokenProvider.requireToken())

    override suspend fun getPendingInvitations(): List<ServerInvitation> =
        serverApi.getPendingInvitations(accessTokenProvider.requireToken())

    override suspend fun getAllChannels(serverIds: List<String>): List<Channel> {
        val token = accessTokenProvider.requireToken()
        return buildList {
            serverIds.distinct().forEach { serverId ->
                try {
                    addAll(serverApi.getChannels(serverId, token))
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Un servidor inaccesible no debe impedir buscar en los demás.
                }
            }
        }.distinctBy(Channel::id)
    }

    override suspend fun createServer(
        name: String,
        description: String,
        iconFile: PickedAvatarFile?
    ): Server {
        val token = accessTokenProvider.requireToken()
        var server = serverApi.createServer(
            CreateServerRequest(
                name = name.trim(),
                description = description.trim().takeIf(String::isNotBlank),
                iconUrl = null
            ),
            token
        )
        if (iconFile != null) {
            server = serverApi.uploadServerIcon(
                serverId = server.id,
                contentType = iconFile.contentType,
                bytes = iconFile.bytes,
                token = token
            )
        }
        return server
    }

    override suspend fun updateServer(
        serverId: String,
        name: String,
        description: String,
        iconFile: PickedAvatarFile?
    ): Server {
        val token = accessTokenProvider.requireToken()
        var server = serverApi.updateServer(
            serverId = serverId,
            request = UpdateServerRequest(
                name = name.trim().takeIf(String::isNotBlank),
                description = description.trim(),
                iconUrl = null
            ),
            token = token
        )
        if (iconFile != null) {
            server = serverApi.uploadServerIcon(
                serverId = serverId,
                contentType = iconFile.contentType,
                bytes = iconFile.bytes,
                token = token
            )
        }
        return server
    }

    override suspend fun inviteMember(serverId: String, username: String) {
        serverApi.inviteMember(serverId, username.trim(), accessTokenProvider.requireToken())
    }

    override suspend fun updateMemberRole(
        serverId: String,
        userId: String,
        role: String
    ): ServerMember =
        serverApi.updateMemberRole(
            serverId,
            userId,
            role,
            accessTokenProvider.requireToken()
        )

    override suspend fun removeMember(serverId: String, userId: String) {
        serverApi.removeMember(serverId, userId, accessTokenProvider.requireToken())
    }

    override suspend fun acceptInvitation(invitationId: String): Server =
        serverApi.acceptInvitation(invitationId, accessTokenProvider.requireToken())

    override suspend fun rejectInvitation(invitationId: String) {
        serverApi.rejectInvitation(invitationId, accessTokenProvider.requireToken())
    }

    override suspend fun leaveServer(serverId: String) {
        serverApi.leaveServer(serverId, accessTokenProvider.requireToken())
    }

    override suspend fun deleteServer(serverId: String) {
        serverApi.deleteServer(serverId, accessTokenProvider.requireToken())
    }

    override suspend fun createChannel(serverId: String, name: String): Channel =
        serverApi.createChannel(
            serverId,
            CreateChannelRequest(name.trim()),
            accessTokenProvider.requireToken()
        )
}
