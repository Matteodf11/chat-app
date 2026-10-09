package com.matteo.chatapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matteo.chatapp.model.Channel
import com.matteo.chatapp.model.Server
import com.matteo.chatapp.platform.PickedAvatarFile
import com.matteo.chatapp.repository.AccessTokenProvider
import com.matteo.chatapp.repository.ServerEventsSource
import com.matteo.chatapp.repository.ServerRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ServerViewModel(
    private val serverRepository: ServerRepository,
    private val accessTokenProvider: AccessTokenProvider,
    private val serverEventsSource: ServerEventsSource
) : ViewModel() {
    private var serverEventsJob: Job? = null
    private var serverEventsCollectJob: Job? = null
    private var channelsLoadJob: Job? = null
    private var membersLoadJob: Job? = null

    private val _uiState = MutableStateFlow(ServerUiState())
    val uiState: StateFlow<ServerUiState> = _uiState.asStateFlow()

    private val _accessRevoked = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val accessRevoked: SharedFlow<String> = _accessRevoked

    init {
        loadServers()
        loadPendingInvitations()
        connectServerEvents()
    }

    fun loadServers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val servers = serverRepository.getServers()
                _uiState.update { it.copy(servers = servers) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // error silencioso por ahora
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectServer(server: Server) {
        channelsLoadJob?.cancel()
        membersLoadJob?.cancel()
        _uiState.update {
            it.copy(
                selectedServer = server,
                selectedChannel = null,
                channels = emptyList(),
                members = emptyList()
            )
        }
        loadChannels(server.id)
        loadMembers(server.id)
    }

    fun selectChannel(channel: Channel) {
        _uiState.update { it.copy(selectedChannel = channel) }
    }

    fun clearSelectedChannel() {
        _uiState.update { it.copy(selectedChannel = null) }
    }

    private fun loadChannels(serverId: String) {
        channelsLoadJob?.cancel()
        channelsLoadJob = viewModelScope.launch {
            try {
                val channels = serverRepository.getChannels(serverId)
                if (_uiState.value.selectedServer?.id == serverId) {
                    _uiState.update { it.copy(channels = channels) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // error silencioso
            }
        }
    }

    fun loadMembers(serverId: String? = _uiState.value.selectedServer?.id) {
        val targetServerId = serverId ?: return
        membersLoadJob?.cancel()
        membersLoadJob = viewModelScope.launch {
            try {
                val members = serverRepository.getMembers(targetServerId)
                if (_uiState.value.selectedServer?.id == targetServerId) {
                    _uiState.update { it.copy(members = members) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_uiState.value.selectedServer?.id == targetServerId) {
                    _uiState.update { it.copy(error = "No se pudieron cargar los miembros") }
                }
            }
        }
    }

    fun loadPendingInvitations() {
        viewModelScope.launch {
            try {
                val invitations = serverRepository.getPendingInvitations()
                _uiState.update { it.copy(pendingInvitations = invitations) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudieron cargar las invitaciones") }
            }
        }
    }

    fun loadAllChannelsForSearch() {
        viewModelScope.launch {
            try {
                val channels = serverRepository.getAllChannels(
                    _uiState.value.servers.map(Server::id)
                )
                _uiState.update { it.copy(searchChannels = channels) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // La búsqueda conserva los resultados anteriores si la sesión no está disponible.
            }
        }
    }

    fun createServer(name: String, description: String, iconFile: PickedAvatarFile?, onCreated: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val server = serverRepository.createServer(name, description, iconFile)
                _uiState.update { it.copy(servers = it.servers + server, error = null) }
                selectServer(server)
                onCreated()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo crear el servidor") }
            }
        }
    }

    fun updateSelectedServer(name: String, description: String, iconFile: PickedAvatarFile?) {
        val server = _uiState.value.selectedServer ?: return
        viewModelScope.launch {
            try {
                val updated = serverRepository.updateServer(
                    server.id,
                    name,
                    description,
                    iconFile
                )
                _uiState.update {
                    if (it.selectedServer?.id != server.id) {
                        it
                    } else {
                        it.copy(
                            selectedServer = updated,
                            servers = it.servers.upsertServer(updated),
                            error = null
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo actualizar el servidor") }
            }
        }
    }

    fun inviteMember(username: String) {
        val server = _uiState.value.selectedServer ?: return
        viewModelScope.launch {
            try {
                serverRepository.inviteMember(server.id, username)
                if (_uiState.value.selectedServer?.id == server.id) {
                    _uiState.update { it.copy(error = null) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_uiState.value.selectedServer?.id == server.id) {
                    _uiState.update { it.copy(error = "No se pudo enviar la invitacion") }
                }
            }
        }
    }

    fun updateMemberRole(userId: String, role: String) {
        val server = _uiState.value.selectedServer ?: return
        viewModelScope.launch {
            try {
                val updated = serverRepository.updateMemberRole(server.id, userId, role)
                if (_uiState.value.selectedServer?.id == server.id) {
                    _uiState.update {
                        it.copy(members = it.members.replaceAndSortMember(updated), error = null)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_uiState.value.selectedServer?.id == server.id) {
                    _uiState.update { it.copy(error = e.message ?: "No se pudo cambiar el rol") }
                }
            }
        }
    }

    fun removeMember(userId: String) {
        val server = _uiState.value.selectedServer ?: return
        viewModelScope.launch {
            try {
                serverRepository.removeMember(server.id, userId)
                if (_uiState.value.selectedServer?.id == server.id) {
                    _uiState.update {
                        it.copy(
                            members = it.members.filterNot { member -> member.userId == userId },
                            error = null
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_uiState.value.selectedServer?.id == server.id) {
                    _uiState.update { it.copy(error = e.message ?: "No se pudo expulsar al miembro") }
                }
            }
        }
    }

    fun acceptInvitation(invitationId: String, onAccepted: (Server) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val server = serverRepository.acceptInvitation(invitationId)
                _uiState.update {
                    it.copy(
                        pendingInvitations = it.pendingInvitations.filterNot { invitation ->
                            invitation.id == invitationId
                        },
                        servers = it.servers.upsertServer(server),
                        error = null
                    )
                }
                selectServer(server)
                onAccepted(server)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo aceptar la invitacion") }
            }
        }
    }

    fun rejectInvitation(invitationId: String) {
        viewModelScope.launch {
            try {
                serverRepository.rejectInvitation(invitationId)
                _uiState.update {
                    it.copy(
                        pendingInvitations = it.pendingInvitations.filterNot { invitation ->
                            invitation.id == invitationId
                        },
                        error = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo rechazar la invitacion") }
            }
        }
    }

    fun leaveSelectedServer(onLeft: () -> Unit = {}) {
        val server = _uiState.value.selectedServer ?: return
        viewModelScope.launch {
            try {
                serverRepository.leaveServer(server.id)
                removeServerAndClearSelection(server.id)
                onLeft()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo salir del servidor") }
            }
        }
    }

    fun deleteSelectedServer(onDeleted: () -> Unit = {}) {
        val server = _uiState.value.selectedServer ?: return
        viewModelScope.launch {
            try {
                serverRepository.deleteServer(server.id)
                removeServerAndClearSelection(server.id)
                onDeleted()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo borrar el servidor") }
            }
        }
    }

    fun createChannel(name: String) {
        val server = _uiState.value.selectedServer ?: return
        viewModelScope.launch {
            try {
                val channel = serverRepository.createChannel(server.id, name)
                if (_uiState.value.selectedServer?.id == server.id) {
                    _uiState.update { it.copy(channels = it.channels + channel, error = null) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_uiState.value.selectedServer?.id == server.id) {
                    _uiState.update { it.copy(error = "No se pudo crear el canal") }
                }
            }
        }
    }

    fun clearSelection() {
        channelsLoadJob?.cancel()
        membersLoadJob?.cancel()
        _uiState.update {
            it.copy(
                selectedServer = null,
                selectedChannel = null,
                channels = emptyList(),
                members = emptyList()
            )
        }
    }

    private fun removeServerAndClearSelection(serverId: String) {
        if (_uiState.value.selectedServer?.id == serverId) {
            channelsLoadJob?.cancel()
            membersLoadJob?.cancel()
        }
        _uiState.update { state ->
            val updatedServers = state.servers.filterNot { server -> server.id == serverId }
            if (state.selectedServer?.id == serverId) {
                state.copy(
                    servers = updatedServers,
                    selectedServer = null,
                    selectedChannel = null,
                    channels = emptyList(),
                    members = emptyList(),
                    error = null
                )
            } else {
                state.copy(servers = updatedServers, error = null)
            }
        }
    }

    private fun connectServerEvents() {
        serverEventsCollectJob = viewModelScope.launch {
            serverEventsSource.events.collect { event ->
                when (event.type) {
                    "ROLE_UPDATED" -> {
                        val role = event.role ?: return@collect
                        _uiState.update { state ->
                            state.copy(
                                servers = state.servers.map { server ->
                                    if (server.id == event.serverId) server.copy(myRole = role) else server
                                },
                                selectedServer = state.selectedServer?.let { server ->
                                    if (server.id == event.serverId) server.copy(myRole = role) else server
                                }
                            )
                        }
                    }
                    "MEMBER_REMOVED", "SERVER_DELETED" -> {
                        removeServerAndClearSelection(event.serverId)
                        _accessRevoked.emit(event.serverId)
                    }
                }
            }
        }
        serverEventsJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val token = accessTokenProvider.currentToken() ?: break
                    serverEventsSource.connect(token)
                } catch (_: Exception) {}
                if (isActive) delay(1500)
            }
        }
    }

    override fun onCleared() {
        channelsLoadJob?.cancel()
        membersLoadJob?.cancel()
        serverEventsJob?.cancel()
        serverEventsCollectJob?.cancel()
        serverEventsSource.disconnect()
        super.onCleared()
    }
}
