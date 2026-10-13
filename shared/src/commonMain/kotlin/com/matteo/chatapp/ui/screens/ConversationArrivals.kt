package com.matteo.chatapp.ui.screens

import com.matteo.chatapp.model.Message

/** Counts arrivals after the last known message, never pages prepended to the history. */
internal class ConversationArrivals {
    private var previous: List<String> = emptyList()
    private val unseen = mutableSetOf<String>()
    val count get() = unseen.size

    fun update(messages: List<Message>, ownUserId: String?, following: Boolean) {
        val ids = messages.map { it.id }
        val positions = ids.withIndex().associate { it.value to it.index }
        val known = previous.toSet()
        val anchor = previous.asReversed().firstNotNullOfOrNull { positions[it] }
        if (!following && anchor != null) {
            messages.drop(anchor + 1).filter { it.id !in known && !it.deleted && it.senderId != ownUserId }
                .forEach { unseen.add(it.id) }
        }
        unseen.retainAll(messages.filterNot { it.deleted }.map { it.id }.toSet())
        previous = ids
        if (following) clear()
    }

    fun clear() = unseen.clear()
}
