package com.matteo.chatapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matteo.chatapp.model.Message
import com.matteo.chatapp.network.MessageApi
import com.matteo.chatapp.network.WebSocketClient
import com.matteo.chatapp.network.createHttpClient
import com.matteo.chatapp.repository.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val client = createHttpClient()
    private val api = MessageApi(client)
    private val wsClient = WebSocketClient(client)

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var wsJob: Job? = null
    private var currentChannelId: String? = null

    fun loadMessages(channelId: String) {
        if (currentChannelId == channelId) return
        currentChannelId = channelId
        wsJob?.cancel()
        _messages.value = emptyList()

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = SessionManager.getToken() ?: return@launch
                _messages.value = api.getChannelMessages(channelId, token)
            } catch (e: Exception) {
                // error silencioso
            } finally {
                _isLoading.value = false
            }
        }

        viewModelScope.launch {
            wsClient.incomingMessages.collect { message ->
                _messages.value = _messages.value + message
            }
        }

        wsJob = viewModelScope.launch {
            try {
                val token = SessionManager.getToken() ?: return@launch
                wsClient.connect(token, channelId)
            } catch (e: Exception) {
                // reconexión se puede añadir luego
            }
        }
    }

    fun loadDmMessages(dmId: String) {
        currentChannelId = dmId
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = SessionManager.getToken() ?: return@launch
                _messages.value = api.getDmMessages(dmId, token)
            } catch (e: Exception) {
                _messages.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendDmMessage(content: String, dmId: String) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken() ?: return@launch
                val message = api.sendDmMessage(dmId, content, token)
                _messages.value = _messages.value + message
            } catch (e: Exception) {}
        }
    }

    fun sendMessage(content: String) {
        val channelId = currentChannelId ?: return
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken() ?: return@launch
                val message = api.sendMessage(channelId, content, token)
                _messages.value = _messages.value + message
            } catch (e: Exception) {
                // error silencioso
            }
        }
    }

    fun clearMessages() {
        wsJob?.cancel()
        _messages.value = emptyList()
        currentChannelId = null
    }

    override fun onCleared() {
        super.onCleared()
        wsClient.disconnect()
    }

}