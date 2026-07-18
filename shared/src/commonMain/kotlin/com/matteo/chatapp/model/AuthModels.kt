package com.matteo.chatapp.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val rememberMe: Boolean = false,
    val deviceName: String = "Chimera app"
)

@Serializable
data class AuthResponse(
    val token: String,
    val refreshToken: String,
    val username: String,
    val email: String
)

@Serializable
data class RefreshTokenRequest(val refreshToken: String)

@Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)
