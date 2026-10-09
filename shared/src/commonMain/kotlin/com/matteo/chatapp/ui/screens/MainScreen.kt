package com.matteo.chatapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val unreadDmIds by chatViewModel.unreadDmIds.collectAsStateWithLifecycle()
    val messages = conversationState.messages
    val typingUsers = conversationState.typingUsers
    val hasOlderMessages = conversationState.hasOlderMessages
    val isLoadingOlder = conversationState.isLoadingOlder
    val messageError = conversationState.error
    val isUploadingAttachment = conversationState.isUploadingAttachment
    val friendState by friendViewModel.uiState.collectAsStateWithLifecycle()
    val dms = friendState.dms
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

    var messageInput by remember { mutableStateOf("") }
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
    val activeChatKey = when (val destination = currentDestination) {
        is MainDestination.DirectMessage -> "dm:${destination.dm.id}"
        is MainDestination.ServerChat -> selectedChannel?.id?.let { "channel:$it" }
        MainDestination.Friends -> null
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

    LaunchedEffect(dms.map { it.id to it.hasUnread }) {
        chatViewModel.restoreUnreadDms(dms.filter { it.hasUnread }.mapTo(mutableSetOf()) { it.id })
    }

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

    LaunchedEffect(messages.size) {
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        val totalItems = listState.layoutInfo.totalItemsCount
        val isNearBottom = totalItems == 0 || lastVisibleIndex >= totalItems - 4
        val shouldForceScroll = activeChatKey != null && forceScrollChatKey == activeChatKey
        if (messages.isNotEmpty() && (shouldForceScroll || isNearBottom)) {
            val dateSeparatorCount = messages.mapNotNull { it.createdAt?.take(10) }.distinct().size
            val targetIndex = (messages.size + dateSeparatorCount - 1).coerceAtLeast(0)
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
        avatarUrl = avatarUrl,
        online = online,
        typingText = typingUsers[typingKey]?.let { "$it esta escribiendo..." },
        highlightedMessageId = highlightedMessageId
    )

    fun buildChatAreaActions(
        onTyping: (String) -> Unit,
        onSendText: (String, String?, () -> Unit) -> Unit,
        onBack: (() -> Unit)? = null,
        onProfileClick: (() -> Unit)? = null
    ) = ChatAreaActions(
        onLoadOlder = chatViewModel::loadOlderMessages,
        onEditMessage = chatViewModel::editMessage,
        onDeleteMessage = chatViewModel::deleteMessage,
        onReplyMessage = { replyingTo = it },
        onCancelReply = { replyingTo = null },
        onInputChange = { text ->
            messageInput = text
            onTyping(text)
        },
        onSendAttachment = { file, clearSelection ->
            chatViewModel.sendAttachment(file, messageInput, replyingTo?.id) {
                messageInput = ""
                replyingTo = null
                clearSelection()
            }
        },
        onSend = {
            val content = messageInput
            if (content.isNotBlank()) {
                onSendText(content, replyingTo?.id) {
                    messageInput = ""
                    replyingTo = null
                }
            }
        },
        onBack = onBack,
        onSearch = openMessageSearch,
        onProfileClick = onProfileClick
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Background).windowInsetsPadding(WindowInsets.safeDrawing)) {
    val isMobile = maxWidth < 760.dp
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
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
                            onViewProfile = { friendViewModel.loadUserProfile(it) },
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
                        if (dms.isEmpty()) {
                            EmptyState("No hay conversaciones")
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp)) {
                                items(dms, key = { it.id }) { dm ->
                                    DirectMessageRow(
                                        dm = dm,
                                        selected = false,
                                        hasUnread = unreadDmIds.contains(dm.id),
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
    Row(modifier = Modifier.fillMaxSize()) {
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
                .width(276.dp)
                .fillMaxHeight()
                .background(Surface)
        ) {
            when (currentDestination) {
                is MainDestination.Friends, is MainDestination.DirectMessage -> {
                    SidebarHeader(title = "Mensajes directos", subtitle = "${dms.size} conversaciones")
                    LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        items(dms, key = { it.id }) { dm ->
                            DirectMessageRow(
                                dm = dm,
                                selected = selectedDm?.id == dm.id,
                                hasUnread = unreadDmIds.contains(dm.id),
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
            modifier = Modifier.weight(1f).fillMaxHeight().background(Background)
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
                        onViewProfile = { userId ->
                            friendViewModel.loadUserProfile(userId)
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

    if (showProfileSettings) {
        ProfileSettingsDialog(
            username = profile?.username ?: SessionManager.getUsername().orEmpty(),
            avatarUrl = profile?.avatarUrl.orEmpty(),
            bannerUrl = profile?.profileBannerUrl.orEmpty(),
            status = profile?.status.orEmpty(),
            email = profile?.email ?: SessionManager.getEmail().orEmpty(),
            error = friendError,
            onDismiss = { showProfileSettings = false },
            onSave = { username, status, avatarFile, bannerFile ->
                friendViewModel.saveProfile(username, status, avatarFile, bannerFile) {
                    showProfileSettings = false
                }
            },
            onChangePassword = { currentPassword, newPassword ->
                friendViewModel.changePassword(currentPassword, newPassword) {
                    showProfileSettings = false
                    chatViewModel.disconnectRealtime()
                    onLogout()
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
        PublicProfileDialog(
            profile = user,
            onDismiss = { friendViewModel.clearViewedProfile() },
            onOpenDm = {
                friendViewModel.openDm(user.id) { dm ->
                    friendViewModel.clearViewedProfile()
                    friendViewModel.selectDm(dm)
                    currentDestination = MainDestination.DirectMessage(dm)
                    forceScrollChatKey = "dm:${dm.id}"
                    chatViewModel.loadDmMessages(dm.id)
                }
            }
        )
    }
}
