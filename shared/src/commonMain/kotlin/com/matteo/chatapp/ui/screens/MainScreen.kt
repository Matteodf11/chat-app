package com.matteo.chatapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matteo.chatapp.model.DmConversation
import com.matteo.chatapp.model.ServerInvitation
import com.matteo.chatapp.model.ServerMember
import com.matteo.chatapp.platform.PickedAvatarFile
import com.matteo.chatapp.platform.PickedAttachmentFile
import com.matteo.chatapp.platform.pickAttachmentFile
import com.matteo.chatapp.platform.startVoiceRecording
import com.matteo.chatapp.platform.stopVoiceRecording
import com.matteo.chatapp.platform.cancelVoiceRecording
import com.matteo.chatapp.platform.pickProfileImageFile
import com.matteo.chatapp.platform.PlatformBackHandler
import com.matteo.chatapp.repository.SessionManager
import com.matteo.chatapp.repository.AppVisibility
import com.matteo.chatapp.ui.components.ChannelItem
import com.matteo.chatapp.ui.components.AvatarImage
import com.matteo.chatapp.ui.components.MessageItem
import com.matteo.chatapp.ui.components.ProfileBannerImage
import com.matteo.chatapp.ui.components.ServerItem
import com.matteo.chatapp.ui.theme.*
import com.matteo.chatapp.viewmodel.ChatViewModel
import com.matteo.chatapp.viewmodel.FriendViewModel
import com.matteo.chatapp.viewmodel.ServerViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.painterResource
import chat_app.shared.generated.resources.Res
import chat_app.shared.generated.resources.chimera_logo

sealed class MainDestination {
    object Friends : MainDestination()
    data class ServerChat(val serverId: String) : MainDestination()
    data class DirectMessage(val dm: DmConversation) : MainDestination()
}

private enum class MobileSection { FRIENDS, MESSAGES, SERVERS }

@Composable
fun MainScreen(
    onLogout: () -> Unit,
    themeMode: ThemeMode,
    themeAccent: ThemeAccent,
    onThemeModeChange: (ThemeMode) -> Unit,
    onThemeAccentChange: (ThemeAccent) -> Unit,
    sessionKey: Int = 0
) {
    val serverViewModel: ServerViewModel = viewModel(key = "server-$sessionKey") { ServerViewModel() }
    val chatViewModel: ChatViewModel = viewModel(key = "chat-$sessionKey") { ChatViewModel() }
    val friendViewModel: FriendViewModel = viewModel(key = "friend-$sessionKey") { FriendViewModel() }

    val servers by serverViewModel.servers.collectAsStateWithLifecycle()
    val channels by serverViewModel.channels.collectAsStateWithLifecycle()
    val serverMembers by serverViewModel.members.collectAsStateWithLifecycle()
    val serverInvitations by serverViewModel.pendingInvitations.collectAsStateWithLifecycle()
    val selectedServer by serverViewModel.selectedServer.collectAsStateWithLifecycle()
    val selectedChannel by serverViewModel.selectedChannel.collectAsStateWithLifecycle()
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val unreadDmIds by chatViewModel.unreadDmIds.collectAsStateWithLifecycle()
    val typingUsers by chatViewModel.typingUsers.collectAsStateWithLifecycle()
    val hasOlderMessages by chatViewModel.hasOlderMessages.collectAsStateWithLifecycle()
    val isLoadingOlder by chatViewModel.isLoadingOlder.collectAsStateWithLifecycle()
    val messageError by chatViewModel.messageError.collectAsStateWithLifecycle()
    val isUploadingAttachment by chatViewModel.isUploadingAttachment.collectAsStateWithLifecycle()
    val dms by friendViewModel.dms.collectAsStateWithLifecycle()
    val selectedDm by friendViewModel.selectedDm.collectAsStateWithLifecycle()
    val profile by friendViewModel.profile.collectAsStateWithLifecycle()
    val viewedProfile by friendViewModel.viewedProfile.collectAsStateWithLifecycle()
    val friendError by friendViewModel.error.collectAsStateWithLifecycle()
    val serverError by serverViewModel.error.collectAsStateWithLifecycle()
    val friends by friendViewModel.friends.collectAsStateWithLifecycle()
    val allSearchChannels by serverViewModel.searchChannels.collectAsStateWithLifecycle()
    val messageSearchResults by chatViewModel.searchResults.collectAsStateWithLifecycle()
    val isSearchingMessages by chatViewModel.isSearching.collectAsStateWithLifecycle()
    val messageSearchError by chatViewModel.searchError.collectAsStateWithLifecycle()
    val scrollTargetMessageId by chatViewModel.scrollTargetMessageId.collectAsStateWithLifecycle()
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
        val dmChatOpen = currentDestination is MainDestination.DirectMessage && selectedDm != null
        val channelChatOpen = currentDestination is MainDestination.ServerChat && selectedChannel != null
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    dmChatOpen -> selectedDm?.let { dm ->
                        ChatArea(
                            destinationKey = "dm:${dm.id}",
                            title = dm.otherUsername,
                            subtitle = if (dm.otherIsOnline) "En linea" else "Desconectado",
                            avatarUrl = dm.otherAvatar,
                            online = dm.otherIsOnline,
                            typingText = typingUsers[dm.id]?.let { "$it esta escribiendo..." },
                            onSearch = openMessageSearch,
                            highlightedMessageId = highlightedMessageId,
                            onBack = {
                                friendViewModel.clearSelectedDm()
                                currentDestination = MainDestination.Friends
                            },
                            onProfileClick = { friendViewModel.loadUserProfile(dm.otherUserId) },
                            messages = messages,
                            listState = listState,
                            hasOlderMessages = hasOlderMessages,
                            isLoadingOlder = isLoadingOlder,
                            onLoadOlder = chatViewModel::loadOlderMessages,
                            onEditMessage = chatViewModel::editMessage,
                            onDeleteMessage = chatViewModel::deleteMessage,
                            messageError = messageError,
                            replyingTo = replyingTo,
                            onReplyMessage = { replyingTo = it },
                            onCancelReply = { replyingTo = null },
                            messageInput = messageInput,
                            onInputChange = {
                                messageInput = it
                                chatViewModel.onMessageInputChanged(it, dmId = dm.id)
                            },
                            isUploadingAttachment = isUploadingAttachment,
                            onSendAttachment = { file, clearSelection ->
                                chatViewModel.sendAttachment(file, messageInput, replyingTo?.id) {
                                    messageInput = ""
                                    replyingTo = null
                                    clearSelection()
                                }
                            },
                            onSend = {
                                if (messageInput.isNotBlank()) {
                                    val content = messageInput
                                    chatViewModel.sendDmMessage(content, dm.id, replyingTo?.id) {
                                        messageInput = ""
                                        replyingTo = null
                                    }
                                }
                            }
                        )
                    }
                    channelChatOpen -> selectedChannel?.let { channel ->
                        ChatArea(
                            destinationKey = "channel:${channel.id}",
                            title = "# ${channel.name}",
                            subtitle = selectedServer?.name ?: "Servidor",
                            typingText = typingUsers[channel.id]?.let { "$it esta escribiendo..." },
                            onSearch = openMessageSearch,
                            highlightedMessageId = highlightedMessageId,
                            onBack = { serverViewModel.clearSelectedChannel() },
                            messages = messages,
                            listState = listState,
                            hasOlderMessages = hasOlderMessages,
                            isLoadingOlder = isLoadingOlder,
                            onLoadOlder = chatViewModel::loadOlderMessages,
                            onEditMessage = chatViewModel::editMessage,
                            onDeleteMessage = chatViewModel::deleteMessage,
                            messageError = messageError,
                            replyingTo = replyingTo,
                            onReplyMessage = { replyingTo = it },
                            onCancelReply = { replyingTo = null },
                            messageInput = messageInput,
                            onInputChange = {
                                messageInput = it
                                chatViewModel.onMessageInputChanged(it, channelId = channel.id)
                            },
                            isUploadingAttachment = isUploadingAttachment,
                            onSendAttachment = { file, clearSelection ->
                                chatViewModel.sendAttachment(file, messageInput, replyingTo?.id) {
                                    messageInput = ""
                                    replyingTo = null
                                    clearSelection()
                                }
                            },
                            onSend = {
                                if (messageInput.isNotBlank()) {
                                    val content = messageInput
                                    chatViewModel.sendMessage(content, replyingTo?.id) {
                                        messageInput = ""
                                        replyingTo = null
                                    }
                                }
                            }
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
            if (!dmChatOpen && !channelChatOpen) {
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
                            destinationKey = "dm:${dm.id}",
                            title = dm.otherUsername,
                            subtitle = if (dm.otherIsOnline) "En linea" else "Desconectado",
                            avatarUrl = dm.otherAvatar,
                            online = dm.otherIsOnline,
                            typingText = typingUsers[dm.id]?.let { "$it esta escribiendo..." },
                            onSearch = openMessageSearch,
                            highlightedMessageId = highlightedMessageId,
                            onProfileClick = { friendViewModel.loadUserProfile(dm.otherUserId) },
                            messages = messages,
                            listState = listState,
                            hasOlderMessages = hasOlderMessages,
                            isLoadingOlder = isLoadingOlder,
                            onLoadOlder = chatViewModel::loadOlderMessages,
                            onEditMessage = chatViewModel::editMessage,
                            onDeleteMessage = chatViewModel::deleteMessage,
                            messageError = messageError,
                            replyingTo = replyingTo,
                            onReplyMessage = { replyingTo = it },
                            onCancelReply = { replyingTo = null },
                            messageInput = messageInput,
                            onInputChange = {
                                messageInput = it
                                chatViewModel.onMessageInputChanged(it, dmId = dm.id)
                            },
                            isUploadingAttachment = isUploadingAttachment,
                            onSendAttachment = { file, clearSelection ->
                                chatViewModel.sendAttachment(file, messageInput, replyingTo?.id) {
                                    messageInput = ""
                                    replyingTo = null
                                    clearSelection()
                                }
                            },
                            onSend = {
                                if (messageInput.isNotBlank()) {
                                    val content = messageInput
                                    chatViewModel.sendDmMessage(content, dm.id, replyingTo?.id) {
                                        messageInput = ""
                                        replyingTo = null
                                    }
                                }
                            }
                        )
                    }
                }

                is MainDestination.ServerChat -> {
                    selectedChannel?.let { channel ->
                        ChatArea(
                            destinationKey = "channel:${channel.id}",
                            title = "# ${channel.name}",
                            subtitle = selectedServer?.name ?: "Servidor",
                            avatarUrl = null,
                            online = null,
                            typingText = typingUsers[channel.id]?.let { "$it esta escribiendo..." },
                            onSearch = openMessageSearch,
                            highlightedMessageId = highlightedMessageId,
                            onProfileClick = null,
                            messages = messages,
                            listState = listState,
                            hasOlderMessages = hasOlderMessages,
                            isLoadingOlder = isLoadingOlder,
                            onLoadOlder = chatViewModel::loadOlderMessages,
                            onEditMessage = chatViewModel::editMessage,
                            onDeleteMessage = chatViewModel::deleteMessage,
                            messageError = messageError,
                            replyingTo = replyingTo,
                            onReplyMessage = { replyingTo = it },
                            onCancelReply = { replyingTo = null },
                            messageInput = messageInput,
                            onInputChange = {
                                messageInput = it
                                chatViewModel.onMessageInputChanged(it, channelId = channel.id)
                            },
                            isUploadingAttachment = isUploadingAttachment,
                            onSendAttachment = { file, clearSelection ->
                                chatViewModel.sendAttachment(file, messageInput, replyingTo?.id) {
                                    messageInput = ""
                                    replyingTo = null
                                    clearSelection()
                                }
                            },
                            onSend = {
                                if (messageInput.isNotBlank()) {
                                    val content = messageInput
                                    chatViewModel.sendMessage(content, replyingTo?.id) {
                                        messageInput = ""
                                        replyingTo = null
                                    }
                                }
                            }
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
                    currentDestination = serverViewModel.selectedServer.value?.let { MainDestination.ServerChat(it.id) }
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

@Composable
private fun GlobalSearchDialog(
    friends: List<com.matteo.chatapp.model.FriendRequest>,
    dms: List<DmConversation>,
    servers: List<com.matteo.chatapp.model.Server>,
    channels: List<com.matteo.chatapp.model.Channel>,
    onDismiss: () -> Unit,
    onFriend: (String) -> Unit,
    onDm: (DmConversation) -> Unit,
    onServer: (com.matteo.chatapp.model.Server) -> Unit,
    onChannel: (com.matteo.chatapp.model.Channel) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val clean = query.trim().lowercase()
    val matchingDms = dms.filter { clean.isNotBlank() && it.otherUsername.lowercase().contains(clean) }
    val matchingFriends = friends.filter { clean.isNotBlank() && it.friendUsername.lowercase().contains(clean) }
    val matchingServers = servers.filter { clean.isNotBlank() && it.name.lowercase().contains(clean) }
    val matchingChannels = channels.filter { clean.isNotBlank() && it.name.lowercase().contains(clean) }
    val hasResults = matchingDms.isNotEmpty() || matchingFriends.isNotEmpty() ||
        matchingServers.isNotEmpty() || matchingChannels.isNotEmpty()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp).heightIn(max = 680.dp),
            shape = RoundedCornerShape(8.dp),
            color = Surface
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Buscar", color = OnBackground, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = Muted)
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(100) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    placeholder = { Text("Amigos, conversaciones, servidores o canales") },
                    colors = searchFieldColors()
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false)) {
                    if (clean.isBlank()) {
                        item { SearchEmptyText("Escribe para buscar") }
                    } else if (!hasResults) {
                        item { SearchEmptyText("No hay resultados") }
                    }
                    if (matchingDms.isNotEmpty()) {
                        item { SearchSectionTitle("CONVERSACIONES") }
                        items(matchingDms, key = { "dm-${it.id}" }) { dm ->
                            SearchResultRow(Icons.Rounded.ChatBubbleOutline, dm.otherUsername, "Mensaje directo") { onDm(dm) }
                        }
                    }
                    if (matchingFriends.isNotEmpty()) {
                        item { SearchSectionTitle("AMIGOS") }
                        items(matchingFriends, key = { "friend-${it.friendId}" }) { friend ->
                            SearchResultRow(Icons.Rounded.PersonOutline, friend.friendUsername, "Ver perfil") { onFriend(friend.friendId) }
                        }
                    }
                    if (matchingServers.isNotEmpty()) {
                        item { SearchSectionTitle("SERVIDORES") }
                        items(matchingServers, key = { "server-${it.id}" }) { server ->
                            SearchResultRow(Icons.Rounded.Dns, server.name, roleLabel(server.myRole)) { onServer(server) }
                        }
                    }
                    if (matchingChannels.isNotEmpty()) {
                        item { SearchSectionTitle("CANALES") }
                        items(matchingChannels, key = { "channel-${it.id}" }) { channel ->
                            val serverName = servers.firstOrNull { it.id == channel.serverId }?.name ?: "Servidor"
                            SearchResultRow(Icons.Rounded.Tag, "# ${channel.name}", serverName) { onChannel(channel) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageSearchDialog(
    results: List<com.matteo.chatapp.model.Message>,
    isSearching: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSearch: (String, String, String, String, Boolean) -> Unit,
    onOpenResult: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var sender by remember { mutableStateOf("") }
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var attachmentsOnly by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val canSearch = query.isNotBlank() || sender.isNotBlank() || from.isNotBlank() || to.isNotBlank() || attachmentsOnly
    val submitSearch = {
        if (!isValidSearchDate(from) || !isValidSearchDate(to)) {
            localError = "Usa fechas con formato AAAA-MM-DD"
        } else if (from.isNotBlank() && to.isNotBlank() && from > to) {
            localError = "La fecha inicial no puede ser posterior a la final"
        } else {
            localError = null
            onSearch(query, sender, from, to, attachmentsOnly)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp).heightIn(max = 720.dp),
            shape = RoundedCornerShape(8.dp),
            color = Surface
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Buscar mensajes", color = OnBackground, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = Muted) }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(120) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    placeholder = { Text("Texto o nombre de archivo") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (canSearch) submitSearch()
                    }),
                    colors = searchFieldColors()
                )
                Spacer(Modifier.height(10.dp))
                SearchFilterField(sender, { sender = it.take(80) }, "Autor", Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SearchFilterField(from, { from = it.take(10) }, "Desde", Modifier.weight(1f), "2026-07-01")
                    SearchFilterField(to, { to = it.take(10) }, "Hasta", Modifier.weight(1f), "2026-07-31")
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = attachmentsOnly,
                        onClick = { attachmentsOnly = !attachmentsOnly },
                        label = { Text("Solo archivos") }
                    )
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = submitSearch,
                        enabled = canSearch && !isSearching,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Buscar")
                    }
                }
                val visibleError = localError ?: error
                if (visibleError != null) {
                    Text(visibleError, color = Error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(Modifier.height(10.dp))
                when {
                    isSearching -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                    results.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Sin resultados", color = Muted, fontSize = 13.sp)
                    }
                    else -> LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(results, key = { it.id }) { message ->
                            val preview = message.content.ifBlank {
                                if (message.attachmentContentType?.startsWith("audio/") == true) "Nota de voz"
                                else message.attachmentName ?: "Archivo adjunto"
                            }
                            SearchResultRow(
                                Icons.Rounded.ChatBubbleOutline,
                                message.senderUsername,
                                "${message.createdAt?.replace('T', ' ')?.take(16).orEmpty()}  $preview"
                            ) { onOpenResult(message.id) }
                        }
                    }
                }
            }
        }
    }
}

private fun isValidSearchDate(value: String): Boolean {
    if (value.isBlank()) return true
    val parts = value.split('-')
    if (parts.size != 3 || parts[0].length != 4 || parts[1].length != 2 || parts[2].length != 2) return false
    val year = parts[0].toIntOrNull() ?: return false
    val month = parts[1].toIntOrNull() ?: return false
    val day = parts[2].toIntOrNull() ?: return false
    if (year !in 2000..9999 || month !in 1..12) return false
    val leapYear = year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)
    val maxDay = when (month) {
        2 -> if (leapYear) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
    return day in 1..maxDay
}

@Composable
private fun SearchFilterField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier,
    placeholder: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        label = { Text(label) },
        placeholder = { if (placeholder.isNotBlank()) Text(placeholder, fontSize = 11.sp) },
        colors = searchFieldColors()
    )
}

@Composable
private fun SearchResultRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = OnBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(subtitle, color = Muted, fontSize = 11.sp, maxLines = 2)
        }
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = Muted, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun SearchSectionTitle(text: String) {
    Text(text, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 5.dp, start = 8.dp))
}

@Composable
private fun SearchEmptyText(text: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Muted, fontSize = 13.sp)
    }
}

@Composable
private fun searchFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Primary,
    unfocusedBorderColor = Outline,
    focusedTextColor = OnBackground,
    unfocusedTextColor = OnBackground,
    cursorColor = Primary,
    focusedContainerColor = SurfaceVariant,
    unfocusedContainerColor = SurfaceVariant
)

@Composable
private fun MobileTopBar(
    title: String,
    onSearch: (() -> Unit)? = null,
    onProfile: () -> Unit,
    onAppearance: () -> Unit,
    onLogout: () -> Unit,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(58.dp).background(Navigation).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = OnSurface)
            }
        } else {
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)).background(Navigation),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.chimera_logo),
                    contentDescription = "Chimera",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.width(10.dp))
        }
        Text(title, color = OnBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
        actions()
        onSearch?.let {
            IconButton(onClick = it, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Rounded.Search, contentDescription = "Buscar", tint = OnSurface, modifier = Modifier.size(19.dp))
            }
        }
        IconButton(onClick = onProfile, modifier = Modifier.size(38.dp)) {
            Icon(Icons.Rounded.PersonOutline, contentDescription = "Perfil", tint = OnSurface, modifier = Modifier.size(19.dp))
        }
        IconButton(onClick = onAppearance, modifier = Modifier.size(38.dp)) {
            Icon(Icons.Rounded.Palette, contentDescription = "Apariencia", tint = OnSurface, modifier = Modifier.size(19.dp))
        }
        IconButton(onClick = onLogout, modifier = Modifier.size(38.dp)) {
            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Cerrar sesion", tint = Muted, modifier = Modifier.size(19.dp))
        }
    }
    HorizontalDivider(color = Outline)
}

@Composable
private fun MobileBottomNavigation(
    selected: MobileSection,
    hasUnreadMessages: Boolean,
    onSelect: (MobileSection) -> Unit
) {
    NavigationBar(containerColor = Navigation, tonalElevation = 0.dp) {
        NavigationBarItem(
            selected = selected == MobileSection.FRIENDS,
            onClick = { onSelect(MobileSection.FRIENDS) },
            icon = { Icon(Icons.Rounded.People, contentDescription = "Amigos") },
            label = { Text("Amigos") },
            colors = mobileNavigationColors()
        )
        NavigationBarItem(
            selected = selected == MobileSection.MESSAGES,
            onClick = { onSelect(MobileSection.MESSAGES) },
            icon = {
                BadgedBox(badge = { if (hasUnreadMessages) Badge() }) {
                    Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = "Mensajes")
                }
            },
            label = { Text("Mensajes") },
            colors = mobileNavigationColors()
        )
        NavigationBarItem(
            selected = selected == MobileSection.SERVERS,
            onClick = { onSelect(MobileSection.SERVERS) },
            icon = { Icon(Icons.Rounded.Dns, contentDescription = "Servidores") },
            label = { Text("Servidores") },
            colors = mobileNavigationColors()
        )
    }
}

@Composable
private fun mobileNavigationColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Primary,
    selectedTextColor = Primary,
    indicatorColor = PrimarySoft,
    unselectedIconColor = Muted,
    unselectedTextColor = Muted
)

@Composable
private fun MobileServerRow(server: com.matteo.chatapp.model.Server, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Surface)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(avatarUrl = server.iconUrl, name = server.name, size = 46)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(server.name, color = OnBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(roleLabel(server.myRole), color = Muted, fontSize = 12.sp)
        }
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = Muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun RailAction(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.width(72.dp).height(48.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (selected) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                    .background(Primary)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(44.dp)
                .clip(RoundedCornerShape(if (selected) 12.dp else 22.dp))
                .background(if (selected) Primary else SurfaceElevated)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (selected) OnPrimary else OnSurface,
                modifier = Modifier.size(21.dp)
            )
        }
        badge?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp)
                    .height(18.dp)
                    .defaultMinSize(minWidth = 18.dp)
                    .clip(CircleShape)
                    .background(Error)
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(it, color = OnBackground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SidebarHeader(
    title: String,
    subtitle: String,
    action: String? = null,
    onAction: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth().height(82.dp).padding(horizontal = 16.dp, vertical = 13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = OnBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
            action?.let {
                IconButton(onClick = onAction, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Settings, contentDescription = it, tint = OnSurface, modifier = Modifier.size(19.dp))
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
    HorizontalDivider(color = Outline)
}

@Composable
private fun SidebarSectionLabel(text: String) {
    Text(
        text = text,
        color = Muted,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp)
    )
}

@Composable
private fun DirectMessageRow(dm: DmConversation, selected: Boolean, hasUnread: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) SurfaceVariant else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PresenceAvatar(name = dm.otherUsername, avatarUrl = dm.otherAvatar, online = dm.otherIsOnline, size = 34)
        Spacer(Modifier.width(10.dp))
        Text(
            dm.otherUsername,
            color = if (selected) OnBackground else OnSurface,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (hasUnread) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Primary)
            )
        }
    }
}

@Composable
private fun UserFooter(
    username: String,
    avatarUrl: String?,
    status: String,
    onSettings: () -> Unit,
    onAppearanceSettings: () -> Unit,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Navigation)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PresenceAvatar(name = username.ifBlank { "?" }, avatarUrl = avatarUrl, online = true, size = 34)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f).clickable { onSettings() }) {
            Text(username, color = OnBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(status.ifBlank { "Disponible" }, color = Accent, fontSize = 11.sp)
        }
        IconButton(onClick = onSettings, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Rounded.PersonOutline, contentDescription = "Ajustes de perfil", tint = OnSurface, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onAppearanceSettings, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Rounded.Palette, contentDescription = "Apariencia", tint = OnSurface, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onLogout, modifier = Modifier.size(34.dp)) {
            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Cerrar sesion", tint = Muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun PresenceAvatar(name: String, avatarUrl: String? = null, online: Boolean, size: Int = 40) {
    Box {
        AvatarImage(avatarUrl = avatarUrl, name = name, size = size)
        Box(
            modifier = Modifier
                .size((size / 3).dp)
                .clip(CircleShape)
                .background(if (online) Accent else Muted)
                .align(Alignment.BottomEnd)
        )
    }
}

@Composable
fun ChatArea(
    destinationKey: String,
    title: String,
    subtitle: String,
    avatarUrl: String? = null,
    online: Boolean? = null,
    onBack: (() -> Unit)? = null,
    typingText: String? = null,
    onSearch: (() -> Unit)? = null,
    highlightedMessageId: String? = null,
    onProfileClick: (() -> Unit)? = null,
    messages: List<com.matteo.chatapp.model.Message>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    hasOlderMessages: Boolean,
    isLoadingOlder: Boolean,
    onLoadOlder: () -> Unit,
    onEditMessage: (String, String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    messageError: String?,
    replyingTo: com.matteo.chatapp.model.Message?,
    onReplyMessage: (com.matteo.chatapp.model.Message) -> Unit,
    onCancelReply: () -> Unit,
    messageInput: String,
    onInputChange: (String) -> Unit,
    isUploadingAttachment: Boolean,
    onSendAttachment: (PickedAttachmentFile, () -> Unit) -> Unit,
    onSend: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    var pendingAttachment by remember(destinationKey) { mutableStateOf<PickedAttachmentFile?>(null) }
    var isRecordingVoice by remember(destinationKey) { mutableStateOf(false) }
    var recordingSeconds by remember(destinationKey) { mutableStateOf(0) }
    var recordingError by remember(destinationKey) { mutableStateOf<String?>(null) }
    val canSend = (messageInput.isNotBlank() || pendingAttachment != null) && !isUploadingAttachment && !isRecordingVoice
    val sendCurrent = {
        val attachment = pendingAttachment
        if (attachment != null) {
            onSendAttachment(attachment) { pendingAttachment = null }
        } else {
            onSend()
        }
    }
    DisposableEffect(destinationKey) {
        onDispose { cancelVoiceRecording() }
    }
    LaunchedEffect(isRecordingVoice) {
        recordingSeconds = 0
        while (isRecordingVoice && recordingSeconds < 300) {
            delay(1000)
            if (isRecordingVoice) recordingSeconds++
        }
        if (isRecordingVoice && recordingSeconds >= 300) {
            pendingAttachment = stopVoiceRecording()
            isRecordingVoice = false
        }
    }
    LaunchedEffect(listState, hasOlderMessages) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstVisible ->
                if (firstVisible <= 2 && hasOlderMessages) onLoadOlder()
            }
    }
    LaunchedEffect(imeBottom, destinationKey) {
        if (imeBottom > 0 && messages.isNotEmpty()) {
            val dateSeparatorCount = messages.mapNotNull { it.createdAt?.take(10) }.distinct().size
            val targetIndex = (messages.size + dateSeparatorCount - 1).coerceAtLeast(0)
            listState.scrollToItem(targetIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp).background(Background).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            onBack?.let {
                IconButton(onClick = it, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = OnSurface, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(6.dp))
            }
            if (online != null) {
                PresenceAvatar(name = title, avatarUrl = avatarUrl, online = online, size = 38)
            } else {
                Box(
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Tag, contentDescription = null, tint = OnSurface, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = OnBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Spacer(Modifier.height(3.dp))
                Text(
                    typingText ?: subtitle,
                    color = if (typingText != null) Accent else Muted,
                    fontSize = 12.sp,
                    fontWeight = if (typingText != null) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1
                )
            }
            onSearch?.let {
                IconButton(onClick = it, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Rounded.Search, contentDescription = "Buscar mensajes", tint = OnSurface, modifier = Modifier.size(20.dp))
                }
            }
            onProfileClick?.let {
                IconButton(onClick = it, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Rounded.PersonOutline, contentDescription = "Ver perfil", tint = OnSurface, modifier = Modifier.size(20.dp))
                }
            }
        }
        HorizontalDivider(color = Outline)
        LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(vertical = 10.dp)) {
            if (isLoadingOlder) {
                item(key = "loading-older") {
                    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Primary)
                    }
                }
            }
            messages.forEachIndexed { index, message ->
                val date = message.createdAt?.take(10).orEmpty()
                val previousDate = messages.getOrNull(index - 1)?.createdAt?.take(10).orEmpty()
                if (date.isNotBlank() && date != previousDate) {
                    item(key = "date-$date") {
                        DateSeparator(date)
                    }
                }
                item(key = message.id) {
                    MessageItem(
                        message = message,
                        canModify = message.senderId == SessionManager.getUserId(),
                        onReply = { onReplyMessage(message) },
                        onReplyClick = { replyId ->
                            val targetIndex = messages.indexOfFirst { it.id == replyId }
                            if (targetIndex >= 0) {
                                val separators = messages.take(targetIndex + 1)
                                    .mapNotNull { it.createdAt?.take(10) }
                                    .distinct()
                                    .size
                                scope.launch { listState.animateScrollToItem(targetIndex + separators) }
                            }
                        },
                        onEdit = { content -> onEditMessage(message.id, content) },
                        onDelete = { onDeleteMessage(message.id) },
                        highlighted = message.id == highlightedMessageId
                    )
                }
            }
        }
        if (messageError != null || recordingError != null) {
            Text(
                text = messageError ?: recordingError.orEmpty(),
                color = Error,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
        if (replyingTo != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceVariant)
                    .padding(start = 20.dp, end = 10.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(3.dp).height(34.dp).background(Primary))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Respondiendo a ${replyingTo.senderUsername}", color = OnBackground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        replyingTo.content.ifBlank {
                            if (replyingTo.attachmentContentType?.startsWith("audio/") == true) "Nota de voz"
                            else replyingTo.attachmentName ?: "Mensaje"
                        },
                        color = Muted,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                IconButton(onClick = onCancelReply, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cancelar respuesta", tint = Muted)
                }
            }
        }
        if (pendingAttachment != null) {
            Row(
                modifier = Modifier.fillMaxWidth().background(SurfaceVariant).padding(horizontal = 20.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (pendingAttachment!!.contentType.startsWith("audio/")) Icons.Rounded.GraphicEq else Icons.Rounded.Description,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(pendingAttachment!!.fileName, color = OnBackground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(formatAttachmentSize(pendingAttachment!!.size), color = Muted, fontSize = 11.sp)
                }
                IconButton(onClick = { pendingAttachment = null }, enabled = !isUploadingAttachment, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Quitar archivo", tint = Muted)
                }
            }
        }
        if (isRecordingVoice) {
            Row(
                modifier = Modifier.fillMaxWidth().background(SurfaceVariant).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(Error))
                Spacer(Modifier.width(10.dp))
                Text("Grabando ${formatVoiceDuration(recordingSeconds)}", color = OnBackground, fontSize = 13.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    cancelVoiceRecording()
                    isRecordingVoice = false
                    recordingSeconds = 0
                }) { Text("Cancelar", color = Muted) }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(Background).padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = {
                    scope.launch { pickAttachmentFile()?.let { pendingAttachment = it } }
                },
                enabled = !isUploadingAttachment && !isRecordingVoice,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(Icons.Rounded.AttachFile, contentDescription = "Adjuntar archivo", tint = OnSurface)
            }
            OutlinedTextField(
                value = messageInput,
                onValueChange = onInputChange,
                enabled = !isRecordingVoice,
                placeholder = { Text("Escribe un mensaje", color = Muted) },
                modifier = Modifier
                    .weight(1f)
                    .onPreviewKeyEvent { event ->
                        if (event.key == Key.Enter && event.type == KeyEventType.KeyDown && !event.isShiftPressed) {
                            if (canSend) sendCurrent()
                            true
                        } else {
                            false
                        }
                    },
                shape = RoundedCornerShape(8.dp),
                singleLine = false,
                minLines = 1,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (canSend) sendCurrent() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Outline,
                    focusedTextColor = OnBackground,
                    unfocusedTextColor = OnBackground,
                    cursorColor = Primary,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface
                )
            )
            IconButton(
                onClick = {
                    scope.launch {
                        if (isRecordingVoice) {
                            val voice = stopVoiceRecording()
                            isRecordingVoice = false
                            recordingSeconds = 0
                            if (voice != null) {
                                pendingAttachment = voice
                                recordingError = null
                            } else {
                                recordingError = "La nota de voz es demasiado corta"
                            }
                        } else {
                            pendingAttachment = null
                            if (startVoiceRecording()) {
                                isRecordingVoice = true
                                recordingError = null
                            } else {
                                recordingError = "No se pudo acceder al microfono"
                            }
                        }
                    }
                },
                enabled = !isUploadingAttachment,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isRecordingVoice) Error else SurfaceVariant)
            ) {
                Icon(
                    if (isRecordingVoice) Icons.Rounded.Stop else Icons.Rounded.Mic,
                    contentDescription = if (isRecordingVoice) "Detener grabacion" else "Grabar nota de voz",
                    tint = if (isRecordingVoice) Color.White else OnSurface
                )
            }
            IconButton(
                onClick = sendCurrent,
                enabled = canSend,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(if (canSend) Primary else SurfaceVariant)
            ) {
                if (isUploadingAttachment) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Muted)
                } else {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Enviar mensaje", tint = if (canSend) OnPrimary else Muted)
                }
            }
        }
    }
}

private fun formatAttachmentSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "${bytes / (1024L * 1024L)} MB"
    bytes >= 1024L -> "${bytes / 1024L} KB"
    else -> "$bytes B"
}

private fun formatVoiceDuration(seconds: Int): String =
    "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

@Composable
private fun DateSeparator(date: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(color = SurfaceVariant, modifier = Modifier.weight(1f))
        Text(
            text = date,
            color = Muted,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        HorizontalDivider(color = SurfaceVariant, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = OnSurface, fontSize = 15.sp)
    }
}

@Composable
private fun GeneralSettingsDialog(
    themeMode: ThemeMode,
    themeAccent: ThemeAccent,
    onThemeModeChange: (ThemeMode) -> Unit,
    onThemeAccentChange: (ThemeAccent) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 0.dp,
        title = { Text("Apariencia", color = OnBackground, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tema claro", color = OnBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (themeMode == ThemeMode.LIGHT) "Activado" else "Tema oscuro activo",
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = themeMode == ThemeMode.LIGHT,
                        onCheckedChange = { enabled ->
                            onThemeModeChange(if (enabled) ThemeMode.LIGHT else ThemeMode.DARK)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OnPrimary,
                            checkedTrackColor = Primary,
                            uncheckedThumbColor = Muted,
                            uncheckedTrackColor = SurfaceElevated,
                            uncheckedBorderColor = Outline
                        )
                    )
                }
                HorizontalDivider(color = Outline)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Color del tema", color = OnBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ThemeAccent.entries.forEach { accent ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = 2.dp,
                                            color = if (accent == themeAccent) OnBackground else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .padding(4.dp)
                                        .clip(CircleShape)
                                        .background(accent.color)
                                        .clickable { onThemeAccentChange(accent) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (accent == themeAccent) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                                    }
                                }
                                Spacer(Modifier.height(5.dp))
                                Text(accent.label, color = Muted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar", color = Primary) }
        }
    )
}

@Composable
private fun ProfileSettingsDialog(
    username: String,
    avatarUrl: String,
    bannerUrl: String,
    status: String,
    email: String,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, PickedAvatarFile?, PickedAvatarFile?) -> Unit,
    onChangePassword: (String, String) -> Unit
) {
    val statusOptions = listOf("Disponible", "Ausente", "Ocupado", "Invisible")
    var usernameInput by remember(username) { mutableStateOf(username) }
    var statusInput by remember(status) { mutableStateOf(status.ifBlank { "Disponible" }) }
    var selectedAvatarName by remember { mutableStateOf<String?>(null) }
    var selectedAvatarFile by remember { mutableStateOf<PickedAvatarFile?>(null) }
    var selectedBannerName by remember { mutableStateOf<String?>(null) }
    var selectedBannerFile by remember { mutableStateOf<PickedAvatarFile?>(null) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 0.dp,
        title = { Text("Ajustes de perfil", color = OnBackground, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                error?.let { Text(it, color = Error, fontSize = 12.sp) }
                ProfilePreviewCard(
                    username = usernameInput.ifBlank { username },
                    avatarUrl = avatarUrl,
                    bannerUrl = bannerUrl,
                    status = statusInput,
                    online = true
                )
                Text("Email: $email", color = Muted, fontSize = 12.sp)
                SettingsField("Nombre de usuario", usernameInput) { usernameInput = it }
                Text("Estado", color = OnBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    statusOptions.forEach { option ->
                        FilterChip(
                            selected = statusInput == option,
                            onClick = { statusInput = option },
                            label = { Text(option) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SurfaceElevated,
                                selectedLabelColor = OnBackground,
                                labelColor = OnSurface
                            )
                        )
                    }
                }
                HorizontalDivider(color = Outline)
                Text("Imagenes", color = OnBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            scope.launch {
                                val picked = pickProfileImageFile("Seleccionar avatar")
                                if (picked != null) {
                                    selectedAvatarName = picked.fileName
                                    selectedAvatarFile = picked
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated)
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null, tint = OnBackground, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Avatar", color = OnBackground)
                    }
                    Text(selectedAvatarName ?: "Sin cambios", color = Muted, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            scope.launch {
                                val picked = pickProfileImageFile("Seleccionar banner de perfil")
                                if (picked != null) {
                                    selectedBannerName = picked.fileName
                                    selectedBannerFile = picked
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated)
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null, tint = OnBackground, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Banner", color = OnBackground)
                    }
                    Text(selectedBannerName ?: "JPG, PNG o WEBP - max. 2 MB", color = Muted, fontSize = 12.sp)
                }
                HorizontalDivider(color = Outline)
                Text("Seguridad", color = OnBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                PasswordSettingsField("Contrasena actual", currentPassword) { currentPassword = it }
                PasswordSettingsField("Nueva contrasena", newPassword) { newPassword = it }
                Button(
                    onClick = { onChangePassword(currentPassword, newPassword) },
                    enabled = currentPassword.isNotBlank() && newPassword.length in 10..72,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated)
                ) {
                    Text("Cambiar contrasena", color = OnBackground)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(usernameInput, statusInput, selectedAvatarFile, selectedBannerFile) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Guardar", color = OnPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = OnSurface) }
        }
    )
}

@Composable
private fun PasswordSettingsField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = Outline,
            focusedTextColor = OnBackground,
            unfocusedTextColor = OnBackground,
            focusedContainerColor = Background,
            unfocusedContainerColor = Background
        )
    )
}

@Composable
private fun ProfilePreviewCard(
    username: String,
    avatarUrl: String,
    bannerUrl: String,
    status: String,
    online: Boolean
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .height(184.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceVariant)
        ) {
            ProfileBannerImage(
                bannerUrl = bannerUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 26.dp, top = 70.dp)
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariant)
                    .padding(6.dp)
            ) {
                AvatarImage(avatarUrl = avatarUrl, name = username.ifBlank { "?" }, size = 84)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 102.dp, top = 132.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariant)
                    .padding(5.dp)
            ) {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(if (online) Accent else Muted))
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 138.dp, end = 18.dp, bottom = 18.dp)
            ) {
                Text(username.ifBlank { "Tu perfil" }, color = OnBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(status.ifBlank { "Disponible" }, color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PublicProfileDialog(
    profile: com.matteo.chatapp.model.UserProfile,
    onDismiss: () -> Unit,
    onOpenDm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 0.dp,
        title = { Text("Perfil", color = OnBackground, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ProfilePreviewCard(
                    username = profile.username,
                    avatarUrl = profile.avatarUrl.orEmpty(),
                    bannerUrl = profile.profileBannerUrl.orEmpty(),
                    status = profile.status.orEmpty(),
                    online = profile.isOnline
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenDm,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Mensaje", color = OnPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar", color = OnSurface) }
        }
    )
}

@Composable
private fun CreateServerDialog(
    error: String?,
    onDismiss: () -> Unit,
    onCreate: (String, String, PickedAvatarFile?) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }
    var iconFile by remember { mutableStateOf<PickedAvatarFile?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 0.dp,
        title = { Text("Crear servidor", color = OnBackground, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                error?.let { Text(it, color = Error, fontSize = 12.sp) }
                SettingsField("Nombre", nameInput, onValueChange = { nameInput = it })
                SettingsField(
                    "Descripcion",
                    descriptionInput,
                    onValueChange = { descriptionInput = it },
                    onSubmit = {
                        if (nameInput.trim().length >= 2) onCreate(nameInput, descriptionInput, iconFile)
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    AvatarImage(avatarUrl = null, name = nameInput.ifBlank { "S" }, size = 48)
                    Button(
                        onClick = {
                            scope.launch {
                                iconFile = pickProfileImageFile("Seleccionar icono del servidor")
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated)
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null, tint = OnBackground, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Elegir icono", color = OnBackground)
                    }
                }
                Text(iconFile?.fileName ?: "JPG, PNG o WEBP - max. 2 MB", color = Muted, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                enabled = nameInput.trim().length >= 2,
                onClick = { onCreate(nameInput, descriptionInput, iconFile) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Crear", color = OnPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = OnSurface) }
        }
    )
}

@Composable
private fun ServerInvitationsDialog(
    invitations: List<ServerInvitation>,
    error: String?,
    onDismiss: () -> Unit,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 0.dp,
        title = { Text("Invitaciones", color = OnBackground, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                error?.let { Text(it, color = Error, fontSize = 12.sp) }
                if (invitations.isEmpty()) {
                    Text("No tienes invitaciones pendientes", color = Muted, fontSize = 13.sp)
                } else {
                    invitations.forEach { invitation ->
                        ServerInvitationRow(
                            invitation = invitation,
                            onAccept = { onAccept(invitation.id) },
                            onReject = { onReject(invitation.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar", color = OnSurface) }
        }
    )
}

@Composable
private fun ServerInvitationRow(
    invitation: ServerInvitation,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Background)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(avatarUrl = invitation.serverIconUrl, name = invitation.serverName, size = 42)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(invitation.serverName, color = OnBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("Invita ${invitation.inviterUsername}", color = Muted, fontSize = 12.sp)
        }
        IconButton(onClick = onReject, modifier = Modifier.size(38.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = "Rechazar invitacion", tint = Error, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onAccept, modifier = Modifier.size(38.dp)) {
            Icon(Icons.Rounded.Check, contentDescription = "Aceptar invitacion", tint = Accent, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ServerSettingsDialog(
    server: com.matteo.chatapp.model.Server,
    serverName: String,
    description: String,
    members: List<ServerMember>,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, PickedAvatarFile?) -> Unit,
    onCreateChannel: (String) -> Unit,
    onInvite: (String) -> Unit,
    onRoleChange: (String, String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit
) {
    var nameInput by remember(serverName) { mutableStateOf(serverName) }
    var descriptionInput by remember(description) { mutableStateOf(description) }
    var iconFile by remember { mutableStateOf<PickedAvatarFile?>(null) }
    var channelInput by remember { mutableStateOf("") }
    var inviteInput by remember { mutableStateOf("") }
    val canManage = server.myRole == "owner" || server.myRole == "admin"
    val isOwner = server.myRole == "owner"
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 0.dp,
        title = { Text("Ajustes del servidor", color = OnBackground, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                error?.let { Text(it, color = Error, fontSize = 12.sp) }
                Text(roleLabel(server.myRole), color = Muted, fontSize = 12.sp)
                if (canManage) {
                    SettingsField("Nombre", nameInput) { nameInput = it }
                    SettingsField("Descripcion", descriptionInput) { descriptionInput = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        AvatarImage(avatarUrl = server.iconUrl, name = server.name, size = 48)
                        Button(
                            onClick = {
                                scope.launch {
                                    iconFile = pickProfileImageFile("Seleccionar icono del servidor")
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated)
                        ) { Text("Cambiar icono", color = OnBackground) }
                    }
                    Text(iconFile?.fileName ?: "JPG, PNG o WEBP - max. 2 MB", color = Muted, fontSize = 12.sp)
                    HorizontalDivider(color = Outline)
                    Text("Nuevo canal", color = OnBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            SettingsField(
                                "Nombre del canal",
                                channelInput,
                                onValueChange = { channelInput = it },
                                onSubmit = {
                                    if (channelInput.isNotBlank()) {
                                        onCreateChannel(channelInput)
                                        channelInput = ""
                                    }
                                }
                            )
                        }
                        Button(
                            onClick = {
                                if (channelInput.isNotBlank()) {
                                    onCreateChannel(channelInput)
                                    channelInput = ""
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated)
                        ) {
                            Text("Crear", color = OnBackground)
                        }
                    }
                    Text("Enviar invitacion", color = OnBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            SettingsField(
                                "Usuario o email",
                                inviteInput,
                                onValueChange = { inviteInput = it },
                                onSubmit = {
                                    if (inviteInput.isNotBlank()) {
                                        onInvite(inviteInput)
                                        inviteInput = ""
                                    }
                                }
                            )
                        }
                        Button(
                            onClick = {
                                if (inviteInput.isNotBlank()) {
                                    onInvite(inviteInput)
                                    inviteInput = ""
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated)
                        ) {
                            Text("Enviar", color = OnBackground)
                        }
                    }
                }
                HorizontalDivider(color = Outline)
                Text("Miembros", color = OnBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                members.forEach { member ->
                    ServerMemberRow(
                        member = member,
                        viewerRole = server.myRole,
                        currentUserId = SessionManager.getUserId(),
                        onRoleChange = onRoleChange,
                        onRemove = onRemoveMember
                    )
                }
                HorizontalDivider(color = Outline)
                if (!isOwner) {
                    TextButton(onClick = onLeave, colors = ButtonDefaults.textButtonColors(contentColor = Error)) {
                        Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Salir del servidor")
                    }
                }
                if (isOwner) {
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = Error)) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Borrar servidor")
                    }
                }
            }
        },
        confirmButton = {
            if (canManage) {
                Button(
                    onClick = { onSave(nameInput, descriptionInput, iconFile) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Guardar", color = OnPrimary)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar", color = OnSurface) }
        }
    )
}

@Composable
private fun ServerMemberRow(
    member: ServerMember,
    viewerRole: String?,
    currentUserId: String?,
    onRoleChange: (String, String) -> Unit,
    onRemove: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    val isSelf = member.userId == currentUserId
    val canChangeRole = viewerRole == "owner" && member.role != "owner" && !isSelf
    val canRemove = !isSelf && member.role != "owner" && (
        viewerRole == "owner" || (viewerRole == "admin" && member.role == "member")
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PresenceAvatar(name = member.username, avatarUrl = member.avatarUrl, online = member.isOnline, size = 34)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(member.username, color = OnBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(roleLabel(member.role), color = Muted, fontSize = 11.sp)
        }
        if (canChangeRole || canRemove) {
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Gestionar miembro", tint = Muted)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = SurfaceElevated
                ) {
                    if (canChangeRole) {
                        DropdownMenuItem(
                            text = { Text(if (member.role == "admin") "Quitar administrador" else "Hacer administrador") },
                            leadingIcon = { Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onRoleChange(member.userId, if (member.role == "admin") "member" else "admin")
                            }
                        )
                    }
                    if (canRemove) {
                        DropdownMenuItem(
                            text = { Text("Expulsar", color = Error) },
                            leadingIcon = { Icon(Icons.Rounded.PersonRemove, contentDescription = null, tint = Error) },
                            onClick = {
                                showMenu = false
                                confirmRemove = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Expulsar miembro") },
            text = { Text("${member.username} perdera el acceso al servidor y a sus canales.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = false
                    onRemove(member.userId)
                }) { Text("Expulsar", color = Error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text("Cancelar") }
            }
        )
    }
}

private fun roleLabel(role: String?): String {
    return when (role) {
        "owner" -> "Dueno"
        "admin" -> "Admin"
        else -> "Miembro"
    }
}

@Composable
private fun SettingsField(
    label: String,
    value: String,
    onSubmit: (() -> Unit)? = null,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (onSubmit != null && event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                    onSubmit()
                    true
                } else {
                    false
                }
            },
        shape = RoundedCornerShape(8.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = if (onSubmit != null) ImeAction.Done else ImeAction.Next),
        keyboardActions = KeyboardActions(onDone = { onSubmit?.invoke() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = SurfaceElevated,
            focusedLabelColor = Primary,
            unfocusedLabelColor = Muted,
            focusedTextColor = OnBackground,
            unfocusedTextColor = OnBackground,
            cursorColor = Primary,
            focusedContainerColor = Background,
            unfocusedContainerColor = Background
        )
    )
}
