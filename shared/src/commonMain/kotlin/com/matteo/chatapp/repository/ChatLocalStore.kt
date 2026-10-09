package com.matteo.chatapp.repository

import com.matteo.chatapp.viewmodel.PendingMessage
import com.matteo.chatapp.viewmodel.DmSummary
import com.matteo.chatapp.model.DmConversation

interface ChatLocalStore {
    suspend fun pending(): List<PendingMessage> = emptyList()
    suspend fun put(message: PendingMessage) {}
    suspend fun putBatch(messages: List<PendingMessage>) {
        try { messages.forEach { put(it) } }
        catch (e: Exception) { messages.forEach { runCatching { remove(it.id) } }; throw e }
    }
    suspend fun remove(id: String) {}
    suspend fun summaries(): Map<String, DmSummary> = emptyMap()
    suspend fun saveSummaries(value: Map<String, DmSummary>) {}
    suspend fun preferences(): List<DmConversation> = emptyList()
    suspend fun savePreferences(value: List<DmConversation>) {}
}

expect fun platformChatLocalStore(account: String): ChatLocalStore
internal expect fun atomicReplaceChatFile(from: String, to: String)
