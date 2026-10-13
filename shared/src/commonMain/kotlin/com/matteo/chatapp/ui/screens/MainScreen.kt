package com.matteo.chatapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matteo.chatapp.model.DmConversation
import com.matteo.chatapp.platform.PlatformBackHandler
import com.matteo.chatapp.repository.SessionManager
import com.matteo.chatapp.repository.AppVisibility
import com.matteo.chatapp.ui.components.ChannelItem
import com.matteo.chatapp.ui.components.ServerItem
import com.matteo.chatapp.ui.theme.*
import com.matteo.chatapp.viewmodel.ChatViewModel
import com.matteo.chatapp.viewmodel.FriendViewModel
import com.matteo.chatapp.viewmodel.ServerViewModel
import kotlinx.coroutines.delay

sealed class MainDestination {
    object Friends : MainDestination()
    data class ServerChat(val serverId: String) : MainDestination()
    data class DirectMessage(val dm: DmConversation) : MainDestination()
}
@Composable
fun MainScreen(
    createServerViewModel: () -> ServerViewModel,
    createChatViewModel: () -> ChatViewModel,
    createFriendViewModel: () -> FriendViewModel,
    onLogout: () -> Unit,
    themeMode: ThemeMode,
    themeAccent: ThemeAccent,
    onThemeModeChange: (ThemeMode) -> Unit,
    onThemeAccentChange: (ThemeAccent) -> Unit,
    sessionKey: Int = 0
) {
    val serverViewModel: ServerViewModel = viewModel(key = "server-$sessionKey") {
        createServerViewModel()
    }
    val chatViewModel: ChatViewModel = viewModel(key = "chat-$sessionKey") {
        createChatViewModel()
    }
    val friendViewModel: FriendViewModel = viewModel(key = "friend-$sessionKey") {
        createFriendViewModel()
    }

    val serverState by serverViewModel.uiState.collectAsStateWithLifecycle()
    val servers = serverState.servers
    val channels = serverState.channels
    val serverMembers = serverState.members
    val serverInvitations = serverState.pendingInvitations
    val selectedServer = serverState.selectedServer
    val selectedChannel = serverState.selectedChannel
    val conversationState by chatViewModel.conversationState.collectAsStateWithLifecycle()
    val searchState by chatViewModel.searchState.collectAsStateWithLifecycle()
    val sharedItems by chatViewModel.sharedItems.collectAsStateWithLifecycle()
    val preferencesError by chatViewModel.preferencesError.collectAsStateWithLifecycle()
    val unreadDmIds by chatViewModel.unreadDmIds.collectAsStateWithLifecycle()
    val messages = conversationState.messages
    val typingUsers = conversationState.typingUsers
    val hasOlderMessages = conversationState.hasOlderMessages
    val isLoadingOlder = conversationState.isLoadingOlder
    val messageError = conversationState.error
    val isUploadingAttachment = conversationState.isUploadingAttachment
    val imageGallery by chatViewModel.imageGallery.collectAsStateWithLifecycle()
    val imageToView = imageGallery.anchor
    var forwardSource by remember { mutableStateOf<com.matteo.chatapp.model.Message?>(null) }
    var forwarding by remember { mutableStateOf(false) }
    var forwardError by remember { mutableStateOf<String?>(null) }
    val forwardSnackbar = remember { SnackbarHostState() }
    val openForward: (com.matteo.chatapp.model.Message) -> Unit = {
        forwardSource = it
        forwardError = null
        serverViewModel.loadAllChannelsForSearch()
    }
    var mediaNoticeId by remember { mutableStateOf<String?>(null) }
    var mediaError by remember { mutableStateOf<String?>(null) }
    var savingAttachment by remember { mutableStateOf(false) }
    var mediaNotice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(imageToView?.id) { mediaNotice = null }
    val mediaScope = rememberCoroutineScope()
    val downloadMedia: (com.matteo.chatapp.model.Message) -> Unit = { message ->
        if (!savingAttachment) {
            savingAttachment = true
            mediaNotice = null
            mediaNoticeId = message.id
            mediaScope.launch {
                try {
                    val fresh = chatViewModel.refreshAttachment(message)
                    val url = fresh.attachmentUrl ?: error("Este archivo ya no está disponible")
                    if (com.matteo.chatapp.platform.saveAttachment(url, fresh.attachmentName ?: "archivo")) mediaNotice = "Archivo guardado"
                } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (e: Exception) { mediaError = e.message ?: "No se pudo guardar el archivo" }
                finally { savingAttachment = false }
            }
        }
    }
    val shareMedia: (com.matteo.chatapp.model.Message) -> Unit = { message ->
        if (!savingAttachment) {
            savingAttachment = true
            mediaNotice = null
            mediaNoticeId = message.id
            mediaScope.launch {
                try {
                    val fresh = chatViewModel.refreshAttachment(message)
                    val url = fresh.attachmentUrl ?: error("Esta imagen ya no está disponible")
                    val result = com.matteo.chatapp.platform.shareImage(url, fresh.attachmentName ?: "imagen",
                        fresh.attachmentContentType ?: "image/jpeg")
                    if (result == com.matteo.chatapp.platform.ImageShareResult.COPIED) mediaNotice = "Imagen copiada. Puedes pegarla en otra aplicación."
                } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (e: Exception) { mediaError = e.message ?: "No se pudo compartir la imagen" }
                finally { savingAttachment = false }
            }
        }
    }
    val friendState by friendViewModel.uiState.collectAsStateWithLifecycle()
    val dmSummaries by chatViewModel.dmSummaries.collectAsStateWithLifecycle()
    val dmPreferences by chatViewModel.dmPreferences.collectAsStateWithLifecycle()
    val dms = com.matteo.chatapp.viewmodel.orderedConversations(friendState.dms, dmSummaries, dmPreferences)
    var showArchived by remember { mutableStateOf(false) }
    val listedDms = dms.filter { it.archived == showArchived }
    val selectedDm = friendState.selectedDm
    val profile = friendState.profile
    val viewedProfile = friendState.viewedProfile
    val friendError = friendState.error
    val serverError = serverState.error
    val friends = friendState.friends
    val allSearchChannels = serverState.searchChannels
    val messageSearchResults = searchState.results
    val isSearchingMessages = searchState.isSearching
    val messageSearchError = searchState.error
    val scrollTargetMessageId = searchState.scrollTargetMessageId
    val isAppInForeground by AppVisibility.isInForeground.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var currentDestination by remember { mutableStateOf<MainDestination>(MainDestination.Friends) }
    var showProfileSettings by remember { mutableStateOf(false) }
    var showCreateServer by remember { mutableStateOf(false) }
    var showServerInvitations by remember { mutableStateOf(false) }
    var showServerSettings by remember { mutableStateOf(false) }
    var showGeneralSettings by remember { mutableStateOf(false) }
    var showGlobalSearch by remember { mutableStateOf(false) }
    var showMessageSearch by remember { mutableStateOf(false) }
    var mobileSection by remember { mutableStateOf(MobileSection.FRIENDS) }
    var forceScrollChatKey by remember { mutableStateOf<String?>(null) }
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }
    val notificationDmId by com.matteo.chatapp.platform.NotificationNavigation.pendingDm.collectAsStateWithLifecycle()

    LaunchedEffect(notificationDmId, dms) {
        val dmId = notificationDmId ?: return@LaunchedEffect
        val dm = dms.firstOrNull { it.id == dmId } ?: return@LaunchedEffect
        mobileSection = MobileSection.MESSAGES
        friendViewModel.selectDm(dm)
        currentDestination = MainDestination.DirectMessage(dm)
        forceScrollChatKey = "dm:${dm.id}"
        chatViewModel.loadDmMessages(dm.id)
        com.matteo.chatapp.platform.NotificationNavigation.consume(dmId)
    }
    val activeChatKey = when (val destination = currentDestination) {
        is MainDestination.DirectMessage -> "dm:${destination.dm.id}"
        is MainDestination.ServerChat -> selectedChannel?.id?.let { "channel:$it" }
        MainDestination.Friends -> null
    }

    val composerDrafts by chatViewModel.composerDrafts.state.collectAsStateWithLifecycle()
    val composerDraft = activeChatKey?.let { composerDrafts[it] } ?: com.matteo.chatapp.viewmodel.ComposerDraft()
    val replyingTo = composerDraft.reply
    fun setReply(message: com.matteo.chatapp.model.Message?) {
        activeChatKey?.let { key -> chatViewModel.composerDrafts.update(key) { it.copy(reply = message) } }
    }
    val draftOwner = SessionManager.getEmail().orEmpty()
    val textDrafts = remember(draftOwner) { TextDrafts(draftOwner) }
    val messageInput = activeChatKey?.let { textDrafts.state(it).value }.orEmpty()
    fun updateDraft(text: String) {
        activeChatKey?.let { textDrafts.update(it, text) }
    }
    val inboxRevision by chatViewModel.inboxRevision.collectAsStateWithLifecycle()
    LaunchedEffect(inboxRevision, isAppInForeground) {
        if (isAppInForeground) {
            chatViewModel.refreshConversation()
            friendViewModel.loadAll()
        }
    }
    LaunchedEffect(dmSummaries.keys, friendState.dms.map { it.id }) {
        if (dmSummaries.keys.any { id -> friendState.dms.none { it.id == id } }) friendViewModel.loadAll()
    }

    LaunchedEffect(activeChatKey, selectedDm?.id, isAppInForeground) {
        val visibleDm = (currentDestination as? MainDestination.DirectMessage)
            ?.dm
            ?.id
            ?.takeIf { it == selectedDm?.id && isAppInForeground }
        chatViewModel.setVisibleDm(visibleDm)
    }

    LaunchedEffect(selectedChannel?.id) {
        selectedChannel?.let { chatViewModel.loadMessages(it.id) }
    }

    LaunchedEffect(friendState.blockedUsers) { chatViewModel.setBlockedUserIds(friendState.blockedUsers.map { it.id }.toSet()) }
    LaunchedEffect(friendState.dms) { chatViewModel.restoreDmSummaries(friendState.dms) }

    LaunchedEffect(serverViewModel) {
        serverViewModel.accessRevoked.collect { serverId ->
            val openServerId = (currentDestination as? MainDestination.ServerChat)?.serverId
            if (openServerId == serverId) {
                chatViewModel.clearMessages()
                currentDestination = MainDestination.Friends
                showServerSettings = false
            }
        }
    }

    val scrollDensity = androidx.compose.ui.platform.LocalDensity.current
    var wasNearConversationEnd by remember(activeChatKey) { mutableStateOf(true) }
    var programmaticScroll by remember(activeChatKey) { mutableStateOf(false) }
    var didPositionConversation by remember(activeChatKey) { mutableStateOf(false) }
    LaunchedEffect(activeChatKey, isAppInForeground, messages.lastOrNull()?.id) {
        val channelId = activeChatKey?.takeIf { it.startsWith("channel:") }?.removePrefix("channel:")
            ?: return@LaunchedEffect
        val lastMessage = messages.lastOrNull()?.takeIf { it.channelId == channelId } ?: return@LaunchedEffect
        while (isAppInForeground) {
            delay(600)
            val layout = listState.layoutInfo
            val last = layout.visibleItemsInfo.lastOrNull()
            if (didPositionConversation && !listState.isScrollInProgress &&
                isNearConversationEnd(last?.index, layout.totalItemsCount,
                    last?.let { it.offset + it.size + layout.afterContentPadding } ?: 0,
                    layout.viewportEndOffset, with(scrollDensity) { 2.dp.toPx() })) {
                serverViewModel.markChannelRead(channelId, lastMessage.id)
            }
            delay(1400)
        }
    }
    LaunchedEffect(isAppInForeground) { if (isAppInForeground) serverViewModel.refreshActivity() }
    val arrivals = remember(activeChatKey) { ConversationArrivals() }
    var newMessageCount by remember(activeChatKey) { mutableStateOf(0) }
    LaunchedEffect(activeChatKey, messages) {
        arrivals.update(messages, SessionManager.getUserId(), wasNearConversationEnd)
        newMessageCount = arrivals.count
    }
    LaunchedEffect(listState, activeChatKey, scrollDensity) {
        snapshotFlow {
            Triple(listState.layoutInfo, listState.isScrollInProgress,
                wasNearConversationEnd && !programmaticScroll && didPositionConversation)
        }.collect { (layout, scrolling, following) ->
            if (activeChatKey == null) return@collect
            val last = layout.visibleItemsInfo.lastOrNull()
            val nearEnd = isNearConversationEnd(last?.index, layout.totalItemsCount,
                last?.let { it.offset + it.size + layout.afterContentPadding } ?: 0,
                layout.viewportEndOffset, with(scrollDensity) { 2.dp.toPx() })
            if (nearEnd && !scrolling) {
                wasNearConversationEnd = true
                arrivals.clear(); newMessageCount = 0
            } else if (following && !scrolling && layout.totalItemsCount > 0) {
                // A decoded photo or a resized viewport can move the bottom without a new message.
                programmaticScroll = true
                try { listState.scrollToLatest(layout.totalItemsCount - 1, animated = false) }
                finally { programmaticScroll = false }
            }
        }
    }
    LaunchedEffect(activeChatKey, messages.lastOrNull()?.id, conversationState.pendingMessages.lastOrNull()?.id, forceScrollChatKey) {
        val shouldForceScroll = activeChatKey != null && forceScrollChatKey == activeChatKey
        if ((messages.isNotEmpty() || conversationState.pendingMessages.isNotEmpty()) &&
            (shouldForceScroll || (wasNearConversationEnd && !listState.isScrollInProgress))) {
            val dateSeparatorCount = messages.mapNotNull { it.createdAt?.take(10) }.distinct().size
            val targetIndex = (messages.size + dateSeparatorCount + conversationState.pendingMessages.size +
                (if (isLoadingOlder) 1 else 0) - 1).coerceAtLeast(0)
            programmaticScroll = true
            try { listState.scrollToLatest(targetIndex, animated = didPositionConversation) }
            finally { programmaticScroll = false }
            didPositionConversation = true
            wasNearConversationEnd = true
            arrivals.clear()
            newMessageCount = 0
            if (shouldForceScroll) forceScrollChatKey = null
        }
    }

    LaunchedEffect(scrollTargetMessageId, messages) {
        val targetId = scrollTargetMessageId ?: return@LaunchedEffect
        val targetIndex = messages.indexOfFirst { it.id == targetId }
        if (targetIndex >= 0) {
            val separators = messages.take(targetIndex + 1)
                .mapNotNull { it.createdAt?.take(10) }
                .distinct()
                .size
            wasNearConversationEnd = false
            listState.scrollToItem(targetIndex + separators)
            highlightedMessageId = targetId
            chatViewModel.consumeScrollTarget(targetId)
            delay(1800)
            if (highlightedMessageId == targetId) highlightedMessageId = null
        }
    }

    val openGlobalSearch = {
        serverViewModel.loadAllChannelsForSearch()
        showGlobalSearch = true
    }
    val openMessageSearch = {
        chatViewModel.clearMessageSearch()
        showMessageSearch = true
    }

    fun buildChatAreaState(
        destinationKey: String,
        title: String,
        subtitle: String,
        typingKey: String,
        avatarUrl: String? = null,
        online: Boolean? = null
    ) = ChatAreaState(
        destinationKey = destinationKey,
        newMessageCount = newMessageCount,
        title = title,
        subtitle = subtitle,
        messages = messages,
        hasOlderMessages = hasOlderMessages,
        isLoadingOlder = isLoadingOlder,
        messageError = messageError,
        replyingTo = replyingTo,
        draft = composerDraft,
        mentionMembers = if (destinationKey.startsWith("channel:")) serverMembers else emptyList(),
        messageInput = messageInput,
        isUploadingAttachment = isUploadingAttachment,
        pendingMessages = conversationState.pendingMessages,
        ownAvatarUrl = profile?.avatarUrl,
        isLoading = conversationState.isLoading,
        isConnected = conversationState.isConnected,
        isSyncing = conversationState.isSyncing,
        loadError = conversationState.loadError,
        avatarUrl = avatarUrl,
        online = online,
        typingText = typingUsers[typingKey]?.let { "$it esta escribiendo..." },
        highlightedMessageId = highlightedMessageId,
        canMessage = if (destinationKey.startsWith("dm:")) dms.find { it.id == destinationKey.removePrefix("dm:") }?.canMessage ?: true else true
    )

    val latestActiveChatKey by rememberUpdatedState(activeChatKey)
    fun clearSentDraft(submission: DraftSubmission?, replyId: String?) {
        if (submission == null) return
        textDrafts.clearIfUnchanged(submission)
        chatViewModel.composerDrafts.update(submission.conversation) { if (it.reply?.id == replyId) it.copy(reply = null) else it }
        if (latestActiveChatKey == submission.conversation) forceScrollChatKey = submission.conversation
    }

    fun buildChatAreaActions(
        onTyping: (String) -> Unit,
        onSendText: (String, String?, () -> Unit) -> Unit,
        onBack: (() -> Unit)? = null,
        onProfileClick: (() -> Unit)? = null
    ) = ChatAreaActions(
        onScrollToLatest = { wasNearConversationEnd = true; forceScrollChatKey = activeChatKey },
        onUserScroll = { wasNearConversationEnd = false },
        onOpenImage = chatViewModel::openImageGallery,
        onDownload = downloadMedia,
        onForward = openForward,
        onSendLocation = { onSendText(it, null, {}) },
        onLoadOlder = chatViewModel::loadOlderMessages,
        onRetryMessage = chatViewModel::retryMessage,
        onDiscardMessage = chatViewModel::discardPendingMessage,
        onRefresh = chatViewModel::refreshConversation,
        onEditMessage = chatViewModel::editMessage,
        onDeleteMessage = chatViewModel::deleteMessage,
        onDraftChange = { change -> activeChatKey?.let { chatViewModel.composerDrafts.update(it, change) } ?: false },
        onReplyMessage = { setReply(it) },
        onCancelReply = { setReply(null) },
        onInputChange = { text ->
            updateDraft(text.take(4000))
            onTyping(text)
        },
        onSendPhotos = { files, comment, result ->
            val submission = activeChatKey?.let { textDrafts.capture(it) }
            val reply = replyingTo?.id
            chatViewModel.sendPhotos(files, comment, reply) { queued ->
                if (queued) clearSentDraft(submission, reply)
                result(queued)
            }
        },
        onSendAttachment = { file, clearSelection ->
            val submission = activeChatKey?.let { textDrafts.capture(it) }
            val reply = replyingTo?.id
            chatViewModel.sendAttachment(file, submission?.text.orEmpty(), reply) {
                clearSentDraft(submission, reply)
                clearSelection()
            }
        },
        onSend = {
            val submission = activeChatKey?.let { textDrafts.capture(it) }
            val content = submission?.text.orEmpty()
            if (content.isNotBlank()) {
                val reply = replyingTo?.id
                onSendText(content, reply) { clearSentDraft(submission, reply) }
            }
        },
        onBack = onBack,
        onSearch = openMessageSearch,
        onProfileClick = onProfileClick
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Background).windowInsetsPadding(WindowInsets.safeDrawing)
        .onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.isCtrlPressed && !event.isAltPressed && !event.isMetaPressed) {
                when (event.key) {
                    Key.F -> if (activeChatKey != null) { openMessageSearch(); true } else false
                    Key.K -> { openGlobalSearch(); true }
                    Key.Comma -> { showGeneralSettings = true; true }
                    else -> false
                }
            } else false
        }) {
    val isMobile = maxWidth < 840.dp
    val sidebarWidth = (maxWidth * 0.22f).coerceIn(232.dp, 280.dp)
    val navigationKey = activeChatKey ?: "${mobileSection}:${selectedServer?.id}:${currentDestination}"
    PlatformBackHandler(
        enabled = isMobile && (
            currentDestination is MainDestination.DirectMessage ||
                selectedChannel != null ||
                selectedServer != null ||
                mobileSection != MobileSection.FRIENDS
            )
    ) {
        when {
            currentDestination is MainDestination.DirectMessage -> {
                friendViewModel.clearSelectedDm()
                currentDestination = MainDestination.Friends
                chatViewModel.clearMessages()
            }
            selectedChannel != null -> {
                serverViewModel.clearSelectedChannel()
                chatViewModel.clearMessages()
            }
            selectedServer != null -> {
                serverViewModel.clearSelection()
                currentDestination = MainDestination.Friends
                chatViewModel.clearMessages()
            }
            mobileSection != MobileSection.FRIENDS -> {
                mobileSection = MobileSection.FRIENDS
                currentDestination = MainDestination.Friends
                friendViewModel.clearSelectedDm()
                chatViewModel.clearMessages()
            }
        }
    }
    if (isMobile) {
        val openDm = selectedDm.takeIf {
            currentDestination is MainDestination.DirectMessage
        }
        val openChannel = selectedChannel.takeIf {
            currentDestination is MainDestination.ServerChat
        }
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().then(contentEntrance(navigationKey))) {
                when {
                    openDm != null -> {
                        val dm = openDm
                        ChatArea(
                            state = buildChatAreaState(
                                destinationKey = "dm:${dm.id}",
                                title = dm.otherUsername,
                                subtitle = if (dm.otherIsOnline) "En linea" else "Desconectado",
                                typingKey = dm.id,
                                avatarUrl = dm.otherAvatar,
                                online = dm.otherIsOnline
                            ),
                            listState = listState,
                            actions = buildChatAreaActions(
                                onTyping = { text ->
                                    chatViewModel.onMessageInputChanged(text, dmId = dm.id)
                                },
                                onSendText = { content, replyToId, onSent ->
                                    chatViewModel.sendDmMessage(content, dm.id, replyToId, onSent)
                                },
                                onBack = {
                                    friendViewModel.clearSelectedDm()
                                    currentDestination = MainDestination.Friends
                                },
                                onProfileClick = { friendViewModel.loadUserProfile(dm.otherUserId) }
                            )
                        )
                    }
                    openChannel != null -> {
                        val channel = openChannel
                        ChatArea(
                            state = buildChatAreaState(
                                destinationKey = "channel:${channel.id}",
                                title = "# ${channel.name}",
                                subtitle = selectedServer?.name ?: "Servidor",
                                typingKey = channel.id
                            ),
                            listState = listState,
                            actions = buildChatAreaActions(
                                onTyping = { text ->
                                    chatViewModel.onMessageInputChanged(text, channelId = channel.id)
                                },
                                onSendText = { content, replyToId, onSent ->
                                    chatViewModel.sendMessage(content, replyToId, onSent)
                                },
                                onBack = { serverViewModel.clearSelectedChannel() },
                            )
                        )
                    }
                    mobileSection == MobileSection.FRIENDS -> Column(Modifier.fillMaxSize()) {
                        MobileTopBar(
                            title = "Chimera",
                            onSearch = openGlobalSearch,
                            onProfile = { showProfileSettings = true },
                            onAppearance = { showGeneralSettings = true },
                            onLogout = {
                                chatViewModel.disconnectRealtime()
                                friendViewModel.logout(onLogout)
                            }
                        )
                        FriendsScreen(
                            onOpenDm = { dm ->
                                mobileSection = MobileSection.MESSAGES
                                friendViewModel.selectDm(dm)
                                currentDestination = MainDestination.DirectMessage(dm)
                                forceScrollChatKey = "dm:${dm.id}"
                                chatViewModel.loadDmMessages(dm.id)
                            },
                            viewModel = friendViewModel,
                            conversations = dms,
                            onPreferences = { dm, value -> chatViewModel.updateDmPreferences(dm.id, value) }
                        )
                    }
                    mobileSection == MobileSection.MESSAGES -> Column(Modifier.fillMaxSize()) {
                        MobileTopBar(
                            title = "Mensajes",
                            onSearch = openGlobalSearch,
                            onProfile = { showProfileSettings = true },
                            onAppearance = { showGeneralSettings = true },
                            onLogout = {
                                chatViewModel.disconnectRealtime()
                                friendViewModel.logout(onLogout)
                            }
                        )
                        TextButton(onClick = { showArchived = !showArchived }) {
                            Text(if (showArchived) "Volver a mensajes" else "Archivados (${dms.count { it.archived }})")
                        }
                        if (listedDms.isEmpty()) {
                            EmptyState("No hay conversaciones")
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp)) {
                                items(listedDms, key = { it.id }) { dm ->
                                    DirectMessageRow(
                                        dm = dm,
                                        selected = false,
                                        hasUnread = unreadDmIds.contains(dm.id),
                                        onProfile = { friendViewModel.loadUserProfile(dm.otherUserId) },
                                        onPreferences = { chatViewModel.updateDmPreferences(dm.id, it) },
                                        onClick = {
                                            friendViewModel.selectDm(dm)
                                            currentDestination = MainDestination.DirectMessage(dm)
                                            forceScrollChatKey = "dm:${dm.id}"
                                            chatViewModel.loadDmMessages(dm.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    else -> Column(Modifier.fillMaxSize()) {
                        MobileTopBar(
                            title = if (selectedServer == null) "Servidores" else "Canales",
                            onSearch = openGlobalSearch,
                            onBack = selectedServer?.let {
                                {
                                    serverViewModel.clearSelection()
                                    currentDestination = MainDestination.Friends
                                }
                            },
                            onProfile = { showProfileSettings = true },
                            onAppearance = { showGeneralSettings = true },
                            onLogout = {
                                chatViewModel.disconnectRealtime()
                                friendViewModel.logout(onLogout)
                            },
                            actions = {
                                if (selectedServer == null) {
                                    IconButton(onClick = {
                                        serverViewModel.loadPendingInvitations()
                                        serverViewModel.clearFeedback(); showServerInvitations = true
                                    }) {
                                        BadgedBox(badge = {
                                            if (serverInvitations.isNotEmpty()) Badge { Text(serverInvitations.size.toString()) }
                                        }) {
                                            Icon(Icons.Rounded.MailOutline, contentDescription = "Invitaciones", tint = OnSurface)
                                        }
                                    }
                                    IconButton(onClick = { serverViewModel.clearFeedback(); showCreateServer = true }) {
                                        Icon(Icons.Rounded.Add, contentDescription = "Crear servidor", tint = Primary)
                                    }
                                }
                            }
                        )
                        if (selectedServer == null) {
                            ServerDirectory(servers, serverState.isLoading, serverState.serversError,
                                onRetry = serverViewModel::loadServers,
                                onCreate = { serverViewModel.clearFeedback(); showCreateServer = true },
                                onOpen = { server ->
                                    serverViewModel.selectServer(server)
                                    currentDestination = MainDestination.ServerChat(server.id)
                                    chatViewModel.clearMessages()
                                })
                        } else {
                            ServerChannelBrowser(selectedServer, channels, selectedChannel?.id,
                                serverState.isLoadingChannels, serverState.channelsError,
                                onRetry = serverViewModel::reloadSelectedServer,
                                onSettings = { serverViewModel.clearFeedback(); showServerSettings = true },
                                onOpenChannel = { channel ->
                                    forceScrollChatKey = "channel:${channel.id}"
                                    serverViewModel.selectChannel(channel)
                                })
                        }
                    }
                }
            }
            if (openDm == null && openChannel == null) {
                MobileBottomNavigation(
                    selected = mobileSection,
                    hasUnreadMessages = unreadDmIds.isNotEmpty(),
                    hasUnreadServers = servers.any { it.unreadCount > 0 },
                    onSelect = { section ->
                        mobileSection = section
                        when (section) {
                            MobileSection.FRIENDS -> {
                                currentDestination = MainDestination.Friends
                                friendViewModel.clearSelectedDm()
                                serverViewModel.clearSelection()
                            }
                            MobileSection.MESSAGES -> {
                                currentDestination = MainDestination.Friends
                                friendViewModel.clearSelectedDm()
                            }
                            MobileSection.SERVERS -> {
                                currentDestination = selectedServer?.let { MainDestination.ServerChat(it.id) }
                                    ?: MainDestination.Friends
                            }
                        }
                        chatViewModel.clearMessages()
                    }
                )
            }
        }
    } else {
    Row(modifier = Modifier.fillMaxSize().background(Navigation).padding(top = 12.dp, end = 12.dp, bottom = 12.dp)) {
        Column(
            modifier = Modifier
                .width(72.dp)
                .fillMaxHeight()
                .background(Navigation)
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val isFriends = currentDestination is MainDestination.Friends
            RailAction(
                icon = Icons.Rounded.ChatBubbleOutline,
                contentDescription = "Mensajes y amigos",
                selected = isFriends,
                onClick = {
                    currentDestination = MainDestination.Friends
                    serverViewModel.clearSelection()
                    friendViewModel.clearSelectedDm()
                    chatViewModel.clearMessages()
                }
            )

            RailAction(
                icon = Icons.Rounded.Search,
                contentDescription = "Buscar",
                selected = false,
                onClick = openGlobalSearch
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Outline, modifier = Modifier.width(32.dp))
            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    RailAction(
                        icon = Icons.Rounded.MailOutline,
                        contentDescription = "Invitaciones a servidores",
                        badge = serverInvitations.size.takeIf { it > 0 }?.toString(),
                        selected = false,
                        onClick = {
                            serverViewModel.loadPendingInvitations()
                            serverViewModel.clearFeedback(); showServerInvitations = true
                        }
                    )
                }
                items(servers, key = { it.id }) { server ->
                    ServerItem(
                        server = server,
                        isSelected = selectedServer?.id == server.id,
                        onClick = {
                            serverViewModel.selectServer(server)
                            currentDestination = MainDestination.ServerChat(server.id)
                            friendViewModel.clearSelectedDm()
                            chatViewModel.clearMessages()
                        }
                    )
                }
            }

            RailAction(
                icon = Icons.Rounded.Add,
                contentDescription = "Crear servidor",
                selected = false,
                onClick = { serverViewModel.clearFeedback(); showCreateServer = true }
            )
        }

        Column(
            modifier = Modifier
                .width(sidebarWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                .background(Surface)
        ) {
            when (currentDestination) {
                is MainDestination.Friends, is MainDestination.DirectMessage -> {
                    SidebarHeader(title = "Mensajes directos", subtitle = "${dms.size} conversaciones")
                    TextButton(onClick = { showArchived = !showArchived }) {
                        Text(if (showArchived) "Volver a mensajes" else "Archivados (${dms.count { it.archived }})")
                    }
                    LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        items(listedDms, key = { it.id }) { dm ->
                            DirectMessageRow(
                                dm = dm,
                                selected = selectedDm?.id == dm.id,
                                hasUnread = unreadDmIds.contains(dm.id),
                                onProfile = { friendViewModel.loadUserProfile(dm.otherUserId) },
                                        onPreferences = { chatViewModel.updateDmPreferences(dm.id, it) },
                                onClick = {
                                    friendViewModel.selectDm(dm)
                                    currentDestination = MainDestination.DirectMessage(dm)
                                    forceScrollChatKey = "dm:${dm.id}"
                                    chatViewModel.loadDmMessages(dm.id)
                                }
                            )
                        }
                    }
                }

                is MainDestination.ServerChat -> {
                    selectedServer?.let { server ->
                        ServerChannelBrowser(server, channels, selectedChannel?.id,
                            serverState.isLoadingChannels, serverState.channelsError,
                            onRetry = serverViewModel::reloadSelectedServer,
                            onSettings = { serverViewModel.clearFeedback(); showServerSettings = true },
                            onOpenChannel = { channel ->
                                forceScrollChatKey = "channel:${channel.id}"
                                serverViewModel.selectChannel(channel)
                            }, modifier = Modifier.weight(1f))
                    }
                }
            }

            UserFooter(
                username = profile?.username ?: SessionManager.getUsername().orEmpty(),
                avatarUrl = profile?.avatarUrl,
                status = profile?.status ?: "Disponible",
                onSettings = { showProfileSettings = true },
                onAppearanceSettings = { showGeneralSettings = true },
                onLogout = {
                    chatViewModel.disconnectRealtime()
                    friendViewModel.logout(onLogout)
                }
            )
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp))
                .background(Background).then(contentEntrance(navigationKey))
        ) {
            when (currentDestination) {
                is MainDestination.Friends -> {
                    FriendsScreen(
                        onOpenDm = { dm ->
                            friendViewModel.selectDm(dm)
                            currentDestination = MainDestination.DirectMessage(dm)
                            forceScrollChatKey = "dm:${dm.id}"
                            chatViewModel.loadDmMessages(dm.id)
                        },
                        viewModel = friendViewModel,
                            conversations = dms,
                            onPreferences = { dm, value -> chatViewModel.updateDmPreferences(dm.id, value) }
                    )
                }

                is MainDestination.DirectMessage -> {
                    selectedDm?.let { dm ->
                        ChatArea(
                            state = buildChatAreaState(
                                destinationKey = "dm:${dm.id}",
                                title = dm.otherUsername,
                                subtitle = if (dm.otherIsOnline) "En linea" else "Desconectado",
                                typingKey = dm.id,
                                avatarUrl = dm.otherAvatar,
                                online = dm.otherIsOnline
                            ),
                            listState = listState,
                            actions = buildChatAreaActions(
                                onTyping = { text ->
                                    chatViewModel.onMessageInputChanged(text, dmId = dm.id)
                                },
                                onSendText = { content, replyToId, onSent ->
                                    chatViewModel.sendDmMessage(content, dm.id, replyToId, onSent)
                                },
                                onProfileClick = { friendViewModel.loadUserProfile(dm.otherUserId) }
                            )
                        )
                    }
                }

                is MainDestination.ServerChat -> {
                    selectedChannel?.let { channel ->
                        ChatArea(
                            state = buildChatAreaState(
                                destinationKey = "channel:${channel.id}",
                                title = "# ${channel.name}",
                                subtitle = selectedServer?.name ?: "Servidor",
                                typingKey = channel.id
                            ),
                            listState = listState,
                            actions = buildChatAreaActions(
                                onTyping = { text ->
                                    chatViewModel.onMessageInputChanged(text, channelId = channel.id)
                                },
                                onSendText = { content, replyToId, onSent ->
                                    chatViewModel.sendMessage(content, replyToId, onSent)
                                },
                            )
                        )
                    } ?: selectedServer?.let { server ->
                        ServerWelcome(server, channels, serverMembers.size, serverState.isLoadingChannels,
                            onSettings = { serverViewModel.clearFeedback(); showServerSettings = true },
                            onOpen = { channel ->
                                forceScrollChatKey = "channel:${channel.id}"
                                serverViewModel.selectChannel(channel)
                            })
                    } ?: EmptyState("Selecciona un servidor")
                }
            }
        }
    }
    }

        SnackbarHost(forwardSnackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    var showPasswordChanged by remember { mutableStateOf(false) }
    preferencesError?.let { error ->
        AlertDialog(onDismissRequest = chatViewModel::clearPreferencesError,
            title = { Text("Opciones de conversación") }, text = { Text(error) },
            confirmButton = { TextButton(onClick = chatViewModel::clearPreferencesError) { Text("Aceptar") } })
    }
    if (showPasswordChanged) {
        AlertDialog(
            onDismissRequest = onLogout,
            title = { Text("Contraseña actualizada") },
            text = { Text("Tu nueva contraseña se ha guardado. Inicia sesión de nuevo para continuar.") },
            confirmButton = { TextButton(onClick = onLogout) { Text("Ir al inicio de sesión") } }
        )
    }
    if (showProfileSettings) {
        ProfileSettingsDialog(
            username = profile?.username ?: SessionManager.getUsername().orEmpty(),
            avatarUrl = profile?.avatarUrl.orEmpty(),
            bannerUrl = profile?.profileBannerUrl.orEmpty(),
            status = profile?.status.orEmpty(),
            bio = profile?.bio.orEmpty(),
            email = profile?.email ?: SessionManager.getEmail().orEmpty(),
            error = friendError,
            passwordError = friendState.passwordError,
            changingPassword = friendState.changingPassword,
            savingProfile = friendState.savingProfile,
            onDismiss = { showProfileSettings = false },
            onSave = { username, status, bio, avatarFile, bannerFile ->
                friendViewModel.saveProfile(username, status, bio, avatarFile, bannerFile) {
                    showProfileSettings = false
                }
            },
            onChangePassword = { currentPassword, newPassword ->
                friendViewModel.changePassword(currentPassword, newPassword) {
                    showProfileSettings = false
                    chatViewModel.disconnectRealtime()
                    friendViewModel.disconnectRealtime()
                    showPasswordChanged = true
                }
            }
        )
    }

    if (showGeneralSettings) {
        GeneralSettingsDialog(
            themeMode = themeMode,
            themeAccent = themeAccent,
            onThemeModeChange = onThemeModeChange,
            onThemeAccentChange = onThemeAccentChange,
            onDismiss = { showGeneralSettings = false }
        )
    }

    if (showCreateServer) {
        CreateServerDialog(
            error = serverError,
            busy = serverState.action != null,
            onDismiss = { showCreateServer = false },
            onCreate = { name, description, iconFile ->
                serverViewModel.createServer(name, description, iconFile) {
                    showCreateServer = false
                    currentDestination = serverViewModel.uiState.value.selectedServer
                        ?.let { MainDestination.ServerChat(it.id) }
                        ?: MainDestination.Friends
                }
            }
        )
    }

    if (showServerInvitations) {
        ServerInvitationsDialog(
            invitations = serverInvitations,
            error = serverError,
            busy = serverState.action != null,
            loading = serverState.isLoadingInvitations,
            onRetry = serverViewModel::loadPendingInvitations,
            onDismiss = { showServerInvitations = false },
            onAccept = { invitationId ->
                serverViewModel.acceptInvitation(invitationId) { server ->
                    showServerInvitations = false
                    currentDestination = MainDestination.ServerChat(server.id)
                }
            },
            onReject = { invitationId ->
                serverViewModel.rejectInvitation(invitationId)
            }
        )
    }

    if (showServerSettings) {
        selectedServer?.let { server ->
            ServerSettingsDialog(
                server = server,
                serverName = server.name,
                description = server.description.orEmpty(),
                members = serverMembers,
                channels = channels,
                error = serverError,
                notice = serverState.notice,
                action = serverState.action,
                loadingMembers = serverState.isLoadingMembers,
                loadingChannels = serverState.isLoadingChannels,
                channelsError = serverState.channelsError,
                onReload = serverViewModel::reloadSelectedServer,
                onOpenChannel = { channel ->
                    showServerSettings = false
                    forceScrollChatKey = "channel:${channel.id}"
                    serverViewModel.selectChannel(channel)
                },
                onDismiss = { showServerSettings = false },
                onSave = { name, description, iconFile, onSaved ->
                    serverViewModel.updateSelectedServer(name, description, iconFile, onSaved)
                },
                onCreateChannel = { channelName, onCreated ->
                    serverViewModel.createChannel(channelName, onCreated)
                },
                onInvite = { username, onInvited ->
                    serverViewModel.inviteMember(username, onInvited)
                },
                onRoleChange = { userId, role ->
                    serverViewModel.updateMemberRole(userId, role)
                },
                onRemoveMember = { userId ->
                    serverViewModel.removeMember(userId)
                },
                onLeave = {
                    serverViewModel.leaveSelectedServer {
                        showServerSettings = false
                        currentDestination = MainDestination.Friends
                        chatViewModel.clearMessages()
                    }
                },
                onDelete = {
                    serverViewModel.deleteSelectedServer {
                        showServerSettings = false
                        currentDestination = MainDestination.Friends
                        chatViewModel.clearMessages()
                    }
                }
            )
        }
    }

    if (showGlobalSearch) {
        GlobalSearchDialog(
            friends = friends,
            dms = dms,
            servers = servers,
            channels = allSearchChannels,
            onDismiss = { showGlobalSearch = false },
            onFriend = { userId ->
                showGlobalSearch = false
                friendViewModel.loadUserProfile(userId)
            },
            onDm = { dm ->
                showGlobalSearch = false
                mobileSection = MobileSection.MESSAGES
                friendViewModel.selectDm(dm)
                currentDestination = MainDestination.DirectMessage(dm)
                forceScrollChatKey = "dm:${dm.id}"
                chatViewModel.loadDmMessages(dm.id)
            },
            onServer = { server ->
                showGlobalSearch = false
                mobileSection = MobileSection.SERVERS
                serverViewModel.selectServer(server)
                currentDestination = MainDestination.ServerChat(server.id)
                friendViewModel.clearSelectedDm()
                chatViewModel.clearMessages()
            },
            onChannel = { channel ->
                val server = servers.firstOrNull { it.id == channel.serverId } ?: return@GlobalSearchDialog
                showGlobalSearch = false
                mobileSection = MobileSection.SERVERS
                serverViewModel.selectServer(server)
                serverViewModel.selectChannel(channel)
                currentDestination = MainDestination.ServerChat(server.id)
                friendViewModel.clearSelectedDm()
                forceScrollChatKey = "channel:${channel.id}"
            }
        )
    }

    if (showMessageSearch) {
        MessageSearchDialog(
            results = messageSearchResults,
            isSearching = isSearchingMessages,
            error = messageSearchError,
            onDismiss = {
                showMessageSearch = false
                chatViewModel.clearMessageSearch()
            },
            onSearch = chatViewModel::searchCurrentMessages,
            onOpenResult = { messageId ->
                showMessageSearch = false
                chatViewModel.openSearchResult(messageId)
            }
        )
    }

    viewedProfile?.let { user ->
        val sharedDm = dms.firstOrNull { it.otherUserId == user.id }
        PublicProfileDialog(
            profile = user,
            onDismiss = { friendViewModel.clearViewedProfile() },
            onSharedItems = sharedDm?.let { dm -> { chatViewModel.openSharedItems(dm.id) } },
            isFriend = friends.any { it.friendId == user.id },
            blocked = friendState.blockedUsers.any { it.id == user.id },
            canManage = user.id != profile?.id,
            busy = friendState.contactActionLoading,
            actionError = friendError,
            onRemoveFriend = { friendViewModel.removeFriend(user.id) },
            onBlock = { friendViewModel.blockUser(user.id) },
            onUnblock = { friendViewModel.unblockUser(user.id) }
        )
    }
    sharedItems.dmId?.let { dmId ->
        SharedItemsDialog(sharedItems, onTab = { chatViewModel.openSharedItems(dmId, it) },
            onMore = chatViewModel::loadMoreSharedItems, onDismiss = chatViewModel::closeSharedItems,
            onOpenImage = chatViewModel::openImageGallery, onDownload = downloadMedia)
    }
    imageToView?.let { image -> com.matteo.chatapp.ui.components.ImageViewerDialog(image,
        refresh = chatViewModel::refreshAttachment, onDownload = downloadMedia, onShare = shareMedia,
        onDismiss = chatViewModel::closeImageGallery, isWorking = savingAttachment, notice = mediaNotice,
        noticeMessageId = mediaNoticeId, images = imageGallery.images, hasMore = imageGallery.hasMore,
        loadingMore = imageGallery.loading, galleryError = imageGallery.error, onMore = chatViewModel::loadMoreGalleryImages,
        onForward = openForward) }
    forwardSource?.let { source ->
        val targets = forwardingTargets(source, dms.filter { dm -> friendState.blockedUsers.none { it.id == dm.otherUserId } },
            allSearchChannels + channels, servers.associate { it.id to it.name })
        ForwardMessageDialog(source, targets, forwarding, forwardError, onDismiss = { forwardSource = null },
            onSend = { choice ->
                forwarding = true
                forwardError = null
                chatViewModel.forwardMessage(source, choice.target) { error ->
                    forwarding = false
                    if (error != null) forwardError = error else {
                        forwardSource = null
                        if (imageToView != null) {
                            mediaNoticeId = source.id
                            mediaNotice = "Reenvío preparado para ${choice.name}"
                        } else mediaScope.launch { forwardSnackbar.showSnackbar("Reenvío preparado para ${choice.name}") }
                    }
                }
            })
    }
    mediaError?.let { error -> AlertDialog(onDismissRequest = { mediaError = null }, title = { Text("No se pudo completar la acción") },
        text = { Text(error) }, confirmButton = { TextButton(onClick = { mediaError = null }) { Text("Aceptar") } }) }

}
