package com.matteo.chatapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matteo.chatapp.model.Channel
import com.matteo.chatapp.model.CreateChannelRequest
import com.matteo.chatapp.model.CreateServerRequest
import com.matteo.chatapp.model.Server
import com.matteo.chatapp.model.ServerInvitation
import com.matteo.chatapp.model.ServerMember
import com.matteo.chatapp.model.UpdateServerRequest
import com.matteo.chatapp.network.ServerApi
import com.matteo.chatapp.network.createHttpClient
import com.matteo.chatapp.network.WebSocketClient
import com.matteo.chatapp.platform.PickedAvatarFile
import com.matteo.chatapp.repository.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ServerViewModel : ViewModel() {

    private val client = createHttpClient()
    private val api = ServerApi(client)
    private val serverWsClient = WebSocketClient(client)
    private var serverEventsJob: Job? = null
    private var serverEventsCollectJob: Job? = null

    private val _servers = MutableStateFlow<List<Server>>(emptyList())
    val servers: StateFlow<List<Server>> = _servers

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels

    private val _searchChannels = MutableStateFlow<List<Channel>>(emptyList())
    val searchChannels: StateFlow<List<Channel>> = _searchChannels

    private val _members = MutableStateFlow<List<ServerMember>>(emptyList())
    val members: StateFlow<List<ServerMember>> = _members

    private val _pendingInvitations = MutableStateFlow<List<ServerInvitation>>(emptyList())
    val pendingInvitations: StateFlow<List<ServerInvitation>> = _pendingInvitations

    private val _selectedServer = MutableStateFlow<Server?>(null)
    val selectedServer: StateFlow<Server?> = _selectedServer

    private val _selectedChannel = MutableStateFlow<Channel?>(null)
    val selectedChannel: StateFlow<Channel?> = _selectedChannel

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _accessRevoked = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val accessRevoked: SharedFlow<String> = _accessRevoked

    init {
        loadServers()
        loadPendingInvitations()
        connectServerEvents()
    }

    fun loadServers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = SessionManager.getToken() ?: return@launch
                _servers.value = api.getMyServers(token)
            } catch (e: Exception) {
                // error silencioso por ahora
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectServer(server: Server) {
        _selectedServer.value = server
        _selectedChannel.value = null
        loadChannels(server.id)
        loadMembers(server.id)
    }

    fun selectChannel(channel: Channel) {
        _selectedChannel.value = channel
    }

    fun clearSelectedChannel() {
        _selectedChannel.value = null
    }

    private fun loadChannels(serverId: String) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken() ?: return@launch
                _channels.value = api.getChannels(serverId, token)
            } catch (e: Exception) {
                // error silencioso
            }
        }
    }

    fun loadMembers(serverId: String? = _selectedServer.value?.id) {
        val targetServerId = serverId ?: return
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken() ?: return@launch
                _members.value = api.getMembers(targetServerId, token)
            } catch (e: Exception) {
                _error.value = "No se pudieron cargar los miembros"
            }
        }
    }

    fun loadPendingInvitations() {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken() ?: return@launch
                _pendingInvitations.value = api.getPendingInvitations(token)
            } catch (e: Exception) {
                _error.value = "No se pudieron cargar las invitaciones"
            }
        }
    }

    fun loadAllChannelsForSearch() {
        viewModelScope.launch {
            val token = SessionManager.getToken() ?: return@launch
            val collected = mutableListOf<Channel>()
            _servers.value.forEach { server ->
                runCatching { api.getChannels(server.id, token) }
                    .getOrNull()
                    ?.let(collected::addAll)
            }
            _searchChannels.value = collected.distinctBy { it.id }
        }
    }

    fun createServer(name: String, description: String, iconFile: PickedAvatarFile?, onCreated: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken() ?: return@launch
                var server = api.createServer(
                    CreateServerRequest(
                        name = name.trim(),
                        description = description.trim().takeIf { it.isNotBlank() },
                        iconUrl = null
                    ),
                    token
                )
                if (iconFile != null) {
                    server = api.uploadServerIcon(server.id, iconFile.contentType, iconFile.bytes, token)
                }
                _servers.value = _servers.value + server
                selectServer(server)
                _error.value = null
                onCreated()
            } catch (e: Exception) {
                _error.value = "No se pudo crear el servidor"
            }
        }
    }

    fun updateSelectedServer(name: String, description: String, iconFile: PickedAvatarFile?) {
        val server = _selectedServer.value ?: return
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken() ?: return@launch
                var updated = api.updateServer(
                    server.id,
                    UpdateServerRequest(
                        name = name.trim().takeIf { it.isNotBlank() },
                        description = description.trim(),
                        iconUrl = null
                    ),
                    token
                )
                if (iconFile != null) {
                    updated = api.uploadServerIcon(server.id, iconFile.contentType, iconFile.bytes, token)
                }
                _selectedServer.value = updated
                _servers.value = _servers.value.map { if (it.id == updated.id) updated else it }
                _error.value = null
            } catch (e: Exception) {
                _error.value = "No se pudo actualizar el servidor"
            }
        }
    }

    fun inviteMember(username: String) {
        val server = _selectedServer.value ?: return
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken() ?: return@launch
                api.inviteMember(server.id, username.trim(), token)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "No se pudo enviar la invitacion"
            }
        }
    }

    fun updateMemberRole(userId: String, role: String) {
        val server = _selectedServer.value ?: return
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken() ?: return@launch
                val updated = api.updateMemberRole(server.id, userId, role, token)
                _members.value = _members.value.map { if (it.userId == userId) updated else it }
                    .sortedWith(compareBy<ServerMember> { roleOrder(it.role) }.thenBy { it.username.lowercase() })
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "No se pudo cambiar el rol"
            }
        }
    }

    fun removeMember(userId: String) {
        val server = _selectedServer.value ?: return
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken() ?: return@launch
                api.removeMember(server.id, userId, token)
                _members.value = _members.value.filterNot { it.userId == userId }
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "No se pudo expulsar al miembro"
            }
        }
    }

    fun acceptInvitation(invitationId: String, onAccepted: (Server) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken() ?: return@launch
                val server = api.acceptInvitation(invitationId, token)
                _pendingInvitations.value = _pendingInvitations.value.filterNot { it.id == invitationId }
                _servers.value = (_servers.value.filterNot { it.id == server.id } + server)
                selectServer(server)
                _error.value = null
                onAccepted(server)
            } catch (e: Exception) {
                _error.value = "No se pudo aceptar la invitacion"
            }
        }
    }

    fun rejectInvitation(invitationId: String) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken() ?: return@launch
                api.rejectInvitation(invitationId, token)
                _pendingInvitations.value = _pendingInvitations.value.filterNot { it.id == invitationId }
                _error.value = null
            } catch (e: Exception) {
                _error.value = "No se pudo rechazar la invitacion"
            }
        }
    }

    fun leaveSelectedServer(onLeft: () -> Unit = {}) {
        val server = _selectedServer.value ?: return
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken() ?: return@launch
                api.leaveServer(server.id, token)
                _servers.value = _servers.value.filterNot { it.id == server.id }
                clearSelection()
                _error.value = null
                onLeft()
            } catch (e: Exception) {
                _error.value = "No se pudo salir del servidor"
            }
        }
    }

    fun deleteSelectedServer(onDeleted: () -> Unit = {}) {
        val server = _selectedServer.value ?: return
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken() ?: return@launch
                api.deleteServer(server.id, token)
                _servers.value = _servers.value.filterNot { it.id == server.id }
                clearSelection()
                _error.value = null
                onDeleted()
            } catch (e: Exception) {
                _error.value = "No se pudo borrar el servidor"
            }
        }
    }

    fun createChannel(name: String) {
        val server = _selectedServer.value ?: return
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken() ?: return@launch
                val channel = api.createChannel(server.id, CreateChannelRequest(name.trim()), token)
                _channels.value = _channels.value + channel
                _error.value = null
            } catch (e: Exception) {
                _error.value = "No se pudo crear el canal"
            }
        }
    }

    fun clearSelection() {
        _selectedServer.value = null
        _selectedChannel.value = null
        _channels.value = emptyList()
        _members.value = emptyList()
    }

    private fun roleOrder(role: String): Int = when (role) {
        "owner" -> 0
        "admin" -> 1
        else -> 2
    }

    private fun connectServerEvents() {
        serverEventsCollectJob = viewModelScope.launch {
            serverWsClient.serverEvents.collect { event ->
                when (event.type) {
                    "ROLE_UPDATED" -> {
                        val role = event.role ?: return@collect
                        _servers.value = _servers.value.map { server ->
                            if (server.id == event.serverId) server.copy(myRole = role) else server
                        }
                        _selectedServer.value = _selectedServer.value?.let { server ->
                            if (server.id == event.serverId) server.copy(myRole = role) else server
                        }
                    }
                    "MEMBER_REMOVED", "SERVER_DELETED" -> {
                        _servers.value = _servers.value.filterNot { it.id == event.serverId }
                        if (_selectedServer.value?.id == event.serverId) clearSelection()
                        _accessRevoked.emit(event.serverId)
                    }
                }
            }
        }
        serverEventsJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val token = SessionManager.getToken() ?: break
                    serverWsClient.connectServerEvents(token)
                } catch (_: Exception) {}
                if (isActive) delay(1500)
            }
        }
    }

    override fun onCleared() {
        serverEventsJob?.cancel()
        serverEventsCollectJob?.cancel()
        serverWsClient.disconnect()
        super.onCleared()
    }
}
