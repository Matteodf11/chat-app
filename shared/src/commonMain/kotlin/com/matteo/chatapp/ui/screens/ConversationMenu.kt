package com.matteo.chatapp.ui.screens

import androidx.compose.material3.*
import com.matteo.chatapp.ui.components.ActionSheet
import com.matteo.chatapp.ui.components.ActionSheetItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.runtime.Composable
import com.matteo.chatapp.model.DmConversation
import com.matteo.chatapp.model.DmPreferencesRequest

@Composable
internal fun ConversationMenu(expanded: Boolean, onDismiss: () -> Unit, dm: DmConversation?,
    onChat: () -> Unit, onProfile: () -> Unit, onPreferences: (DmPreferencesRequest) -> Unit,
    title: String = dm?.otherUsername ?: "Opciones", avatarUrl: String? = dm?.otherAvatar) {
    if (!expanded) return
    ActionSheet(title, onDismiss, avatarName = title, avatarUrl = avatarUrl) {
        ActionSheetItem("Abrir chat", Icons.Rounded.ChatBubbleOutline) { onDismiss(); onChat() }
        ActionSheetItem("Ver perfil", Icons.Rounded.PersonOutline) { onDismiss(); onProfile() }
        dm?.let {
            HorizontalDivider()
            ActionSheetItem(if (it.pinned) "Desfijar chat" else "Fijar chat", Icons.Rounded.PushPin) {
                onDismiss(); onPreferences(DmPreferencesRequest(pinned = !it.pinned))
            }
            ActionSheetItem(if (it.muted) "Activar notificaciones" else "Silenciar",
                if (it.muted) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff) {
                onDismiss(); onPreferences(DmPreferencesRequest(muted = !it.muted))
            }
            ActionSheetItem(if (it.archived) "Desarchivar chat" else "Archivar chat",
                if (it.archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive) {
                onDismiss(); onPreferences(DmPreferencesRequest(archived = !it.archived))
            }
        }
    }
}
