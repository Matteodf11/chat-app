package com.matteo.chatapp.viewmodel

import com.matteo.chatapp.model.Channel
import com.matteo.chatapp.model.Server
import com.matteo.chatapp.model.ServerInvitation
import com.matteo.chatapp.model.ServerMember

data class ServerUiState(
    val servers: List<Server> = emptyList(),
    val channels: List<Channel> = emptyList(),
    val searchChannels: List<Channel> = emptyList(),
    val members: List<ServerMember> = emptyList(),
    val pendingInvitations: List<ServerInvitation> = emptyList(),
    val selectedServer: Server? = null,
    val selectedChannel: Channel? = null,
    val isLoading: Boolean = false,
    val isLoadingChannels: Boolean = false,
    val isLoadingMembers: Boolean = false,
    val isLoadingInvitations: Boolean = false,
    val channelsError: String? = null,
    val serversError: String? = null,
    val action: String? = null,
    val notice: String? = null,
    val error: String? = null
)

internal fun List<Server>.upsertServer(server: Server): List<Server> {
    val existingIndex = indexOfFirst { it.id == server.id }
    return if (existingIndex < 0) {
        this + server
    } else {
        toMutableList().apply { this[existingIndex] = server }
    }
}

internal fun List<ServerMember>.replaceAndSortMember(updated: ServerMember): List<ServerMember> =
    map { if (it.userId == updated.userId) updated else it }
        .sortedWith(
            compareBy<ServerMember> { memberRoleOrder(it.role) }
                .thenBy { it.username.lowercase() }
        )

private fun memberRoleOrder(role: String): Int = when (role) {
    "owner" -> 0
    "admin" -> 1
    else -> 2
}
