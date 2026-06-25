package com.matteo.chatapp.model

import kotlinx.serialization.Serializable

@Serializable
data class Server(
    val id: String,
    val name: String,
    val description: String? = null,
    val iconUrl: String? = null,
    val ownerUsername: String,
    val createdAt: String? = null
)

@Serializable
data class Channel(
    val id: String,
    val name: String,
    val type: String,
    val serverId: String,
    val createdAt: String? = null
)

@Serializable
data class CreateServerRequest(
    val name: String,
    val description: String? = null
)

@Serializable
data class CreateChannelRequest(
    val name: String,
    val type: String = "text"
)