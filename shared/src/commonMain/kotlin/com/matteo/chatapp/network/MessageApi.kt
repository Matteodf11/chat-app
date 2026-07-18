package com.matteo.chatapp.network

import com.matteo.chatapp.model.Message
import com.matteo.chatapp.model.MessagePage
import com.matteo.chatapp.platform.PickedAttachmentFile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.delete
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

class MessageApi(private val client: HttpClient) {

    suspend fun getChannelMessages(channelId: String, token: String, before: String? = null): MessagePage {
        return client.get("$BASE_URL/api/messages/channel/$channelId") {
            bearerAuth(token)
            parameter("limit", 50)
            before?.let { parameter("before", it) }
        }.body()
    }

    suspend fun sendMessage(channelId: String, content: String, replyToId: String?, token: String): Message {
        return client.post("$BASE_URL/api/messages/channel/$channelId") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(buildMap {
                put("content", content)
                put("channelId", channelId)
                replyToId?.let { put("replyToId", it) }
            })
        }.body()
    }

    suspend fun getDmMessages(dmId: String, token: String, before: String? = null): MessagePage {
        return client.get("$BASE_URL/api/messages/dm/$dmId") {
            bearerAuth(token)
            parameter("limit", 50)
            before?.let { parameter("before", it) }
        }.body()
    }

    suspend fun searchChannelMessages(
        channelId: String,
        query: String,
        sender: String,
        from: String,
        to: String,
        attachmentsOnly: Boolean,
        token: String
    ): List<Message> = searchMessages("channel", channelId, query, sender, from, to, attachmentsOnly, token)

    suspend fun searchDmMessages(
        dmId: String,
        query: String,
        sender: String,
        from: String,
        to: String,
        attachmentsOnly: Boolean,
        token: String
    ): List<Message> = searchMessages("dm", dmId, query, sender, from, to, attachmentsOnly, token)

    suspend fun getMessageContext(messageId: String, token: String): MessagePage {
        return client.get("$BASE_URL/api/messages/context/$messageId") {
            bearerAuth(token)
        }.body()
    }

    suspend fun sendDmMessage(dmId: String, content: String, replyToId: String?, token: String): Message {
        return client.post("$BASE_URL/api/messages/dm/$dmId") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(buildMap {
                put("content", content)
                put("dmId", dmId)
                replyToId?.let { put("replyToId", it) }
            })
        }.body()
    }

    suspend fun sendChannelAttachment(
        channelId: String,
        file: PickedAttachmentFile,
        content: String,
        replyToId: String?,
        token: String
    ): Message = sendAttachment("channel", channelId, file, content, replyToId, token)

    suspend fun sendDmAttachment(
        dmId: String,
        file: PickedAttachmentFile,
        content: String,
        replyToId: String?,
        token: String
    ): Message = sendAttachment("dm", dmId, file, content, replyToId, token)

    private suspend fun sendAttachment(
        destination: String,
        destinationId: String,
        file: PickedAttachmentFile,
        content: String,
        replyToId: String?,
        token: String
    ): Message {
        return client.post("$BASE_URL/api/messages/$destination/$destinationId/attachment") {
            bearerAuth(token)
            setBody(MultiPartFormDataContent(formData {
                append("content", content)
                replyToId?.let { append("replyToId", it) }
                append("file", file.bytes, Headers.build {
                    append(HttpHeaders.ContentType, file.contentType)
                    append(HttpHeaders.ContentDisposition, "filename=\"${file.fileName.replace("\"", "")}\"")
                })
            }))
        }.body()
    }

    suspend fun editMessage(messageId: String, content: String, token: String): Message {
        return client.put("$BASE_URL/api/messages/$messageId") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(mapOf("content" to content))
        }.body()
    }

    suspend fun deleteMessage(messageId: String, token: String) {
        client.delete("$BASE_URL/api/messages/$messageId") {
            bearerAuth(token)
        }
    }

    private suspend fun searchMessages(
        destination: String,
        destinationId: String,
        query: String,
        sender: String,
        from: String,
        to: String,
        attachmentsOnly: Boolean,
        token: String
    ): List<Message> {
        return client.get("$BASE_URL/api/messages/$destination/$destinationId/search") {
            bearerAuth(token)
            if (query.isNotBlank()) parameter("q", query.trim())
            if (sender.isNotBlank()) parameter("sender", sender.trim())
            if (from.isNotBlank()) parameter("from", from.trim())
            if (to.isNotBlank()) parameter("to", to.trim())
            if (attachmentsOnly) parameter("attachmentsOnly", true)
            parameter("limit", 50)
        }.body()
    }
}
