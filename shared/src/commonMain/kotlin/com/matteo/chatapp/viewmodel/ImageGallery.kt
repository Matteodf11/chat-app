package com.matteo.chatapp.viewmodel

import com.matteo.chatapp.model.Message

data class ImageGalleryUiState(val anchor: Message? = null, val images: List<Message> = emptyList(),
    val loading: Boolean = false, val hasMore: Boolean = true, val cursor: String? = null, val error: String? = null)

internal fun galleryImages(anchor: Message, messages: List<Message>): List<Message> = messages
    .filter { !it.deleted && it.attachmentContentType?.startsWith("image/") == true &&
        if (anchor.dmId != null) it.dmId == anchor.dmId else anchor.channelId != null && it.channelId == anchor.channelId }
    .distinctBy { it.id }.sortedWith(compareBy<Message> { it.createdAt.orEmpty() }.thenBy { it.id })
