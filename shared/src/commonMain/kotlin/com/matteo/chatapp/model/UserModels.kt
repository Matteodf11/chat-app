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
    val isOnline: Boolean = false
)

@Serializable
data class UpdateProfileRequest(
    val username: String? = null,
    val avatarUrl: String? = null,
    val profileBannerUrl: String? = null,
    val status: String? = null,
    val profileColor: String? = null
)

@Serializable
data class ProfileEvent(
    val userId: String,
    val username: String,
    val avatarUrl: String? = null,
    val profileBannerUrl: String? = null,
    val status: String? = null,
    val profileColor: String? = null
)
