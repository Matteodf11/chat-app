package com.matteo.chatapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matteo.chatapp.model.Message
import com.matteo.chatapp.model.ProfileEvent
import com.matteo.chatapp.model.TypingEvent
import com.matteo.chatapp.platform.PickedAttachmentFile
import com.matteo.chatapp.network.MessageApi
import com.matteo.chatapp.network.FriendApi
import com.matteo.chatapp.network.WebSocketClient
import com.matteo.chatapp.network.createHttpClient
import com.matteo.chatapp.repository.SessionManager
import com.matteo.chatapp.repository.AppVisibility
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val client = createHttpClient()
    private val api = MessageApi(client)
    private val friendApi = FriendApi(client)
    private val wsClient = WebSocketClient(client)
    private val profileWsClient = WebSocketClient(client)
    private val personalWsClient = WebSocketClient(client)
    private val inboxWsClient = WebSocketClient(client)

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoadingOlder = MutableStateFlow(false)
    val isLoadingOlder: StateFlow<Boolean> = _isLoadingOlder

    private val _hasOlderMessages = MutableStateFlow(false)
    val hasOlderMessages: StateFlow<Boolean> = _hasOlderMessages

    private val _messageError = MutableStateFlow<String?>(null)
    val messageError: StateFlow<String?> = _messageError

    private val _isUploadingAttachment = MutableStateFlow(false)
    val isUploadingAttachment: StateFlow<Boolean> = _isUploadingAttachment

    private val _unreadDmIds = MutableStateFlow<Set<String>>(emptySet())
    val unreadDmIds: StateFlow<Set<String>> = _unreadDmIds

    private val _searchResults = MutableStateFlow<List<Message>>(emptyList())
    val searchResults: StateFlow<List<Message>> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError

    private val _scrollTargetMessageId = MutableStateFlow<String?>(null)
    val scrollTargetMessageId: StateFlow<String?> = _scrollTargetMessageId

    private val _typingUsers = MutableStateFlow<Map<String, String>>(emptyMap())
    val typingUsers: StateFlow<Map<String, String>> = _typingUsers

    private var wsJob: Job? = null
    private var messagesCollectJob: Job? = null
    private var typingCollectJob: Job? = null
    private var profileEventsJob: Job? = null
    private var profileEventsCollectJob: Job? = null
    private var personalMessagesJob: Job? = null
    private var personalMessagesCollectJob: Job? = null
    private var inboxJob: Job? = null
    private var inboxCollectJob: Job? = null
    private var typingStopJob: Job? = null
    private var currentChannelId: String? = null
    private var visibleDmId: String? = null
    private var watchedDmIds: List<String> = emptyList()

    init {
        connectProfileEvents()
        connectPersonalMessages()
    }

    fun loadMessages(channelId: String) {
        if (currentChannelId == "channel:$channelId") return
        currentChannelId = "channel:$channelId"
        resetRealtime()

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = SessionManager.getToken() ?: return@launch
                val page = api.getChannelMessages(channelId, token)
                _messages.value = page.messages
                _hasOlderMessages.value = page.hasMore
            } catch (e: Exception) {
                // error silencioso
            } finally {
                _isLoading.value = false
            }
        }

        collectRealtimeMessages()
        collectTypingEvents()

        wsJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val token = SessionManager.getToken() ?: break
                    wsClient.connect(token, channelId)
                } catch (_: Exception) {}
                if (isActive) delay(1500)
            }
        }
    }

    fun loadDmMessages(dmId: String) {
        if (currentChannelId == "dm:$dmId") return
        currentChannelId = "dm:$dmId"
        markDmRead(dmId)
        resetRealtime()

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = SessionManager.getToken() ?: return@launch
                val page = api.getDmMessages(dmId, token)
                _messages.value = page.messages
                _hasOlderMessages.value = page.hasMore
            } catch (e: Exception) {
                _messages.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }

        collectRealtimeMessages()
        collectTypingEvents()

        wsJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val token = SessionManager.getToken() ?: break
                    wsClient.connectDmMessages(token, dmId)
                } catch (_: Exception) {}
                if (isActive) delay(1500)
            }
        }
    }

    fun sendDmMessage(content: String, dmId: String, replyToId: String? = null, onSent: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken() ?: return@launch
                val message = api.sendDmMessage(dmId, content, replyToId, token)
                appendMessage(message)
                safeSendTyping(dmId = dmId, typing = false)
                _messageError.value = null
                onSent()
            } catch (_: Exception) {
                _messageError.value = "No se pudo enviar el mensaje"
            }
        }
    }

    fun sendMessage(content: String, replyToId: String? = null, onSent: () -> Unit = {}) {
        val channelId = currentChannelId?.removePrefix("channel:") ?: return
        if (channelId == currentChannelId) return
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken() ?: return@launch
                val message = api.sendMessage(channelId, content, replyToId, token)
                appendMessage(message)
                safeSendTyping(channelId = channelId, typing = false)
                _messageError.value = null
                onSent()
            } catch (_: Exception) {
                _messageError.value = "No se pudo enviar el mensaje"
            }
        }
    }

    fun sendAttachment(
        file: PickedAttachmentFile,
        content: String = "",
        replyToId: String? = null,
        onSent: () -> Unit = {}
    ) {
        val destination = currentChannelId ?: return
        if (_isUploadingAttachment.value) return
        viewModelScope.launch {
            _isUploadingAttachment.value = true
            try {
                val token = SessionManager.getToken() ?: return@launch
                val message = if (destination.startsWith("channel:")) {
                    api.sendChannelAttachment(destination.removePrefix("channel:"), file, content, replyToId, token)
                } else {
                    api.sendDmAttachment(destination.removePrefix("dm:"), file, content, replyToId, token)
                }
                appendMessage(message)
                _messageError.value = null
                onSent()
            } catch (_: Exception) {
                _messageError.value = "No se pudo enviar el archivo"
            } finally {
                _isUploadingAttachment.value = false
            }
        }
    }

    fun loadOlderMessages() {
        val destination = currentChannelId ?: return
        val before = _messages.value.firstOrNull()?.createdAt ?: return
        if (_isLoadingOlder.value || !_hasOlderMessages.value) return

        viewModelScope.launch {
            _isLoadingOlder.value = true
            try {
                val token = SessionManager.getToken() ?: return@launch
                val page = if (destination.startsWith("channel:")) {
                    api.getChannelMessages(destination.removePrefix("channel:"), token, before)
                } else {
                    api.getDmMessages(destination.removePrefix("dm:"), token, before)
                }
                val existingIds = _messages.value.mapTo(mutableSetOf()) { it.id }
                _messages.value = page.messages.filterNot { it.id in existingIds } + _messages.value
                _hasOlderMessages.value = page.hasMore
            } catch (_: Exception) {
                // Se conserva la pagina actual y se permite reintentar al volver arriba.
            } finally {
                _isLoadingOlder.value = false
            }
        }
    }

    fun searchCurrentMessages(
        query: String,
        sender: String,
        from: String,
        to: String,
        attachmentsOnly: Boolean
    ) {
        val destination = currentChannelId ?: return
        viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null
            try {
                val token = SessionManager.getToken() ?: return@launch
                _searchResults.value = if (destination.startsWith("channel:")) {
                    api.searchChannelMessages(
                        destination.removePrefix("channel:"), query, sender, from, to, attachmentsOnly, token
                    )
                } else {
                    api.searchDmMessages(
                        destination.removePrefix("dm:"), query, sender, from, to, attachmentsOnly, token
                    )
                }
            } catch (e: Exception) {
                _searchResults.value = emptyList()
                _searchError.value = e.message?.takeIf { it.isNotBlank() } ?: "No se pudo completar la busqueda"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun openSearchResult(messageId: String) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken() ?: return@launch
                val page = api.getMessageContext(messageId, token)
                _messages.value = page.messages
                _hasOlderMessages.value = page.hasMore
                _scrollTargetMessageId.value = messageId
            } catch (e: Exception) {
                _searchError.value = "No se pudo abrir el mensaje"
            }
        }
    }

    fun clearMessageSearch() {
        _searchResults.value = emptyList()
        _searchError.value = null
    }

    fun consumeScrollTarget(messageId: String) {
        if (_scrollTargetMessageId.value == messageId) _scrollTargetMessageId.value = null
    }

    fun editMessage(messageId: String, content: String) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken() ?: return@launch
                applyMessageChange(api.editMessage(messageId, content, token))
            } catch (_: Exception) {}
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken() ?: return@launch
                api.deleteMessage(messageId, token)
                _messages.value = _messages.value.filterNot { it.id == messageId }
            } catch (_: Exception) {}
        }
    }

    fun clearMessages() {
        resetRealtime()
        _messages.value = emptyList()
        currentChannelId = null
    }

    fun disconnectRealtime() {
        resetRealtime()
        profileEventsJob?.cancel()
        profileEventsCollectJob?.cancel()
        personalMessagesJob?.cancel()
        personalMessagesCollectJob?.cancel()
        inboxJob?.cancel()
        inboxCollectJob?.cancel()
        typingStopJob?.cancel()
        profileWsClient.disconnect()
        personalWsClient.disconnect()
        inboxWsClient.disconnect()
    }

    fun watchDmInbox(dmIds: List<String>) {
        val cleanIds = dmIds.distinct().sorted()
        if (cleanIds == watchedDmIds) return
        watchedDmIds = cleanIds

        inboxJob?.cancel()
        inboxCollectJob?.cancel()
        inboxWsClient.disconnect()

        if (cleanIds.isEmpty()) return

        inboxCollectJob = viewModelScope.launch {
            inboxWsClient.personalMessages.collect { message ->
                applyPersonalMessage(message)
            }
        }

        inboxJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val token = SessionManager.getToken() ?: break
                    inboxWsClient.connectDmInbox(token, cleanIds)
                } catch (e: Exception) {
                    delay(2500)
                }
            }
        }
    }

    fun onMessageInputChanged(text: String, dmId: String? = null, channelId: String? = null) {
        if (text.isBlank()) {
            typingStopJob?.cancel()
            viewModelScope.launch { safeSendTyping(dmId = dmId, channelId = channelId, typing = false) }
            return
        }

        viewModelScope.launch { safeSendTyping(dmId = dmId, channelId = channelId, typing = true) }
        typingStopJob?.cancel()
        typingStopJob = viewModelScope.launch {
            delay(1800)
            safeSendTyping(dmId = dmId, channelId = channelId, typing = false)
        }
    }

    fun markDmRead(dmId: String) {
        _unreadDmIds.value = _unreadDmIds.value - dmId
    }

    fun setVisibleDm(dmId: String?) {
        visibleDmId = dmId
        if (dmId != null) markDmRead(dmId)
    }

    fun restoreUnreadDms(dmIds: Set<String>) {
        _unreadDmIds.value = (_unreadDmIds.value + dmIds) - setOfNotNull(visibleDmId)
    }

    private fun collectRealtimeMessages() {
        messagesCollectJob = viewModelScope.launch {
            wsClient.incomingMessages.collect { message ->
                appendMessage(message)
            }
        }
    }

    private fun collectTypingEvents() {
        typingCollectJob = viewModelScope.launch {
            wsClient.typingEvents.collect { event ->
                applyTypingEvent(event)
            }
        }
    }

    private fun appendMessage(message: Message) {
        applyMessageChange(message)
    }

    private fun applyMessageChange(message: Message) {
        if (message.deleted) {
            _messages.value = _messages.value.filterNot { it.id == message.id }
            return
        }
        val index = _messages.value.indexOfFirst { it.id == message.id }
        _messages.value = if (index < 0) {
            _messages.value + message
        } else {
            _messages.value.toMutableList().apply { this[index] = message }
        }
    }

    private fun connectPersonalMessages() {
        if (personalMessagesJob != null) return

        personalMessagesCollectJob = viewModelScope.launch {
            personalWsClient.personalMessages.collect { message ->
                applyPersonalMessage(message)
            }
        }

        personalMessagesJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val token = SessionManager.getToken() ?: break
                    personalWsClient.connectPersonalMessages(token)
                } catch (e: Exception) {
                    delay(2500)
                }
            }
        }
    }

    private fun applyPersonalMessage(message: Message) {
        val dmId = message.dmId ?: return
        if (message.deleted || message.edited) {
            if (currentChannelId == "dm:$dmId") applyMessageChange(message)
            return
        }
        val isOwnMessage =
            (message.senderId != null && message.senderId == SessionManager.getUserId()) ||
                message.senderUsername == SessionManager.getUsername()
        val isOpenDm = visibleDmId == dmId && AppVisibility.isInForeground.value

        if (isOpenDm) {
            appendMessage(message)
            if (!isOwnMessage) {
                viewModelScope.launch {
                    val token = SessionManager.getToken() ?: return@launch
                    runCatching { friendApi.markDmRead(dmId, token) }
                }
            }
            return
        }

        if (!isOwnMessage) {
            _unreadDmIds.value = _unreadDmIds.value + dmId
        }
    }

    private fun applyTypingEvent(event: TypingEvent) {
        val selfId = SessionManager.getUserId()
        if (event.senderId == selfId) return

        val key = event.dmId ?: event.channelId ?: return
        _typingUsers.value = _typingUsers.value.toMutableMap().apply {
            if (event.typing) put(key, event.senderUsername) else remove(key)
        }

        if (event.typing) {
            viewModelScope.launch {
                delay(2500)
                _typingUsers.value = _typingUsers.value - key
            }
        }
    }

    private suspend fun safeSendTyping(dmId: String? = null, channelId: String? = null, typing: Boolean) {
        runCatching { wsClient.sendTyping(channelId = channelId, dmId = dmId, typing = typing) }
    }

    private fun connectProfileEvents() {
        if (profileEventsJob != null) return

        profileEventsCollectJob = viewModelScope.launch {
            profileWsClient.profileEvents.collect { event ->
                applyProfileEvent(event)
            }
        }

        profileEventsJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val token = SessionManager.getToken() ?: break
                    profileWsClient.connectProfileEvents(token)
                } catch (_: Exception) {}
                if (isActive) delay(1500)
            }
        }
    }

    private fun applyProfileEvent(event: ProfileEvent) {
        _messages.value = _messages.value.map { message ->
            if (message.senderId == event.userId) {
                message.copy(senderUsername = event.username, senderAvatar = event.avatarUrl)
            } else {
                message
            }
        }
    }

    private fun resetRealtime() {
        wsJob?.cancel()
        messagesCollectJob?.cancel()
        typingCollectJob?.cancel()
        wsClient.disconnect()
        _messages.value = emptyList()
        _typingUsers.value = emptyMap()
        _hasOlderMessages.value = false
        _isLoadingOlder.value = false
    }

    override fun onCleared() {
        super.onCleared()
        disconnectRealtime()
    }

}
