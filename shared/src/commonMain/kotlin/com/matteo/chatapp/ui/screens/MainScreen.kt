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
    var imageToView by remember { mutableStateOf<com.matteo.chatapp.model.Message?>(null) }
    var mediaError by remember { mutableStateOf<String?>(null) }
    var savingAttachment by remember { mutableStateOf(false) }
    var mediaNotice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(imageToView?.id) { mediaNotice = null }
    val mediaScope = rememberCoroutineScope()
    val downloadMedia: (com.matteo.chatapp.model.Message) -> Unit = { message ->
        if (!savingAttachment) {
            savingAttachment = true
            mediaNotice = null
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

    var replyingTo by remember { mutableStateOf<com.matteo.chatapp.model.Message?>(null) }
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

    val draftOwner = SessionManager.getEmail().orEmpty()
    var messageInput by remember(draftOwner, activeChatKey) {
        mutableStateOf(activeChatKey?.let { com.matteo.chatapp.repository.DraftStorage.load(draftOwner, it) }.orEmpty())
    }
    fun updateDraft(text: String) {
        messageInput = text
        activeChatKey?.let { com.matteo.chatapp.repository.DraftStorage.save(draftOwner, it, text) }
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
        replyingTo = null
        val visibleDm = (currentDestination as? MainDestination.DirectMessage)
            ?.dm
            ?.id
            ?.takeIf { it == selectedDm?.id && isAppInForeground }
        chatViewModel.setVisibleDm(visibleDm)
    }

    LaunchedEffect(selectedChannel) {
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

    LaunchedEffect(messages.size, conversationState.pendingMessages.size) {
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        val totalItems = listState.layoutInfo.totalItemsCount
        val isNearBottom = totalItems == 0 || lastVisibleIndex >= totalItems - 4
        val shouldForceScroll = activeChatKey != null && forceScrollChatKey == activeChatKey
        if ((messages.isNotEmpty() || conversationState.pendingMessages.isNotEmpty()) &&
            (shouldForceScroll || isNearBottom || conversationState.pendingMessages.any { it.status == com.matteo.chatapp.viewmodel.SendStatus.SENDING })) {
            val dateSeparatorCount = messages.mapNotNull { it.createdAt?.take(10) }.distinct().size
            val targetIndex = (messages.size + dateSeparatorCount + conversationState.pendingMessages.size - 1).coerceAtLeast(0)
            listState.animateScrollToItem(targetIndex)
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
        title = title,
        subtitle = subtitle,
        messages = messages,
        hasOlderMessages = hasOlderMessages,
        isLoadingOlder = isLoadingOlder,
        messageError = messageError,
        replyingTo = replyingTo,
        messageInput = messageInput,
        isUploadingAttachment = isUploadingAttachment,
        pendingMessages = conversationState.pendingMessages,
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

    fun buildChatAreaActions(
        onTyping: (String) -> Unit,
        onSendText: (String, String?, () -> Unit) -> Unit,
        onBack: (() -> Unit)? = null,
        onProfileClick: (() -> Unit)? = null
    ) = ChatAreaActions(
        onOpenImage = { imageToView = it },
        onDownload = downloadMedia,
        onSendLocation = { onSendText(it, null, {}) },
        onLoadOlder = chatViewModel::loadOlderMessages,
        onRetryMessage = chatViewModel::retryMessage,
        onDiscardMessage = chatViewModel::discardPendingMessage,
        onRefresh = chatViewModel::refreshConversation,
        onEditMessage = chatViewModel::editMessage,
        onDeleteMessage = chatViewModel::deleteMessage,
        onReplyMessage = { replyingTo = it },
        onCancelReply = { replyingTo = null },
        onInputChange = { text ->
            updateDraft(text.take(4000))
            onTyping(text)
        },
        onSendPhotos = { files, comment, result ->
            val draft = messageInput
            val reply = replyingTo?.id
            val chatKey = activeChatKey
            chatViewModel.sendPhotos(files, comment, reply) { queued ->
                if (queued && latestActiveChatKey == chatKey) {
                    if (messageInput == draft) updateDraft("")
                    if (replyingTo?.id == reply) replyingTo = null
                }
                result(queued)
            }
        },
        onSendAttachment = { file, clearSelection ->
            val content = messageInput
            val reply = replyingTo?.id
            chatViewModel.sendAttachment(file, content, reply) {
                if (messageInput == content) updateDraft("")
                if (replyingTo?.id == reply) replyingTo = null
                clearSelection()
            }
        },
        onSend = {
            val content = messageInput
            if (content.isNotBlank()) {
                val reply = replyingTo?.id
                onSendText(content, reply) {
                    if (messageInput == content) updateDraft("")
                    if (replyingTo?.id == reply) replyingTo = null
                }
            }
        },
        onBack = onBack,
        onSearch = openMessageSearch,
        onProfileClick = onProfileClick
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Background).windowInsetsPadding(WindowInsets.safeDrawing)) {
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
                            viewModel = friendViewModel
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
                            title = selectedServer?.name ?: "Servidores",
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
                                if (selectedServer != null) {
                                    IconButton(onClick = { showServerSettings = true }) {
                                        Icon(Icons.Rounded.Settings, contentDescription = "Ajustes del servidor", tint = OnSurface)
                                    }
                                } else {
                                    IconButton(onClick = {
                                        serverViewModel.loadPendingInvitations()
                                        showServerInvitations = true
                                    }) {
                                        BadgedBox(badge = {
                                            if (serverInvitations.isNotEmpty()) Badge { Text(serverInvitations.size.toString()) }
                                        }) {
                                            Icon(Icons.Rounded.MailOutline, contentDescription = "Invitaciones", tint = OnSurface)
                                        }
                                    }
                                    IconButton(onClick = { showCreateServer = true }) {
                                        Icon(Icons.Rounded.Add, contentDescription = "Crear servidor", tint = Primary)
                                    }
                                }
                            }
                        )
                        if (selectedServer == null) {
                            if (servers.isEmpty()) EmptyState("Aun no tienes servidores")
                            else LazyColumn(modifier = Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(servers, key = { it.id }) { server ->
                                    MobileServerRow(server = server) {
                                        serverViewModel.selectServer(server)
                                        currentDestination = MainDestination.ServerChat(server.id)
                                        chatViewModel.clearMessages()
                                    }
                                }
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                                item { SidebarSectionLabel("CANALES DE TEXTO") }
                                items(channels, key = { it.id }) { channel ->
                                    ChannelItem(
                                        channel = channel,
                                        isSelected = false,
                                        onClick = {
                                            forceScrollChatKey = "channel:${channel.id}"
                                            serverViewModel.selectChannel(channel)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (openDm == null && openChannel == null) {
                MobileBottomNavigation(
                    selected = mobileSection,
                    hasUnreadMessages = unreadDmIds.isNotEmpty(),
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
                            showServerInvitations = true
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
                onClick = { showCreateServer = true }
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
                        val canManage = server.myRole == "owner" || server.myRole == "admin"
                        SidebarHeader(
                            title = server.name,
                            subtitle = roleLabel(server.myRole),
                            action = if (canManage) "Editar" else "Servidor",
                            onAction = { showServerSettings = true }
                        )
                        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            item { SidebarSectionLabel("CANALES DE TEXTO") }
                            items(channels, key = { it.id }) { channel ->
                                ChannelItem(
                                    channel = channel,
                                    isSelected = selectedChannel?.id == channel.id,
                                    onClick = {
                                        forceScrollChatKey = "channel:${channel.id}"
                                        serverViewModel.selectChannel(channel)
                                    }
                                )
                            }
                        }
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
                        viewModel = friendViewModel
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
                    } ?: EmptyState("Selecciona un canal")
                }
            }
        }
    }
    }

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
                error = serverError,
                onDismiss = { showServerSettings = false },
                onSave = { name, description, iconFile ->
                    serverViewModel.updateSelectedServer(name, description, iconFile)
                    showServerSettings = false
                },
                onCreateChannel = { channelName ->
                    serverViewModel.createChannel(channelName)
                },
                onInvite = { username ->
                    serverViewModel.inviteMember(username)
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
            onOpenImage = { imageToView = it }, onDownload = downloadMedia)
    }
    imageToView?.let { image -> com.matteo.chatapp.ui.components.ImageViewerDialog(image,
        refresh = chatViewModel::refreshAttachment, onDownload = downloadMedia, onShare = shareMedia,
        onDismiss = { imageToView = null }, isWorking = savingAttachment, notice = mediaNotice) }
    mediaError?.let { error -> AlertDialog(onDismissRequest = { mediaError = null }, title = { Text("No se pudo completar la acción") },
        text = { Text(error) }, confirmButton = { TextButton(onClick = { mediaError = null }) { Text("Aceptar") } }) }

}
