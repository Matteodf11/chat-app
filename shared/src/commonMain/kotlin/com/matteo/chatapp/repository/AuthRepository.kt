package com.matteo.chatapp.repository

import com.matteo.chatapp.model.LoginRequest
import com.matteo.chatapp.model.RegisterRequest
import com.matteo.chatapp.network.AuthApi

interface AuthRepository {
    suspend fun login(email: String, password: String, rememberMe: Boolean)

    suspend fun register(username: String, email: String, password: String)

    suspend fun logout(refreshToken: String)

    suspend fun changePassword(currentPassword: String, newPassword: String)
}

class NetworkAuthRepository(
    private val authApi: AuthApi,
    private val accessTokenProvider: AccessTokenProvider
) : AuthRepository {
    override suspend fun login(email: String, password: String, rememberMe: Boolean) {
        val response = authApi.login(LoginRequest(email, password, rememberMe))
        SessionManager.saveAuthSession(
            token = response.token,
            refreshToken = response.refreshToken,
            username = response.username,
            email = response.email,
            remember = rememberMe
        )
    }

    override suspend fun register(username: String, email: String, password: String) {
        val response = authApi.register(RegisterRequest(username, email, password))
        SessionManager.saveAuthSession(
            token = response.token,
            refreshToken = response.refreshToken,
            username = response.username,
            email = response.email,
            remember = false
        )
    }

    override suspend fun logout(refreshToken: String) {
        authApi.logout(refreshToken)
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String) {
        authApi.changePassword(
            currentPassword,
            newPassword,
            accessTokenProvider.requireToken()
        )
    }
}
