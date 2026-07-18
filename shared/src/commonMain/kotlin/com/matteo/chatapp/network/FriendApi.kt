package com.matteo.chatapp.network

import com.matteo.chatapp.model.DmConversation
import com.matteo.chatapp.model.FriendRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put

class FriendApi(private val client: HttpClient) {

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

    suspend fun markDmRead(dmId: String, token: String) {
        client.put("$BASE_URL/api/friends/dm/$dmId/read") {
            bearerAuth(token)
        }
    }
}
