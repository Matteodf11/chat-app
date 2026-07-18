package com.matteo.chatapp.network

import com.matteo.chatapp.model.UserProfile
import com.matteo.chatapp.model.UpdateProfileRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess

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

    suspend fun getUserById(userId: String, token: String): UserProfile {
        return client.get("$BASE_URL/api/users/$userId") {
            bearerAuth(token)
        }.body()
    }

    suspend fun markOffline(token: String) {
        client.post("$BASE_URL/api/users/me/offline") {
            bearerAuth(token)
        }
    }

    suspend fun markOnline(token: String) {
        client.post("$BASE_URL/api/users/me/online") {
            bearerAuth(token)
        }
    }

    suspend fun updateMyProfile(request: UpdateProfileRequest, token: String): UserProfile {
        return client.put("$BASE_URL/api/users/me") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun uploadAvatar(fileName: String, contentType: String, bytes: ByteArray, token: String): UserProfile {
        return uploadProfileImage("avatar", fileName, contentType, bytes, token)
    }

    suspend fun uploadBanner(fileName: String, contentType: String, bytes: ByteArray, token: String): UserProfile {
        return uploadProfileImage("banner", fileName, contentType, bytes, token)
    }

    private suspend fun uploadProfileImage(kind: String, fileName: String, contentType: String, bytes: ByteArray, token: String): UserProfile {
        if (bytes.size > MAX_IMAGE_BYTES) {
            throw RuntimeException("La imagen no puede superar 2 MB")
        }

        val safeFileName = fileName
            .replace("\\", "_")
            .replace("/", "_")
            .replace("\"", "_")

        val response = client.post("$BASE_URL/api/users/me/$kind") {
            bearerAuth(token)
            contentType(ContentType.parse(contentType))
            header(HttpHeaders.ContentDisposition, "attachment; filename=\"$safeFileName\"")
            setBody(bytes)
        }

        if (!response.status.isSuccess()) {
            throw RuntimeException(response.bodyAsText())
        }

        return response.body()
    }

    private companion object {
        private const val MAX_IMAGE_BYTES = 2 * 1024 * 1024
    }
}
