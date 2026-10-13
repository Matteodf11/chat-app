package com.matteo.chatapp.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
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
    val newMessageCount: Int = 0,
    val draft: com.matteo.chatapp.viewmodel.ComposerDraft = com.matteo.chatapp.viewmodel.ComposerDraft(),
    val title: String,
    val subtitle: String,
    val messages: List<Message>,
    val hasOlderMessages: Boolean,
    val isLoadingOlder: Boolean,
    val messageError: String?,
    val replyingTo: Message?,
    val messageInput: String,
    val isUploadingAttachment: Boolean,
    val pendingMessages: List<com.matteo.chatapp.viewmodel.PendingMessage> = emptyList(),
    val ownAvatarUrl: String? = null,
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val isSyncing: Boolean = false,
    val loadError: String? = null,
    val avatarUrl: String? = null,
    val online: Boolean? = null,
    val typingText: String? = null,
    val highlightedMessageId: String? = null,
    val canMessage: Boolean = true,
    val mentionMembers: List<com.matteo.chatapp.model.ServerMember> = emptyList()
)

internal data class ChatAreaActions(
    val onScrollToLatest: () -> Unit,
    val onUserScroll: () -> Unit,
    val onDraftChange: ((com.matteo.chatapp.viewmodel.ComposerDraft) -> com.matteo.chatapp.viewmodel.ComposerDraft) -> Boolean,
    val onLoadOlder: () -> Unit,
    val onEditMessage: (String, String) -> Unit,
    val onDeleteMessage: (String) -> Unit,
    val onReplyMessage: (Message) -> Unit,
    val onCancelReply: () -> Unit,
    val onInputChange: (String) -> Unit,
    val onSendAttachment: (PickedAttachmentFile, () -> Unit) -> Unit,
    val onSendPhotos: (List<PickedAttachmentFile>, String, (Boolean) -> Unit) -> Unit,
    val onSend: () -> Unit,
    val onRetryMessage: (String) -> Unit,
    val onDiscardMessage: (String) -> Unit,
    val onRefresh: () -> Unit,
    val onBack: (() -> Unit)? = null,
    val onSearch: (() -> Unit)? = null,
    val onProfileClick: (() -> Unit)? = null,
    val onForward: ((Message) -> Unit)? = null,
    val onOpenImage: (Message) -> Unit = {},
    val onDownload: (Message) -> Unit = {},
    val onSendLocation: (String) -> Unit = {}
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
    var composerValue by remember(destinationKey) { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(messageInput,
        androidx.compose.ui.text.TextRange(messageInput.length))) }
    LaunchedEffect(messageInput) {
        if (composerValue.text != messageInput) composerValue = androidx.compose.ui.text.input.TextFieldValue(messageInput,
            androidx.compose.ui.text.TextRange(messageInput.length))
    }
    val mention = if (composerValue.selection.collapsed) mentionQuery(messageInput, composerValue.selection.end) else null
    val suggestions = if (mention != null && state.canMessage) state.mentionMembers
        .filter { it.userId != SessionManager.getUserId() && it.username.startsWith(mention.query, ignoreCase = true) }
        .sortedBy { it.username.lowercase() }.take(5) else emptyList()
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
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var composerFocused by remember(destinationKey) { mutableStateOf(false) }
    val composerFocusRequester = remember(destinationKey) { FocusRequester() }
    var mentionsDismissed by remember(destinationKey) { mutableStateOf(false) }
    var selectedMentionIndex by remember(destinationKey, suggestions.map { it.userId }) { mutableIntStateOf(0) }
    fun completeMention(member: com.matteo.chatapp.model.ServerMember) {
        val query = mention ?: return
        val (text, cursor) = insertMention(composerValue.text, query, member.username)
        composerValue = androidx.compose.ui.text.input.TextFieldValue(text, androidx.compose.ui.text.TextRange(cursor))
        mentionsDismissed = true
        onInputChange(text)
        composerFocusRequester.requestFocus()
        keyboard?.show()
    }
    val composerBorder by animateColorAsState(if (composerFocused) Primary else Primary.copy(alpha = 0.55f), tween(120))
    val pendingAttachment = state.draft.attachment
    var isRecordingVoice by remember(destinationKey) { mutableStateOf(false) }
    var recordingSeconds by remember(destinationKey) { mutableStateOf(0) }
    var recordingError by remember(destinationKey) { mutableStateOf<String?>(null) }
    var attachmentMenu by remember(destinationKey) { mutableStateOf(false) }
    var locationDialog by remember(destinationKey) { mutableStateOf(false) }
    val pendingPhotos = state.draft.photos
    var reviewPhotos by remember(destinationKey) { mutableStateOf(false) }
    val fromCamera = state.draft.fromCamera
    var photoWorking by remember(destinationKey) { mutableStateOf(false) }
    var photoError by remember(destinationKey) { mutableStateOf<String?>(null) }
    val latestDestination by rememberUpdatedState(destinationKey)
    val latestCanMessage by rememberUpdatedState(state.canMessage)
    fun changeDraft(change: (com.matteo.chatapp.viewmodel.ComposerDraft) -> com.matteo.chatapp.viewmodel.ComposerDraft): Boolean {
        val saved = actions.onDraftChange(change)
        if (!saved) recordingError = "Los adjuntos de tus borradores superan 100 MB. Envía o quita alguno antes de añadir más."
        return saved
    }
    fun setAttachment(file: PickedAttachmentFile?) = changeDraft { it.copy(attachment = file) }
    fun selectPhotos(camera: Boolean) {
        val target = destinationKey
        photoWorking = true
        scope.launch {
            try {
                val photos = if (camera) listOfNotNull(com.matteo.chatapp.platform.capturePhoto())
                    else com.matteo.chatapp.platform.pickPhotoAttachments()
                if (latestDestination == target && latestCanMessage && photos.isNotEmpty()) {
                    if (changeDraft { it.copy(photos = photos, fromCamera = camera, attachment = null) }) {
                        reviewPhotos = true
                        photoError = null
                        recordingError = null
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) {
                if (pendingPhotos.isEmpty()) recordingError = e.message ?: "No se pudo abrir la foto"
                else photoError = e.message ?: "No se pudo abrir la cámara"
            } finally { photoWorking = false }
        }
    }
    if (reviewPhotos && pendingPhotos.isNotEmpty() && state.canMessage) com.matteo.chatapp.ui.components.PhotoReviewDialog(
        files = pendingPhotos, initialComment = messageInput, camera = fromCamera, working = photoWorking,
        error = photoError,
        onCommentChange = onInputChange,
        onRemove = { index -> changeDraft { it.copy(photos = it.photos.filterIndexed { i, _ -> i != index }) } },
        onRetake = { selectPhotos(true) },
        onFlip = { index ->
            val photo = pendingPhotos[index]
            val target = destinationKey
            photoWorking = true
            photoError = null
            scope.launch {
                try {
                    val flipped = com.matteo.chatapp.platform.flipCameraPhoto(photo)
                    if (latestDestination == target && latestCanMessage) {
                        changeDraft { it.copy(photos = it.photos.map { if (it === photo) flipped else it }) }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (e: Exception) { photoError = e.message ?: "No se pudo voltear la foto" }
                finally { photoWorking = false }
            }
        },
        onDismiss = { reviewPhotos = false; photoError = null },
        onSend = { comment ->
            photoWorking = true
            photoError = null
            actions.onSendPhotos(pendingPhotos, comment) { queued ->
                photoWorking = false
                if (queued) { changeDraft { if (it.photos == pendingPhotos) it.copy(photos = emptyList()) else it }; reviewPhotos = false }
                else photoError = "No se pudieron guardar las fotos. Vuelve a intentarlo."
            }
        }
    )
    val canSend = state.canMessage && (messageInput.isNotBlank() || pendingAttachment != null || pendingPhotos.isNotEmpty()) && !isUploadingAttachment && !isRecordingVoice && !photoWorking
    val showMentionSuggestions = suggestions.isNotEmpty() && !mentionsDismissed && !isRecordingVoice &&
        !attachmentMenu && !locationDialog && !reviewPhotos
    val sendColor by animateColorAsState(if (canSend) Primary else SurfaceVariant, tween(120))
    val sendCurrent = {
        val attachment = pendingAttachment
        if (pendingPhotos.isNotEmpty()) reviewPhotos = true
        else if (attachment != null) {
            onSendAttachment(attachment) { changeDraft { if (it.attachment === attachment) it.copy(attachment = null) else it } }
        } else {
            onSend()
        }
    }
    if (locationDialog) LocationShareDialog(onDismiss = { locationDialog = false }, onSend = { if (state.canMessage) actions.onSendLocation(it) })
    LaunchedEffect(state.canMessage) {
        if (!state.canMessage) {
            cancelVoiceRecording()
            isRecordingVoice = false
            attachmentMenu = false
            locationDialog = false
            reviewPhotos = false
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
            setAttachment(stopVoiceRecording())
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
    LaunchedEffect(imeBottom > 0, destinationKey) {
        if (imeBottom > 0 && messages.isNotEmpty() && !listState.canScrollForward && !listState.isScrollInProgress) {
            val dateSeparatorCount = messages.mapNotNull { it.createdAt?.take(10) }.distinct().size
            val targetIndex = (messages.size + dateSeparatorCount + state.pendingMessages.size - 1).coerceAtLeast(0)
            listState.scrollToLatest(targetIndex + if (isLoadingOlder) 1 else 0, animated = false)
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth().height(68.dp).background(Background).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            onBack?.let {
                IconButton(onClick = it, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = OnSurface, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(6.dp))
            }
            Row(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .then(if (onProfileClick != null) Modifier.clickable(
                        onClickLabel = "Ver perfil de $title",
                        role = androidx.compose.ui.semantics.Role.Button,
                        onClick = onProfileClick
                    ) else Modifier)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                Text(title, color = OnBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(
                    typingText ?: subtitle,
                    color = if (typingText != null) Accent else Muted,
                    fontSize = 12.sp,
                    fontWeight = if (typingText != null) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1
                )
            }
            }
            onSearch?.let {
                IconButton(onClick = it, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Rounded.Search, contentDescription = "Buscar mensajes", tint = OnSurface, modifier = Modifier.size(20.dp))
                }
            }
        }
        HorizontalDivider(color = Outline)
        if (state.loadError != null) {
            Row(Modifier.fillMaxWidth().background(SurfaceVariant).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(state.loadError,
                    modifier = Modifier.weight(1f), color = Error, fontSize = 12.sp)
                TextButton(onClick = actions.onRefresh, enabled = !state.isSyncing) { Text("Reintentar") }
            }
        }
        Box(Modifier.weight(1f).widthIn(max = 1000.dp).fillMaxWidth()) {
        val latestUserScroll by rememberUpdatedState(actions.onUserScroll)
        val scrollObserver = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (source == NestedScrollSource.UserInput && available.y != 0f) latestUserScroll()
                    return Offset.Zero
                }
            }
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().nestedScroll(scrollObserver), contentPadding = PaddingValues(vertical = 16.dp)) {
            if (messages.isEmpty() && state.pendingMessages.isEmpty()) {
                item(key = "empty-conversation") {
                    Box(Modifier.fillParentMaxHeight().fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (state.isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = Primary)
                        else EmptyState(if (state.loadError != null) "No se pudieron cargar los mensajes" else "Aquí empieza la conversación")
                    }
                }
            }
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
                item(key = if (message.senderId == SessionManager.getUserId()) message.clientMessageId?.let { "pending-$it" } ?: message.id else message.id) {
                    MessageItem(
                        message = message,
                        canModify = message.senderId == SessionManager.getUserId(),
                        onReply = { onReplyMessage(message) },
                        onReplyClick = { replyId ->
                            actions.onUserScroll()
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
                        highlighted = message.id == highlightedMessageId,
                        onForward = actions.onForward,
                        onOpenImage = actions.onOpenImage, onDownload = actions.onDownload,
                        allowEdit = state.canMessage
                    )
                }
            }
            state.pendingMessages.filterNot { pending -> messages.any {
                it.clientMessageId == pending.id && it.senderId == SessionManager.getUserId()
            } }.forEach { pending ->
                item(key = "pending-${pending.id}") {
                    val reply = messages.find { it.id == pending.replyToId }
                    MessageItem(
                        message = Message(
                            id = pending.id, content = pending.content,
                            senderId = SessionManager.getUserId(),
                            senderUsername = SessionManager.getUsername().orEmpty(),
                            senderAvatar = state.ownAvatarUrl,
                            createdAt = pending.createdAt,
                            replyToId = pending.replyToId,
                            replyToSenderUsername = reply?.senderUsername,
                            replyToContent = reply?.content ?: "Respuesta a un mensaje",
                            attachmentName = pending.file?.fileName,
                            attachmentContentType = pending.file?.contentType
                        ),
                        canModify = true, pending = true,
                        onReply = {}, onReplyClick = {}, onEdit = {}, onDelete = {}
                    )
                    if (pending.status == com.matteo.chatapp.viewmodel.SendStatus.FAILED) {
                        Column(Modifier.fillMaxWidth().padding(start = 70.dp, end = 20.dp, bottom = 8.dp)) {
                            Text(pending.error ?: "No se pudo confirmar el envío", color = Error, fontSize = 12.sp)
                            Row {
                                TextButton(enabled = state.canMessage, onClick = { actions.onRetryMessage(pending.id) }) { Text("Reintentar") }
                                TextButton(onClick = { actions.onDiscardMessage(pending.id) }) { Text("Descartar") }
                            }
                        }
                    }
                }
            }
        }
        NewMessagesButton(state.newMessageCount, actions.onScrollToLatest,
            Modifier.align(Alignment.BottomCenter).padding(12.dp))
        }
        Column(Modifier.widthIn(max = 1000.dp).fillMaxWidth().animateContentSize(tween(160))) {
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
        if (!state.canMessage) Text("No puedes enviar mensajes en esta conversación", color = Muted, modifier = Modifier.padding(12.dp))
        if (pendingPhotos.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().background(SurfaceVariant).padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { reviewPhotos = true }, modifier = Modifier.weight(1f), enabled = !photoWorking) {
                    Icon(Icons.Rounded.PhotoLibrary, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${pendingPhotos.size} fotos preparadas · Revisar")
                }
                IconButton(enabled = !photoWorking, onClick = { changeDraft { it.copy(photos = emptyList()) } }) {
                    Icon(Icons.Rounded.Close, "Quitar fotos")
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
                IconButton(onClick = { setAttachment(null) }, enabled = !isUploadingAttachment, modifier = Modifier.size(34.dp)) {
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
        // On desktop a mouse press can move focus before its release. Keep the rows
        // mounted until selection completes so their click handler is not cancelled.
        if (showMentionSuggestions) {
            Surface(Modifier.fillMaxWidth().padding(horizontal = 12.dp), color = SurfaceElevated,
                shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(8.dp)) {
                    Text("Mencionar a · ↑↓ elegir · Tab completar", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                    suggestions.forEachIndexed { index, member ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(if (index == selectedMentionIndex) PrimarySoft else Color.Transparent)
                            .focusProperties { canFocus = false }.clickable {
                            completeMention(member)
                        }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            com.matteo.chatapp.ui.components.AvatarImage(member.avatarUrl, member.username, 28)
                            Spacer(Modifier.width(10.dp))
                            Text("@${member.username}", color = OnBackground, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp)).background(Surface)
                .border(1.dp, composerBorder, RoundedCornerShape(24.dp)).padding(4.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Box {
            IconButton(
                onClick = { keyboard?.hide(); focusManager.clearFocus(); attachmentMenu = true },
                enabled = state.canMessage && !isUploadingAttachment && !isRecordingVoice && !photoWorking,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Rounded.AttachFile, contentDescription = "Adjuntar archivo", tint = OnSurface)
            }
            }
            if (attachmentMenu) com.matteo.chatapp.ui.components.ActionSheet("Adjuntar", { attachmentMenu = false }) {
                if (com.matteo.chatapp.platform.supportsPhotoCapture())
                    com.matteo.chatapp.ui.components.ActionSheetItem("Cámara", Icons.Rounded.PhotoCamera) {
                        attachmentMenu = false; selectPhotos(true)
                    }
                com.matteo.chatapp.ui.components.ActionSheetItem("Galería", Icons.Rounded.PhotoLibrary) {
                    attachmentMenu = false; selectPhotos(false)
                }
                com.matteo.chatapp.ui.components.ActionSheetItem("Archivo", Icons.Rounded.Description) {
                    attachmentMenu = false
                        val target = destinationKey
                        scope.launch {
                            try {
                                val file = pickAttachmentFile()
                                if (latestDestination == target && latestCanMessage) {
                                    file?.let { picked ->
                                        if (changeDraft { it.copy(attachment = picked, photos = emptyList()) }) recordingError = null
                                    }
                                }
                            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                            catch (e: Exception) { recordingError = e.message ?: "No se pudo abrir el archivo" }
                        }

                }
                com.matteo.chatapp.ui.components.ActionSheetItem("Ubicación", Icons.Rounded.LocationOn) {
                    attachmentMenu = false; locationDialog = true
                }
            }
            BasicTextField(
                value = composerValue,
                onValueChange = { composerValue = it; mentionsDismissed = false; onInputChange(it.text) },
                enabled = state.canMessage && !isRecordingVoice,
                modifier = Modifier
                    .weight(1f).heightIn(min = 48.dp).padding(horizontal = 8.dp)
                    .focusRequester(composerFocusRequester)
                    .onFocusChanged { composerFocused = it.isFocused }
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && showMentionSuggestions &&
                            event.key == Key.Tab && !event.isShiftPressed) {
                            suggestions.getOrNull(selectedMentionIndex)?.let(::completeMention)
                            true
                        } else if (event.type == KeyEventType.KeyDown && showMentionSuggestions &&
                            (event.key == Key.DirectionDown || event.key == Key.DirectionUp)) {
                            val step = if (event.key == Key.DirectionDown) 1 else -1
                            selectedMentionIndex = (selectedMentionIndex + step + suggestions.size) % suggestions.size
                            true
                        } else if (event.key == Key.Escape && event.type == KeyEventType.KeyDown &&
                            suggestions.isNotEmpty() && !mentionsDismissed) {
                            mentionsDismissed = true
                            true
                        } else if (event.key == Key.Escape && event.type == KeyEventType.KeyDown && replyingTo != null) {
                            onCancelReply()
                            true
                        } else if (event.key == Key.Enter && event.type == KeyEventType.KeyDown && !event.isShiftPressed) {
                            if (canSend) sendCurrent()
                            true
                        } else {
                            false
                        }
                    },
                singleLine = false,
                minLines = 1,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (canSend) sendCurrent() }),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = OnBackground),
                cursorBrush = SolidColor(Primary),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth().padding(vertical = 13.dp), contentAlignment = Alignment.CenterStart) {
                        if (messageInput.isEmpty()) Text("Mensaje", color = Muted, style = MaterialTheme.typography.bodyLarge)
                        inner()
                    }
                }
            )
            if (isRecordingVoice || (messageInput.isBlank() && pendingAttachment == null && pendingPhotos.isEmpty() && !isUploadingAttachment)) IconButton(
                onClick = {
                    scope.launch {
                        if (isRecordingVoice) {
                            val voice = stopVoiceRecording()
                            isRecordingVoice = false
                            recordingSeconds = 0
                            if (voice != null) {
                                if (setAttachment(voice)) recordingError = null
                            } else {
                                recordingError = "La nota de voz es demasiado corta"
                            }
                        } else {
                            setAttachment(null)
                            if (startVoiceRecording()) {
                                isRecordingVoice = true
                                recordingError = null
                            } else {
                                recordingError = "No se pudo acceder al microfono"
                            }
                        }
                    }
                },
                enabled = state.canMessage && !isUploadingAttachment,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isRecordingVoice) Error else Color.Transparent)
            ) {
                Icon(
                    if (isRecordingVoice) Icons.Rounded.Stop else Icons.Rounded.Mic,
                    contentDescription = if (isRecordingVoice) "Detener grabacion" else "Grabar nota de voz",
                    tint = if (isRecordingVoice) Color.White else OnSurface
                )
            }
            else IconButton(
                onClick = sendCurrent,
                enabled = canSend,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(sendColor)
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
        Column(Modifier.widthIn(max = 320.dp).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(PrimarySoft), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(text, color = OnBackground, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}


@Composable
private fun NewMessagesButton(count: Int, onClick: () -> Unit, modifier: Modifier) {
    AnimatedVisibility(count > 0, modifier,
        enter = androidx.compose.animation.fadeIn(tween(120)), exit = androidx.compose.animation.fadeOut(tween(100))) {
        FilledTonalButton(onClick = onClick,
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = PrimarySoft, contentColor = OnBackground)) {
            Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (count == 1) "1 mensaje nuevo" else "$count mensajes nuevos")
        }
    }
}
