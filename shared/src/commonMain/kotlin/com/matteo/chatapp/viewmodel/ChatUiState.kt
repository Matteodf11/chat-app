package com.matteo.chatapp.viewmodel

import com.matteo.chatapp.model.Message
import com.matteo.chatapp.repository.ConversationTarget
import com.matteo.chatapp.platform.PickedAttachmentFile

enum class SendStatus { SENDING, FAILED }

data class SharedItemsUiState(val dmId: String? = null, val kind: String = "media",
    val messages: List<Message> = emptyList(), val loading: Boolean = false,
    val hasMore: Boolean = true, val nextBefore: String? = null, val error: String? = null)

@kotlinx.serialization.Serializable
data class DmSummary(val preview: String?, val at: String?, val unreadCount: Int)

data class PendingMessage(
    val id: String,
    val target: ConversationTarget,
    val content: String,
    val replyToId: String?,
    val file: PickedAttachmentFile? = null,
    val status: SendStatus = SendStatus.SENDING,
    val error: String? = null,
    val createdAt: String = kotlin.time.Clock.System.now().toString().removeSuffix("Z")
)

data class ChatConversationUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingOlder: Boolean = false,
    val hasOlderMessages: Boolean = false,
    val error: String? = null,
    val isUploadingAttachment: Boolean = false,
    val typingUsers: Map<String, String> = emptyMap(),
    val pendingMessages: List<PendingMessage> = emptyList(),
    val isConnected: Boolean = false,
    val isSyncing: Boolean = false,
    val loadError: String? = null
)

data class MessageSearchUiState(
    val results: List<Message> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null,
    val scrollTargetMessageId: String? = null
)

internal fun List<Message>.withMessageChange(message: Message): List<Message> {
    if (message.deleted) return filterNot { it.id == message.id }

    val existingIndex = indexOfFirst { it.id == message.id }
    return if (existingIndex < 0) {
        this + message
    } else {
        toMutableList().apply { this[existingIndex] = message }
    }
}

internal fun List<Message>.prependNewMessages(olderMessages: List<Message>): List<Message> {
    val existingIds = mapTo(mutableSetOf()) { it.id }
    return olderMessages.filterNot { it.id in existingIds } + this
}
