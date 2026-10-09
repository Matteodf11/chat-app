package com.matteo.chatapp.network

import com.matteo.chatapp.model.DmConversation
import com.matteo.chatapp.model.FriendRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody

class FriendApi(private val client: HttpClient) {
    suspend fun getBlockedUsers(token: String): List<com.matteo.chatapp.model.BlockedUser> = try {
        client.get("$BASE_URL/api/friends/blocked") { bearerAuth(token) }.bodyOrApiError()
    } catch (e: ApiException) {
        // Keep existing contacts available during a staggered client/server rollout.
        if (e.statusCode == 404) emptyList() else throw e
    }
    suspend fun blockUser(id: String, token: String) {
        client.put("$BASE_URL/api/friends/block/$id") { bearerAuth(token) }.requireApiSuccess()
    }
    suspend fun unblockUser(id: String, token: String) {
        client.delete("$BASE_URL/api/friends/block/$id") { bearerAuth(token) }.requireApiSuccess()
    }
    suspend fun removeFriend(id: String, token: String) {
        client.delete("$BASE_URL/api/friends/user/$id") { bearerAuth(token) }.requireApiSuccess()
    }


    suspend fun getFriends(token: String): List<FriendRequest> {
        return client.get("$BASE_URL/api/friends") {
            bearerAuth(token)
        }.body()
    }

    suspend fun getPendingRequests(token: String): List<FriendRequest> {
        return client.get("$BASE_URL/api/friends/pending") {
            bearerAuth(token)
        }.body()
    }

    suspend fun sendFriendRequest(receiverId: String, token: String): FriendRequest {
        return client.post("$BASE_URL/api/friends/request/$receiverId") {
            bearerAuth(token)
        }.body()
    }

    suspend fun acceptRequest(friendshipId: String, token: String): FriendRequest {
        return client.put("$BASE_URL/api/friends/accept/$friendshipId") {
            bearerAuth(token)
        }.body()
    }

    suspend fun rejectRequest(friendshipId: String, token: String): FriendRequest {
        return client.put("$BASE_URL/api/friends/reject/$friendshipId") {
            bearerAuth(token)
        }.body()
    }

    suspend fun getOrCreateDm(otherUserId: String, token: String): DmConversation {
        return client.post("$BASE_URL/api/friends/dm/$otherUserId") {
            bearerAuth(token)
        }.body()
    }

    suspend fun getMyDms(token: String): List<DmConversation> {
        return client.get("$BASE_URL/api/friends/dm") {
            bearerAuth(token)
        }.body()
    }

    suspend fun updatePreferences(dmId: String, value: com.matteo.chatapp.model.DmPreferencesRequest, token: String): DmConversation =
        client.put("$BASE_URL/api/friends/dm/$dmId/preferences") {
            bearerAuth(token)
            setBody(value)
            headers.append("Content-Type", "application/json")
        }.body()

    suspend fun markDmRead(dmId: String, token: String, upToMessageId: String? = null) {
        client.put("$BASE_URL/api/friends/dm/$dmId/read") {
            bearerAuth(token)
            upToMessageId?.let { url.parameters.append("upToMessageId", it) }
        }
    }
}
