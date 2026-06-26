package com.matteo.chatapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matteo.chatapp.model.DmConversation
import com.matteo.chatapp.repository.SessionManager
import com.matteo.chatapp.ui.components.ChannelItem
import com.matteo.chatapp.ui.components.MessageItem
import com.matteo.chatapp.ui.components.ServerItem
import com.matteo.chatapp.ui.theme.*
import com.matteo.chatapp.viewmodel.ChatViewModel
import com.matteo.chatapp.viewmodel.FriendViewModel
import com.matteo.chatapp.viewmodel.ServerViewModel

sealed class MainDestination {
    object Friends : MainDestination()
    data class ServerChat(val serverId: String) : MainDestination()
    data class DirectMessage(val dm: DmConversation) : MainDestination()
}

@Composable
fun MainScreen(
    onLogout: () -> Unit,
    serverViewModel: ServerViewModel = viewModel { ServerViewModel() },
    chatViewModel: ChatViewModel = viewModel { ChatViewModel() },
    friendViewModel: FriendViewModel = viewModel { FriendViewModel() }
) {
    val servers by serverViewModel.servers.collectAsStateWithLifecycle()
    val channels by serverViewModel.channels.collectAsStateWithLifecycle()
    val selectedServer by serverViewModel.selectedServer.collectAsStateWithLifecycle()
    val selectedChannel by serverViewModel.selectedChannel.collectAsStateWithLifecycle()
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val dms by friendViewModel.dms.collectAsStateWithLifecycle()
    val selectedDm by friendViewModel.selectedDm.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var messageInput by remember { mutableStateOf("") }
    var currentDestination by remember { mutableStateOf<MainDestination>(MainDestination.Friends) }

    LaunchedEffect(selectedChannel) {
        selectedChannel?.let { chatViewModel.loadMessages(it.id) }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Row(modifier = Modifier.fillMaxSize()) {

        // Columna 1: barra izquierda (DMs + servidores)
        Column(
            modifier = Modifier
                .width(72.dp)
                .fillMaxHeight()
                .background(Background)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Botón amigos
            val isFriends = currentDestination is MainDestination.Friends
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(if (isFriends) RoundedCornerShape(16.dp) else CircleShape)
                    .background(if (isFriends) Primary else SurfaceVariant)
                    .clickable {
                        currentDestination = MainDestination.Friends
                        serverViewModel.clearSelection()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("👥", fontSize = 20.sp)
            }

            HorizontalDivider(
                color = SurfaceVariant,
                modifier = Modifier.width(32.dp)
            )

            // Servidores
            servers.forEach { server ->
                ServerItem(
                    server = server,
                    isSelected = selectedServer?.id == server.id,
                    onClick = {
                        serverViewModel.selectServer(server)
                        currentDestination = MainDestination.ServerChat(server.id)
                        friendViewModel.clearSelectedDm()
                    }
                )
            }
        }

        // Columna 2: panel lateral (canales o DMs)
        Column(
            modifier = Modifier
                .width(240.dp)
                .fillMaxHeight()
                .background(Surface)
        ) {
            when (currentDestination) {
                is MainDestination.Friends, is MainDestination.DirectMessage -> {
                    // Header DMs
                    Text(
                        text = "Mensajes directos",
                        color = OnSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    dms.forEach { dm ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (selectedDm?.id == dm.id) SurfaceVariant
                                    else Color.Transparent
                                )
                                .clickable {
                                    friendViewModel.selectDm(dm)
                                    currentDestination = MainDestination.DirectMessage(dm)
                                    chatViewModel.loadDmMessages(dm.id)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                Box(
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(dm.otherUsername.take(1).uppercase(), color = OnBackground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (dm.otherIsOnline) Color(0xFF23A55A) else OnSurface)
                                        .align(Alignment.BottomEnd)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(dm.otherUsername, color = if (selectedDm?.id == dm.id) OnBackground else OnSurface, fontSize = 14.sp)
                        }
                    }
                }
                is MainDestination.ServerChat -> {
                    selectedServer?.let { server ->
                        Text(
                            text = server.name,
                            color = OnBackground,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                        HorizontalDivider(color = SurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "CANALES DE TEXTO",
                            color = OnSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        channels.forEach { channel ->
                            ChannelItem(
                                channel = channel,
                                isSelected = selectedChannel?.id == channel.id,
                                onClick = { serverViewModel.selectChannel(channel) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Panel usuario abajo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Background)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = SessionManager.getUsername()?.take(1)?.uppercase() ?: "?",
                        color = OnBackground,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(SessionManager.getUsername() ?: "", color = OnBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("En línea", color = Color(0xFF23A55A), fontSize = 11.sp)
                }
                TextButton(onClick = onLogout) {
                    Text("↩", color = OnSurface, fontSize = 16.sp)
                }
            }
        }

        // Columna 3: contenido principal
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().background(Background)
        ) {
            when (currentDestination) {
                is MainDestination.Friends -> {
                    FriendsScreen(
                        onOpenDm = { dm ->
                            friendViewModel.selectDm(dm)
                            currentDestination = MainDestination.DirectMessage(dm)
                            chatViewModel.loadDmMessages(dm.id)
                        },
                        viewModel = friendViewModel
                    )
                }
                is MainDestination.DirectMessage -> {
                    val dm = selectedDm
                    if (dm != null) {
                        ChatArea(
                            title = dm.otherUsername,
                            messages = messages,
                            listState = listState,
                            messageInput = messageInput,
                            onInputChange = { messageInput = it },
                            onSend = {
                                if (messageInput.isNotBlank()) {
                                    chatViewModel.sendDmMessage(messageInput, dm.id)
                                    messageInput = ""
                                }
                            }
                        )
                    }
                }
                is MainDestination.ServerChat -> {
                    selectedChannel?.let { channel ->
                        ChatArea(
                            title = "# ${channel.name}",
                            messages = messages,
                            listState = listState,
                            messageInput = messageInput,
                            onInputChange = { messageInput = it },
                            onSend = {
                                if (messageInput.isNotBlank()) {
                                    chatViewModel.sendMessage(messageInput)
                                    messageInput = ""
                                }
                            }
                        )
                    } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Selecciona un canal", color = OnSurface, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatArea(
    title: String,
    messages: List<com.matteo.chatapp.model.Message>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    messageInput: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Background).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = OnBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        HorizontalDivider(color = SurfaceVariant)
        LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
            items(messages) { message -> MessageItem(message = message) }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = messageInput,
                onValueChange = onInputChange,
                placeholder = { Text("Escribe un mensaje...", color = OnSurface) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SurfaceVariant,
                    unfocusedBorderColor = SurfaceVariant,
                    focusedTextColor = OnBackground,
                    unfocusedTextColor = OnBackground,
                    cursorColor = Primary,
                    focusedContainerColor = SurfaceVariant,
                    unfocusedContainerColor = SurfaceVariant
                )
            )
            Button(onClick = onSend, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                Text("Enviar", color = OnPrimary)
            }
        }
    }
}