package com.matteo.chatapp.viewmodel

import com.matteo.chatapp.model.Message
import com.matteo.chatapp.model.MessagePage
import com.matteo.chatapp.model.ProfileEvent
import com.matteo.chatapp.model.TypingEvent
import com.matteo.chatapp.platform.PickedAttachmentFile
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

    private fun createViewModel(
        repository: ChatTestRepository,
        personalMessagesSource: ChatTestPersonalMessagesSource =
            ChatTestPersonalMessagesSource()
    ) = ChatViewModel(
        chatRepository = repository,
        accessTokenProvider = ChatTestAccessTokenProvider,
        sessionIdentityProvider = ChatTestIdentityProvider,
        conversationEventsSource = ChatTestConversationEventsSource(),
        profileEventsSource = ChatTestProfileEventsSource(),
        personalMessagesSource = personalMessagesSource,
        inboxEventsSource = ChatTestInboxEventsSource()
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
    var lastLoadedTarget: ConversationTarget? = null
    var lastSentTarget: ConversationTarget? = null
    var lastAttachmentTarget: ConversationTarget? = null
    var lastAttachment: PickedAttachmentFile? = null

    override suspend fun loadMessages(
        target: ConversationTarget,
        before: String?
    ): MessagePage {
        lastLoadedTarget = target
        return pages[target] ?: MessagePage(emptyList(), false)
    }

    override suspend fun sendMessage(
        target: ConversationTarget,
        content: String,
        replyToId: String?
    ): Message {
        lastSentTarget = target
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
        replyToId: String?
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
    override val messages: Flow<Message> = MutableSharedFlow()
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
