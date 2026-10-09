package com.matteo.chatapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matteo.chatapp.model.Message
import com.matteo.chatapp.platform.PickedAttachmentFile
import com.matteo.chatapp.platform.cancelVoiceRecording
import com.matteo.chatapp.platform.pickAttachmentFile
import com.matteo.chatapp.platform.startVoiceRecording
import com.matteo.chatapp.platform.stopVoiceRecording
import com.matteo.chatapp.repository.SessionManager
import com.matteo.chatapp.ui.components.MessageItem
import com.matteo.chatapp.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

internal data class ChatAreaState(
    val destinationKey: String,
    val title: String,
    val subtitle: String,
    val messages: List<Message>,
    val hasOlderMessages: Boolean,
    val isLoadingOlder: Boolean,
    val messageError: String?,
    val replyingTo: Message?,
    val messageInput: String,
    val isUploadingAttachment: Boolean,
    val avatarUrl: String? = null,
    val online: Boolean? = null,
    val typingText: String? = null,
    val highlightedMessageId: String? = null
)

internal data class ChatAreaActions(
    val onLoadOlder: () -> Unit,
    val onEditMessage: (String, String) -> Unit,
    val onDeleteMessage: (String) -> Unit,
    val onReplyMessage: (Message) -> Unit,
    val onCancelReply: () -> Unit,
    val onInputChange: (String) -> Unit,
    val onSendAttachment: (PickedAttachmentFile, () -> Unit) -> Unit,
    val onSend: () -> Unit,
    val onBack: (() -> Unit)? = null,
    val onSearch: (() -> Unit)? = null,
    val onProfileClick: (() -> Unit)? = null
)

@Composable
internal fun ChatArea(
    state: ChatAreaState,
    listState: LazyListState,
    actions: ChatAreaActions
) {
    val destinationKey = state.destinationKey
    val title = state.title
    val subtitle = state.subtitle
    val avatarUrl = state.avatarUrl
    val online = state.online
    val typingText = state.typingText
    val highlightedMessageId = state.highlightedMessageId
    val messages = state.messages
    val hasOlderMessages = state.hasOlderMessages
    val isLoadingOlder = state.isLoadingOlder
    val messageError = state.messageError
    val replyingTo = state.replyingTo
    val messageInput = state.messageInput
    val isUploadingAttachment = state.isUploadingAttachment
    val onBack = actions.onBack
    val onSearch = actions.onSearch
    val onProfileClick = actions.onProfileClick
    val onLoadOlder = actions.onLoadOlder
    val onEditMessage = actions.onEditMessage
    val onDeleteMessage = actions.onDeleteMessage
    val onReplyMessage = actions.onReplyMessage
    val onCancelReply = actions.onCancelReply
    val onInputChange = actions.onInputChange
    val onSendAttachment = actions.onSendAttachment
    val onSend = actions.onSend
    val scope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    var pendingAttachment by remember(destinationKey) { mutableStateOf<PickedAttachmentFile?>(null) }
    var isRecordingVoice by remember(destinationKey) { mutableStateOf(false) }
    var recordingSeconds by remember(destinationKey) { mutableStateOf(0) }
    var recordingError by remember(destinationKey) { mutableStateOf<String?>(null) }
    val canSend = (messageInput.isNotBlank() || pendingAttachment != null) && !isUploadingAttachment && !isRecordingVoice
    val sendCurrent = {
        val attachment = pendingAttachment
        if (attachment != null) {
            onSendAttachment(attachment) { pendingAttachment = null }
        } else {
            onSend()
        }
    }
    DisposableEffect(destinationKey) {
        onDispose { cancelVoiceRecording() }
    }
    LaunchedEffect(isRecordingVoice) {
        recordingSeconds = 0
        while (isRecordingVoice && recordingSeconds < 300) {
            delay(1000)
            if (isRecordingVoice) recordingSeconds++
        }
        if (isRecordingVoice && recordingSeconds >= 300) {
            pendingAttachment = stopVoiceRecording()
            isRecordingVoice = false
        }
    }
    LaunchedEffect(listState, hasOlderMessages) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstVisible ->
                if (firstVisible <= 2 && hasOlderMessages) onLoadOlder()
            }
    }
    LaunchedEffect(imeBottom, destinationKey) {
        if (imeBottom > 0 && messages.isNotEmpty()) {
            val dateSeparatorCount = messages.mapNotNull { it.createdAt?.take(10) }.distinct().size
            val targetIndex = (messages.size + dateSeparatorCount - 1).coerceAtLeast(0)
            listState.scrollToItem(targetIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp).background(Background).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            onBack?.let {
                IconButton(onClick = it, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = OnSurface, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(6.dp))
            }
            if (online != null) {
                PresenceAvatar(name = title, avatarUrl = avatarUrl, online = online, size = 38)
            } else {
                Box(
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Tag, contentDescription = null, tint = OnSurface, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = OnBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Spacer(Modifier.height(3.dp))
                Text(
                    typingText ?: subtitle,
                    color = if (typingText != null) Accent else Muted,
                    fontSize = 12.sp,
                    fontWeight = if (typingText != null) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1
                )
            }
            onSearch?.let {
                IconButton(onClick = it, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Rounded.Search, contentDescription = "Buscar mensajes", tint = OnSurface, modifier = Modifier.size(20.dp))
                }
            }
            onProfileClick?.let {
                IconButton(onClick = it, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Rounded.PersonOutline, contentDescription = "Ver perfil", tint = OnSurface, modifier = Modifier.size(20.dp))
                }
            }
        }
        HorizontalDivider(color = Outline)
        LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(vertical = 10.dp)) {
            if (isLoadingOlder) {
                item(key = "loading-older") {
                    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Primary)
                    }
                }
            }
            messages.forEachIndexed { index, message ->
                val date = message.createdAt?.take(10).orEmpty()
                val previousDate = messages.getOrNull(index - 1)?.createdAt?.take(10).orEmpty()
                if (date.isNotBlank() && date != previousDate) {
                    item(key = "date-$date") {
                        DateSeparator(date)
                    }
                }
                item(key = message.id) {
                    MessageItem(
                        message = message,
                        canModify = message.senderId == SessionManager.getUserId(),
                        onReply = { onReplyMessage(message) },
                        onReplyClick = { replyId ->
                            val targetIndex = messages.indexOfFirst { it.id == replyId }
                            if (targetIndex >= 0) {
                                val separators = messages.take(targetIndex + 1)
                                    .mapNotNull { it.createdAt?.take(10) }
                                    .distinct()
                                    .size
                                scope.launch { listState.animateScrollToItem(targetIndex + separators) }
                            }
                        },
                        onEdit = { content -> onEditMessage(message.id, content) },
                        onDelete = { onDeleteMessage(message.id) },
                        highlighted = message.id == highlightedMessageId
                    )
                }
            }
        }
        if (messageError != null || recordingError != null) {
            Text(
                text = messageError ?: recordingError.orEmpty(),
                color = Error,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
        if (replyingTo != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceVariant)
                    .padding(start = 20.dp, end = 10.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(3.dp).height(34.dp).background(Primary))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Respondiendo a ${replyingTo.senderUsername}", color = OnBackground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        replyingTo.content.ifBlank {
                            if (replyingTo.attachmentContentType?.startsWith("audio/") == true) "Nota de voz"
                            else replyingTo.attachmentName ?: "Mensaje"
                        },
                        color = Muted,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                IconButton(onClick = onCancelReply, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cancelar respuesta", tint = Muted)
                }
            }
        }
        if (pendingAttachment != null) {
            Row(
                modifier = Modifier.fillMaxWidth().background(SurfaceVariant).padding(horizontal = 20.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (pendingAttachment!!.contentType.startsWith("audio/")) Icons.Rounded.GraphicEq else Icons.Rounded.Description,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(pendingAttachment!!.fileName, color = OnBackground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(formatAttachmentSize(pendingAttachment!!.size), color = Muted, fontSize = 11.sp)
                }
                IconButton(onClick = { pendingAttachment = null }, enabled = !isUploadingAttachment, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Quitar archivo", tint = Muted)
                }
            }
        }
        if (isRecordingVoice) {
            Row(
                modifier = Modifier.fillMaxWidth().background(SurfaceVariant).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(Error))
                Spacer(Modifier.width(10.dp))
                Text("Grabando ${formatVoiceDuration(recordingSeconds)}", color = OnBackground, fontSize = 13.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    cancelVoiceRecording()
                    isRecordingVoice = false
                    recordingSeconds = 0
                }) { Text("Cancelar", color = Muted) }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(Background).padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = {
                    scope.launch { pickAttachmentFile()?.let { pendingAttachment = it } }
                },
                enabled = !isUploadingAttachment && !isRecordingVoice,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(Icons.Rounded.AttachFile, contentDescription = "Adjuntar archivo", tint = OnSurface)
            }
            OutlinedTextField(
                value = messageInput,
                onValueChange = onInputChange,
                enabled = !isRecordingVoice,
                placeholder = { Text("Escribe un mensaje", color = Muted) },
                modifier = Modifier
                    .weight(1f)
                    .onPreviewKeyEvent { event ->
                        if (event.key == Key.Enter && event.type == KeyEventType.KeyDown && !event.isShiftPressed) {
                            if (canSend) sendCurrent()
                            true
                        } else {
                            false
                        }
                    },
                shape = RoundedCornerShape(8.dp),
                singleLine = false,
                minLines = 1,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (canSend) sendCurrent() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Outline,
                    focusedTextColor = OnBackground,
                    unfocusedTextColor = OnBackground,
                    cursorColor = Primary,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface
                )
            )
            IconButton(
                onClick = {
                    scope.launch {
                        if (isRecordingVoice) {
                            val voice = stopVoiceRecording()
                            isRecordingVoice = false
                            recordingSeconds = 0
                            if (voice != null) {
                                pendingAttachment = voice
                                recordingError = null
                            } else {
                                recordingError = "La nota de voz es demasiado corta"
                            }
                        } else {
                            pendingAttachment = null
                            if (startVoiceRecording()) {
                                isRecordingVoice = true
                                recordingError = null
                            } else {
                                recordingError = "No se pudo acceder al microfono"
                            }
                        }
                    }
                },
                enabled = !isUploadingAttachment,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isRecordingVoice) Error else SurfaceVariant)
            ) {
                Icon(
                    if (isRecordingVoice) Icons.Rounded.Stop else Icons.Rounded.Mic,
                    contentDescription = if (isRecordingVoice) "Detener grabacion" else "Grabar nota de voz",
                    tint = if (isRecordingVoice) Color.White else OnSurface
                )
            }
            IconButton(
                onClick = sendCurrent,
                enabled = canSend,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(if (canSend) Primary else SurfaceVariant)
            ) {
                if (isUploadingAttachment) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Muted)
                } else {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Enviar mensaje", tint = if (canSend) OnPrimary else Muted)
                }
            }
        }
    }
}

private fun formatAttachmentSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "${bytes / (1024L * 1024L)} MB"
    bytes >= 1024L -> "${bytes / 1024L} KB"
    else -> "$bytes B"
}

private fun formatVoiceDuration(seconds: Int): String =
    "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

@Composable
private fun DateSeparator(date: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(color = SurfaceVariant, modifier = Modifier.weight(1f))
        Text(
            text = date,
            color = Muted,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        HorizontalDivider(color = SurfaceVariant, modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = OnSurface, fontSize = 15.sp)
    }
}
