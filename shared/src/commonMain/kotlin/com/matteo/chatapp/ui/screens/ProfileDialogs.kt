package com.matteo.chatapp.ui.screens

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
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
internal fun ProfileSettingsDialog(
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
internal fun PublicProfileDialog(
    profile: UserProfile,
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
