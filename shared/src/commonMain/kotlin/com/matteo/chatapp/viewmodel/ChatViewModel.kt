package com.matteo.chatapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matteo.chatapp.model.Message
import com.matteo.chatapp.model.ProfileEvent
import com.matteo.chatapp.model.TypingEvent
import com.matteo.chatapp.platform.PickedAttachmentFile
import com.matteo.chatapp.platform.MessageNotificationSink
import com.matteo.chatapp.platform.DirectMessageNotification
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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val accessTokenProvider: AccessTokenProvider,
    private val sessionIdentityProvider: SessionIdentityProvider,
    private val conversationEventsSource: ConversationEventsSource,
    private val profileEventsSource: ProfileEventsSource,
    private val personalMessagesSource: PersonalMessagesSource,
    private val inboxEventsSource: DmInboxEventsSource,
    private val notifications: MessageNotificationSink = MessageNotificationSink { },
    private val localStore: com.matteo.chatapp.repository.ChatLocalStore = object : com.matteo.chatapp.repository.ChatLocalStore {}
) : ViewModel() {
    suspend fun refreshAttachment(message: Message): Message = chatRepository.refreshMessage(message.id)


    private val _conversationState = MutableStateFlow(ChatConversationUiState())
    val conversationState: StateFlow<ChatConversationUiState> = _conversationState.asStateFlow()

    private val _unreadDmIds = MutableStateFlow<Set<String>>(emptySet())
    val unreadDmIds: StateFlow<Set<String>> = _unreadDmIds.asStateFlow()
    private val _dmSummaries = MutableStateFlow<Map<String, DmSummary>>(emptyMap())
    val dmSummaries = _dmSummaries.asStateFlow()
    private val _dmPreferences = MutableStateFlow<Map<String, com.matteo.chatapp.model.DmConversation>>(emptyMap())
    val dmPreferences = _dmPreferences.asStateFlow()
    private val _preferencesError = MutableStateFlow<String?>(null)
    val preferencesError = _preferencesError.asStateFlow()
    private val _sharedItems = MutableStateFlow(SharedItemsUiState())
    val sharedItems = _sharedItems.asStateFlow()
    private var sharedItemsJob: Job? = null
    private val restored = kotlinx.coroutines.CompletableDeferred<Unit>()
    private val readWatermarks = mutableMapOf<String, com.matteo.chatapp.model.DmReadEvent>()
    private val preferenceUpdates = mutableSetOf<String>()
    private val confirmedPreferences = mutableMapOf<String, com.matteo.chatapp.model.DmConversation>()
    private val enqueueingTargets = mutableSetOf<ConversationTarget>()
    private var stopped = false
    private val _inboxRevision = MutableStateFlow(0)
    val inboxRevision = _inboxRevision.asStateFlow()

    private val _searchState = MutableStateFlow(MessageSearchUiState())
    val searchState: StateFlow<MessageSearchUiState> = _searchState.asStateFlow()

    private val outbox = linkedMapOf<String, PendingMessage>()
    private var connectionCollectJob: Job? = null
    private var refreshRequested = false
    private val changesDuringSync = linkedMapOf<String, Message>()
    private var wsJob: Job? = null
    private var messagesCollectJob: Job? = null
    private var typingCollectJob: Job? = null
    private var profileEventsJob: Job? = null
    private var profileEventsCollectJob: Job? = null
    private var personalMessagesJob: Job? = null
    private var personalMessagesCollectJob: Job? = null
    private var personalConnectionJob: Job? = null
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
    private val receivedMessageIds = linkedSetOf<String>()

    init {
        viewModelScope.launch {
            try {
                localStore.pending().forEach { outbox.putIfAbsent(it.id, it) }
                val summaries = localStore.summaries()
                val preferences = localStore.preferences()
                _dmSummaries.update { current -> current + summaries.filter { (id, cached) ->
                    current[id] == null || cached.at.orEmpty() > current[id]?.at.orEmpty()
                } }
                _dmPreferences.update { preferences.associateBy { dm -> dm.id } + it }
                publishPending()
            } catch (_: Exception) {
                _conversationState.update { it.copy(error = "No se pudieron recuperar los envíos pendientes") }
            } finally { restored.complete(Unit); if (!stopped) connectPersonalMessages() }
        }
        viewModelScope.launch { restored.await(); _dmSummaries.collect { runCatching { localStore.saveSummaries(it) } } }
        viewModelScope.launch { restored.await(); _dmPreferences.collect { runCatching { localStore.savePreferences(it.values.toList()) } } }
        viewModelScope.launch { conversationEventsSource.readEvents.collect(::applyReadEvent) }
        viewModelScope.launch { personalMessagesSource.readEvents.collect(::applyReadEvent) }
        viewModelScope.launch { inboxEventsSource.readEvents.collect(::applyReadEvent) }
        viewModelScope.launch { personalMessagesSource.settings.collect { dm ->
            _dmPreferences.update { it + (dm.id to dm) }
            _inboxRevision.value++
        } }
        connectProfileEvents()
    }

    fun loadMessages(channelId: String) = openConversation(ConversationTarget.Channel(channelId))

    fun loadDmMessages(dmId: String) {
        markDmRead(dmId)
        openConversation(ConversationTarget.DirectMessage(dmId))
    }

    private fun openConversation(destination: ConversationTarget) {
        if (activeConversation == destination) return
        resetRealtime()
        activeConversation = destination
        publishPending()
        refreshConversation()
        collectRealtimeMessages()
        collectTypingEvents()
        connectionCollectJob = viewModelScope.launch {
            conversationEventsSource.connected.collect { connected ->
                if (activeConversation == destination) {
                    _conversationState.update { it.copy(isConnected = connected) }
                    if (connected) refreshConversation()
                }
            }
        }
        wsJob = viewModelScope.launch {
            var retryDelay = 1500L
            while (isActive) {
                try {
                    val token = accessTokenProvider.currentToken() ?: break
                    conversationEventsSource.connect(token, destination)
                    retryDelay = 1500L
                } catch (_: Exception) {
                    currentCoroutineContext().ensureActive()
                }
                if (activeConversation == destination) _conversationState.update { it.copy(isConnected = false) }
                delay(retryDelay)
                retryDelay = (retryDelay * 2).coerceAtMost(15000L)
            }
        }
    }

    fun refreshConversation() {
        val destination = activeConversation ?: return
        if (conversationLoadJob?.isActive == true) {
            refreshRequested = true
            return
        }
        conversationLoadJob = viewModelScope.launch {
            restored.await()
            do {
                refreshRequested = false
                val previous = _conversationState.value.messages
                changesDuringSync.clear()
                _conversationState.update { it.copy(isLoading = previous.isEmpty(), isSyncing = true, loadError = null) }
                try {
                    val newest = chatRepository.loadMessages(destination)
                    val recovered = newest.messages.toMutableList()
                    var page = newest
                    val knownIds = previous.mapTo(mutableSetOf()) { it.id }
                    // Recover every missed page until the history overlaps.
                    while (previous.isNotEmpty() && page.hasMore && page.messages.none { it.id in knownIds }) {
                        val cursor = page.messages.firstOrNull()?.createdAt ?: break
                        val older = chatRepository.loadMessages(destination, cursor)
                        if (older.messages.isEmpty() || older.messages.first().id == page.messages.first().id) break
                        recovered.addAll(0, older.messages)
                        page = older
                    }
                    if (activeConversation != destination) return@launch
                    recovered.forEach(::acknowledgePending)
                    _conversationState.update { state ->
                        val oldest = recovered.firstOrNull()?.createdAt
                        val retained = if (oldest == null) emptyList() else state.messages.filter {
                            it.createdAt != null && it.createdAt < oldest
                        }
                        val snapshot = (retained + recovered).associateBy { it.id }.values.toList()
                        val merged = changesDuringSync.values.fold(snapshot) { list, message -> list.withMessageChange(message) }
                            .sortedWith(compareBy<Message> { it.createdAt.orEmpty() }.thenBy { it.id })
                        state.copy(messages = merged.map(::withReadReceipt), hasOlderMessages = if (retained.isEmpty()) page.hasMore else state.hasOlderMessages)
                    }
                    publishPending()
                    if (destination is ConversationTarget.DirectMessage && visibleDmId == destination.id) {
                        markVisibleMessagesRead()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    if (activeConversation == destination) _conversationState.update {
                        it.copy(loadError = "No se pudo actualizar la conversación. Reintenta cuando tengas conexión.")
                    }
                } finally {
                    if (activeConversation == destination) _conversationState.update { it.copy(isLoading = false, isSyncing = false) }
                }
            } while (refreshRequested && activeConversation == destination)
        }
    }

    fun sendDmMessage(content: String, dmId: String, replyToId: String? = null, onSent: () -> Unit = {}) {
        enqueue(ConversationTarget.DirectMessage(dmId), content, replyToId, null, onSent)
    }

    fun sendMessage(content: String, replyToId: String? = null, onSent: () -> Unit = {}) {
        val destination = activeConversation as? ConversationTarget.Channel ?: return
        enqueue(destination, content, replyToId, null, onSent)
    }

    fun sendAttachment(file: PickedAttachmentFile, content: String = "", replyToId: String? = null, onSent: () -> Unit = {}) {
        val destination = activeConversation ?: return
        enqueue(destination, content, replyToId, file, onSent)
    }

    @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
    fun sendPhotos(files: List<PickedAttachmentFile>, content: String, replyToId: String?, onQueued: (Boolean) -> Unit) {
        val target = activeConversation
        val validation = runCatching {
            com.matteo.chatapp.platform.validatePhotoSelection(files)
            require(content.length <= 4000) { "El comentario no puede superar 4000 caracteres" }
        }.exceptionOrNull()
        if (validation != null) {
            _conversationState.update { it.copy(error = validation.message) }
            onQueued(false)
            return
        }
        if (target == null || stopped || !enqueueingTargets.add(target)) { onQueued(false); return }
        val pending = files.mapIndexed { index, file ->
            PendingMessage(kotlin.uuid.Uuid.random().toString(), target, if (index == 0) content.trim() else "",
                if (index == 0) replyToId else null, file)
        }
        viewModelScope.launch {
            var saved = false
            try {
                restored.await()
                localStore.putBatch(pending)
                pending.forEach { outbox[it.id] = it }
                publishPending()
                saved = true
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _conversationState.update { it.copy(error = "No se pudieron guardar las fotos. Puedes volver a intentarlo.") } }
            finally { enqueueingTargets.remove(target) }
            onQueued(saved)
            if (saved) pending.forEach { deliverPending(it) }
        }
    }

    @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
    private fun enqueue(target: ConversationTarget, content: String, replyToId: String?, file: PickedAttachmentFile?, onQueued: () -> Unit) {
        if (content.isBlank() && file == null) return
        if (content.length > 4000) {
            _conversationState.update { it.copy(error = "El mensaje no puede superar 4000 caracteres") }
            return
        }
        val pending = PendingMessage(kotlin.uuid.Uuid.random().toString(), target, content.trim(), replyToId, file)
        if (stopped || !enqueueingTargets.add(target)) return
        viewModelScope.launch {
            try {
                restored.await()
                localStore.put(pending)
                outbox[pending.id] = pending
                publishPending()
                onQueued()
                if (!stopped) deliver(pending)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                _conversationState.update { it.copy(error = "No se pudo guardar el envío. El mensaje sigue en el editor.") }
            } finally { enqueueingTargets.remove(target) }
        }
    }

    fun retryMessage(id: String) {
        val pending = outbox[id]?.takeIf { it.status == SendStatus.FAILED } ?: return
        val retry = pending.copy(status = SendStatus.SENDING, error = null)
        outbox[id] = retry
        publishPending()
        deliver(retry)
    }

    fun discardPendingMessage(id: String) {
        if (outbox[id]?.status != SendStatus.FAILED) return
        viewModelScope.launch {
            try { localStore.remove(id); outbox.remove(id); publishPending() }
            catch (_: Exception) { _conversationState.update { it.copy(error = "No se pudo descartar el envío") } }
        }
    }

    private fun deliver(pending: PendingMessage) {
        viewModelScope.launch { deliverPending(pending) }
    }

    private suspend fun deliverPending(pending: PendingMessage) {
        if (stopped) return
        try {
            val message = if (pending.file == null) chatRepository.sendMessage(
                pending.target, pending.content, pending.replyToId, pending.id
            ) else chatRepository.sendAttachment(pending.target, pending.file, pending.content, pending.replyToId, pending.id)
            outbox.remove(pending.id)
            runCatching { localStore.remove(pending.id) }
            if (message.dmId != null) applyPersonalMessage(message)
            if (activeConversation == pending.target) {
                appendMessage(message)
                _conversationState.update { it.copy(error = null) }
                safeSendTyping(channelId = message.channelId, dmId = message.dmId, typing = false)
            }
            publishPending()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            outbox[pending.id]?.let {
                outbox[pending.id] = it.copy(status = SendStatus.FAILED, error =
                    if (e is com.matteo.chatapp.network.ApiException) e.message else "No se pudo confirmar el envío")
            }
            publishPending()
        }
    }

    private fun acknowledgePending(message: Message) {
        val id = message.clientMessageId ?: return
        val pending = outbox[id] ?: return
        if (message.senderId == sessionIdentityProvider.currentUserId() &&
            (message.dmId == pending.target.id || message.channelId == pending.target.id)) {
            outbox.remove(id)
            viewModelScope.launch { runCatching { localStore.remove(id) } }
        }
    }

    private fun publishPending() {
        _conversationState.update { it.copy(pendingMessages = outbox.values.filter { pending -> pending.target == activeConversation }) }
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
        stopped = true
        sharedItemsJob?.cancel()
        resetRealtime()
        profileEventsJob?.cancel()
        profileEventsCollectJob?.cancel()
        personalMessagesJob?.cancel()
        personalMessagesCollectJob?.cancel()
        personalConnectionJob?.cancel()
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
        _dmSummaries.update { summaries -> summaries[dmId]?.let { summaries + (dmId to it.copy(unreadCount = 0)) } ?: summaries }
    }

    fun restoreDmSummaries(dms: List<com.matteo.chatapp.model.DmConversation>) {
        val freshPreferences = dms.filter { dm ->
            val confirmed = confirmedPreferences[dm.id]
            val matches = confirmed == null || (dm.pinned == confirmed.pinned && dm.muted == confirmed.muted && dm.archived == confirmed.archived)
            if (matches) confirmedPreferences.remove(dm.id)
            matches && dm.id !in preferenceUpdates
        }
        _dmPreferences.update { current -> current + freshPreferences.associateBy { it.id } }
        _dmSummaries.update { current ->
            current.toMutableMap().apply {
                dms.forEach { dm ->
                    val existing = current[dm.id]
                    val at = maxOf(dm.lastActivityAt.orEmpty(), dm.lastMessageAt.orEmpty()).takeIf { it.isNotEmpty() }
                    if (existing == null || at.orEmpty() >= existing.at.orEmpty()) {
                        put(dm.id, DmSummary(dm.lastMessagePreview, at,
                            if (visibleDmId == dm.id) 0 else dm.unreadCount.coerceAtLeast(if (dm.hasUnread) 1 else 0)))
                    }
                }
            }
        }
        _unreadDmIds.value = _dmSummaries.value.filterValues { it.unreadCount > 0 }.keys - setOfNotNull(visibleDmId)
    }

    fun setVisibleDm(dmId: String?) {
        visibleDmId = dmId
        if (dmId != null) { markDmRead(dmId); markVisibleMessagesRead() }
    }

    private fun markVisibleMessagesRead() {
        val target = activeConversation as? ConversationTarget.DirectMessage ?: return
        if (visibleDmId != target.id || !AppVisibility.isInForeground.value) return
        val last = _conversationState.value.messages.lastOrNull() ?: return
        viewModelScope.launch { runCatching { chatRepository.markDmReadThrough(target.id, last.id) } }
    }

    private fun applyReadEvent(event: com.matteo.chatapp.model.DmReadEvent) {
        if (event.readerId == sessionIdentityProvider.currentUserId()) {
            markDmRead(event.dmId)
            _inboxRevision.value++
            return
        }
        val previous = readWatermarks[event.dmId]
        if (previous != null && previous.readAt >= event.readAt) return
        readWatermarks[event.dmId] = event
        _conversationState.update { it.copy(messages = it.messages.map(::withReadReceipt)) }
    }

    private fun withReadReceipt(message: Message): Message {
        val receipt = readWatermarks[message.dmId] ?: return message
        return if (message.senderId != receipt.readerId && message.createdAt != null && message.createdAt <= receipt.readAt)
            message.copy(readAt = maxOf(message.readAt.orEmpty(), receipt.readAt)) else message
    }

    fun updateDmPreferences(dmId: String, value: com.matteo.chatapp.model.DmPreferencesRequest) {
        if (!preferenceUpdates.add(dmId)) return
        viewModelScope.launch {
            try {
                val dm = chatRepository.updatePreferences(dmId, value)
                confirmedPreferences[dmId] = dm
                _dmPreferences.update { it + (dm.id to dm) }
                _inboxRevision.value++
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _preferencesError.value = "No se pudieron guardar las opciones del chat. Reintenta cuando tengas conexión." }
            finally { preferenceUpdates.remove(dmId) }
        }
    }

    fun clearPreferencesError() { _preferencesError.value = null }

    fun openSharedItems(dmId: String, kind: String = "media") {
        sharedItemsJob?.cancel()
        _sharedItems.value = SharedItemsUiState(dmId = dmId, kind = kind)
        loadMoreSharedItems()
    }

    fun closeSharedItems() { sharedItemsJob?.cancel(); _sharedItems.value = SharedItemsUiState() }

    fun loadMoreSharedItems() {
        val state = _sharedItems.value
        val dmId = state.dmId ?: return
        if (state.loading || !state.hasMore) return
        _sharedItems.update { it.copy(loading = true, error = null) }
        sharedItemsJob = viewModelScope.launch {
            try {
                // Scan bounded pages until this tab has results, without limiting the whole history.
                var cursor = state.nextBefore
                var page: com.matteo.chatapp.model.SharedItemsPage
                do {
                    page = chatRepository.sharedItems(dmId, state.kind, cursor)
                    if (page.nextBefore == cursor && page.hasMore) error("Cursor de galería repetido")
                    cursor = page.nextBefore
                } while (page.messages.isEmpty() && page.hasMore)
                _sharedItems.update { it.copy(messages = (it.messages + page.messages).distinctBy { m -> m.id },
                    hasMore = page.hasMore, nextBefore = page.nextBefore, loading = false) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _sharedItems.update { it.copy(loading = false, error = "No se pudo cargar el contenido compartido") } }
        }
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
        val matches = when (val target = activeConversation) {
            is ConversationTarget.Channel -> message.channelId == target.id
            is ConversationTarget.DirectMessage -> message.dmId == target.id
            null -> false
        }
        if (!matches) return
        if (_conversationState.value.isSyncing) changesDuringSync[message.id] = message
        acknowledgePending(message)
        publishPending()
        _conversationState.update {
            it.copy(messages = it.messages.withMessageChange(withReadReceipt(message)))
        }
    }

    private fun connectPersonalMessages() {
        if (personalMessagesJob != null) return
        personalConnectionJob = viewModelScope.launch {
            personalMessagesSource.connected.collect { if (it) _inboxRevision.value++ }
        }

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

    private var blockedUserIds: Set<String> = emptySet()
    fun setBlockedUserIds(ids: Set<String>) { blockedUserIds = ids }

    private fun applyPersonalMessage(message: Message) {
        if (message.senderId in blockedUserIds && !message.deleted) return
        val dmId = message.dmId ?: return
        if (message.deleted || message.edited) {
            if (activeConversation == ConversationTarget.DirectMessage(dmId)) applyMessageChange(message)
            _inboxRevision.value++
            return
        }
        val isOwnMessage =
            (message.senderId != null && message.senderId == sessionIdentityProvider.currentUserId()) ||
                message.senderUsername == sessionIdentityProvider.currentUsername()
        val isOpenDm = visibleDmId == dmId && AppVisibility.isInForeground.value
        val isFirstDelivery = receivedMessageIds.add(message.id)
        if (receivedMessageIds.size > 256) receivedMessageIds.remove(receivedMessageIds.first())
        if (isFirstDelivery) {
            _dmSummaries.update { current ->
                val previous = current[dmId]
                val preview = message.content.takeIf(String::isNotBlank) ?: message.attachmentName ?: "Nuevo mensaje"
                val unread = if (isOpenDm) 0 else (previous?.unreadCount ?: 0) + if (isOwnMessage) 0 else 1
                val isLatest = previous == null || message.createdAt.orEmpty() >= previous.at.orEmpty()
                current + (dmId to DmSummary(if (isLatest) preview.take(180) else previous?.preview,
                    if (isLatest) message.createdAt else previous?.at, unread))
            }
        }

        if (isOpenDm) {
            appendMessage(message)
            if (!isOwnMessage) {
                viewModelScope.launch {
                    runCatching { chatRepository.markDmReadThrough(dmId, message.id) }
                }
            }
            return
        }

        if (!isOwnMessage) {
            _unreadDmIds.value = _unreadDmIds.value + dmId
            if (!AppVisibility.isInForeground.value && isFirstDelivery && _dmPreferences.value[dmId]?.muted != true) {
                val preview = message.content.takeIf { it.isNotBlank() }
                    ?: if (message.attachmentContentType?.startsWith("audio/") == true) "Nota de voz"
                    else message.attachmentName ?: "Nuevo mensaje"
                runCatching { notifications.show(DirectMessageNotification(message.id, dmId, message.senderUsername, preview.take(180))) }
            }
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
        changesDuringSync.clear()
        connectionCollectJob?.cancel()
        refreshRequested = false
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
                pendingMessages = emptyList(),
                isConnected = false,
                isSyncing = false,
                loadError = null,
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
