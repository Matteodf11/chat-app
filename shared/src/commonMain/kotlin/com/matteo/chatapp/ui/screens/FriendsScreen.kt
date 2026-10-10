package com.matteo.chatapp.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PersonAddAlt
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
    viewModel: FriendViewModel,
    conversations: List<DmConversation> = emptyList(),
    onPreferences: (DmConversation, com.matteo.chatapp.model.DmPreferencesRequest) -> Unit = { _, _ -> }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val friends = state.friends
    val pending = state.pendingRequests
    val searchResult = state.searchResult
    val error = state.error

    var searchUsername by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("En línea", "Todos", "Pendientes", "Añadir", "Bloqueados")

    LaunchedEffect(Unit) {
        viewModel.loadAll()
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {
            Text("Tu gente", color = OnBackground, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("${friends.count { it.friendIsOnline }} en línea · ${friends.size} amigos", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { index, tab ->
                    val selected = selectedTab == index
                    val tabBackground by animateColorAsState(if (selected) PrimarySoft else androidx.compose.ui.graphics.Color.Transparent, tween(140))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(tabBackground)
                            .clickable { selectedTab = index }
                            .padding(horizontal = 14.dp, vertical = 14.dp)
                    ) {
                        Text(
                            tab,
                            color = if (selected) Primary else OnSurface,
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

        Box(Modifier.weight(1f).fillMaxWidth().then(contentEntrance(selectedTab))) {
        when (selectedTab) {
            0 -> FriendsList(
                friends = friends.filter { it.friendIsOnline },
                emptyText = "No hay amigos conectados",
                conversations = conversations,
                onProfile = { viewModel.loadUserProfile(it.friendId) },
                onPreferences = onPreferences,
                onOpenDm = { viewModel.openDm(it.friendId, onOpenDm) }
            )
            1 -> FriendsList(
                friends = friends,
                emptyText = "Aun no tienes amigos",
                conversations = conversations,
                onProfile = { viewModel.loadUserProfile(it.friendId) },
                onPreferences = onPreferences,
                onOpenDm = { viewModel.openDm(it.friendId, onOpenDm) }
            )
            2 -> PendingList(
                pending = pending,
                onAccept = { viewModel.acceptRequest(it) },
                onReject = { viewModel.rejectRequest(it) }
            )
            4 -> LazyColumn(Modifier.fillMaxSize().padding(18.dp)) {
                if (state.blockedUsers.isEmpty()) item { Text("No tienes usuarios bloqueados", color = Muted, modifier = Modifier.padding(16.dp)) }
                items(state.blockedUsers, key = { it.id }) { user ->
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        com.matteo.chatapp.ui.components.AvatarImage(user.avatarUrl, user.username, 40)
                        Text(user.username, color = OnBackground, modifier = Modifier.weight(1f).padding(12.dp))
                        TextButton(enabled = !state.contactActionLoading, onClick = { viewModel.unblockUser(user.id) }) { Text("Desbloquear") }
                    }
                }
            }
            3 -> AddFriendTab(
                searchUsername = searchUsername,
                onSearchChange = { searchUsername = it },
                onSearch = { viewModel.searchUser(searchUsername) },
                searchResult = searchResult,
                isSearching = state.isSearchingUser,
                onSendRequest = { viewModel.sendFriendRequest(it) }
            )
        }
        }
    }
}

@Composable
fun FriendsList(
    friends: List<FriendRequest>,
    emptyText: String,
    onOpenDm: (FriendRequest) -> Unit,
    conversations: List<DmConversation> = emptyList(),
    onProfile: (FriendRequest) -> Unit = {},
    onPreferences: (DmConversation, com.matteo.chatapp.model.DmPreferencesRequest) -> Unit = { _, _ -> }
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
                dm = conversations.firstOrNull { it.otherUserId == friend.friendId },
                onProfile = { onProfile(friend) },
                onPreferences = { value -> conversations.firstOrNull { it.otherUserId == friend.friendId }?.let { onPreferences(it, value) } }
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
    isSearching: Boolean = false,
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
                enabled = searchUsername.isNotBlank() && !isSearching,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Primary)
            ) {
                if (isSearching) CircularProgressIndicator(Modifier.size(20.dp), color = OnPrimary, strokeWidth = 2.dp)
                else Icon(Icons.Rounded.Search, contentDescription = "Buscar usuario", tint = OnPrimary)
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
    dm: DmConversation?,
    onProfile: () -> Unit,
    onPreferences: (com.matteo.chatapp.model.DmPreferencesRequest) -> Unit
) {
    var expanded by remember(name) { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Surface)
                .combinedClickable(onClickLabel = "Abrir chat con $name", role = androidx.compose.ui.semantics.Role.Button,
                    onClick = onOpenDm, onLongClickLabel = "Opciones de $name", onLongClick = { expanded = true })
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PresenceAvatar(name = name, avatarUrl = avatarUrl, online = online, size = 42)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = OnBackground, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(description, color = if (online) Accent else Muted, fontSize = 12.sp)
            }
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Rounded.MoreVert, "Opciones de $name", tint = Muted)
            }
        }
        ConversationMenu(expanded, { expanded = false }, dm, onOpenDm, onProfile, onPreferences,
            title = name, avatarUrl = avatarUrl)
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
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
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
    EmptyState(text)
}
