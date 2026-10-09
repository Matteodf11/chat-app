package com.matteo.chatapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PersonAddAlt
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matteo.chatapp.model.DmConversation
import com.matteo.chatapp.model.FriendRequest
import com.matteo.chatapp.ui.theme.*
import com.matteo.chatapp.viewmodel.FriendViewModel

@Composable
fun FriendsScreen(
    onOpenDm: (DmConversation) -> Unit,
    onViewProfile: (String) -> Unit,
    viewModel: FriendViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val friends = state.friends
    val pending = state.pendingRequests
    val searchResult = state.searchResult
    val error = state.error

    var searchUsername by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("En linea", "Todos", "Pendientes", "Anadir")

    LaunchedEffect(Unit) {
        viewModel.loadAll()
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp)) {
            Text("Amigos", color = OnBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { index, tab ->
                    val selected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) PrimarySoft else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable { selectedTab = index }
                            .padding(horizontal = 13.dp, vertical = 8.dp)
                    ) {
                        Text(
                            tab,
                            color = if (selected) OnBackground else OnSurface,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Outline)

        error?.let {
            Text(it, color = Error, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp))
        }

        when (selectedTab) {
            0 -> FriendsList(
                friends = friends.filter { it.friendIsOnline },
                emptyText = "No hay amigos conectados",
                onOpenDm = { viewModel.openDm(it.friendId, onOpenDm) },
                onViewProfile = { onViewProfile(it.friendId) }
            )
            1 -> FriendsList(
                friends = friends,
                emptyText = "Aun no tienes amigos",
                onOpenDm = { viewModel.openDm(it.friendId, onOpenDm) },
                onViewProfile = { onViewProfile(it.friendId) }
            )
            2 -> PendingList(
                pending = pending,
                onAccept = { viewModel.acceptRequest(it) },
                onReject = { viewModel.rejectRequest(it) }
            )
            3 -> AddFriendTab(
                searchUsername = searchUsername,
                onSearchChange = { searchUsername = it },
                onSearch = { viewModel.searchUser(searchUsername) },
                searchResult = searchResult,
                onSendRequest = { viewModel.sendFriendRequest(it) }
            )
        }
    }
}

@Composable
fun FriendsList(
    friends: List<FriendRequest>,
    emptyText: String,
    onOpenDm: (FriendRequest) -> Unit,
    onViewProfile: (FriendRequest) -> Unit
) {
    if (friends.isEmpty()) {
        EmptyFriendsState(emptyText)
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(friends) { friend ->
            FriendRow(
                name = friend.friendUsername,
                avatarUrl = friend.friendAvatar,
                description = if (friend.friendIsOnline) "En linea" else "Desconectado",
                online = friend.friendIsOnline,
                onOpenDm = { onOpenDm(friend) },
                onViewProfile = { onViewProfile(friend) }
            )
        }
    }
}

@Composable
fun PendingList(
    pending: List<FriendRequest>,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit
) {
    if (pending.isEmpty()) {
        EmptyFriendsState("No hay solicitudes pendientes")
        return
    }

    val incoming = pending.filter { it.isIncoming }
    val outgoing = pending.filter { !it.isIncoming }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (incoming.isNotEmpty()) {
            item { SectionLabel("Recibidas", incoming.size) }
            items(incoming) { request ->
                RequestRow(
                    request = request,
                    detail = "Quiere anadirte como amigo",
                    primaryAction = "Aceptar",
                    secondaryAction = "Rechazar",
                    onPrimary = { onAccept(request.id) },
                    onSecondary = { onReject(request.id) }
                )
            }
        }

        if (outgoing.isNotEmpty()) {
            item { SectionLabel("Enviadas", outgoing.size) }
            items(outgoing) { request ->
                RequestRow(
                    request = request,
                    detail = "Esperando respuesta",
                    primaryAction = "En espera",
                    secondaryAction = null,
                    onPrimary = {},
                    onSecondary = {}
                )
            }
        }
    }
}

@Composable
fun AddFriendTab(
    searchUsername: String,
    onSearchChange: (String) -> Unit,
    onSearch: () -> Unit,
    searchResult: com.matteo.chatapp.model.UserProfile?,
    onSendRequest: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Anadir amigo", color = OnBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Busca por nombre de usuario.", color = OnSurface, fontSize = 13.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = searchUsername,
                onValueChange = onSearchChange,
                placeholder = { Text("Nombre de usuario", color = Muted) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = SurfaceElevated,
                    focusedTextColor = OnBackground,
                    unfocusedTextColor = OnBackground,
                    cursorColor = Primary,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface
                )
            )
            IconButton(
                onClick = onSearch,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Primary)
            ) {
                Icon(Icons.Rounded.Search, contentDescription = "Buscar usuario", tint = OnPrimary)
            }
        }

        searchResult?.let { user ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Surface)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PresenceAvatar(name = user.username, avatarUrl = user.avatarUrl, online = user.isOnline, size = 42)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.username, color = OnBackground, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(user.email, color = Muted, fontSize = 12.sp)
                }
                IconButton(
                    onClick = { onSendRequest(user.id) },
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Primary)
                ) {
                    Icon(Icons.Rounded.PersonAddAlt, contentDescription = "Enviar solicitud", tint = OnPrimary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun FriendRow(
    name: String,
    avatarUrl: String?,
    description: String,
    online: Boolean,
    onOpenDm: () -> Unit,
    onViewProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Surface.copy(alpha = 0.55f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.clickable { onViewProfile() }) {
            PresenceAvatar(name = name, avatarUrl = avatarUrl, online = online, size = 42)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f).clickable { onViewProfile() }) {
            Text(name, color = OnBackground, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(description, color = if (online) Accent else Muted, fontSize = 12.sp)
        }
        IconButton(onClick = onViewProfile, modifier = Modifier.size(38.dp)) {
            Icon(Icons.Rounded.PersonOutline, contentDescription = "Ver perfil", tint = OnSurface, modifier = Modifier.size(19.dp))
        }
        IconButton(onClick = onOpenDm, modifier = Modifier.size(38.dp)) {
            Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = "Enviar mensaje", tint = Primary, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun RequestRow(
    request: FriendRequest,
    detail: String,
    primaryAction: String,
    secondaryAction: String?,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Surface.copy(alpha = 0.55f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PresenceAvatar(name = request.friendUsername, avatarUrl = request.friendAvatar, online = request.friendIsOnline, size = 42)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(request.friendUsername, color = OnBackground, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(detail, color = Muted, fontSize = 12.sp)
        }
        if (secondaryAction == null) {
            Text(primaryAction, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp))
        } else {
            IconButton(onClick = onPrimary, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Rounded.Check, contentDescription = primaryAction, tint = Accent, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onSecondary, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = secondaryAction, tint = Error, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, count: Int) {
    Text(
        "$text - $count",
        color = Muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

@Composable
private fun EmptyFriendsState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = Muted, fontSize = 14.sp)
    }
}
