package com.matteo.chatapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.matteo.chatapp.platform.openExternalUrl
import com.matteo.chatapp.ui.theme.*
import com.matteo.chatapp.viewmodel.SharedItemsUiState

internal fun sharedLinks(content: String): List<String> = Regex("https?://[^\\s<>]+", RegexOption.IGNORE_CASE)
    .findAll(content).map { it.value.trimEnd('.', ',', ';', ')', ']', '!', '?') }.distinct().toList()

@Composable
internal fun SharedItemsDialog(state: SharedItemsUiState, onTab: (String) -> Unit,
    onMore: () -> Unit, onDismiss: () -> Unit,
    onOpenImage: (com.matteo.chatapp.model.Message) -> Unit, onDownload: (com.matteo.chatapp.model.Message) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = Surface,
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().heightIn(max = 680.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Contenido compartido", style = MaterialTheme.typography.titleMedium, color = OnBackground,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cerrar") }
                }
                Row(Modifier.fillMaxWidth()) {
                    listOf("media" to "Multimedia", "files" to "Archivos", "links" to "Enlaces").forEach { (kind, label) ->
                        TextButton(onClick = { onTab(kind) }, modifier = Modifier.weight(1f)) {
                            Text(label, color = if (state.kind == kind) Primary else Muted)
                        }
                    }
                }
                HorizontalDivider(color = Outline)
                LazyColumn(Modifier.weight(1f, fill = false).heightIn(min = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                    items(state.messages, key = { it.id }) { message ->
                        Surface(color = SurfaceVariant, shape = MaterialTheme.shapes.medium) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (state.kind == "links") {
                                    sharedLinks(message.content).forEach { url ->
                                        Text(url, color = Primary, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.fillMaxWidth().clickable { openExternalUrl(url) }.padding(vertical = 4.dp))
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().clickable(enabled = message.attachmentUrl != null) {
                                            if (message.attachmentContentType?.startsWith("image/") == true) onOpenImage(message) else onDownload(message)
                                        }) {
                                        if (message.attachmentContentType?.startsWith("image/") == true) {
                                            com.matteo.chatapp.ui.components.SharedMediaThumbnail(message.attachmentUrl,
                                                message.attachmentName, Modifier.size(72.dp))
                                            Spacer(Modifier.width(12.dp))
                                        }
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(message.attachmentName ?: "Archivo", color = OnBackground,
                                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Text(if (message.attachmentUrl != null) "${if (message.attachmentContentType?.startsWith("image/") == true) "Ver imagen" else "Guardar archivo"} · ${message.attachmentContentType.orEmpty()}"
                                                else "Archivo no disponible", style = MaterialTheme.typography.bodySmall, color = Muted)
                                        }
                                    }
                                }
                                Text("${message.senderUsername} · ${message.createdAt.orEmpty().take(10)}",
                                    style = MaterialTheme.typography.labelSmall, color = Muted)
                            }
                        }
                    }
                    if (state.messages.isEmpty() && !state.loading && state.error == null) item {
                        Text("Todavía no hay contenido en esta sección", color = Muted, modifier = Modifier.padding(16.dp))
                    }
                    state.error?.let { error -> item { Text(error, color = Error) } }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            if (state.loading) CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp), strokeWidth = 2.dp)
                            else if (state.hasMore || state.error != null) TextButton(onClick = onMore) {
                                Text(if (state.error != null) "Reintentar" else "Cargar más")
                            }
                        }
                    }
                }
            }
        }
    }
}
