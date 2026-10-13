package com.matteo.chatapp.viewmodel

import com.matteo.chatapp.model.Message
import com.matteo.chatapp.platform.PickedAttachmentFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class ComposerDraft(val reply: Message? = null, val attachment: PickedAttachmentFile? = null,
    val photos: List<PickedAttachmentFile> = emptyList(), val fromCamera: Boolean = false) {
    val bytes get() = (attachment?.size ?: 0L) + photos.sumOf { it.size }
}

/** Session-owned drafts survive navigation and Activity recreation; never silently evict files. */
internal class ComposerDrafts(private val maxBytes: Long = 100L * 1024 * 1024) {
    private val _state = MutableStateFlow<Map<String, ComposerDraft>>(emptyMap())
    val state = _state.asStateFlow()
    fun update(key: String, change: (ComposerDraft) -> ComposerDraft): Boolean {
        val current = _state.value
        val next = change(current[key] ?: ComposerDraft())
        if (current.filterKeys { it != key }.values.sumOf { it.bytes } + next.bytes > maxBytes) return false
        _state.value = if (next == ComposerDraft()) current - key else current + (key to next)
        return true
    }
    fun clear() { _state.value = emptyMap() }
}
