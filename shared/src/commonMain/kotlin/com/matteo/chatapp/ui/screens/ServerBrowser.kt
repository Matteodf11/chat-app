package com.matteo.chatapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matteo.chatapp.model.Channel
import com.matteo.chatapp.model.Server
import com.matteo.chatapp.ui.components.AvatarImage
import com.matteo.chatapp.ui.components.ChannelItem
import com.matteo.chatapp.ui.theme.*

@Composable
internal fun ServerChannelBrowser(server: Server, channels: List<Channel>, selectedChannelId: String?,
    loading: Boolean, error: String?, onRetry: () -> Unit, onSettings: () -> Unit,
    onOpenChannel: (Channel) -> Unit, modifier: Modifier = Modifier) {
    var query by remember(server.id) { mutableStateOf("") }
    val filtered = remember(channels, query) { channels.filter { it.name.contains(query.trim().removePrefix("#"), true) } }
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AvatarImage(server.iconUrl, server.name, 42)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(server.name, color = OnBackground, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(roleLabel(server.myRole), color = Primary, fontSize = 12.sp)
            }
            IconButton(onSettings, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.Settings, "Panel del servidor", tint = OnSurface, modifier = Modifier.size(20.dp)) }
        }
        HorizontalDivider(color = Outline)
        LazyColumn(Modifier.weight(1f).padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 12.dp)) {
            server.description?.takeIf { it.isNotBlank() }?.let { about -> item {
                Text(about, Modifier.padding(horizontal = 6.dp, vertical = 6.dp), color = Muted, fontSize = 12.sp,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
            } }
            item {
                OutlinedTextField(query, { query = it.take(100) }, Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Buscar canal", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, Modifier.size(18.dp)) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton({ query = "" }) { Icon(Icons.Rounded.Close, "Limpiar búsqueda") } },
                    shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary, unfocusedBorderColor = Outline, focusedTextColor = OnBackground, unfocusedTextColor = OnBackground))
            }
            item { SidebarSectionLabel("CANALES · ${channels.size}") }
            if (error != null) item {
                Text(error, Modifier.padding(8.dp), color = Error, fontSize = 12.sp)
                TextButton(onRetry, enabled = !loading) { Text("Reintentar") }
            }
            if (loading && channels.isEmpty()) items(3) { ServerPlaceholder() }
            else if (filtered.isEmpty() && error == null) item {
                Text(if (query.isNotBlank()) "No hay canales con ese nombre." else "Todavía no hay canales.",
                    Modifier.padding(12.dp), color = Muted, fontSize = 13.sp)
                if (query.isBlank() && server.myRole in setOf("owner", "admin"))
                    TextButton(onSettings) { Text("Gestionar canales") }
            }
            items(filtered, key = { it.id }) { channel ->
                ChannelItem(channel, selectedChannelId == channel.id) { onOpenChannel(channel) }
            }
        }
    }
}

@Composable
internal fun ServerDirectory(servers: List<Server>, loading: Boolean, error: String?, onRetry: () -> Unit,
    onOpen: (Server) -> Unit, onCreate: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(servers, query) { servers.filter { it.name.contains(query.trim(), true) } }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Tus espacios", color = OnBackground, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            Text("${servers.size} servidores · Conversaciones organizadas por canales", color = Muted, fontSize = 12.sp)
        }
        if (error != null) item {
            Text(error, color = Error, fontSize = 13.sp)
            TextButton(onRetry, enabled = !loading) { Text("Volver a cargar") }
        }
        if (servers.isNotEmpty()) item {
            OutlinedTextField(query, { query = it.take(100) }, Modifier.fillMaxWidth(), singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, null) }, placeholder = { Text("Buscar servidor") }, shape = RoundedCornerShape(14.dp))
        }
        if (loading && servers.isEmpty()) items(3) { ServerPlaceholder() }
        else if (servers.isEmpty() && error == null) item {
            ServerEmpty(Icons.Rounded.Dns, "Tu próximo grupo empieza aquí", "Crea un servidor o acepta una invitación para unirte.")
            Button(onCreate, Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Crear mi primer servidor")
            }
        }
        else if (filtered.isEmpty()) item { ServerEmpty(Icons.Rounded.Search, "Sin resultados", "Prueba con otro nombre de servidor.") }
        items(filtered, key = { it.id }) { server -> MobileServerRow(server) { onOpen(server) } }
    }
}

@Composable
internal fun ServerWelcome(server: Server, channels: List<Channel>, memberCount: Int, loading: Boolean,
    onSettings: () -> Unit, onOpen: (Channel) -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Surface(Modifier.widthIn(max = 620.dp).fillMaxWidth().heightIn(max = 560.dp), color = Surface, shape = RoundedCornerShape(24.dp)) {
            LazyColumn(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                item { AvatarImage(server.iconUrl, server.name, 80) }
                item { Text(server.name, color = OnBackground, fontWeight = FontWeight.Bold, fontSize = 26.sp) }
                item { Text(server.description?.takeIf { it.isNotBlank() } ?: "Un espacio para compartir y conversar.", color = Muted, fontSize = 14.sp) }
                item { Text("${channels.size} canales · $memberCount miembros · ${roleLabel(server.myRole)}", color = Primary, fontSize = 13.sp) }
                item { HorizontalDivider(color = Outline) }
                item { Text("Elige un canal para empezar", color = OnBackground, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
                if (loading && channels.isEmpty()) item { ServerPlaceholder() }
                else channels.firstOrNull()?.let { channel -> item {
                    Button({ onOpen(channel) }, shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Rounded.Tag, null); Spacer(Modifier.width(8.dp))
                        Text("Abrir ${channel.name}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                } }
                item { TextButton(onSettings) { Icon(Icons.Rounded.Settings, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Abrir panel del servidor") } }
            }
        }
    }
}
