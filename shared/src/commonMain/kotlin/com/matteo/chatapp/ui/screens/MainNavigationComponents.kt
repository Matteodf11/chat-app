package com.matteo.chatapp.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat_app.shared.generated.resources.Res
import chat_app.shared.generated.resources.chimera_logo
import com.matteo.chatapp.model.DmConversation
import com.matteo.chatapp.model.Server
import com.matteo.chatapp.ui.components.AvatarImage
import com.matteo.chatapp.ui.theme.*
import org.jetbrains.compose.resources.painterResource

internal enum class MobileSection { FRIENDS, MESSAGES, SERVERS }

@Composable
internal fun MobileTopBar(
    title: String,
    onSearch: (() -> Unit)? = null,
    onProfile: () -> Unit,
    onAppearance: () -> Unit,
    onLogout: () -> Unit,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).background(Background).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = OnSurface)
            }
        } else {
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)).background(Navigation),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.chimera_logo),
                    contentDescription = "Chimera",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.width(10.dp))
        }
        Text(title, color = OnBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
        onSearch?.let {
            IconButton(onClick = it, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Search, contentDescription = "Buscar", tint = OnSurface, modifier = Modifier.size(19.dp))
            }
        }
        AccountMenu(onProfile, onAppearance, onLogout)
    }
    HorizontalDivider(color = Outline)
}

@Composable
internal fun MobileBottomNavigation(
    selected: MobileSection,
    hasUnreadMessages: Boolean,
    hasUnreadServers: Boolean = false,
    onSelect: (MobileSection) -> Unit
) {
    NavigationBar(containerColor = Surface, tonalElevation = 0.dp) {
        NavigationBarItem(
            selected = selected == MobileSection.FRIENDS,
            onClick = { onSelect(MobileSection.FRIENDS) },
            icon = { Icon(Icons.Rounded.People, contentDescription = "Amigos") },
            label = { Text("Amigos") },
            colors = mobileNavigationColors()
        )
        NavigationBarItem(
            selected = selected == MobileSection.MESSAGES,
            onClick = { onSelect(MobileSection.MESSAGES) },
            icon = {
                BadgedBox(badge = { if (hasUnreadMessages) Badge() }) {
                    Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = "Mensajes")
                }
            },
            label = { Text("Mensajes") },
            colors = mobileNavigationColors()
        )
        NavigationBarItem(
            selected = selected == MobileSection.SERVERS,
            onClick = { onSelect(MobileSection.SERVERS) },
            icon = { BadgedBox(badge = { if (hasUnreadServers) Badge() }) {
                Icon(Icons.Rounded.Dns, contentDescription = "Servidores")
            } },
            label = { Text("Servidores") },
            colors = mobileNavigationColors()
        )
    }
}

@Composable
private fun mobileNavigationColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Primary,
    selectedTextColor = Primary,
    indicatorColor = PrimarySoft,
    unselectedIconColor = Muted,
    unselectedTextColor = Muted
)

@Composable
internal fun MobileServerRow(server: Server, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(avatarUrl = server.iconUrl, name = server.name, size = 46)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(server.name, color = OnBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Tu rol: ${roleLabel(server.myRole)}", color = Primary, fontSize = 12.sp)
            server.description?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = Muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        com.matteo.chatapp.ui.components.ActivityBadge(server.unreadCount, server.mentionCount)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = Muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
internal fun RailAction(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    val background by animateColorAsState(if (selected) PrimarySoft else Surface, tween(140))
    val corner by animateDpAsState(if (selected) 14.dp else 22.dp, tween(160))
    Box(
        modifier = Modifier.width(72.dp).height(48.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (selected) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                    .background(Primary)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(44.dp)
                .clip(RoundedCornerShape(corner))
                .background(background)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (selected) Primary else OnSurface,
                modifier = Modifier.size(21.dp)
            )
        }
        badge?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp)
                    .height(18.dp)
                    .defaultMinSize(minWidth = 18.dp)
                    .clip(CircleShape)
                    .background(Error)
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(it, color = OnBackground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun SidebarHeader(
    title: String,
    subtitle: String,
    action: String? = null,
    onAction: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp).padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = OnBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            action?.let {
                IconButton(onClick = onAction, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Settings, contentDescription = it, tint = OnSurface, modifier = Modifier.size(19.dp))
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
    HorizontalDivider(color = Outline)
}

@Composable
internal fun SidebarSectionLabel(text: String) {
    Text(
        text = text,
        color = Muted,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp)
    )
}

@Composable
internal fun DirectMessageRow(dm: DmConversation, selected: Boolean, hasUnread: Boolean, onClick: () -> Unit,
    onProfile: () -> Unit,
    onPreferences: (com.matteo.chatapp.model.DmPreferencesRequest) -> Unit = {}) {
    val background by animateColorAsState(if (selected) PrimarySoft else Color.Transparent, tween(140))
    var expanded by remember(dm.id) { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(background)
                .combinedClickable(onClick = onClick, onLongClick = { expanded = true },
                    onLongClickLabel = "Opciones de conversación")
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PresenceAvatar(name = dm.otherUsername, avatarUrl = dm.otherAvatar, online = dm.otherIsOnline, size = 40)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(dm.otherUsername, color = OnBackground, fontSize = 15.sp,
                        modifier = Modifier.weight(1f, fill = false),
                        fontWeight = if (selected || hasUnread) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (dm.pinned) Icon(Icons.Rounded.PushPin, "Conversación fijada", tint = Primary, modifier = Modifier.size(14.dp))
                    if (dm.muted) Icon(Icons.Rounded.NotificationsOff, "Silenciada", tint = Muted, modifier = Modifier.size(14.dp))
                }
                Text(dm.lastMessagePreview ?: if (hasUnread) "Mensajes nuevos" else if (dm.otherIsOnline) "En línea" else "Desconectado",
                    color = if (hasUnread) Primary else OnSurface, fontSize = 13.sp, lineHeight = 18.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            dm.lastMessageAt?.let { at -> Text(conversationTimeLabel(at), color = Muted, fontSize = 11.sp) }
            if (hasUnread) {
                Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Primary).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(if (dm.unreadCount > 99) "99+" else dm.unreadCount.coerceAtLeast(1).toString(), color = OnPrimary, fontSize = 10.sp)
                }
            }
            }
            Box {
                IconButton(onClick = { expanded = true }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.MoreVert, "Opciones de conversación", tint = Muted, modifier = Modifier.size(20.dp))
                }

            }
        }
        ConversationMenu(expanded, { expanded = false }, dm, onClick, onProfile, onPreferences)
    }
}

private fun conversationTimeLabel(timestamp: String): String {
    val today = kotlin.time.Clock.System.now().toString().take(10)
    return if (timestamp.take(10) == today) timestamp.substringAfter('T').take(5)
    else timestamp.take(10).split('-').let { if (it.size == 3) "${it[2]}/${it[1]}" else "" }
}

@Composable
internal fun UserFooter(
    username: String,
    avatarUrl: String?,
    status: String,
    onSettings: () -> Unit,
    onAppearanceSettings: () -> Unit,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Navigation)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PresenceAvatar(name = username.ifBlank { "?" }, avatarUrl = avatarUrl, online = true, size = 34)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f).clickable { onSettings() }) {
            Text(username, color = OnBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(status.ifBlank { "Disponible" }, color = Accent, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        AccountMenu(onSettings, onAppearanceSettings, onLogout)
    }
}

@Composable
internal fun PresenceAvatar(name: String, avatarUrl: String? = null, online: Boolean, size: Int = 40) {
    Box {
        AvatarImage(avatarUrl = avatarUrl, name = name, size = size)
        Box(
            modifier = Modifier
                .size((size / 4).dp)
                .clip(CircleShape)
                .background(if (online) Accent else Muted)
                .border(2.dp, Surface, CircleShape)
                .align(Alignment.BottomEnd)
        )
    }
}

@Composable
private fun AccountMenu(onProfile: () -> Unit, onAppearance: () -> Unit, onLogout: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.PersonOutline, contentDescription = "Mi cuenta y ajustes", tint = OnSurface)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = Surface) {
            DropdownMenuItem(text = { Text("Mi perfil") },
                leadingIcon = { Icon(Icons.Rounded.PersonOutline, null) },
                onClick = { expanded = false; onProfile() })
            DropdownMenuItem(text = { Text("Ajustes") },
                leadingIcon = { Icon(Icons.Rounded.Settings, null) },
                onClick = { expanded = false; onAppearance() })
            HorizontalDivider(color = Outline)
            DropdownMenuItem(text = { Text("Cerrar sesión", color = Error) },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Logout, null, tint = Error) },
                onClick = { expanded = false; onLogout() })
        }
    }
}
