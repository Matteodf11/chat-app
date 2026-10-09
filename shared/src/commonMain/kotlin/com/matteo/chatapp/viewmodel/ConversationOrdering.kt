package com.matteo.chatapp.viewmodel

import com.matteo.chatapp.model.DmConversation

fun orderedConversations(dms: List<DmConversation>, summaries: Map<String, DmSummary>,
    preferences: Map<String, DmConversation>): List<DmConversation> = dms.map { dm ->
    val settings = preferences[dm.id] ?: dm
    val summary = summaries[dm.id]
    val activity = listOfNotNull(dm.lastActivityAt, dm.lastMessageAt, summary?.at, dm.createdAt)
        .maxOrNull()
    val latestSummary = summary != null && summary.at.orEmpty() >= dm.lastMessageAt.orEmpty()
    dm.copy(pinned = settings.pinned, muted = settings.muted, archived = settings.archived,
        lastActivityAt = activity,
        lastMessageAt = if (latestSummary) summary?.at else dm.lastMessageAt,
        lastMessagePreview = if (latestSummary) summary?.preview else dm.lastMessagePreview,
        unreadCount = summary?.unreadCount ?: dm.unreadCount,
        hasUnread = summary?.let { it.unreadCount > 0 } ?: dm.hasUnread)
}.sortedWith(compareByDescending<DmConversation> { it.pinned }
    .thenByDescending { it.lastActivityAt.orEmpty() }.thenBy { it.id })
