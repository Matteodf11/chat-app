package com.matteo.chatapp.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val username: String,
    val email: String,
    val avatarUrl: String? = null,
    val status: String? = null,
    val isOnline: Boolean = false
)