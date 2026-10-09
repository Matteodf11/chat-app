package com.matteo.chatapp.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val username: String,
    val email: String,
    val avatarUrl: String? = null,
    val profileBannerUrl: String? = null,
    val status: String? = null,
    val profileColor: String? = null,
    val isOnline: Boolean = false,
    val bio: String? = null,
    val createdAt: String? = null
)

@Serializable
data class UpdateProfileRequest(
    val username: String? = null,
    val avatarUrl: String? = null,
    val profileBannerUrl: String? = null,
    val status: String? = null,
    val profileColor: String? = null,
    val bio: String? = null
)

@Serializable
data class ProfileEvent(
    val userId: String,
    val username: String,
    val avatarUrl: String? = null,
    val profileBannerUrl: String? = null,
    val status: String? = null,
    val profileColor: String? = null,
    val bio: String? = null
)
