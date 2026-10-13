package com.matteo.chatapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.matteo.chatapp.model.*
import com.matteo.chatapp.platform.PickedAvatarFile
import com.matteo.chatapp.platform.pickProfileImageFile
import com.matteo.chatapp.repository.SessionManager
import com.matteo.chatapp.ui.components.*
import com.matteo.chatapp.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
private fun ServerPanel(title: String, subtitle: String, name: String, iconUrl: String? = null,
    busy: Boolean = false, onDismiss: () -> Unit, footer: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(16.dp).widthIn(max = 760.dp).fillMaxWidth().heightIn(max = 760.dp),
            color = Surface, shape = RoundedCornerShape(24.dp), tonalElevation = 0.dp) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AvatarImage(iconUrl, name, 48)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(title, color = OnBackground, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(subtitle, color = Muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = onDismiss, enabled = !busy) { Icon(Icons.Rounded.Close, "Cerrar panel", tint = Muted) }
                }
                content()
                footer()
            }
        }
    }
}

@Composable
private fun ServerSection(title: String, subtitle: String? = null, icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit) {
    Surface(color = SurfaceVariant, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, null, tint = Primary, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = OnBackground, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    subtitle?.let { Text(it, color = Muted, fontSize = 12.sp) }
                }
            }
            content()
        }
    }
}

@Composable
private fun ServerFeedback(error: String?, notice: String? = null) {
    val text = error ?: notice ?: return
    Surface(color = if (error != null) Error.copy(alpha = .10f) else PrimarySoft, shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(if (error != null) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle,
                null, tint = if (error != null) Error else Primary, modifier = Modifier.size(18.dp))
            Text(text, color = if (error != null) Error else OnBackground, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ServerSubmit(label: String, working: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick, enabled = enabled && !working, shape = RoundedCornerShape(14.dp)) {
        if (working) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
        else Icon(Icons.Rounded.Check, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun ServerIconEditor(name: String, url: String?, file: PickedAvatarFile?, enabled: Boolean,
    onPicking: (Boolean) -> Unit = {}, onChange: (PickedAvatarFile?) -> Unit) {
    val scope = rememberCoroutineScope()
    var picking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val preview by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, file) {
        value = file?.let { decodePickedImage(it.bytes, 256) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically) {
            val image = preview
            if (image != null) Image(image, "Vista previa del icono", Modifier.size(64.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            else AvatarImage(url, name.ifBlank { "Servidor" }, 64)
            OutlinedButton(enabled = enabled && !picking, onClick = {
                picking = true; onPicking(true); error = null
                scope.launch {
                    try {
                        val picked = pickProfileImageFile("Elegir icono del servidor")
                        if (picked != null) {
                            require(picked.bytes.size <= 2 * 1024 * 1024) { "El icono no puede superar 2 MB" }
                            require(picked.contentType in setOf("image/jpeg", "image/png", "image/webp")) { "Elige una imagen JPG, PNG o WEBP" }
                            require(decodePickedImage(picked.bytes, 256) != null) { "No se pudo abrir la imagen" }
                            onChange(picked)
                        }
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { error = e.message ?: "No se pudo elegir el icono" }
                    finally { picking = false; onPicking(false) }
                }
            }) {
                Icon(Icons.Rounded.Image, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text(if (file == null) "Elegir icono" else "Cambiar selección")
            }
            if (file != null) TextButton(enabled = enabled && !picking, onClick = { onChange(null) }) { Text("Deshacer") }
        }
        Text(file?.fileName ?: "JPG, PNG o WEBP · Hasta 2 MB", color = Muted, fontSize = 12.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        error?.let { Text(it, color = Error, fontSize = 12.sp) }
    }
}

@Composable
internal fun CreateServerDialog(error: String?, busy: Boolean = false, onDismiss: () -> Unit,
    onCreate: (String, String, PickedAvatarFile?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf<PickedAvatarFile?>(null) }
    var pickingIcon by remember { mutableStateOf(false) }
    val valid = name.trim().length in 2..100
    ServerPanel("Crear servidor", "Un espacio para conversar con tu grupo", name.ifBlank { "Servidor" },
        busy = busy || pickingIcon, onDismiss = onDismiss, footer = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss, enabled = !busy && !pickingIcon) { Text("Cancelar") }
                Spacer(Modifier.width(8.dp))
                ServerSubmit(if (busy) "Creando…" else "Crear servidor", busy, valid && !pickingIcon) { onCreate(name.trim(), description.trim(), icon) }
            }
        }) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ServerFeedback(error)
            ServerSection("Identidad del servidor", "Así lo verán los miembros", Icons.Rounded.Dns) {
                SettingsField("Nombre del servidor", name, enabled = !busy) { name = it.take(100) }
                Text("Entre 2 y 100 caracteres", color = Muted, fontSize = 12.sp)
                SettingsField("Descripción (opcional)", description, enabled = !busy, multiline = true) { description = it }
                ServerIconEditor(name, null, icon, !busy, onPicking = { pickingIcon = it }) { icon = it }
            }
            Text("Después podrás crear canales e invitar a otras personas desde el panel del servidor.", color = Muted, fontSize = 13.sp)
        }
    }
}

@Composable
internal fun ServerInvitationsDialog(invitations: List<ServerInvitation>, error: String?,
    busy: Boolean = false, loading: Boolean = false, onRetry: () -> Unit = {},
    onDismiss: () -> Unit, onAccept: (String) -> Unit, onReject: (String) -> Unit) {
    ServerPanel("Invitaciones", "Únete a los espacios a los que te han invitado", "Invitaciones",
        busy = busy, onDismiss = onDismiss) {
        LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ServerFeedback(error) }
            if (error != null) item { TextButton(onClick = onRetry, enabled = !loading && !busy) { Text("Volver a cargar") } }
            if (loading && invitations.isEmpty()) items(3) { ServerPlaceholder() }
            else if (invitations.isEmpty()) item { ServerEmpty(Icons.Rounded.MailOutline,
                "Todo al día", "Cuando alguien te invite a un servidor, aparecerá aquí.") }
            items(invitations, key = { it.id }) { invitation ->
                Surface(color = SurfaceVariant, shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AvatarImage(invitation.serverIconUrl, invitation.serverName, 48)
                            Column(Modifier.weight(1f)) {
                                Text(invitation.serverName, color = OnBackground, fontWeight = FontWeight.SemiBold,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("Te invita ${invitation.inviterUsername}", color = Muted, fontSize = 12.sp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton({ onReject(invitation.id) }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Rechazar") }
                            Button({ onAccept(invitation.id) }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Unirme") }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ServerSettingsDialog(server: Server, serverName: String, description: String,
    members: List<ServerMember>, channels: List<Channel>, error: String?, notice: String?, action: String?,
    loadingMembers: Boolean = false, loadingChannels: Boolean = false, channelsError: String? = null,
    onReload: () -> Unit, onDismiss: () -> Unit,
    onSave: (String, String, PickedAvatarFile?, () -> Unit) -> Unit,
    onCreateChannel: (String, () -> Unit) -> Unit, onInvite: (String, () -> Unit) -> Unit,
    onOpenChannel: (Channel) -> Unit, onRoleChange: (String, String) -> Unit,
    onRemoveMember: (String) -> Unit, onLeave: () -> Unit, onDelete: () -> Unit) {
    var name by remember(server.id, serverName) { mutableStateOf(serverName) }
    var about by remember(server.id, description) { mutableStateOf(description) }
    var icon by remember(server.id) { mutableStateOf<PickedAvatarFile?>(null) }
    var newChannel by remember(server.id) { mutableStateOf("") }
    var invite by remember(server.id) { mutableStateOf("") }
    var query by remember(server.id) { mutableStateOf("") }
    var tab by remember(server.id) { mutableStateOf(0) }
    var confirmDanger by remember(server.id) { mutableStateOf(false) }
    val tabStates = remember(server.id) { List(3) { androidx.compose.foundation.lazy.LazyListState() } }
    val canManage = server.myRole in setOf("owner", "admin")
    val isOwner = server.myRole == "owner"
    var pickingIcon by remember(server.id) { mutableStateOf(false) }
    val busy = action != null || pickingIcon
    val dirty = name.trim() != serverName || about.trim() != description || icon != null
    val shownMembers = remember(members, query) { members.filter { it.username.contains(query.trim(), ignoreCase = true) } }
    val shownChannels = remember(channels, query) { channels.filter { it.name.contains(query.trim().removePrefix("#"), ignoreCase = true) } }
    ServerPanel(server.name, "Tu rol: ${roleLabel(server.myRole)}", server.name, server.iconUrl, busy, onDismiss,
        footer = {
            if (tab == 0 && canManage) {
                HorizontalDivider(color = Outline)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    ServerSubmit(if (action == "save") "Guardando…" else "Guardar cambios", action == "save",
                        !busy && dirty && name.trim().length in 2..100) {
                        onSave(name.trim(), about.trim(), icon) { icon = null }
                    }
                }
            }
        }) {
        ScrollableTabRow(tab, containerColor = Surface, contentColor = Primary, edgePadding = 0.dp, divider = {}) {
            listOf("General", "Canales", "Miembros").forEachIndexed { index, label ->
                Tab(selected = tab == index, enabled = !busy, onClick = { tab = index; query = "" },
                    text = { Text(label) }, icon = { Icon(when (index) {
                        0 -> Icons.Rounded.Tune; 1 -> Icons.Rounded.Tag; else -> Icons.Rounded.People
                    }, null, Modifier.size(18.dp)) })
            }
        }
        ServerFeedback(error, notice)
        LazyColumn(Modifier.weight(1f), state = tabStates[tab], verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (tab) {
                0 -> {
                    item {
                        ServerSection("Identidad", if (canManage) "Nombre, descripción e icono" else "Sobre este servidor", Icons.Rounded.Dns) {
                            if (canManage) {
                                SettingsField("Nombre del servidor", name, enabled = !busy) { name = it.take(100) }
                                SettingsField("Descripción (opcional)", about, enabled = !busy, multiline = true) { about = it }
                                ServerIconEditor(name, server.iconUrl, icon, !busy, onPicking = { pickingIcon = it }) { icon = it }
                            } else {
                                Text(server.description?.takeIf { it.isNotBlank() } ?: "Este servidor todavía no tiene descripción.", color = OnSurface, fontSize = 14.sp)
                            }
                        }
                    }
                    item {
                        ServerSection("Tu espacio", "Creado por ${server.ownerUsername}", Icons.Rounded.Groups) {
                            Text("${channels.size} canales · ${members.size} miembros", color = OnBackground, fontSize = 14.sp)
                            Text(when (server.myRole) {
                                "owner" -> "Como dueño puedes gestionar el servidor, los canales y los roles de sus miembros."
                                "admin" -> "Puedes editar el servidor, crear canales, invitar y gestionar miembros."
                                else -> "Puedes participar en los canales y consultar quién forma parte del servidor."
                            }, color = Muted, fontSize = 13.sp)
                        }
                    }
                    item {
                        ServerSection(if (isOwner) "Eliminar servidor" else "Salir del servidor", null,
                            if (isOwner) Icons.Rounded.DeleteOutline else Icons.AutoMirrored.Rounded.ExitToApp) {
                            Text(if (isOwner) "Se eliminarán el servidor, sus canales y sus mensajes para todos los miembros."
                                else "Perderás el acceso a sus canales. Para volver necesitarás otra invitación.", color = Muted, fontSize = 13.sp)
                            OutlinedButton({ confirmDanger = true }, enabled = !busy,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Error)) {
                                Text(if (isOwner) "Eliminar servidor" else "Salir del servidor")
                            }
                        }
                    }
                }
                1 -> {
                    if (canManage) item {
                        ServerSection("Crear canal", "Organiza las conversaciones por temas", Icons.Rounded.Add) {
                            SettingsField("Nombre del canal", newChannel, enabled = !busy,
                                onSubmit = { if (!busy && newChannel.trim().length in 2..100) onCreateChannel(newChannel.trim()) { newChannel = "" } }) {
                                newChannel = it.take(100)
                            }
                            ServerSubmit(if (action == "channel") "Creando…" else "Crear canal", action == "channel",
                                !busy && newChannel.trim().length in 2..100) { onCreateChannel(newChannel.trim()) { newChannel = "" } }
                        }
                    }
                    item { ServerListSearch(query, "Buscar canal", channels.size, "canales") { query = it } }
                    if (channelsError != null) item {
                        ServerFeedback(channelsError)
                        TextButton(onClick = onReload, enabled = !loadingChannels && !busy) { Text("Volver a cargar") }
                    }
                    if (loadingChannels && channels.isEmpty()) items(3) { ServerPlaceholder() }
                    else if (shownChannels.isEmpty()) item { ServerEmpty(Icons.Rounded.Tag,
                        if (query.isBlank()) "Todavía no hay canales" else "Sin resultados",
                        if (query.isNotBlank()) "Prueba con otro nombre." else if (canManage) "Crea el primer canal para empezar a conversar." else "Un administrador puede crear canales para este servidor.") }
                    items(shownChannels, key = { it.id }) { channel ->
                        Surface(onClick = { if (!busy) onOpenChannel(channel) }, enabled = !busy,
                            color = SurfaceVariant, shape = RoundedCornerShape(14.dp)) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Rounded.Tag, null, tint = Primary)
                                Text(channel.name, Modifier.weight(1f), color = OnBackground, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Icon(Icons.Rounded.ChevronRight, "Abrir canal", tint = Muted)
                            }
                        }
                    }
                }
                2 -> {
                    if (canManage) item {
                        ServerSection("Invitar a alguien", "Recibirá una invitación dentro de Chimera", Icons.Rounded.PersonAdd) {
                            SettingsField("Nombre de usuario o correo", invite, enabled = !busy,
                                onSubmit = { if (!busy && invite.isNotBlank()) onInvite(invite.trim()) { invite = "" } }) { invite = it.take(254) }
                            ServerSubmit(if (action == "invite") "Enviando…" else "Enviar invitación", action == "invite",
                                !busy && invite.isNotBlank()) { onInvite(invite.trim()) { invite = "" } }
                        }
                    }
                    item { ServerListSearch(query, "Buscar miembro", members.size, "miembros") { query = it } }
                    if (loadingMembers && members.isEmpty()) items(3) { ServerPlaceholder() }
                    else if (shownMembers.isEmpty()) item { ServerEmpty(Icons.Rounded.People,
                        "No hay miembros que mostrar", "Prueba con otro nombre o vuelve a cargar la lista.") }
                    items(shownMembers, key = { it.userId }) { member ->
                        ServerMemberRow(member, server.myRole, SessionManager.getUserId(), busy, onRoleChange, onRemoveMember)
                    }
                    item { TextButton(onClick = onReload, enabled = !loadingMembers && !busy) { Text("Actualizar miembros") } }
                }
            }
        }
    }
    if (confirmDanger) AlertDialog(onDismissRequest = { confirmDanger = false },
        containerColor = Surface, shape = RoundedCornerShape(24.dp),
        title = { Text(if (isOwner) "¿Eliminar ${server.name}?" else "¿Salir de ${server.name}?") },
        text = { Text(if (isOwner) "Esta acción no se puede deshacer. Se perderán todos sus canales y mensajes."
            else "Necesitarás una nueva invitación para volver a entrar.") },
        confirmButton = { TextButton(enabled = !busy, onClick = { confirmDanger = false; if (isOwner) onDelete() else onLeave() }) {
            Text(if (isOwner) "Eliminar servidor" else "Salir", color = Error)
        } }, dismissButton = { TextButton({ confirmDanger = false }) { Text("Cancelar") } })
}

@Composable
private fun ServerListSearch(query: String, placeholder: String, count: Int, noun: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$count $noun", color = Muted, fontSize = 12.sp)
        OutlinedTextField(query, { onChange(it.take(100)) }, Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text(placeholder) }, leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton({ onChange("") }) { Icon(Icons.Rounded.Close, "Limpiar búsqueda") } },
            shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary,
                unfocusedBorderColor = Outline, focusedTextColor = OnBackground, unfocusedTextColor = OnBackground))
    }
}

@Composable
internal fun ServerEmpty(icon: ImageVector, title: String, detail: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = Primary, modifier = Modifier.size(32.dp))
        Text(title, color = OnBackground, fontWeight = FontWeight.SemiBold)
        Text(detail, color = Muted, fontSize = 13.sp)
    }
}

@Composable
internal fun ServerPlaceholder() {
    Box(Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(14.dp)).background(SurfaceVariant))
}

@Composable
private fun ServerMemberRow(member: ServerMember, viewerRole: String?, currentUserId: String?, busy: Boolean,
    onRoleChange: (String, String) -> Unit, onRemove: (String) -> Unit) {
    var showMenu by remember(member.userId) { mutableStateOf(false) }
    var confirmRemove by remember(member.userId) { mutableStateOf(false) }
    val isSelf = member.userId == currentUserId
    val canChangeRole = viewerRole == "owner" && member.role != "owner" && !isSelf
    val canRemove = !isSelf && member.role != "owner" && (viewerRole == "owner" || (viewerRole == "admin" && member.role == "member"))
    Surface(color = SurfaceVariant, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            PresenceAvatar(member.username, member.avatarUrl, member.isOnline, 40)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(member.username + if (isSelf) " (tú)" else "", color = OnBackground, fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${roleLabel(member.role)} · ${if (member.isOnline) "En línea" else "Desconectado"}", color = Muted, fontSize = 12.sp)
            }
            if (canChangeRole || canRemove) IconButton({ showMenu = true }, enabled = !busy) {
                Icon(Icons.Rounded.MoreVert, "Opciones de ${member.username}", tint = Muted)
            }
        }
    }
    if (showMenu && !busy) ActionSheet("Gestionar a ${member.username}", { showMenu = false }, member.username, member.avatarUrl) {
        if (canChangeRole) ActionSheetItem(if (member.role == "admin") "Quitar rol de administrador" else "Hacer administrador",
            Icons.Rounded.AdminPanelSettings) { showMenu = false; onRoleChange(member.userId, if (member.role == "admin") "member" else "admin") }
        if (canRemove) ActionSheetItem("Expulsar del servidor", Icons.Rounded.PersonRemove) { showMenu = false; confirmRemove = true }
    }
    if (confirmRemove) AlertDialog(onDismissRequest = { confirmRemove = false }, containerColor = Surface,
        shape = RoundedCornerShape(24.dp), title = { Text("¿Expulsar a ${member.username}?") },
        text = { Text("Perderá el acceso al servidor y a sus canales. Podrá volver si recibe otra invitación.") },
        confirmButton = { TextButton(enabled = !busy, onClick = { confirmRemove = false; onRemove(member.userId) }) { Text("Expulsar", color = Error) } },
        dismissButton = { TextButton({ confirmRemove = false }) { Text("Cancelar") } })
}

internal fun roleLabel(role: String?): String = when (role) {
    "owner" -> "Dueño"
    "admin" -> "Administrador"
    else -> "Miembro"
}

@Composable
internal fun SettingsField(label: String, value: String, onSubmit: (() -> Unit)? = null,
    enabled: Boolean = true, multiline: Boolean = false, onValueChange: (String) -> Unit) {
    OutlinedTextField(value, onValueChange, label = { Text(label) }, enabled = enabled,
        modifier = Modifier.fillMaxWidth().onPreviewKeyEvent { event ->
            if (enabled && onSubmit != null && event.key == Key.Enter && event.type == KeyEventType.KeyDown) { onSubmit(); true } else false
        }, shape = RoundedCornerShape(14.dp), singleLine = !multiline, minLines = if (multiline) 3 else 1, maxLines = if (multiline) 5 else 1,
        keyboardOptions = KeyboardOptions(imeAction = if (multiline) ImeAction.Default else if (onSubmit != null) ImeAction.Done else ImeAction.Next),
        keyboardActions = KeyboardActions(onDone = { if (enabled) onSubmit?.invoke() }),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = Outline,
            focusedLabelColor = Primary, unfocusedLabelColor = Muted, focusedTextColor = OnBackground,
            unfocusedTextColor = OnBackground, cursorColor = Primary, focusedContainerColor = Background,
            unfocusedContainerColor = Background))
}
