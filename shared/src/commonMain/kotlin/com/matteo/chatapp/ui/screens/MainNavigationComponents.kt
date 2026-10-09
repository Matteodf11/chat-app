package com.matteo.chatapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
        modifier = Modifier.fillMaxWidth().height(58.dp).background(Navigation).padding(horizontal = 6.dp),
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
        Text(title, color = OnBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
        actions()
        onSearch?.let {
            IconButton(onClick = it, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Rounded.Search, contentDescription = "Buscar", tint = OnSurface, modifier = Modifier.size(19.dp))
            }
        }
        IconButton(onClick = onProfile, modifier = Modifier.size(38.dp)) {
            Icon(Icons.Rounded.PersonOutline, contentDescription = "Perfil", tint = OnSurface, modifier = Modifier.size(19.dp))
        }
        IconButton(onClick = onAppearance, modifier = Modifier.size(38.dp)) {
            Icon(Icons.Rounded.Palette, contentDescription = "Apariencia", tint = OnSurface, modifier = Modifier.size(19.dp))
        }
        IconButton(onClick = onLogout, modifier = Modifier.size(38.dp)) {
            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Cerrar sesion", tint = Muted, modifier = Modifier.size(19.dp))
        }
    }
    HorizontalDivider(color = Outline)
}

@Composable
internal fun MobileBottomNavigation(
    selected: MobileSection,
    hasUnreadMessages: Boolean,
    onSelect: (MobileSection) -> Unit
) {
    NavigationBar(containerColor = Navigation, tonalElevation = 0.dp) {
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
            icon = { Icon(Icons.Rounded.Dns, contentDescription = "Servidores") },
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
            .clip(RoundedCornerShape(8.dp))
            .background(Surface)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(avatarUrl = server.iconUrl, name = server.name, size = 46)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(server.name, color = OnBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(roleLabel(server.myRole), color = Muted, fontSize = 12.sp)
        }
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
                .clip(RoundedCornerShape(if (selected) 12.dp else 22.dp))
                .background(if (selected) Primary else SurfaceElevated)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (selected) OnPrimary else OnSurface,
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
    Column(modifier = Modifier.fillMaxWidth().height(82.dp).padding(horizontal = 16.dp, vertical = 13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = OnBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
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
internal fun DirectMessageRow(dm: DmConversation, selected: Boolean, hasUnread: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) SurfaceVariant else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PresenceAvatar(name = dm.otherUsername, avatarUrl = dm.otherAvatar, online = dm.otherIsOnline, size = 34)
        Spacer(Modifier.width(10.dp))
        Text(
            dm.otherUsername,
            color = if (selected) OnBackground else OnSurface,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (hasUnread) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Primary)
            )
        }
    }
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
            Text(username, color = OnBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(status.ifBlank { "Disponible" }, color = Accent, fontSize = 11.sp)
        }
        IconButton(onClick = onSettings, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Rounded.PersonOutline, contentDescription = "Ajustes de perfil", tint = OnSurface, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onAppearanceSettings, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Rounded.Palette, contentDescription = "Apariencia", tint = OnSurface, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onLogout, modifier = Modifier.size(34.dp)) {
            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Cerrar sesion", tint = Muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun PresenceAvatar(name: String, avatarUrl: String? = null, online: Boolean, size: Int = 40) {
    Box {
        AvatarImage(avatarUrl = avatarUrl, name = name, size = size)
        Box(
            modifier = Modifier
                .size((size / 3).dp)
                .clip(CircleShape)
                .background(if (online) Accent else Muted)
                .align(Alignment.BottomEnd)
        )
    }
}
