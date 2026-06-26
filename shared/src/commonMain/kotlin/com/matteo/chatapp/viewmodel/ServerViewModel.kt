package com.matteo.chatapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matteo.chatapp.model.Channel
import com.matteo.chatapp.model.Server
import com.matteo.chatapp.network.ServerApi
import com.matteo.chatapp.network.createHttpClient
import com.matteo.chatapp.repository.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ServerViewModel : ViewModel() {

    private val api = ServerApi(createHttpClient())

    private val _servers = MutableStateFlow<List<Server>>(emptyList())
    val servers: StateFlow<List<Server>> = _servers

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels

    private val _selectedServer = MutableStateFlow<Server?>(null)
    val selectedServer: StateFlow<Server?> = _selectedServer

    private val _selectedChannel = MutableStateFlow<Channel?>(null)
    val selectedChannel: StateFlow<Channel?> = _selectedChannel

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadServers()
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
    }

    fun selectChannel(channel: Channel) {
        _selectedChannel.value = channel
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

    fun clearSelection() {
        _selectedServer.value = null
        _selectedChannel.value = null
        _channels.value = emptyList()
    }
}