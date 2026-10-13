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
    private var activityJob: Job? = null
    private var activityRefreshJob: Job? = null
    private var activityDirty = false
    private var activity: List<com.matteo.chatapp.model.ChannelActivity> = emptyList()
    private val readJobs = mutableMapOf<String, Job>()
    private val readMessages = mutableMapOf<String, String>()

    private val _uiState = MutableStateFlow(ServerUiState())
    val uiState: StateFlow<ServerUiState> = _uiState.asStateFlow()

    private val _accessRevoked = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val accessRevoked: SharedFlow<String> = _accessRevoked

    init {
        loadServers()
        loadPendingInvitations()
        connectServerEvents()
        activityJob = viewModelScope.launch {
            while (isActive) {
                if (accessTokenProvider.currentToken() == null) break
                refreshActivity()
                delay(30_000)
            }
        }
    }

    fun refreshActivity() {
        activityDirty = true
        if (activityRefreshJob?.isActive == true) return
        activityRefreshJob = viewModelScope.launch {
            do {
                delay(150) // Coalesce bursts of activity without repainting or reconnecting the chat.
                activityDirty = false
                try {
                    activity = serverRepository.getActivity()
                    applyActivity()
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { /* Keep the last known counts until the connection recovers. */ }
            } while (activityDirty)
        }
    }

    private fun applyActivity() {
        val byChannel = activity.associateBy { it.channelId }
        val byServer = activity.groupBy { it.serverId }
        fun Channel.withActivity(): Channel = byChannel[id]?.let { copy(unreadCount = it.unreadCount, mentionCount = it.mentionCount) } ?: this
        fun Server.withActivity(): Server = byServer[id]?.let { copy(unreadCount = it.sumOf { it.unreadCount }, mentionCount = it.sumOf { it.mentionCount }) } ?: this
        _uiState.update { state -> state.copy(
            servers = state.servers.map { it.withActivity() },
            channels = state.channels.map { it.withActivity() },
            selectedServer = state.selectedServer?.withActivity(),
            selectedChannel = state.selectedChannel?.withActivity()
        ) }
    }

    fun markChannelRead(channelId: String, messageId: String) {
        if (readMessages[channelId] == messageId || readJobs[channelId]?.isActive == true) return
        readJobs[channelId] = viewModelScope.launch {
            try {
                serverRepository.markChannelRead(channelId, messageId)
                readMessages[channelId] = messageId
                refreshActivity()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* Retried while this message remains visible. */ }
        }
    }

    fun clearFeedback() { _uiState.update { it.copy(error = null, notice = null) } }

    fun reloadSelectedServer() {
        _uiState.value.selectedServer?.let { loadChannels(it.id); loadMembers(it.id) }
    }

    private fun launchAction(action: String, block: suspend () -> Unit) {
        if (_uiState.value.action != null) return
        _uiState.update { it.copy(action = action, error = null, notice = null) }
        viewModelScope.launch {
            try { block() }
            finally { _uiState.update { it.copy(action = null) } }
        }
    }

    fun loadServers() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, serversError = null) }
        viewModelScope.launch {
            try {
                val servers = serverRepository.getServers()
                _uiState.update { it.copy(servers = servers) }
                applyActivity()
                refreshActivity()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(serversError = "No se pudieron cargar tus servidores") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectServer(server: Server) {
        if (_uiState.value.selectedServer?.id == server.id) {
            clearSelectedChannel()
            return
        }
        channelsLoadJob?.cancel()
        membersLoadJob?.cancel()
        _uiState.update {
            it.copy(
                selectedServer = server,
                selectedChannel = null,
                channels = emptyList(),
                members = emptyList(), error = null, notice = null, channelsError = null,
                isLoadingChannels = false, isLoadingMembers = false
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
            _uiState.update { it.copy(isLoadingChannels = true, channelsError = null) }
            try {
                val channels = serverRepository.getChannels(serverId)
                if (_uiState.value.selectedServer?.id == serverId) {
                    _uiState.update { it.copy(channels = channels) }
                    applyActivity()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_uiState.value.selectedServer?.id == serverId)
                    _uiState.update { it.copy(channelsError = "No se pudieron cargar los canales") }
            } finally {
                if (_uiState.value.selectedServer?.id == serverId)
                    _uiState.update { it.copy(isLoadingChannels = false) }
            }
        }
    }

    fun loadMembers(serverId: String? = _uiState.value.selectedServer?.id) {
        val targetServerId = serverId ?: return
        membersLoadJob?.cancel()
        membersLoadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMembers = true) }
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
            } finally {
                if (_uiState.value.selectedServer?.id == targetServerId)
                    _uiState.update { it.copy(isLoadingMembers = false) }
            }
        }
    }

    fun loadPendingInvitations() {
        if (_uiState.value.isLoadingInvitations) return
        _uiState.update { it.copy(isLoadingInvitations = true) }
        viewModelScope.launch {
            try {
                val invitations = serverRepository.getPendingInvitations()
                _uiState.update { it.copy(pendingInvitations = invitations) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudieron cargar las invitaciónes") }
            } finally { _uiState.update { it.copy(isLoadingInvitations = false) } }
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
        launchAction("create-server") {
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

    fun updateSelectedServer(name: String, description: String, iconFile: PickedAvatarFile?, onSaved: () -> Unit = {}) {
        val server = _uiState.value.selectedServer ?: return
        launchAction("save") {
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
                            error = null, notice = "Cambios guardados"
                        )
                    }
                }
                if (_uiState.value.selectedServer?.id == server.id) onSaved()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo actualizar el servidor") }
            }
        }
    }

    fun inviteMember(username: String, onInvited: () -> Unit = {}) {
        val server = _uiState.value.selectedServer ?: return
        launchAction("invite") {
            try {
                serverRepository.inviteMember(server.id, username)
                if (_uiState.value.selectedServer?.id == server.id) {
                    _uiState.update { it.copy(error = null, notice = "Invitación enviada") }
                    onInvited()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_uiState.value.selectedServer?.id == server.id) {
                    _uiState.update { it.copy(error = "No se pudo enviar la invitación") }
                }
            }
        }
    }

    fun updateMemberRole(userId: String, role: String) {
        val server = _uiState.value.selectedServer ?: return
        launchAction("role") {
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
        launchAction("remove") {
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
        launchAction("accept") {
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
                _uiState.update { it.copy(error = "No se pudo aceptar la invitación") }
            }
        }
    }

    fun rejectInvitation(invitationId: String) {
        launchAction("reject") {
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
                _uiState.update { it.copy(error = "No se pudo rechazar la invitación") }
            }
        }
    }

    fun leaveSelectedServer(onLeft: () -> Unit = {}) {
        val server = _uiState.value.selectedServer ?: return
        launchAction("leave") {
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
        launchAction("delete") {
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

    fun createChannel(name: String, onCreated: () -> Unit = {}) {
        val server = _uiState.value.selectedServer ?: return
        launchAction("channel") {
            try {
                val channel = serverRepository.createChannel(server.id, name)
                if (_uiState.value.selectedServer?.id == server.id) {
                    _uiState.update { it.copy(channels = (it.channels + channel).distinctBy { channel -> channel.id }, error = null, notice = "Canal creado") }
                    onCreated()
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
                members = emptyList(), isLoadingChannels = false, isLoadingMembers = false, channelsError = null
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
                    "CHANNEL_ACTIVITY" -> refreshActivity()
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
        activityJob?.cancel()
        activityRefreshJob?.cancel()
        readJobs.values.forEach { it.cancel() }
        channelsLoadJob?.cancel()
        membersLoadJob?.cancel()
        serverEventsJob?.cancel()
        serverEventsCollectJob?.cancel()
        serverEventsSource.disconnect()
        super.onCleared()
    }
}
