package com.matteo.chatapp.network

import com.matteo.chatapp.model.UserProfile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get

class UserApi(private val client: HttpClient) {

    suspend fun getMyProfile(token: String): UserProfile {
        return client.get("$BASE_URL/api/users/me") {
            bearerAuth(token)
        }.body()
    }

    suspend fun getUserByUsername(username: String, token: String): UserProfile {
        return client.get("$BASE_URL/api/users/username/$username") {
            bearerAuth(token)
        }.body()
    }
}