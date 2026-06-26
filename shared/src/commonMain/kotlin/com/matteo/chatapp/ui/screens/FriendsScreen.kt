package com.matteo.chatapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matteo.chatapp.model.DmConversation
import com.matteo.chatapp.model.FriendRequest
import com.matteo.chatapp.ui.theme.*
import com.matteo.chatapp.viewmodel.FriendViewModel

@Composable
fun FriendsScreen(
    onOpenDm: (DmConversation) -> Unit,
    viewModel: FriendViewModel = viewModel { FriendViewModel() }
) {
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val pending by viewModel.pendingRequests.collectAsStateWithLifecycle()
    val dms by viewModel.dms.collectAsStateWithLifecycle()
    val searchResult by viewModel.searchResult.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var searchUsername by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("En línea", "Todos", "Pendientes", "Añadir amigo")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Background)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Amigos",
                color = OnBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(16.dp))
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) SurfaceVariant else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { selectedTab = index }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tab,
                        color = if (isSelected) OnBackground else OnSurface,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        HorizontalDivider(color = SurfaceVariant)

        when (selectedTab) {
            0 -> FriendsList(
                friends = friends.filter { it.friendIsOnline },
                onOpenDm = { viewModel.openDm(it.friendId); onOpenDm(dms.find { dm -> dm.otherUserId == it.friendId } ?: return@FriendsList) }
            )
            1 -> FriendsList(
                friends = friends,
                onOpenDm = { viewModel.openDm(it.friendId) }
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
                error = error,
                onSendRequest = { viewModel.sendFriendRequest(it) }
            )
        }
    }
}

@Composable
fun FriendsList(
    friends: List<FriendRequest>,
    onOpenDm: (FriendRequest) -> Unit
) {
    if (friends.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay amigos aquí todavía", color = OnSurface, fontSize = 14.sp)
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        items(friends) { friend ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenDm(friend) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(friend.friendUsername.take(1).uppercase(), color = OnBackground, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (friend.friendIsOnline) androidx.compose.ui.graphics.Color(0xFF23A55A) else OnSurface)
                            .align(Alignment.BottomEnd)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(friend.friendUsername, color = OnBackground, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(if (friend.friendIsOnline) "En línea" else "Desconectado", color = OnSurface, fontSize = 12.sp)
                }
            }
            HorizontalDivider(color = SurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp))
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
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay solicitudes pendientes", color = OnSurface, fontSize = 14.sp)
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        items(pending) { request ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(request.friendUsername.take(1).uppercase(), color = OnBackground, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Text(request.friendUsername, color = OnBackground, modifier = Modifier.weight(1f), fontSize = 15.sp)
                IconButton(onClick = { onAccept(request.id) }) {
                    Text("✓", color = androidx.compose.ui.graphics.Color(0xFF23A55A), fontSize = 18.sp)
                }
                IconButton(onClick = { onReject(request.id) }) {
                    Text("✗", color = Error, fontSize = 18.sp)
                }
            }
            HorizontalDivider(color = SurfaceVariant)
        }
    }
}

@Composable
fun AddFriendTab(
    searchUsername: String,
    onSearchChange: (String) -> Unit,
    onSearch: () -> Unit,
    searchResult: com.matteo.chatapp.model.UserProfile?,
    error: String?,
    onSendRequest: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Añadir amigo", color = OnBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Puedes añadir amigos usando su nombre de usuario.", color = OnSurface, fontSize = 14.sp)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = searchUsername,
                onValueChange = onSearchChange,
                placeholder = { Text("Nombre de usuario", color = OnSurface) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = SurfaceVariant,
                    focusedTextColor = OnBackground,
                    unfocusedTextColor = OnBackground,
                    cursorColor = Primary,
                    focusedContainerColor = SurfaceVariant,
                    unfocusedContainerColor = SurfaceVariant
                )
            )
            Button(
                onClick = onSearch,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Buscar", color = OnPrimary)
            }
        }

        error?.let {
            Text(it, color = Error, fontSize = 14.sp)
        }

        searchResult?.let { user ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Surface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(user.username.take(1).uppercase(), color = OnBackground, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.username, color = OnBackground, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(user.email, color = OnSurface, fontSize = 12.sp)
                }
                Button(
                    onClick = { onSendRequest(user.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Añadir", color = OnPrimary, fontSize = 13.sp)
                }
            }
        }
    }
}