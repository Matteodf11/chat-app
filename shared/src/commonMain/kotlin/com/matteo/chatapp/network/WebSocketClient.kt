package com.matteo.chatapp.network

import com.matteo.chatapp.model.Message
import com.matteo.chatapp.model.FriendshipEvent
import com.matteo.chatapp.model.PresenceEvent
import com.matteo.chatapp.model.ProfileEvent
import com.matteo.chatapp.model.TypingEvent
import com.matteo.chatapp.model.ServerEvent
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

class WebSocketClient(private val client: HttpClient) {
    private val _readEvents = MutableSharedFlow<com.matteo.chatapp.model.DmReadEvent>()
    val readEvents = _readEvents as SharedFlow<com.matteo.chatapp.model.DmReadEvent>
    private val _dmSettings = MutableSharedFlow<com.matteo.chatapp.model.DmConversation>()
    val dmSettings = _dmSettings as SharedFlow<com.matteo.chatapp.model.DmConversation>
    private fun isReadEvent(body: String) = decoder.parseToJsonElement(body).let {
        (it as? kotlinx.serialization.json.JsonObject)?.get("type")?.toString() == "\"dm_read\""
    }
    private val _connected = MutableStateFlow(false)
    val connected = _connected.asStateFlow()
    private val decoder = Json { ignoreUnknownKeys = true }
    private val closeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _incomingMessages = MutableSharedFlow<Message>()
    val incomingMessages: SharedFlow<Message> = _incomingMessages

    private val _personalMessages = MutableSharedFlow<Message>()
    val personalMessages: SharedFlow<Message> = _personalMessages

    private val _typingEvents = MutableSharedFlow<TypingEvent>()
    val typingEvents: SharedFlow<TypingEvent> = _typingEvents

    private val _contactsChanged = MutableSharedFlow<Unit>()
    val contactsChanged: SharedFlow<Unit> = _contactsChanged
    private val _friendshipEvents = MutableSharedFlow<FriendshipEvent>()
    val friendshipEvents: SharedFlow<FriendshipEvent> = _friendshipEvents

    private val _presenceEvents = MutableSharedFlow<PresenceEvent>()
    val presenceEvents: SharedFlow<PresenceEvent> = _presenceEvents

    private val _profileEvents = MutableSharedFlow<ProfileEvent>()
    val profileEvents: SharedFlow<ProfileEvent> = _profileEvents

    private val _serverEvents = MutableSharedFlow<ServerEvent>()
    val serverEvents: SharedFlow<ServerEvent> = _serverEvents

    private var session: WebSocketSession? = null

    suspend fun connect(token: String, channelId: String) {
        connectMessages(
            token = token,
            subscriptionId = "channel-$channelId",
            messageDestination = "/topic/channel.$channelId",
            typingDestination = "/topic/typing.channel.$channelId"
        )
    }

    suspend fun connectDmMessages(token: String, dmId: String) {
        connectMessages(
            token = token,
            subscriptionId = "dm-$dmId",
            messageDestination = "/topic/dm.$dmId",
            typingDestination = "/topic/typing.dm.$dmId"
        )
    }

    suspend fun connectPersonalMessages(token: String) {
        connectEvents(token, "personal-messages", listOf("/user/queue/messages", "/user/queue/read-receipts", "/user/queue/dm-settings")) { subscription, body ->
            when (subscription) {
                "personal-messages-1" -> _readEvents.emit(decoder.decodeFromString(body))
                "personal-messages-2" -> _dmSettings.emit(decoder.decodeFromString(body))
                else -> _personalMessages.emit(decoder.decodeFromString<Message>(body))
            }
        }
    }

    suspend fun connectDmInbox(token: String, dmIds: List<String>) {
        if (dmIds.isEmpty()) return
        connectEvents(token, "dm-inbox", dmIds.distinct().map { "/topic/dm.$it" }) { _, body ->
            if (isReadEvent(body)) _readEvents.emit(decoder.decodeFromString(body))
            else _personalMessages.emit(decoder.decodeFromString<Message>(body))
        }
    }

    suspend fun connectFriendEvents(token: String, userId: String? = null) {
        connectEvents(token, "friends", listOfNotNull("/user/queue/contacts", "/user/queue/friends", userId?.let { "/topic/friends.$it" })) { subscription, body ->
            if (subscription == "friends-0") _contactsChanged.emit(Unit)
            else _friendshipEvents.emit(decoder.decodeFromString<FriendshipEvent>(body))
        }
    }

    suspend fun connectPresenceEvents(token: String) {
        connectEvents(token, "presence", "/topic/presence") { body ->
            _presenceEvents.emit(decoder.decodeFromString<PresenceEvent>(body))
        }
    }

    suspend fun connectProfileEvents(token: String) {
        connectEvents(token, "profile", "/topic/profile") { body ->
            _profileEvents.emit(decoder.decodeFromString<ProfileEvent>(body))
        }
    }

    suspend fun connectServerEvents(token: String) {
        connectEvents(token, "server-events", "/user/queue/server-events") { body ->
            _serverEvents.emit(decoder.decodeFromString<ServerEvent>(body))
        }
    }

    private suspend fun connectMessages(
        token: String,
        subscriptionId: String,
        messageDestination: String,
        typingDestination: String
    ) {
        connectEvents(
            token = token,
            subscriptionId = subscriptionId,
            destinations = listOf(messageDestination, typingDestination)
        ) { subscription, body ->
            if (subscription == "$subscriptionId-0") {
                if (isReadEvent(body)) _readEvents.emit(decoder.decodeFromString(body))
                else _incomingMessages.emit(decoder.decodeFromString<Message>(body))
            } else {
                _typingEvents.emit(decoder.decodeFromString<TypingEvent>(body))
            }
        }
    }

    private suspend fun connectEvents(
        token: String,
        subscriptionId: String,
        destination: String,
        onBody: suspend (String) -> Unit
    ) {
        connectEvents(token, subscriptionId, listOf(destination)) { _, body -> onBody(body) }
    }

    private suspend fun connectEvents(
        token: String,
        subscriptionId: String,
        destinations: List<String>,
        onBody: suspend (String, String) -> Unit
    ) {
        val wsUrl = BASE_URL
            .replace("https://", "wss://")
            .replace("http://", "ws://")

        client.webSocket(
            urlString = "$wsUrl/ws-native?token=$token"
        ) {
            session = this
            connectStomp(token)
            val heartbeat = launch {
                while (isActive) {
                    delay(10000)
                    send(Frame.Text("\n"))
                }
            }
            var subscribed = false
            var receiveTimeout = 15000L
            try {
                while (true) {
                    val frame = if (receiveTimeout > 0) withTimeout(receiveTimeout) { incoming.receive() } else incoming.receive()
                    if (frame is Frame.Text) {
                        val text = frame.readText().trimStart('\n', '\r')
                        if (text.startsWith("ERROR")) {
                            throw IllegalStateException(extractStompBody(text).ifBlank { "STOMP subscription rejected" })
                        }
                        try {
                            if (!subscribed && text.startsWith("CONNECTED")) {
                                val serverHeartbeat = extractStompHeader(text, "heart-beat").substringBefore(',').toLongOrNull() ?: 0
                                receiveTimeout = if (serverHeartbeat > 0) maxOf(10000L, serverHeartbeat) * 3 + 5000 else 0
                                destinations.forEachIndexed { index, destination ->
                                    subscribeToDestination("$subscriptionId-$index", destination)
                                }
                                subscribed = true
                                _connected.value = true
                                continue
                            }
                            if (!subscribed || !text.startsWith("MESSAGE")) continue

                            val subscription = extractStompHeader(text, "subscription")
                            val body = extractStompBody(text)
                            if (body.isNotBlank()) onBody(subscription, body)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            // ignorar frames no parseables
                        }
                    }
                }
            } finally {
                heartbeat.cancel()
                if (session === this) {
                    session = null
                    _connected.value = false
                }
            }
        }
    }

    private suspend fun connectStomp(token: String) {
        session?.send(
            Frame.Text(
                "CONNECT\n" +
                    "accept-version:1.2\n" +
                    "heart-beat:10000,10000\n" +
                    "Authorization:Bearer $token\n" +
                    "\n" +
                    "\u0000"
            )
        )
    }

    private suspend fun subscribeToDestination(id: String, destination: String) {
        session?.send(
            Frame.Text(
                "SUBSCRIBE\n" +
                    "id:$id\n" +
                    "destination:$destination\n" +
                    "\n" +
                    "\u0000"
            )
        )
    }

    suspend fun sendMessage(content: String, channelId: String) {
        session?.send(
            Frame.Text(
                "SEND\n" +
                    "destination:/app/chat.sendMessage\n" +
                    "content-type:application/json\n" +
                    "\n" +
                    """{"content":"$content","channelId":"$channelId"}""" +
                    "\u0000"
            )
        )
    }

    suspend fun sendTyping(channelId: String? = null, dmId: String? = null, typing: Boolean) {
        val body = buildString {
            append("""{"typing":""")
            append(typing)
            if (channelId != null) append(",\"channelId\":\"").append(channelId).append("\"")
            if (dmId != null) append(",\"dmId\":\"").append(dmId).append("\"")
            append("}")
        }
        session?.send(
            Frame.Text(
                "SEND\n" +
                    "destination:/app/chat.typing\n" +
                    "content-type:application/json\n" +
                    "\n" +
                    body +
                    "\u0000"
            )
        )
    }

    private fun extractStompHeader(frame: String, header: String): String {
        return frame
            .lineSequence()
            .firstOrNull { it.startsWith("$header:") }
            ?.substringAfter(":")
            .orEmpty()
    }

    private fun extractStompBody(frame: String): String {
        return frame
            .substringAfter("\n\n", frame)
            .trimEnd('\u0000')
            .trim()
    }

    fun disconnect() {
        _connected.value = false
        val currentSession = session
        session = null
        if (currentSession != null) {
            closeScope.launch {
                currentSession.close(CloseReason(CloseReason.Codes.NORMAL, "client disconnect"))
            }
        }
    }
}
