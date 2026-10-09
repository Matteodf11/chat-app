package com.matteo.chatapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matteo.chatapp.model.Server
import com.matteo.chatapp.model.ServerInvitation
import com.matteo.chatapp.model.ServerMember
import com.matteo.chatapp.platform.PickedAvatarFile
import com.matteo.chatapp.platform.pickProfileImageFile
import com.matteo.chatapp.repository.SessionManager
import com.matteo.chatapp.ui.components.AvatarImage
import com.matteo.chatapp.ui.theme.*
import kotlinx.coroutines.launch

@Composable
internal fun CreateServerDialog(
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
internal fun ServerInvitationsDialog(
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
internal fun ServerSettingsDialog(
    server: Server,
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

internal fun roleLabel(role: String?): String {
    return when (role) {
        "owner" -> "Dueno"
        "admin" -> "Admin"
        else -> "Miembro"
    }
}

@Composable
internal fun SettingsField(
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
