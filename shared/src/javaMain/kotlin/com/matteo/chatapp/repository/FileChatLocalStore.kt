package com.matteo.chatapp.repository

import com.matteo.chatapp.model.DmConversation
import com.matteo.chatapp.platform.PickedAttachmentFile
import com.matteo.chatapp.viewmodel.*
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** One atomic manifest per send; attachment data never lives in preferences. */
class FileChatLocalStore(root: File, account: String) : ChatLocalStore {
    private val directory = File(root, MessageDigest.getInstance("SHA-256").digest(account.toByteArray())
        .joinToString("") { "%02x".format(it) })
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { mutex.withLock { block() } }
    private fun atomic(file: File, bytes: ByteArray) {
        directory.mkdirs()
        val temp = File(file.path + ".tmp")
        temp.outputStream().use { it.write(bytes); it.fd.sync() }
        atomicReplaceChatFile(temp.path, file.path)
    }
    private fun item(id: String, extension: String): File {
        require(id.matches(Regex("[a-fA-F0-9-]{36}")))
        return File(directory, "$id.$extension")
    }
    override suspend fun put(message: PendingMessage) = io {
        message.file?.let { file ->
            val binary = item(message.id, "bin")
            if (!binary.exists()) atomic(binary, file.bytes)
        }
        atomic(item(message.id, "json"), json.encodeToString(StoredPending(message.id,
            message.target.id, message.target is ConversationTarget.DirectMessage, message.content,
            message.replyToId, message.file?.fileName, message.file?.contentType, message.createdAt)).toByteArray())
    }
    override suspend fun putBatch(messages: List<PendingMessage>) = io {
        require(messages.isNotEmpty())
        val created = mutableListOf<File>()
        try {
            val entries = messages.map { message ->
                message.file?.let {
                    val binary = item(message.id, "bin")
                    check(!binary.exists() || binary.isFile)
                    if (!binary.exists()) { created.add(binary); atomic(binary, it.bytes) }
                }
                StoredPending(message.id, message.target.id, message.target is ConversationTarget.DirectMessage,
                    message.content, message.replyToId, message.file?.fileName, message.file?.contentType, message.createdAt)
            }
            atomic(File(directory, "batch-${messages.first().id}.json"), json.encodeToString(entries).toByteArray())
        } catch (e: Exception) { created.forEach { it.delete() }; throw e }
    }

    override suspend fun pending(): List<PendingMessage> = io {
        directory.listFiles().orEmpty().filter { it.extension == "json" && (it.nameWithoutExtension.length == 36 || it.name.startsWith("batch-")) }.flatMap { file ->
            runCatching {
                if (file.name.startsWith("batch-")) json.decodeFromString<List<StoredPending>>(file.readText())
                else listOf(json.decodeFromString<StoredPending>(file.readText()))
            }.getOrDefault(emptyList())
        }.mapNotNull { entry ->
            runCatching {
                PendingMessage(entry.id, if (entry.dm) ConversationTarget.DirectMessage(entry.target)
                    else ConversationTarget.Channel(entry.target), entry.content, entry.replyToId,
                    entry.fileName?.let { PickedAttachmentFile(it, entry.contentType ?: "application/octet-stream", item(entry.id, "bin").readBytes()) },
                    SendStatus.FAILED, "Envío recuperado. Pulsa reintentar para enviarlo.", entry.createdAt)
            }.getOrNull()
        }.sortedBy { it.createdAt }
    }
    override suspend fun remove(id: String) = io {
        // Remove manifest first: an interrupted cleanup cannot resurrect an acknowledged send.
        directory.listFiles().orEmpty().filter { it.name.startsWith("batch-") && it.extension == "json" }.forEach { batch ->
            val entries = runCatching { json.decodeFromString<List<StoredPending>>(batch.readText()) }.getOrNull() ?: return@forEach
            if (entries.any { it.id == id }) {
                val remaining = entries.filterNot { it.id == id }
                if (remaining.isEmpty()) check(batch.delete())
                else atomic(batch, json.encodeToString(remaining).toByteArray())
            }
        }
        val manifest = item(id, "json")
        check(!manifest.exists() || manifest.delete())
        item(id, "bin").delete()
        Unit
    }
    override suspend fun summaries(): Map<String, DmSummary> = io {
        runCatching { json.decodeFromString<Map<String, DmSummary>>(File(directory, "summaries.json").readText()) }.getOrDefault(emptyMap())
    }
    override suspend fun saveSummaries(value: Map<String, DmSummary>) = io {
        atomic(File(directory, "summaries.json"), json.encodeToString(value).toByteArray())
    }
    override suspend fun preferences(): List<DmConversation> = io {
        runCatching { json.decodeFromString<List<DmConversation>>(File(directory, "preferences.json").readText()) }.getOrDefault(emptyList())
    }
    override suspend fun savePreferences(value: List<DmConversation>) = io {
        atomic(File(directory, "preferences.json"), json.encodeToString(value).toByteArray())
    }
}

@Serializable
private data class StoredPending(val id: String, val target: String, val dm: Boolean, val content: String,
    val replyToId: String?, val fileName: String?, val contentType: String?, val createdAt: String)
