package com.matteo.chatapp.viewmodel

import com.matteo.chatapp.model.Message
import com.matteo.chatapp.model.MessagePage
import com.matteo.chatapp.model.ProfileEvent
import com.matteo.chatapp.model.TypingEvent
import com.matteo.chatapp.platform.PickedAttachmentFile
import com.matteo.chatapp.platform.MessageNotificationSink
import com.matteo.chatapp.platform.DirectMessageNotification
import com.matteo.chatapp.repository.AppVisibility
import com.matteo.chatapp.repository.AccessTokenProvider
import com.matteo.chatapp.repository.ChatRepository
import com.matteo.chatapp.repository.ConversationEventsSource
import com.matteo.chatapp.repository.ConversationTarget
import com.matteo.chatapp.repository.DmInboxEventsSource
import com.matteo.chatapp.repository.MessageSearchQuery
import com.matteo.chatapp.repository.PersonalMessagesSource
import com.matteo.chatapp.repository.ProfileEventsSource
import com.matteo.chatapp.repository.SessionIdentityProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    @Test
    fun lateDiskRestorePreservesMoreRecentActivityThanServerSnapshot() = runViewModelTest {
        val restored = CompletableDeferred<Unit>()
        val store = object : com.matteo.chatapp.repository.ChatLocalStore {
            override suspend fun pending(): List<PendingMessage> { restored.await(); return emptyList() }
            override suspend fun summaries() = mapOf("dm-1" to DmSummary("Reciente", "2026-10-09T12:00:00", 0))
        }
        val vm = createViewModel(ChatTestRepository(), localStore = store)
        try {
            vm.restoreDmSummaries(listOf(com.matteo.chatapp.model.DmConversation("dm-1", "friend", "Friend",
                lastMessagePreview = "Antiguo", lastMessageAt = "2026-10-09T10:00:00")))
            restored.complete(Unit)
            advanceUntilIdle()
            assertEquals("2026-10-09T12:00:00", vm.dmSummaries.value["dm-1"]?.at)
        } finally { vm.disconnectRealtime() }
    }

    @Test
    fun restoredPendingWaitsForManualRetryAndKeepsIdentifier() = runViewModelTest {
        val recovered = PendingMessage("00000000-0000-0000-0000-000000000001",
            ConversationTarget.DirectMessage("dm-1"), "Pendiente", null, status = SendStatus.FAILED)
        val items = mutableMapOf(recovered.id to recovered)
        val store = object : com.matteo.chatapp.repository.ChatLocalStore {
            override suspend fun pending() = items.values.toList()
            override suspend fun put(message: PendingMessage) { items[message.id] = message }
            override suspend fun remove(id: String) { items.remove(id) }
        }
        val repository = ChatTestRepository()
        var sentId: String? = null
        repository.sendHandler = { target, text, id -> sentId = id; chatMessage("sent", content = text, dmId = target.id).copy(clientMessageId = id) }
        val vm = createViewModel(repository, localStore = store)
        try {
            vm.loadDmMessages("dm-1")
            advanceUntilIdle()
            assertEquals(null, sentId)
            assertEquals(recovered.id, vm.conversationState.value.pendingMessages.single().id)
            vm.retryMessage(recovered.id)
            advanceUntilIdle()
            assertEquals(recovered.id, sentId)
            assertTrue(items.isEmpty())
        } finally { vm.disconnectRealtime() }
    }

    @Test
    fun failedLocalSaveKeepsComposerAndDoesNotSendAndDoubleClickIsBlocked() = runViewModelTest {
        val save = CompletableDeferred<Unit>()
        val store = object : com.matteo.chatapp.repository.ChatLocalStore {
            override suspend fun put(message: PendingMessage) { save.await(); error("disk full") }
        }
        val repository = ChatTestRepository()
        val vm = createViewModel(repository, localStore = store)
        var cleared = 0
        try {
            advanceUntilIdle()
            vm.sendDmMessage("Texto", "dm-1") { cleared++ }
            vm.sendDmMessage("Texto", "dm-1") { cleared++ }
            advanceUntilIdle()
            save.complete(Unit)
            advanceUntilIdle()
            assertEquals(0, cleared)
            assertEquals(null, repository.lastSentTarget)
            assertTrue(vm.conversationState.value.error.orEmpty().contains("guardar"))
        } finally { vm.disconnectRealtime() }
    }

    @Test
    fun receiptsApplyInRealtimeAndSurviveStaleHistoryRefresh() = runViewModelTest {
        val target = ConversationTarget.DirectMessage("dm-1")
        val own = chatMessage("own", dmId = target.id).copy(createdAt = "2026-10-09T12:00:00")
        val repository = ChatTestRepository(mutableMapOf(target to MessagePage(listOf(own), false)))
        val source = ChatTestConversationEventsSource()
        val vm = createViewModel(repository, conversationSource = source)
        try {
            vm.loadDmMessages(target.id)
            advanceUntilIdle()
            assertTrue(repository.readThrough.isEmpty())
            source.readEvents.emit(com.matteo.chatapp.model.DmReadEvent(dmId = target.id, readerId = "friend", readAt = "2026-10-09T12:00:00"))
            advanceUntilIdle()
            assertEquals("2026-10-09T12:00:00", vm.conversationState.value.messages.single().readAt)
            vm.refreshConversation()
            advanceUntilIdle()
            assertEquals("2026-10-09T12:00:00", vm.conversationState.value.messages.single().readAt)
            val wasForeground = AppVisibility.isInForeground.value
            try {
                AppVisibility.setInForeground(true)
                vm.setVisibleDm(target.id)
                advanceUntilIdle()
                assertEquals(listOf(target.id to own.id), repository.readThrough)
            } finally { AppVisibility.setInForeground(wasForeground) }
        } finally { vm.disconnectRealtime() }
    }

    @Test
    fun mutedConversationStillCountsUnreadWithoutSystemNotification() = runViewModelTest {
        val source = ChatTestPersonalMessagesSource()
        val notifications = mutableListOf<DirectMessageNotification>()
        val vm = createViewModel(ChatTestRepository(), source, MessageNotificationSink { notifications += it })
        val wasForeground = AppVisibility.isInForeground.value
        try {
            advanceUntilIdle()
            vm.restoreDmSummaries(listOf(com.matteo.chatapp.model.DmConversation("dm-1", "friend", "Friend", muted = true)))
            AppVisibility.setInForeground(false)
            source.emit(chatMessage("incoming", dmId = "dm-1", senderId = "friend", senderUsername = "Friend"))
            advanceUntilIdle()
            assertTrue(notifications.isEmpty())
            assertEquals(1, vm.dmSummaries.value["dm-1"]?.unreadCount)
        } finally { vm.disconnectRealtime(); AppVisibility.setInForeground(wasForeground) }
    }

    @Test
    fun deletionDuringRefreshIsNotResurrectedBySnapshot() = runViewModelTest {
        val target = ConversationTarget.DirectMessage("dm-1")
        val original = chatMessage("existing", dmId = target.id)
        val repository = ChatTestRepository(mutableMapOf(target to MessagePage(listOf(original), false)))
        val source = ChatTestConversationEventsSource()
        val viewModel = createViewModel(repository, conversationSource = source)
        try {
            viewModel.loadDmMessages(target.id)
            advanceUntilIdle()
            val snapshot = CompletableDeferred<MessagePage>()
            repository.pageHandler = { _, _ -> snapshot.await() }
            viewModel.refreshConversation()
            advanceUntilIdle()
            source.messages.emit(original.copy(deleted = true))
            advanceUntilIdle()
            snapshot.complete(MessagePage(listOf(original), false))
            advanceUntilIdle()
            assertTrue(viewModel.conversationState.value.messages.isEmpty())
        } finally { viewModel.disconnectRealtime() }
    }

    @Test
    fun unreadCountsExcludeDuplicatesAndResetWhenConversationOpens() = runViewModelTest {
        val source = ChatTestPersonalMessagesSource()
        val viewModel = createViewModel(ChatTestRepository(), personalMessagesSource = source)
        try {
            advanceUntilIdle()
            val dm = com.matteo.chatapp.model.DmConversation("dm-1", "friend", "Friend", hasUnread = true, unreadCount = 2)
            viewModel.restoreDmSummaries(listOf(dm))
            val incoming = chatMessage("new", senderId = "friend", senderUsername = "Friend", dmId = dm.id)
            source.emit(incoming)
            source.emit(incoming)
            advanceUntilIdle()
            assertEquals(3, viewModel.dmSummaries.value[dm.id]?.unreadCount)
            viewModel.setVisibleDm(dm.id)
            assertEquals(0, viewModel.dmSummaries.value[dm.id]?.unreadCount)
            assertTrue(viewModel.unreadDmIds.value.isEmpty())
        } finally { viewModel.disconnectRealtime() }
    }

    @Test
    fun retryKeepsIdentifierAndSurvivesNavigation() = runViewModelTest {
        val repository = ChatTestRepository()
        val identifiers = mutableListOf<String?>()
        repository.sendHandler = { target, content, id ->
            identifiers += id
            if (identifiers.size == 1) error("response lost")
            chatMessage("confirmed", content = content, dmId = target.id).copy(clientMessageId = id)
        }
        val viewModel = createViewModel(repository)
        try {
            viewModel.loadDmMessages("dm-1")
            advanceUntilIdle()
            var queued = false
            viewModel.sendDmMessage("Keep this text", "dm-1") { queued = true }
            advanceUntilIdle()
            assertTrue(queued)
            val failed = viewModel.conversationState.value.pendingMessages.single()
            assertEquals(SendStatus.FAILED, failed.status)
            viewModel.loadDmMessages("dm-2")
            advanceUntilIdle()
            assertTrue(viewModel.conversationState.value.pendingMessages.isEmpty())
            viewModel.loadDmMessages("dm-1")
            advanceUntilIdle()
            viewModel.retryMessage(failed.id)
            viewModel.retryMessage(failed.id)
            advanceUntilIdle()
            assertEquals<List<String?>>(listOf(failed.id, failed.id), identifiers)
            assertTrue(viewModel.conversationState.value.pendingMessages.isEmpty())
            assertEquals(listOf("confirmed"), viewModel.conversationState.value.messages.map { it.id })
        } finally { viewModel.disconnectRealtime() }
    }

    @Test
    fun reconnectRecoversEveryMissingPageWithoutDuplicates() = runViewModelTest {
        val repository = ChatTestRepository()
        val source = ChatTestConversationEventsSource()
        var offline = false
        fun message(id: Int) = chatMessage("m$id", dmId = "dm-1").copy(createdAt = "2026-10-09T12:${id.toString().padStart(2, '0')}:00")
        repository.pageHandler = { _, before ->
            if (!offline) MessagePage(listOf(message(0)), false)
            else if (before == null) MessagePage(listOf(message(3), message(4)), true)
            else if (before == message(3).createdAt) MessagePage(listOf(message(1), message(2)), true)
            else MessagePage(listOf(message(0)), false)
        }
        val viewModel = createViewModel(repository, conversationSource = source)
        try {
            viewModel.loadDmMessages("dm-1")
            advanceUntilIdle()
            offline = true
            source.connected.value = true
            advanceUntilIdle()
            assertEquals((0..4).map { "m$it" }, viewModel.conversationState.value.messages.map { it.id })
            assertTrue(viewModel.conversationState.value.isConnected)
            assertFalse(viewModel.conversationState.value.isSyncing)
        } finally { viewModel.disconnectRealtime() }
    }

    @Test
    fun websocketAcknowledgementWinsOverLostHttpResponseAndDoesNotLeakToOtherChat() = runViewModelTest {
        val repository = ChatTestRepository()
        val source = ChatTestConversationEventsSource()
        val response = CompletableDeferred<Message>()
        var identifier: String? = null
        repository.sendHandler = { _, _, id -> identifier = id; response.await() }
        val viewModel = createViewModel(repository, conversationSource = source)
        try {
            viewModel.loadDmMessages("dm-1")
            advanceUntilIdle()
            viewModel.sendDmMessage("Hello", "dm-1")
            advanceUntilIdle()
            val confirmed = chatMessage("confirmed", dmId = "dm-1").copy(clientMessageId = identifier)
            source.messages.emit(confirmed)
            advanceUntilIdle()
            assertTrue(viewModel.conversationState.value.pendingMessages.isEmpty())
            source.messages.emit(chatMessage("wrong-chat", dmId = "dm-2"))
            response.completeExceptionally(IllegalStateException("response lost"))
            advanceUntilIdle()
            assertEquals(listOf("confirmed"), viewModel.conversationState.value.messages.map { it.id })
            assertTrue(viewModel.conversationState.value.pendingMessages.isEmpty())
        } finally { viewModel.disconnectRealtime() }
    }

    @Test
    fun failedRefreshKeepsExistingMessagesAndOffersRetry() = runViewModelTest {
        val target = ConversationTarget.DirectMessage("dm-1")
        val repository = ChatTestRepository(mutableMapOf(target to MessagePage(listOf(chatMessage("existing", dmId = target.id)), false)))
        val viewModel = createViewModel(repository)
        try {
            viewModel.loadDmMessages(target.id)
            advanceUntilIdle()
            repository.pageHandler = { _, _ -> error("offline") }
            viewModel.refreshConversation()
            advanceUntilIdle()
            assertEquals(listOf("existing"), viewModel.conversationState.value.messages.map { it.id })
            assertTrue(viewModel.conversationState.value.loadError != null)
            assertFalse(viewModel.conversationState.value.isSyncing)
        } finally { viewModel.disconnectRealtime() }
    }

    @Test
    fun channelLoadAndSendUseSameTypedTarget() = runViewModelTest {
        val target = ConversationTarget.Channel("channel-1")
        val repository = ChatTestRepository(
            pages = mutableMapOf(
                target to MessagePage(
                    messages = listOf(chatMessage("existing", channelId = "channel-1")),
                    hasMore = true
                )
            )
        )
        val viewModel = createViewModel(repository)

        try {
            viewModel.loadMessages("channel-1")
            advanceUntilIdle()

            assertEquals(target, repository.lastLoadedTarget)
            assertEquals(listOf("existing"), viewModel.conversationState.value.messages.map(Message::id))
            assertTrue(viewModel.conversationState.value.hasOlderMessages)

            viewModel.sendMessage("Hola")
            advanceUntilIdle()

            assertEquals(target, repository.lastSentTarget)
            assertEquals(listOf("existing", "sent"), viewModel.conversationState.value.messages.map(Message::id))
        } finally {
            viewModel.disconnectRealtime()
        }
    }

    @Test
    fun audioAttachmentUsesActiveDmAndFinishesUploading() = runViewModelTest {
        val repository = ChatTestRepository()
        val viewModel = createViewModel(repository)
        val audio = PickedAttachmentFile(
            fileName = "audio.webm",
            contentType = "audio/webm",
            bytes = byteArrayOf(1, 2, 3)
        )

        try {
            viewModel.loadDmMessages("dm-1")
            advanceUntilIdle()
            viewModel.sendAttachment(audio)
            advanceUntilIdle()

            assertEquals(ConversationTarget.DirectMessage("dm-1"), repository.lastAttachmentTarget)
            assertEquals("audio/webm", repository.lastAttachment?.contentType)
            assertFalse(viewModel.conversationState.value.isUploadingAttachment)
            assertEquals("attachment", viewModel.conversationState.value.messages.last().id)
        } finally {
            viewModel.disconnectRealtime()
        }
    }

    @Test
    fun personalMessageFromAnotherUserMarksClosedDmUnread() = runViewModelTest {
        val repository = ChatTestRepository()
        val personalMessages = ChatTestPersonalMessagesSource()
        val viewModel = createViewModel(
            repository = repository,
            personalMessagesSource = personalMessages
        )

        try {
            advanceUntilIdle()
            personalMessages.emit(
                chatMessage(
                    id = "incoming",
                    dmId = "dm-2",
                    senderId = "friend-2",
                    senderUsername = "Amiga"
                )
            )
            advanceUntilIdle()

            assertEquals(setOf("dm-2"), viewModel.unreadDmIds.value)
        } finally {
            viewModel.disconnectRealtime()
        }
    }

    @Test
    fun backgroundNotificationsExcludeDuplicatesOwnMessagesAndForegroundMessages() = runViewModelTest {
        val incoming = ChatTestPersonalMessagesSource()
        val notifications = mutableListOf<DirectMessageNotification>()
        val viewModel = createViewModel(ChatTestRepository(), incoming, MessageNotificationSink { notifications += it })
        val wasForeground = AppVisibility.isInForeground.value
        try {
            advanceUntilIdle()
            AppVisibility.setInForeground(false)
            val message = chatMessage("notify", content = "Hola", senderId = "friend", senderUsername = "Amiga", dmId = "dm-2")
            incoming.emit(message)
            incoming.emit(message)
            incoming.emit(chatMessage("own", dmId = "dm-2"))
            incoming.emit(message.copy(id = "edited", edited = true))
            advanceUntilIdle()
            assertEquals(listOf(DirectMessageNotification("notify", "dm-2", "Amiga", "Hola")), notifications)
            AppVisibility.setInForeground(true)
            incoming.emit(message.copy(id = "visible"))
            advanceUntilIdle()
            AppVisibility.setInForeground(false)
            incoming.emit(message.copy(id = "visible"))
            advanceUntilIdle()
            assertEquals(1, notifications.size)
        } finally {
            viewModel.disconnectRealtime()
            AppVisibility.setInForeground(wasForeground)
        }
    }

    private fun createViewModel(
        repository: ChatTestRepository,
        personalMessagesSource: ChatTestPersonalMessagesSource =
            ChatTestPersonalMessagesSource(),
        notifications: MessageNotificationSink = MessageNotificationSink { },
        conversationSource: ChatTestConversationEventsSource = ChatTestConversationEventsSource(),
        localStore: com.matteo.chatapp.repository.ChatLocalStore = object : com.matteo.chatapp.repository.ChatLocalStore {}
    ) = ChatViewModel(
        chatRepository = repository,
        accessTokenProvider = ChatTestAccessTokenProvider,
        sessionIdentityProvider = ChatTestIdentityProvider,
        conversationEventsSource = conversationSource,
        profileEventsSource = ChatTestProfileEventsSource(),
        personalMessagesSource = personalMessagesSource,
        inboxEventsSource = ChatTestInboxEventsSource(),
        notifications = notifications,
        localStore = localStore
    )

    private fun runViewModelTest(
        block: suspend TestScope.() -> Unit
    ) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }
}

private object ChatTestAccessTokenProvider : AccessTokenProvider {
    override fun currentToken(): String? = null
}

private object ChatTestIdentityProvider : SessionIdentityProvider {
    override fun currentUserId(): String = "me"

    override fun currentUsername(): String = "Matteo"
}

private class ChatTestRepository(
    private val pages: MutableMap<ConversationTarget, MessagePage> = mutableMapOf()
) : ChatRepository {
    val readThrough = mutableListOf<Pair<String, String>>()
    override suspend fun markDmReadThrough(dmId: String, messageId: String) { readThrough += dmId to messageId }
    var sendHandler: (suspend (ConversationTarget, String, String?) -> Message)? = null
    var pageHandler: (suspend (ConversationTarget, String?) -> MessagePage)? = null
    var lastLoadedTarget: ConversationTarget? = null
    var lastSentTarget: ConversationTarget? = null
    var lastAttachmentTarget: ConversationTarget? = null
    var lastAttachment: PickedAttachmentFile? = null

    override suspend fun loadMessages(
        target: ConversationTarget,
        before: String?
    ): MessagePage {
        lastLoadedTarget = target
        pageHandler?.let { return it(target, before) }
        return pages[target] ?: MessagePage(emptyList(), false)
    }

    override suspend fun sendMessage(
        target: ConversationTarget,
        content: String,
        replyToId: String?,
        clientMessageId: String?
    ): Message {
        lastSentTarget = target
        sendHandler?.let { return it(target, content, clientMessageId) }
        return when (target) {
            is ConversationTarget.Channel ->
                chatMessage("sent", channelId = target.id, content = content)
            is ConversationTarget.DirectMessage ->
                chatMessage("sent", dmId = target.id, content = content)
        }
    }

    override suspend fun sendAttachment(
        target: ConversationTarget,
        file: PickedAttachmentFile,
        content: String,
        replyToId: String?,
        clientMessageId: String?
    ): Message {
        lastAttachmentTarget = target
        lastAttachment = file
        return chatMessage(
            id = "attachment",
            dmId = (target as? ConversationTarget.DirectMessage)?.id,
            channelId = (target as? ConversationTarget.Channel)?.id,
            content = content,
            attachmentContentType = file.contentType
        )
    }

    override suspend fun searchMessages(
        target: ConversationTarget,
        query: MessageSearchQuery
    ): List<Message> = emptyList()

    override suspend fun getMessageContext(messageId: String): MessagePage =
        MessagePage(emptyList(), false)

    override suspend fun editMessage(messageId: String, content: String): Message =
        chatMessage(messageId, content = content)

    override suspend fun deleteMessage(messageId: String) = Unit

    override suspend fun markDmRead(dmId: String) = Unit
}

private class ChatTestConversationEventsSource : ConversationEventsSource {
    override val readEvents = MutableSharedFlow<com.matteo.chatapp.model.DmReadEvent>()
    override val connected = MutableStateFlow(false)
    override val messages = MutableSharedFlow<Message>()
    override val typingEvents: Flow<TypingEvent> = MutableSharedFlow()

    override suspend fun connect(token: String, target: ConversationTarget) = Unit

    override suspend fun sendTyping(
        channelId: String?,
        dmId: String?,
        typing: Boolean
    ) = Unit

    override fun disconnect() = Unit
}

private class ChatTestProfileEventsSource : ProfileEventsSource {
    override val events: Flow<ProfileEvent> = MutableSharedFlow()

    override suspend fun connect(token: String) = Unit

    override fun disconnect() = Unit
}

private class ChatTestPersonalMessagesSource : PersonalMessagesSource {
    private val mutableMessages = MutableSharedFlow<Message>()
    override val messages: Flow<Message> = mutableMessages

    suspend fun emit(message: Message) {
        mutableMessages.emit(message)
    }

    override suspend fun connect(token: String) = Unit

    override fun disconnect() = Unit
}

private class ChatTestInboxEventsSource : DmInboxEventsSource {
    override val messages: Flow<Message> = MutableSharedFlow()

    override suspend fun connect(token: String, dmIds: List<String>) = Unit

    override fun disconnect() = Unit
}

private fun chatMessage(
    id: String,
    content: String = "",
    senderId: String? = "me",
    senderUsername: String = "Matteo",
    channelId: String? = null,
    dmId: String? = null,
    attachmentContentType: String? = null
) = Message(
    id = id,
    content = content,
    senderId = senderId,
    senderUsername = senderUsername,
    channelId = channelId,
    dmId = dmId,
    attachmentContentType = attachmentContentType,
    createdAt = "2026-07-23T12:00:00Z"
)
