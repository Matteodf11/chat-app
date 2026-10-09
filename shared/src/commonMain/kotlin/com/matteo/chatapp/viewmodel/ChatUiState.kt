package com.matteo.chatapp.viewmodel

import com.matteo.chatapp.model.Message

data class ChatConversationUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingOlder: Boolean = false,
    val hasOlderMessages: Boolean = false,
    val error: String? = null,
    val isUploadingAttachment: Boolean = false,
    val typingUsers: Map<String, String> = emptyMap()
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
