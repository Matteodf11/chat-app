package com.matteo.chatapp.model

import kotlinx.serialization.Serializable

@Serializable
data class Server(
    val id: String,
    val name: String,
    val description: String? = null,
    val iconUrl: String? = null,
    val ownerUsername: String,
    val myRole: String? = null,
    val createdAt: String? = null,
    val unreadCount: Long = 0,
    val mentionCount: Long = 0
)

@Serializable
data class Channel(
    val id: String,
    val name: String,
    val type: String,
    val serverId: String,
    val createdAt: String? = null,
    val unreadCount: Long = 0,
    val mentionCount: Long = 0
)

@Serializable
data class CreateServerRequest(
    val name: String,
    val description: String? = null,
    val iconUrl: String? = null
)

@Serializable
data class UpdateServerRequest(
    val name: String? = null,
    val description: String? = null,
    val iconUrl: String? = null
)

@Serializable
data class CreateChannelRequest(
    val name: String,
    val type: String = "text"
)

@Serializable
data class ServerMember(
    val userId: String,
    val username: String,
    val avatarUrl: String? = null,
    val role: String,
    val isOnline: Boolean = false,
    val joinedAt: String? = null
)

@Serializable
data class InviteServerMemberRequest(
    val username: String
)

@Serializable
data class UpdateServerMemberRoleRequest(
    val role: String
)

@Serializable
data class ServerInvitation(
    val id: String,
    val serverId: String,
    val serverName: String,
    val serverIconUrl: String? = null,
    val inviterUsername: String,
    val status: String,
    val createdAt: String? = null
)

@Serializable
data class ServerEvent(
    val type: String,
    val serverId: String,
    val role: String? = null
)

@Serializable
data class ChannelActivity(val channelId: String, val serverId: String, val unreadCount: Long, val mentionCount: Long)

@Serializable
data class ChannelReadRequest(val messageId: String)
