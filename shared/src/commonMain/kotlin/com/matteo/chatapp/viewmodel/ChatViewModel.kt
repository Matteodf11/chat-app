package com.matteo.chatapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matteo.chatapp.model.Message
import com.matteo.chatapp.model.ProfileEvent
import com.matteo.chatapp.model.TypingEvent
import com.matteo.chatapp.platform.PickedAttachmentFile
import com.matteo.chatapp.repository.AccessTokenProvider
import com.matteo.chatapp.repository.AppVisibility
import com.matteo.chatapp.repository.ChatRepository
import com.matteo.chatapp.repository.ConversationEventsSource
import com.matteo.chatapp.repository.ConversationTarget
import com.matteo.chatapp.repository.DmInboxEventsSource
import com.matteo.chatapp.repository.MessageSearchQuery
import com.matteo.chatapp.repository.PersonalMessagesSource
import com.matteo.chatapp.repository.ProfileEventsSource
import com.matteo.chatapp.repository.SessionIdentityProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val accessTokenProvider: AccessTokenProvider,
    private val sessionIdentityProvider: SessionIdentityProvider,
    private val conversationEventsSource: ConversationEventsSource,
    private val profileEventsSource: ProfileEventsSource,
    private val personalMessagesSource: PersonalMessagesSource,
    private val inboxEventsSource: DmInboxEventsSource
) : ViewModel() {

    private val _conversationState = MutableStateFlow(ChatConversationUiState())
    val conversationState: StateFlow<ChatConversationUiState> = _conversationState.asStateFlow()

    private val _unreadDmIds = MutableStateFlow<Set<String>>(emptySet())
    val unreadDmIds: StateFlow<Set<String>> = _unreadDmIds.asStateFlow()

    private val _searchState = MutableStateFlow(MessageSearchUiState())
    val searchState: StateFlow<MessageSearchUiState> = _searchState.asStateFlow()

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
    private var conversationLoadJob: Job? = null
    private var olderMessagesJob: Job? = null
    private var messageSearchJob: Job? = null
    private val typingExpiryJobs = mutableMapOf<String, Job>()
    private var activeConversation: ConversationTarget? = null
    private var visibleDmId: String? = null
    private var watchedDmIds: List<String> = emptyList()

    init {
        connectProfileEvents()
        connectPersonalMessages()
    }

    fun loadMessages(channelId: String) {
        val destination = ConversationTarget.Channel(channelId)
        if (activeConversation == destination) return
        activeConversation = destination
        resetRealtime()

        conversationLoadJob = viewModelScope.launch {
            _conversationState.update { it.copy(isLoading = true) }
            try {
                val page = chatRepository.loadMessages(destination)
                if (activeConversation != destination) return@launch
                _conversationState.update {
                    it.copy(messages = page.messages, hasOlderMessages = page.hasMore)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // error silencioso
            } finally {
                if (activeConversation == destination) {
                    _conversationState.update { it.copy(isLoading = false) }
                }
            }
        }

        collectRealtimeMessages()
        collectTypingEvents()

        wsJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val token = accessTokenProvider.currentToken() ?: break
                    conversationEventsSource.connect(token, destination)
                } catch (_: Exception) {}
                if (isActive) delay(1500)
            }
        }
    }

    fun loadDmMessages(dmId: String) {
        val destination = ConversationTarget.DirectMessage(dmId)
        if (activeConversation == destination) return
        activeConversation = destination
        markDmRead(dmId)
        resetRealtime()

        conversationLoadJob = viewModelScope.launch {
            _conversationState.update { it.copy(isLoading = true) }
            try {
                val page = chatRepository.loadMessages(destination)
                if (activeConversation != destination) return@launch
                _conversationState.update {
                    it.copy(messages = page.messages, hasOlderMessages = page.hasMore)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (activeConversation == destination) {
                    _conversationState.update { it.copy(messages = emptyList()) }
                }
            } finally {
                if (activeConversation == destination) {
                    _conversationState.update { it.copy(isLoading = false) }
                }
            }
        }

        collectRealtimeMessages()
        collectTypingEvents()

        wsJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val token = accessTokenProvider.currentToken() ?: break
                    conversationEventsSource.connect(token, destination)
                } catch (_: Exception) {}
                if (isActive) delay(1500)
            }
        }
    }

    fun sendDmMessage(content: String, dmId: String, replyToId: String? = null, onSent: () -> Unit = {}) {
        val destination = ConversationTarget.DirectMessage(dmId)
        viewModelScope.launch {
            try {
                val message = chatRepository.sendMessage(destination, content, replyToId)
                if (activeConversation != destination) return@launch
                appendMessage(message)
                safeSendTyping(dmId = dmId, typing = false)
                _conversationState.update { it.copy(error = null) }
                onSent()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (activeConversation == destination) {
                    _conversationState.update { it.copy(error = "No se pudo enviar el mensaje") }
                }
            }
        }
    }

    fun sendMessage(content: String, replyToId: String? = null, onSent: () -> Unit = {}) {
        val destination = activeConversation as? ConversationTarget.Channel ?: return
        viewModelScope.launch {
            try {
                val message = chatRepository.sendMessage(destination, content, replyToId)
                if (activeConversation != destination) return@launch
                appendMessage(message)
                safeSendTyping(channelId = destination.id, typing = false)
                _conversationState.update { it.copy(error = null) }
                onSent()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (activeConversation == destination) {
                    _conversationState.update { it.copy(error = "No se pudo enviar el mensaje") }
                }
            }
        }
    }

    fun sendAttachment(
        file: PickedAttachmentFile,
        content: String = "",
        replyToId: String? = null,
        onSent: () -> Unit = {}
    ) {
        val destination = activeConversation ?: return
        if (_conversationState.value.isUploadingAttachment) return
        viewModelScope.launch {
            _conversationState.update { it.copy(isUploadingAttachment = true) }
            try {
                val message = chatRepository.sendAttachment(
                    destination,
                    file,
                    content,
                    replyToId
                )
                if (activeConversation != destination) return@launch
                appendMessage(message)
                _conversationState.update { it.copy(error = null) }
                onSent()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (activeConversation == destination) {
                    _conversationState.update { it.copy(error = "No se pudo enviar el archivo") }
                }
            } finally {
                if (activeConversation == destination) {
                    _conversationState.update { it.copy(isUploadingAttachment = false) }
                }
            }
        }
    }

    fun loadOlderMessages() {
        val destination = activeConversation ?: return
        val currentState = _conversationState.value
        val before = currentState.messages.firstOrNull()?.createdAt ?: return
        if (currentState.isLoadingOlder || !currentState.hasOlderMessages) return

        olderMessagesJob?.cancel()
        olderMessagesJob = viewModelScope.launch {
            _conversationState.update { it.copy(isLoadingOlder = true) }
            try {
                val page = chatRepository.loadMessages(destination, before)
                if (activeConversation != destination) return@launch
                _conversationState.update {
                    it.copy(
                        messages = it.messages.prependNewMessages(page.messages),
                        hasOlderMessages = page.hasMore
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Se conserva la pagina actual y se permite reintentar al volver arriba.
            } finally {
                if (activeConversation == destination) {
                    _conversationState.update { it.copy(isLoadingOlder = false) }
                }
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
        val destination = activeConversation ?: return
        messageSearchJob?.cancel()
        messageSearchJob = viewModelScope.launch {
            _searchState.update { it.copy(isSearching = true, error = null) }
            try {
                val results = chatRepository.searchMessages(
                    destination,
                    MessageSearchQuery(
                        text = query,
                        sender = sender,
                        from = from,
                        to = to,
                        attachmentsOnly = attachmentsOnly
                    )
                )
                if (activeConversation != destination) return@launch
                _searchState.update { it.copy(results = results) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _searchState.update {
                    it.copy(
                        results = emptyList(),
                        error = e.message?.takeIf(String::isNotBlank) ?: "No se pudo completar la busqueda"
                    )
                }
            } finally {
                if (activeConversation == destination) {
                    _searchState.update { it.copy(isSearching = false) }
                }
            }
        }
    }

    fun openSearchResult(messageId: String) {
        val destination = activeConversation ?: return
        messageSearchJob?.cancel()
        messageSearchJob = viewModelScope.launch {
            try {
                val page = chatRepository.getMessageContext(messageId)
                if (activeConversation != destination) return@launch
                _conversationState.update {
                    it.copy(messages = page.messages, hasOlderMessages = page.hasMore)
                }
                _searchState.update { it.copy(scrollTargetMessageId = messageId) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _searchState.update { it.copy(error = "No se pudo abrir el mensaje") }
            }
        }
    }

    fun clearMessageSearch() {
        messageSearchJob?.cancel()
        messageSearchJob = null
        _searchState.value = MessageSearchUiState()
    }

    fun consumeScrollTarget(messageId: String) {
        _searchState.update {
            if (it.scrollTargetMessageId == messageId) it.copy(scrollTargetMessageId = null) else it
        }
    }

    fun editMessage(messageId: String, content: String) {
        val destination = activeConversation ?: return
        viewModelScope.launch {
            try {
                val editedMessage = chatRepository.editMessage(messageId, content)
                if (activeConversation == destination) applyMessageChange(editedMessage)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {}
        }
    }

    fun deleteMessage(messageId: String) {
        val destination = activeConversation ?: return
        viewModelScope.launch {
            try {
                chatRepository.deleteMessage(messageId)
                if (activeConversation == destination) {
                    _conversationState.update {
                        it.copy(messages = it.messages.filterNot { message -> message.id == messageId })
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {}
        }
    }

    fun clearMessages() {
        resetRealtime()
        activeConversation = null
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
        profileEventsSource.disconnect()
        personalMessagesSource.disconnect()
        inboxEventsSource.disconnect()
    }

    fun watchDmInbox(dmIds: List<String>) {
        val cleanIds = dmIds.distinct().sorted()
        if (cleanIds == watchedDmIds) return
        watchedDmIds = cleanIds

        inboxJob?.cancel()
        inboxCollectJob?.cancel()
        inboxEventsSource.disconnect()

        if (cleanIds.isEmpty()) return

        inboxCollectJob = viewModelScope.launch {
            inboxEventsSource.messages.collect { message ->
                applyPersonalMessage(message)
            }
        }

        inboxJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val token = accessTokenProvider.currentToken() ?: break
                    inboxEventsSource.connect(token, cleanIds)
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
            conversationEventsSource.messages.collect { message ->
                appendMessage(message)
            }
        }
    }

    private fun collectTypingEvents() {
        typingCollectJob = viewModelScope.launch {
            conversationEventsSource.typingEvents.collect { event ->
                applyTypingEvent(event)
            }
        }
    }

    private fun appendMessage(message: Message) {
        applyMessageChange(message)
    }

    private fun applyMessageChange(message: Message) {
        _conversationState.update {
            it.copy(messages = it.messages.withMessageChange(message))
        }
    }

    private fun connectPersonalMessages() {
        if (personalMessagesJob != null) return

        personalMessagesCollectJob = viewModelScope.launch {
            personalMessagesSource.messages.collect { message ->
                applyPersonalMessage(message)
            }
        }

        personalMessagesJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val token = accessTokenProvider.currentToken() ?: break
                    personalMessagesSource.connect(token)
                } catch (e: Exception) {
                    delay(2500)
                }
            }
        }
    }

    private fun applyPersonalMessage(message: Message) {
        val dmId = message.dmId ?: return
        if (message.deleted || message.edited) {
            if (activeConversation == ConversationTarget.DirectMessage(dmId)) applyMessageChange(message)
            return
        }
        val isOwnMessage =
            (message.senderId != null && message.senderId == sessionIdentityProvider.currentUserId()) ||
                message.senderUsername == sessionIdentityProvider.currentUsername()
        val isOpenDm = visibleDmId == dmId && AppVisibility.isInForeground.value

        if (isOpenDm) {
            appendMessage(message)
            if (!isOwnMessage) {
                viewModelScope.launch {
                    runCatching { chatRepository.markDmRead(dmId) }
                }
            }
            return
        }

        if (!isOwnMessage) {
            _unreadDmIds.value = _unreadDmIds.value + dmId
        }
    }

    private fun applyTypingEvent(event: TypingEvent) {
        val selfId = sessionIdentityProvider.currentUserId()
        if (event.senderId == selfId) return

        val key = event.dmId ?: event.channelId ?: return
        typingExpiryJobs.remove(key)?.cancel()
        _conversationState.update { state ->
            state.copy(
                typingUsers = state.typingUsers.toMutableMap().apply {
                    if (event.typing) put(key, event.senderUsername) else remove(key)
                }
            )
        }

        if (event.typing) {
            typingExpiryJobs[key] = viewModelScope.launch {
                delay(2500)
                _conversationState.update { it.copy(typingUsers = it.typingUsers - key) }
                typingExpiryJobs.remove(key)
            }
        }
    }

    private suspend fun safeSendTyping(dmId: String? = null, channelId: String? = null, typing: Boolean) {
        runCatching {
            conversationEventsSource.sendTyping(
                channelId = channelId,
                dmId = dmId,
                typing = typing
            )
        }
    }

    private fun connectProfileEvents() {
        if (profileEventsJob != null) return

        profileEventsCollectJob = viewModelScope.launch {
            profileEventsSource.events.collect { event ->
                applyProfileEvent(event)
            }
        }

        profileEventsJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val token = accessTokenProvider.currentToken() ?: break
                    profileEventsSource.connect(token)
                } catch (_: Exception) {}
                if (isActive) delay(1500)
            }
        }
    }

    private fun applyProfileEvent(event: ProfileEvent) {
        _conversationState.update { state ->
            state.copy(
                messages = state.messages.map { message ->
                    if (message.senderId == event.userId) {
                        message.copy(senderUsername = event.username, senderAvatar = event.avatarUrl)
                    } else {
                        message
                    }
                }
            )
        }
    }

    private fun resetRealtime() {
        conversationLoadJob?.cancel()
        conversationLoadJob = null
        olderMessagesJob?.cancel()
        olderMessagesJob = null
        messageSearchJob?.cancel()
        messageSearchJob = null
        _searchState.value = MessageSearchUiState()
        wsJob?.cancel()
        messagesCollectJob?.cancel()
        typingCollectJob?.cancel()
        typingExpiryJobs.values.forEach(Job::cancel)
        typingExpiryJobs.clear()
        conversationEventsSource.disconnect()
        _conversationState.update {
            it.copy(
                messages = emptyList(),
                isLoading = false,
                isLoadingOlder = false,
                hasOlderMessages = false,
                error = null,
                isUploadingAttachment = false,
                typingUsers = emptyMap()
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnectRealtime()
    }

}
