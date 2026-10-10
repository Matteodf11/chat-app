package com.matteo.chatapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Description
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.DialogProperties
import com.matteo.chatapp.ui.components.AvatarImage
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.matteo.chatapp.model.Channel
import com.matteo.chatapp.model.DmConversation
import com.matteo.chatapp.model.FriendRequest
import com.matteo.chatapp.model.Message
import com.matteo.chatapp.model.Server
import com.matteo.chatapp.ui.theme.*

@Composable
internal fun GlobalSearchDialog(
    friends: List<FriendRequest>,
    dms: List<DmConversation>,
    servers: List<Server>,
    channels: List<Channel>,
    onDismiss: () -> Unit,
    onFriend: (String) -> Unit,
    onDm: (DmConversation) -> Unit,
    onServer: (Server) -> Unit,
    onChannel: (Channel) -> Unit
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
internal fun MessageSearchDialog(
    results: List<Message>,
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
    var showFilters by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    val filterCount = listOf(sender, from, to).count { it.isNotBlank() }

    val canSearch = query.isNotBlank() || sender.isNotBlank() || from.isNotBlank() || to.isNotBlank() || attachmentsOnly
    val submitSearch = {
        if (!isValidSearchDate(from) || !isValidSearchDate(to)) {
            localError = "Usa fechas con formato AAAA-MM-DD"
        } else if (from.isNotBlank() && to.isNotBlank() && from > to) {
            localError = "La fecha inicial no puede ser posterior a la final"
        } else {
            localError = null
            submitted = true
            onSearch(query, sender, from, to, attachmentsOnly)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 720.dp).fillMaxWidth().heightIn(max = 720.dp).padding(12.dp).imePadding(),
            shape = RoundedCornerShape(24.dp), color = Surface) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Buscar mensajes", color = OnBackground, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text("Encuentra algo en esta conversación", color = Muted, fontSize = 12.sp)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Cerrar búsqueda", tint = Muted) }
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(query, { query = it.take(120) }, Modifier.weight(1f),
                        singleLine = true, placeholder = { Text("Texto o archivo") },
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Muted) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { if (canSearch && !isSearching) submitSearch() }),
                        colors = searchFieldColors())
                    FilledIconButton(onClick = submitSearch, enabled = canSearch && !isSearching,
                        modifier = Modifier.size(48.dp), shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Rounded.Search, "Buscar mensajes")
                    }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(selected = showFilters || filterCount > 0, onClick = { showFilters = !showFilters },
                        leadingIcon = { Icon(Icons.Rounded.Tune, null, Modifier.size(16.dp)) },
                        label = { Text(if (filterCount > 0) "Filtros · $filterCount" else "Filtros") })
                    FilterChip(selected = attachmentsOnly, onClick = { attachmentsOnly = !attachmentsOnly },
                        leadingIcon = { Icon(Icons.Rounded.AttachFile, null, Modifier.size(16.dp)) },
                        label = { Text("Solo archivos") })
                    if (filterCount > 0 || attachmentsOnly) TextButton(onClick = {
                        sender = ""; from = ""; to = ""; attachmentsOnly = false; localError = null
                    }) { Text("Limpiar filtros") }
                }
                if (isSearching) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Primary)
                HorizontalDivider(color = Outline)
                LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(min = 180.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 14.dp)) {
                    item(key = "filters") {
                        AnimatedVisibility(showFilters) {
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .border(1.dp, Outline, RoundedCornerShape(16.dp)).padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Afinar búsqueda", color = OnBackground, style = MaterialTheme.typography.labelLarge)
                                SearchFilterField(sender, { sender = it.take(80); localError = null }, "Autor", Modifier.fillMaxWidth())
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    SearchFilterField(from, { from = it.take(10); localError = null }, "Desde", Modifier.weight(1f), "AAAA-MM-DD")
                                    SearchFilterField(to, { to = it.take(10); localError = null }, "Hasta", Modifier.weight(1f), "AAAA-MM-DD")
                                }
                            }
                        }
                    }
                    (localError ?: error)?.let { message -> item { Text(message, color = Error, style = MaterialTheme.typography.bodySmall) } }
                    if (results.isEmpty() && !isSearching && localError == null && error == null) item {
                        Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.Search, null, tint = Muted, modifier = Modifier.size(30.dp))
                            Text(if (submitted) "No encontramos mensajes" else "¿Qué quieres encontrar?",
                                color = OnBackground, fontWeight = FontWeight.Medium)
                            Text(if (submitted) "Prueba con otras palabras o ajusta los filtros." else "Busca un texto, un archivo o filtra por autor.",
                                color = Muted, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                    if (results.isNotEmpty()) item {
                        Text(if (isSearching) "Buscando…" else "${results.size} resultados", color = Muted, style = MaterialTheme.typography.labelMedium)
                    }
                    items(results, key = { it.id }) { message ->
                        Surface(color = SurfaceVariant, shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(enabled = !isSearching) { onOpenResult(message.id) }) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                    AvatarImage(message.senderAvatar, message.senderUsername, 30)
                                    Column(Modifier.weight(1f)) {
                                        Text(message.senderUsername, color = OnBackground, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(message.createdAt?.replace('T', ' ')?.take(16).orEmpty(), color = Muted, fontSize = 11.sp)
                                    }
                                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, "Abrir mensaje", tint = Muted, modifier = Modifier.size(18.dp))
                                }
                                if (message.content.isNotBlank()) Text(message.content, color = OnSurface, fontSize = 14.sp,
                                    lineHeight = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                message.attachmentName?.let { name ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Rounded.Description, null, tint = Primary, modifier = Modifier.size(16.dp))
                                        Text(name, color = Primary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
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
