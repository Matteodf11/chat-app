package com.matteo.chatapp.network

import com.matteo.chatapp.model.AuthResponse
import com.matteo.chatapp.model.LoginRequest
import com.matteo.chatapp.model.RegisterRequest
import com.matteo.chatapp.model.RefreshTokenRequest
import com.matteo.chatapp.model.ChangePasswordRequest
import io.ktor.client.request.put
import io.ktor.client.request.bearerAuth
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AuthApi(private val client: HttpClient) {

    suspend fun register(request: RegisterRequest): AuthResponse {
        return client.post("$BASE_URL/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.bodyOrApiError()
    }

    suspend fun login(request: LoginRequest): AuthResponse {
        return client.post("$BASE_URL/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.bodyOrApiError()
    }

    suspend fun refresh(refreshToken: String): AuthResponse {
        return client.post("$BASE_URL/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshTokenRequest(refreshToken))
        }.bodyOrApiError()
    }

    suspend fun logout(refreshToken: String) {
        client.post("$BASE_URL/auth/logout") {
            contentType(ContentType.Application.Json)
            setBody(RefreshTokenRequest(refreshToken))
        }.requireApiSuccess()
    }

    suspend fun changePassword(currentPassword: String, newPassword: String, token: String) {
        client.put("$BASE_URL/auth/password") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(ChangePasswordRequest(currentPassword, newPassword))
        }.requireApiSuccess()
    }
}
