package com.matteo.chatapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.Forward
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matteo.chatapp.model.Message
import com.matteo.chatapp.platform.copyTextToClipboard
import com.matteo.chatapp.platform.openExternalUrl
import com.matteo.chatapp.platform.rememberVoicePlaybackController
import com.matteo.chatapp.ui.theme.Muted
import com.matteo.chatapp.ui.theme.OnBackground
import com.matteo.chatapp.ui.theme.OnSurface
import com.matteo.chatapp.ui.theme.SurfaceElevated
import com.matteo.chatapp.ui.theme.SurfaceVariant
import com.matteo.chatapp.ui.theme.Primary
import com.matteo.chatapp.ui.theme.PrimarySoft

@Composable
fun MessageItem(
    message: Message,
    canModify: Boolean,
    onReply: () -> Unit,
    onReplyClick: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit,
    highlighted: Boolean = false,
    onOpenImage: (Message) -> Unit = {},
    onDownload: (Message) -> Unit = {},
    onForward: ((Message) -> Unit)? = null,
    allowEdit: Boolean = true,
    pending: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editedContent by remember(message.id, message.content) { mutableStateOf(message.content) }
    val mentionedMe = !message.deleted && message.mentionedUserIds.contains(com.matteo.chatapp.repository.SessionManager.getUserId())
    val sentMetadata = listOfNotNull(
        "Editado".takeIf { message.edited },
        message.createdAt?.substringAfter("T", "")?.take(5)?.takeIf { it.isNotBlank() },
        if (pending) "◷" else if (message.readAt != null) "Leído" else "Enviado"
    ).joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { if (pending) stateDescription = "Envío pendiente de confirmación" }
            .background(if (highlighted || mentionedMe) PrimarySoft else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(enabled = !pending) { showMenu = true }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        AvatarImage(
            avatarUrl = message.senderAvatar,
            name = message.senderUsername,
            size = 38
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            if (mentionedMe) Text("Te ha mencionado", color = Primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            if (message.replyToId != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onReplyClick(message.replyToId) }
                        .background(SurfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.width(3.dp).height(30.dp).background(Primary))
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = message.replyToSenderUsername ?: "Mensaje",
                            color = OnBackground,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = message.replyToContent ?: "Mensaje eliminado",
                            color = Muted,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = message.senderUsername,
                    color = OnBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (!canModify || message.attachmentContentType?.startsWith("image/") == true) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = message.createdAt?.substringAfter("T", "")?.take(5).orEmpty(),
                        color = Muted,
                        fontSize = 11.sp
                    )
                }
            }
            val location = com.matteo.chatapp.platform.sharedLocationFromMessage(message.content)
            if (location != null) {
                OutlinedButton(onClick = { openExternalUrl(location.mapUrl) }) {
                    Column { Text("Ubicación compartida"); Text("Ver en el mapa", fontSize = 12.sp) }
                }
            }
            if (message.content.isNotBlank() && location == null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildAnnotatedString {
                        append(message.content)
                        if (message.mentionedUserIds.isNotEmpty()) {
                            Regex("(?<![\\p{L}\\p{N}_@])@[\\p{L}\\p{N}_.-]+").findAll(message.content).forEach {
                                addStyle(SpanStyle(color = Primary, fontWeight = FontWeight.SemiBold), it.range.first, it.range.last + 1)
                            }
                        }
                        if (canModify && message.attachmentName == null) {
                            withStyle(SpanStyle(color = Muted, fontSize = 11.sp)) {
                                append("   $sentMetadata")
                            }
                        }
                    },
                    color = OnSurface,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
            if (message.attachmentName != null) {
                Spacer(Modifier.height(7.dp))
                if (pending) Text(message.attachmentName, color = OnSurface, fontSize = 12.sp)
                else AttachmentCard(message, onOpenImage, onDownload)
            }
            if (message.edited && !canModify) {
                Text(text = "Editado", color = Muted, fontSize = 10.sp)
            }
            if (canModify && message.attachmentName != null && (pending || message.attachmentContentType?.startsWith("image/") != true)) {
                Text(sentMetadata, modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                    color = Muted, fontSize = 11.sp)
            }
            if (showMenu) ActionSheet("Mensaje de ${message.senderUsername}", { showMenu = false },
                avatarName = message.senderUsername, avatarUrl = message.senderAvatar) {
                ActionSheetItem("Responder", Icons.AutoMirrored.Rounded.Reply) {
                    showMenu = false
                    onReply()
                }
                if (!message.deleted) onForward?.let { forward ->
                    ActionSheetItem("Reenviar", Icons.AutoMirrored.Rounded.Forward) {
                        showMenu = false
                        forward(message)
                    }
                }
                if (location != null) {
                    ActionSheetItem("Ver ubicación", Icons.Rounded.LocationOn) {
                        showMenu = false
                        openExternalUrl(location.mapUrl)
                    }
                } else if (message.content.isNotBlank()) {
                    ActionSheetItem("Copiar", Icons.Rounded.ContentCopy) {
                        copyTextToClipboard(message.content)
                        showMenu = false
                    }
                }
                if (canModify && allowEdit && message.content.isNotBlank()) {
                    ActionSheetItem("Editar", Icons.Rounded.Edit) {
                        editedContent = message.content
                        showMenu = false
                        showEditDialog = true
                    }
                }
                if (canModify) {
                    ActionSheetItem("Borrar", Icons.Rounded.DeleteOutline) {
                        showMenu = false
                        showDeleteDialog = true
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar mensaje") },
            text = {
                OutlinedTextField(
                    value = editedContent,
                    onValueChange = { if (it.length <= 4000) editedContent = it },
                    minLines = 2,
                    maxLines = 6
                )
            },
            confirmButton = {
                TextButton(
                    enabled = editedContent.isNotBlank() && editedContent != message.content,
                    onClick = {
                        onEdit(editedContent.trim())
                        showEditDialog = false
                    }
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Borrar mensaje") },
            text = { Text("Esta accion no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun AttachmentCard(message: Message, onOpenImage: (Message) -> Unit, onDownload: (Message) -> Unit) {
    if (message.attachmentContentType?.startsWith("image/") == true) {
        SharedMediaThumbnail(message.attachmentUrl, message.attachmentName,
            Modifier.widthIn(max = 360.dp).clip(MaterialTheme.shapes.medium)
                .clickable(enabled = message.attachmentUrl != null, onClickLabel = "Ver imagen") { onOpenImage(message) },
            maxEdge = 1024, showFullImage = true, cacheKey = message.id)
        return
    }
    if (message.attachmentContentType?.startsWith("audio/") == true && message.attachmentUrl != null) {
        Column { VoiceNoteCard(message); TextButton(onClick = { onDownload(message) }) { Text("Guardar audio") } }
        return
    }
    val url = message.attachmentUrl
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .background(SurfaceVariant, MaterialTheme.shapes.small)
            .clickable(enabled = url != null) { onDownload(message) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Description, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(message.attachmentName ?: "Archivo", color = OnBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(formatFileSize(message.attachmentSize), color = Muted, fontSize = 11.sp)
        }
        Icon(Icons.Rounded.Download, contentDescription = "Guardar archivo", tint = if (url != null) OnSurface else Muted)
    }
}

@Composable
private fun VoiceNoteCard(message: Message) {
    val controller = rememberVoicePlaybackController(message.attachmentUrl!!)
    val isPlaying by controller.isPlaying.collectAsState()
    val isLoading by controller.isLoading.collectAsState()
    val playbackError by controller.error.collectAsState()
    val progress by controller.progress.collectAsState()
    val duration by controller.durationSeconds.collectAsState()
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .background(SurfaceVariant, MaterialTheme.shapes.small)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = controller::playOrPause, enabled = !isLoading,
                modifier = Modifier.size(38.dp).semantics {
                    stateDescription = if (isLoading) "Cargando audio" else if (isPlaying) "Reproduciendo" else "Pausado"
                }) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Primary, strokeWidth = 2.dp)
                else Icon(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar nota de voz" else "Reproducir nota de voz",
                    tint = Primary
                )
            }
            Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = Muted, modifier = Modifier.size(20.dp))
            Slider(
                value = progress.coerceIn(0f, 1f),
                onValueChange = controller::seekTo,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                enabled = duration > 0 && !isLoading,
                colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary, inactiveTrackColor = Muted)
            )
            Text(
                text = if (duration > 0) formatDuration((duration * progress).toInt()) + " / " + formatDuration(duration) else "Audio",
                color = Muted,
                fontSize = 10.sp
            )
        }
        playbackError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
    }
}

private fun formatDuration(seconds: Int): String =
    "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

private fun formatFileSize(bytes: Long?): String {
    val value = bytes ?: return "Archivo"
    return when {
        value >= 1024L * 1024L -> "${(value / (1024L * 1024L))} MB"
        value >= 1024L -> "${value / 1024L} KB"
        else -> "$value B"
    }
}
