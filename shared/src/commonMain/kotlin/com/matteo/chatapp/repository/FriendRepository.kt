package com.matteo.chatapp.repository

import com.matteo.chatapp.model.DmConversation
import com.matteo.chatapp.model.FriendRequest
import com.matteo.chatapp.network.FriendApi

data class FriendSnapshot(
    val friends: List<FriendRequest>,
    val pendingRequests: List<FriendRequest>,
    val dms: List<DmConversation>
)

interface FriendRepository {
    suspend fun loadSnapshot(): FriendSnapshot

    suspend fun sendFriendRequest(userId: String): FriendRequest

    suspend fun acceptRequest(friendshipId: String): FriendRequest

    suspend fun rejectRequest(friendshipId: String)

    suspend fun openDm(otherUserId: String): DmConversation

    suspend fun markDmRead(dmId: String)
}

class NetworkFriendRepository(
    private val friendApi: FriendApi,
    private val accessTokenProvider: AccessTokenProvider
) : FriendRepository {
    override suspend fun loadSnapshot(): FriendSnapshot {
        val token = accessTokenProvider.requireToken()
        return FriendSnapshot(
            friends = friendApi.getFriends(token),
            pendingRequests = friendApi.getPendingRequests(token),
            dms = friendApi.getMyDms(token)
        )
    }

    override suspend fun sendFriendRequest(userId: String): FriendRequest =
        friendApi.sendFriendRequest(userId, accessTokenProvider.requireToken())

    override suspend fun acceptRequest(friendshipId: String): FriendRequest =
        friendApi.acceptRequest(friendshipId, accessTokenProvider.requireToken())

    override suspend fun rejectRequest(friendshipId: String) {
        friendApi.rejectRequest(friendshipId, accessTokenProvider.requireToken())
    }

    override suspend fun openDm(otherUserId: String): DmConversation =
        friendApi.getOrCreateDm(otherUserId, accessTokenProvider.requireToken())

    override suspend fun markDmRead(dmId: String) {
        friendApi.markDmRead(dmId, accessTokenProvider.requireToken())
    }
}
