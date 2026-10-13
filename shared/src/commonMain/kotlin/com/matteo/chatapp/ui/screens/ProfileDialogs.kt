package com.matteo.chatapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.DialogProperties
import com.matteo.chatapp.ui.components.decodePickedImage
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matteo.chatapp.model.UserProfile
import com.matteo.chatapp.platform.PickedAvatarFile
import com.matteo.chatapp.platform.pickProfileImageFile
import com.matteo.chatapp.ui.components.AvatarImage
import com.matteo.chatapp.ui.components.ProfileBannerImage
import com.matteo.chatapp.ui.theme.*
import kotlinx.coroutines.launch

@Composable
internal fun GeneralSettingsDialog(
    themeMode: ThemeMode,
    themeAccent: ThemeAccent,
    onThemeModeChange: (ThemeMode) -> Unit,
    onThemeAccentChange: (ThemeAccent) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp,
        title = { Text("Ajustes", color = OnBackground, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
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
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                HorizontalDivider(color = Outline)
                StorageSettings()
                HorizontalDivider(color = Outline)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Con teclado", style = MaterialTheme.typography.titleMedium, color = OnBackground)
                    Text("Ctrl + F · Buscar en el chat\nCtrl + K · Búsqueda general\nCtrl + , · Ajustes\nEsc · Cancelar respuesta al escribir",
                        color = Muted, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar", color = Primary) }
        }
    )
}

@Composable
internal fun ProfileSettingsDialog(
    username: String,
    avatarUrl: String,
    bannerUrl: String,
    status: String,
    bio: String,
    email: String,
    error: String?,
    passwordError: String?,
    changingPassword: Boolean,
    savingProfile: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, PickedAvatarFile?, PickedAvatarFile?) -> Unit,
    onChangePassword: (String, String) -> Unit
) {
    val statusOptions = listOf("Disponible", "Ausente", "Ocupado", "Invisible")
    var usernameInput by remember(username) { mutableStateOf(username) }
    var statusInput by remember(status) { mutableStateOf(status.ifBlank { "Disponible" }) }
    var bioInput by remember(bio) { mutableStateOf(bio) }
    var selectedAvatarFile by remember { mutableStateOf<PickedAvatarFile?>(null) }
    var selectedBannerFile by remember { mutableStateOf<PickedAvatarFile?>(null) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    var section by remember { mutableStateOf(0) }
    val profileScroll = rememberScrollState()
    val securityScroll = rememberScrollState()
    var pickingImage by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf<String?>(null) }
    val busy = changingPassword || savingProfile || pickingImage
    val avatarPreview by produceState<ImageBitmap?>(null, selectedAvatarFile) {
        value = selectedAvatarFile?.let { decodePickedImage(it.bytes, 256) }
    }
    val bannerPreview by produceState<ImageBitmap?>(null, selectedBannerFile) {
        value = selectedBannerFile?.let { decodePickedImage(it.bytes, 1024) }
    }
    fun chooseImage(avatar: Boolean) {
        pickingImage = true
        imageError = null
        scope.launch {
            try {
                val picked = pickProfileImageFile(if (avatar) "Seleccionar foto de perfil" else "Seleccionar portada")
                if (picked != null) {
                    if (picked.bytes.size > 2 * 1024 * 1024) imageError = "Elige una imagen de hasta 2 MB"
                    else if (avatar) selectedAvatarFile = picked else selectedBannerFile = picked
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { imageError = e.message ?: "No se pudo abrir la imagen" }
            finally { pickingImage = false }
        }
    }

    Dialog(onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 680.dp).fillMaxWidth().padding(12.dp).imePadding()
            .heightIn(max = 760.dp), shape = RoundedCornerShape(24.dp), color = Surface) {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Tu perfil", style = MaterialTheme.typography.titleLarge, color = OnBackground)
                        Text("Dale tu toque personal", style = MaterialTheme.typography.bodyMedium, color = Muted)
                    }
                    IconButton(onClick = onDismiss, enabled = !busy) { Icon(Icons.Rounded.Close, "Cerrar perfil") }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Perfil" to Icons.Rounded.PersonOutline, "Seguridad" to Icons.Rounded.Lock).forEachIndexed { index, (label, icon) ->
                        Surface(onClick = { section = index }, modifier = Modifier.weight(1f).semantics {
                            role = Role.Tab
                            selected = section == index
                        },
                            shape = RoundedCornerShape(14.dp), color = if (section == index) PrimarySoft else SurfaceVariant) {
                            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, null, tint = if (section == index) Primary else Muted, modifier = Modifier.size(20.dp))
                                Text(label, color = OnBackground, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f, fill = false).verticalScroll(if (section == 0) profileScroll else securityScroll)
                    .padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    if (section == 0) {
                        ProfilePreviewCard(usernameInput.ifBlank { username }, avatarUrl, bannerUrl, statusInput,
                            email, avatarPreview, bannerPreview)
                        error?.let { Text(it, color = Error, style = MaterialTheme.typography.bodyMedium) }
                        imageError?.let { Text(it, color = Error, style = MaterialTheme.typography.bodyMedium) }
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Imágenes del perfil", style = MaterialTheme.typography.titleMedium, color = OnBackground)
                            ProfileImageOption("Foto de perfil", selectedAvatarFile?.fileName, !busy,
                                onChoose = { chooseImage(true) }, onReset = { selectedAvatarFile = null })
                            ProfileImageOption("Portada", selectedBannerFile?.fileName, !busy,
                                onChoose = { chooseImage(false) }, onReset = { selectedBannerFile = null })
                            Text("JPG, PNG o WEBP · Hasta 2 MB por imagen", color = Muted, style = MaterialTheme.typography.labelMedium)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Acerca de ti", style = MaterialTheme.typography.titleMedium, color = OnBackground)
                            OutlinedTextField(usernameInput, { usernameInput = it }, label = { Text("Nombre de usuario") },
                                enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                            OutlinedTextField(bioInput, { if (it.length <= 300) bioInput = it },
                                label = { Text("Sobre mí") }, placeholder = { Text("Cuéntanos algo sobre ti") },
                                supportingText = { Text("${bioInput.length}/300 · Opcional") }, enabled = !busy,
                                modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 5, shape = RoundedCornerShape(14.dp))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Tu estado", style = MaterialTheme.typography.titleMedium, color = OnBackground)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                statusOptions.forEach { option ->
                                    FilterChip(selected = statusInput == option, onClick = { statusInput = option }, enabled = !busy,
                                        label = { Text(option) }, leadingIcon = {
                                            Box(Modifier.size(8.dp).clip(CircleShape).background(profileStatusColor(option)))
                                        }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimarySoft,
                                            selectedLabelColor = OnBackground, labelColor = OnSurface))
                                }
                            }
                        }
                    } else {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceVariant).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Lock, null, tint = Primary)
                            Text("Protege tu cuenta", style = MaterialTheme.typography.titleMedium, color = OnBackground)
                            Text("Usa entre 10 y 72 caracteres. Al cambiar la contraseña tendrás que iniciar sesión de nuevo en tus dispositivos.",
                                style = MaterialTheme.typography.bodyMedium, color = Muted)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            PasswordSettingsField("Contraseña actual", currentPassword, !busy) { currentPassword = it }
                            PasswordSettingsField("Nueva contraseña", newPassword, !busy) { newPassword = it }
                            PasswordSettingsField("Repite la nueva contraseña", confirmPassword, !busy) { confirmPassword = it }
                            if (confirmPassword.isNotEmpty() && confirmPassword != newPassword)
                                Text("Las contraseñas no coinciden", color = Error, style = MaterialTheme.typography.bodyMedium)
                            passwordError?.let { Text(it, color = Error, style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }
                HorizontalDivider(color = Outline)
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") }
                    Button(modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp),
                        enabled = !busy && if (section == 0) usernameInput.isNotBlank() else
                            currentPassword.isNotBlank() && newPassword.length in 10..72 && newPassword == confirmPassword,
                        onClick = {
                            if (section == 0) onSave(usernameInput, statusInput, bioInput, selectedAvatarFile, selectedBannerFile)
                            else onChangePassword(currentPassword, newPassword)
                        }) {
                        if (savingProfile || changingPassword) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = OnPrimary)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (section == 0) { if (savingProfile) "Guardando…" else "Guardar cambios" }
                            else if (changingPassword) "Cambiando…" else "Cambiar contraseña")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileImageOption(title: String, selectedName: String?, enabled: Boolean,
    onChoose: () -> Unit, onReset: () -> Unit) {
    Surface(onClick = onChoose, enabled = enabled, shape = RoundedCornerShape(14.dp), color = SurfaceVariant) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.Image, null, tint = Primary, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = OnBackground, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(selectedName ?: "Toca para cambiar", color = Muted, style = MaterialTheme.typography.labelMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (selectedName != null) IconButton(onClick = onReset, enabled = enabled) { Icon(Icons.Rounded.Close, "Deshacer cambio de $title", tint = Muted) }
            else IconButton(onClick = onChoose, enabled = enabled) { Icon(Icons.Rounded.Edit, "Cambiar $title", tint = Muted) }
        }
    }
}

@Composable
private fun profileStatusColor(status: String): Color = when (status) {
    "Disponible" -> Accent
    "Ausente" -> Warning
    "Ocupado" -> Error
    else -> Muted
}

@Composable
private fun PasswordSettingsField(label: String, value: String, enabled: Boolean, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        enabled = enabled,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
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
private fun ProfilePreviewCard(username: String, avatarUrl: String, bannerUrl: String, status: String,
    email: String, avatarPreview: ImageBitmap?, bannerPreview: ImageBitmap?) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(SurfaceVariant)) {
        Box(Modifier.fillMaxWidth().height(144.dp)) {
            if (bannerPreview != null) Image(bannerPreview, "Vista previa de la portada", Modifier.fillMaxWidth().height(100.dp),
                contentScale = ContentScale.Crop)
            else ProfileBannerImage(bannerUrl, Modifier.fillMaxWidth().height(100.dp))
            Box(Modifier.align(Alignment.BottomStart).padding(start = 18.dp).size(88.dp)
                .clip(CircleShape).background(SurfaceVariant).padding(5.dp)) {
                if (avatarPreview != null) Image(avatarPreview, "Vista previa de tu foto", Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop)
                else AvatarImage(avatarUrl, username.ifBlank { "?" }, 78)
            }
            Surface(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 10.dp),
                shape = RoundedCornerShape(50), color = Surface) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(profileStatusColor(status)))
                    Text(status, style = MaterialTheme.typography.labelMedium, color = OnSurface)
                }
            }
        }
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(username.ifBlank { "Tu perfil" }, color = OnBackground, fontSize = 24.sp, lineHeight = 30.sp,
                fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(email, color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
internal fun PublicProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSharedItems: (() -> Unit)? = null,
    isFriend: Boolean = false,
    blocked: Boolean = false,
    canManage: Boolean = true,
    busy: Boolean = false,
    actionError: String? = null,
    onRemoveFriend: () -> Unit = {},
    onBlock: () -> Unit = {},
    onUnblock: () -> Unit = {}
) {
    var confirmAction by remember(profile.id) { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.widthIn(max = 440.dp).fillMaxWidth().heightIn(max = 640.dp)
                .clip(RoundedCornerShape(24.dp)).background(Surface)
                .verticalScroll(rememberScrollState())
                .then(contentEntrance(profile.id))
        ) {
            Box(Modifier.fillMaxWidth().height(196.dp)) {
                ProfileBannerImage(profile.profileBannerUrl, Modifier.fillMaxWidth().height(144.dp))
                Box(Modifier.align(Alignment.BottomStart).padding(start = 24.dp)
                    .size(96.dp).clip(CircleShape).background(Surface).padding(6.dp)) {
                    AvatarImage(profile.avatarUrl, profile.username, size = 84)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)
                    .padding(10.dp).size(48.dp).clip(CircleShape).background(Surface)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cerrar perfil", tint = OnBackground)
                }
            }
            Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(profile.username, color = OnBackground, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (profile.isOnline) Accent else Muted))
                        Text(if (profile.isOnline) "En línea" else "Desconectado", color = OnSurface, fontSize = 13.sp)
                    }
                }
                profile.status?.takeIf { it.isNotBlank() }?.let { status ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SurfaceVariant).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Estado", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text(status, color = OnBackground, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
                profile.bio?.takeIf { it.isNotBlank() }?.let { bio ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Sobre mí", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(bio, color = OnBackground, fontSize = 14.sp, lineHeight = 22.sp)
                    }
                }
                if (canManage) {
                    actionError?.let { Text(it, color = Error, fontSize = 13.sp) }
                    if (blocked) OutlinedButton(enabled = !busy, onClick = onUnblock, modifier = Modifier.fillMaxWidth()) { Text("Desbloquear usuario") }
                    else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isFriend) TextButton(enabled = !busy, onClick = { confirmAction = "remove" }, modifier = Modifier.weight(1f)) { Text("Eliminar amigo") }
                        TextButton(enabled = !busy, onClick = { confirmAction = "block" }, modifier = Modifier.weight(1f)) { Text("Bloquear", color = Error) }
                    }
                }
                onSharedItems?.let { open ->
                    OutlinedButton(onClick = open, modifier = Modifier.fillMaxWidth()) {
                        Text("Archivos y enlaces compartidos")
                    }
                }
                profileMembershipDate(profile.createdAt)?.let { date ->
                    HorizontalDivider(color = Outline)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("En Chimera desde", color = Muted, fontSize = 12.sp)
                        Text(date, color = OnSurface, fontSize = 13.sp)
                    }
                }
            }
        }
    }
    confirmAction?.let { action ->
        AlertDialog(onDismissRequest = { confirmAction = null },
            title = { Text(if (action == "block") "Bloquear a ${profile.username}" else "Eliminar de amigos") },
            text = { Text(if (action == "block") "No podréis enviaros mensajes privados ni solicitudes de amistad. Se conservará el historial y se eliminará la amistad. Los servidores compartidos siguen siendo accesibles."
                else "Se conservará el historial. Podréis volver a enviaros una solicitud de amistad.") },
            confirmButton = { TextButton(enabled = !busy, onClick = { if (action == "block") onBlock() else onRemoveFriend(); confirmAction = null }) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancelar") } })
    }

}

private fun profileMembershipDate(timestamp: String?): String? {
    val parts = timestamp?.substringBefore('T')?.split('-') ?: return null
    val year = parts.getOrNull(0)?.toIntOrNull()?.takeIf { it in 1..9999 } ?: return null
    val month = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..12 } ?: return null
    val months = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
    return "${months[month - 1]} de $year"
}
