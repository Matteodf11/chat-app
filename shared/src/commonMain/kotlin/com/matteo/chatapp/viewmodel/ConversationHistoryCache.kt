package com.matteo.chatapp.viewmodel

import com.matteo.chatapp.model.Message
import com.matteo.chatapp.repository.ConversationTarget

/** Session-only LRU of recent history; attachments stay as URLs, never binary payloads. */
internal class ConversationHistoryCache(
    private val maxConversations: Int = 8,
    private val maxMessages: Int = 200,
    private val maxBytes: Long = 4L * 1024 * 1024
) {
    data class History(val messages: List<Message>, val hasOlder: Boolean)
    private val entries = linkedMapOf<ConversationTarget, History>()
    fun get(target: ConversationTarget): History? = entries.remove(target)?.also { entries[target] = it }
    fun put(target: ConversationTarget, messages: List<Message>, hasOlder: Boolean) {
        val newest = messages.takeLast(maxMessages).toMutableList()
        while (newest.isNotEmpty() && weight(newest) > maxBytes) newest.removeAt(0)
        entries.remove(target)
        entries[target] = History(newest.toList(), hasOlder || newest.size < messages.size)
        while (entries.size > maxConversations || entries.values.sumOf { weight(it.messages) } > maxBytes) {
            entries.remove(entries.keys.first())
        }
    }
    fun update(target: ConversationTarget, message: Message) {
        val old = entries[target] ?: return
        // Don't insert older, newly edited messages outside this cached window.
        if (message.edited && old.messages.none { it.id == message.id }) return
        put(target, old.messages.withMessageChange(message)
            .sortedWith(compareBy<Message> { it.createdAt.orEmpty() }.thenBy { it.id }), old.hasOlder)
    }
    fun mapMessages(transform: (Message) -> Message) {
        entries.toMap().forEach { (target, history) -> put(target, history.messages.map(transform), history.hasOlder) }
    }
    fun clear() = entries.clear()
    private fun weight(messages: List<Message>): Long = messages.sumOf { message ->
        256L + 2L * listOfNotNull(message.id, message.content, message.senderId, message.senderUsername,
            message.senderAvatar, message.channelId, message.dmId, message.replyToId,
            message.replyToSenderUsername, message.replyToContent, message.attachmentName,
            message.attachmentContentType, message.attachmentUrl, message.createdAt,
            message.clientMessageId, message.readAt).sumOf { it.length }
    }
}
