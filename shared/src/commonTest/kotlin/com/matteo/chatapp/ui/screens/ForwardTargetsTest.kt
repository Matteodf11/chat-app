package com.matteo.chatapp.ui.screens

import com.matteo.chatapp.model.*
import com.matteo.chatapp.repository.ConversationTarget
import kotlin.test.*

class ForwardTargetsTest {
    @Test fun excludesBlockedSourceAndVoiceChannelsAndIncludesArchivedChats() {
        val source = Message("source", "Texto", senderUsername = "A", dmId = "current", channelId = "channel-current")
        val dms = listOf(DmConversation("current", "a", "A"),
            DmConversation("blocked", "b", "B", canMessage = false),
            DmConversation("archived", "c", "C", archived = true))
        val channel = Channel("channel-new", "general", "text", "server")
        val targets = forwardingTargets(source, dms, listOf(channel, channel,
            channel.copy(id = "channel-current"), channel.copy(id = "voice", type = "voice")), mapOf("server" to "Grupo"))
        assertEquals(listOf(ConversationTarget.DirectMessage("archived"), ConversationTarget.Channel("channel-new")),
            targets.map { it.target })
        assertEquals("Grupo", targets.last().detail)
    }
}
