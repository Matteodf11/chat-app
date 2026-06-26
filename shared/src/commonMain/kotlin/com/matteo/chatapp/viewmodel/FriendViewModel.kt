package com.matteo.chatapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matteo.chatapp.model.DmConversation
import com.matteo.chatapp.model.FriendRequest
import com.matteo.chatapp.network.FriendApi
import com.matteo.chatapp.network.UserApi
import com.matteo.chatapp.network.createHttpClient
import com.matteo.chatapp.repository.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FriendViewModel : ViewModel() {

    private val client = createHttpClient()
    private val friendApi = FriendApi(client)
    private val userApi = UserApi(client)

    private val _friends = MutableStateFlow<List<FriendRequest>>(emptyList())
    val friends: StateFlow<List<FriendRequest>> = _friends

    private val _pendingRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val pendingRequests: StateFlow<List<FriendRequest>> = _pendingRequests

    private val _dms = MutableStateFlow<List<DmConversation>>(emptyList())
    val dms: StateFlow<List<DmConversation>> = _dms

    private val _selectedDm = MutableStateFlow<DmConversation?>(null)
    val selectedDm: StateFlow<DmConversation?> = _selectedDm

    private val _searchResult = MutableStateFlow<com.matteo.chatapp.model.UserProfile?>(null)
    val searchResult: StateFlow<com.matteo.chatapp.model.UserProfile?> = _searchResult

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            val token = SessionManager.getToken() ?: return@launch
            try { _friends.value = friendApi.getFriends(token) } catch (e: Exception) {}
            try { _pendingRequests.value = friendApi.getPendingRequests(token) } catch (e: Exception) {}
            try { _dms.value = friendApi.getMyDms(token) } catch (e: Exception) {}
        }
    }

    fun searchUser(username: String) {
        viewModelScope.launch {
            val token = SessionManager.getToken() ?: return@launch
            try {
                _searchResult.value = userApi.getUserByUsername(username, token)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Usuario no encontrado"
                _searchResult.value = null
            }
        }
    }

    fun sendFriendRequest(userId: String) {
        viewModelScope.launch {
            val token = SessionManager.getToken() ?: return@launch
            try {
                friendApi.sendFriendRequest(userId, token)
                _error.value = null
                loadAll()
            } catch (e: Exception) {
                _error.value = "Error al enviar solicitud"
            }
        }
    }

    fun acceptRequest(friendshipId: String) {
        viewModelScope.launch {
            val token = SessionManager.getToken() ?: return@launch
            try {
                friendApi.acceptRequest(friendshipId, token)
                loadAll()
            } catch (e: Exception) {}
        }
    }

    fun rejectRequest(friendshipId: String) {
        viewModelScope.launch {
            val token = SessionManager.getToken() ?: return@launch
            try {
                friendApi.rejectRequest(friendshipId, token)
                loadAll()
            } catch (e: Exception) {}
        }
    }

    fun openDm(otherUserId: String) {
        viewModelScope.launch {
            val token = SessionManager.getToken() ?: return@launch
            try {
                val dm = friendApi.getOrCreateDm(otherUserId, token)
                _selectedDm.value = dm
                loadAll()
            } catch (e: Exception) {}
        }
    }

    fun selectDm(dm: DmConversation) {
        _selectedDm.value = dm
    }

    fun clearSelectedDm() {
        _selectedDm.value = null
    }
}