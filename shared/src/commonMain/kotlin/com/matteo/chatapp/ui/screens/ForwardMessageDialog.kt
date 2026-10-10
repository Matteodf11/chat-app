package com.matteo.chatapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.matteo.chatapp.model.Message
import com.matteo.chatapp.model.DmConversation
import com.matteo.chatapp.model.Channel
import com.matteo.chatapp.repository.ConversationTarget

internal data class ForwardTarget(val target: ConversationTarget, val name: String, val detail: String)
internal fun forwardingTargets(message: Message, dms: List<DmConversation>, channels: List<Channel>,
    servers: Map<String, String>): List<ForwardTarget> =
    dms.filter { it.canMessage && it.id != message.dmId }.map {
        ForwardTarget(ConversationTarget.DirectMessage(it.id), it.otherUsername, "Mensaje directo")
    } + channels.filter { it.type == "text" && it.id != message.channelId }.distinctBy { it.id }.map {
        ForwardTarget(ConversationTarget.Channel(it.id), "#${it.name}", servers[it.serverId] ?: "Canal")
    }

@Composable
internal fun ForwardMessageDialog(message: Message, targets: List<ForwardTarget>, working: Boolean, error: String?,
    onSend: (ForwardTarget) -> Unit, onDismiss: () -> Unit) {
    var query by remember(message.id) { mutableStateOf("") }
    var selected by remember(message.id) { mutableStateOf<ConversationTarget?>(null) }
    val chosen = targets.firstOrNull { it.target == selected }
    AlertDialog(onDismissRequest = { if (!working) onDismiss() },
        title = { Text("Reenviar mensaje") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(message.attachmentName?.let { if (message.content.isBlank()) it else "$it\n${message.content}" }
                    ?: message.content, maxLines = 3, overflow = TextOverflow.Ellipsis)
                OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, enabled = !working, placeholder = { Text("Buscar chat o canal") })
                LazyColumn(Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 300.dp)) {
                    val filtered = targets.filter { it.name.contains(query, true) || it.detail.contains(query, true) }
                    items(filtered, key = { "${it.target::class.simpleName}:${it.target.id}" }) { item ->
                        Row(Modifier.fillMaxWidth().clickable(enabled = !working) { selected = item.target }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selected == item.target, enabled = !working, onClick = { selected = item.target })
                            Column(Modifier.weight(1f)) {
                                Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.detail, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (filtered.isEmpty()) item { Text("No hay otros chats disponibles", Modifier.padding(12.dp)) }
                }
                chosen?.let { Text("Enviar a ${it.name}", style = MaterialTheme.typography.bodySmall) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (working) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(enabled = chosen != null && !working, onClick = { chosen?.let(onSend) }) { Text("Reenviar") } },
        dismissButton = { TextButton(enabled = !working, onClick = onDismiss) { Text("Cancelar") } })
}
