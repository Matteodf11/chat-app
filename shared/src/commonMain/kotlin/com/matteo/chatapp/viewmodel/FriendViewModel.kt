package com.matteo.chatapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matteo.chatapp.model.DmConversation
import com.matteo.chatapp.model.FriendRequest
import com.matteo.chatapp.model.ProfileEvent
import com.matteo.chatapp.model.UpdateProfileRequest
import com.matteo.chatapp.model.UserProfile
import com.matteo.chatapp.network.FriendApi
import com.matteo.chatapp.network.AuthApi
import com.matteo.chatapp.network.UserApi
import com.matteo.chatapp.network.WebSocketClient
import com.matteo.chatapp.network.createHttpClient
import com.matteo.chatapp.platform.PickedAvatarFile
import com.matteo.chatapp.repository.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FriendViewModel : ViewModel() {

    private val client = createHttpClient()
    private val friendApi = FriendApi(client)
    private val userApi = UserApi(client)
    private val authApi = AuthApi(client)
    private val friendshipWsClient = WebSocketClient(client)
    private val presenceWsClient = WebSocketClient(client)
    private val profileWsClient = WebSocketClient(client)
    private var friendEventsJob: Job? = null
    private var presenceEventsJob: Job? = null
    private var profileEventsJob: Job? = null

    private val _friends = MutableStateFlow<List<FriendRequest>>(emptyList())
    val friends: StateFlow<List<FriendRequest>> = _friends

    private val _pendingRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val pendingRequests: StateFlow<List<FriendRequest>> = _pendingRequests

    private val _dms = MutableStateFlow<List<DmConversation>>(emptyList())
    val dms: StateFlow<List<DmConversation>> = _dms

    private val _selectedDm = MutableStateFlow<DmConversation?>(null)
    val selectedDm: StateFlow<DmConversation?> = _selectedDm

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile

    private val _searchResult = MutableStateFlow<UserProfile?>(null)
    val searchResult: StateFlow<UserProfile?> = _searchResult

    private val _viewedProfile = MutableStateFlow<UserProfile?>(null)
    val viewedProfile: StateFlow<UserProfile?> = _viewedProfile

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadProfile()
        loadAll()
        connectFriendEvents()
        connectPresenceEvents()
        connectProfileEvents()
    }

    fun loadProfile() {
        viewModelScope.launch {
            val token = SessionManager.getToken() ?: return@launch
            try {
                val profile = userApi.getMyProfile(token)
                _profile.value = profile
                SessionManager.saveUserId(profile.id)
            } catch (e: Exception) {
                _error.value = "No se pudo cargar tu perfil"
            }
        }
    }

    fun updateProfile(username: String, avatarUrl: String, status: String) {
        viewModelScope.launch {
            val token = SessionManager.getToken() ?: return@launch
            try {
                val updated = userApi.updateMyProfile(
                    UpdateProfileRequest(
                        username = username.trim().takeIf { it.isNotBlank() },
                        avatarUrl = avatarUrl.trim().takeIf { it.isNotBlank() },
                        status = status.trim().takeIf { it.isNotBlank() },
                        profileColor = null
                    ),
                    token
                )
                _profile.value = updated
                SessionManager.saveSession(token, updated.username, updated.email, updated.id)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "No se pudo actualizar el perfil"
            }
        }
    }

    fun saveProfile(username: String, status: String, avatarFile: PickedAvatarFile?, bannerFile: PickedAvatarFile?, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val token = SessionManager.getToken() ?: return@launch
            try {
                var updated = userApi.updateMyProfile(
                    UpdateProfileRequest(
                        username = username.trim().takeIf { it.isNotBlank() },
                        avatarUrl = null,
                        profileBannerUrl = null,
                        status = status.trim().takeIf { it.isNotBlank() },
                        profileColor = null
                    ),
                    token
                )
                if (avatarFile != null) {
                    updated = userApi.uploadAvatar(avatarFile.fileName, avatarFile.contentType, avatarFile.bytes, token)
                }
                if (bannerFile != null) {
                    updated = userApi.uploadBanner(bannerFile.fileName, bannerFile.contentType, bannerFile.bytes, token)
                }
                _profile.value = updated
                SessionManager.saveSession(token, updated.username, updated.email, updated.id)
                loadAll()
                _error.value = null
                onComplete()
            } catch (e: Exception) {
                _error.value = e.message?.takeIf { it.isNotBlank() } ?: "No se pudo guardar el perfil"
            }
        }
    }

    fun loadAll() {
        viewModelScope.launch {
            val token = SessionManager.getToken() ?: return@launch
            try {
                _friends.value = friendApi.getFriends(token)
                _pendingRequests.value = friendApi.getPendingRequests(token)
                _dms.value = friendApi.getMyDms(token)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "No se pudieron cargar amigos y solicitudes"
            }
        }
    }

    private fun connectFriendEvents() {
        if (friendEventsJob != null) return

        viewModelScope.launch {
            friendshipWsClient.friendshipEvents.collect { event ->
                applyFriendshipEvent(event.type, event.friendship)
            }
        }

        friendEventsJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val token = SessionManager.getToken() ?: break
                    val userId = SessionManager.getUserId() ?: userApi.getMyProfile(token).id.also {
                        SessionManager.saveUserId(it)
                    }
                    friendshipWsClient.connectFriendEvents(token, userId)
                } catch (e: Exception) {
                    delay(2500)
                }
            }
        }
    }

    private fun connectPresenceEvents() {
        if (presenceEventsJob != null) return

        viewModelScope.launch {
            presenceWsClient.presenceEvents.collect { event ->
                _friends.value = _friends.value.map {
                    if (it.friendId == event.userId) it.copy(friendIsOnline = event.online) else it
                }
                _pendingRequests.value = _pendingRequests.value.map {
                    if (it.friendId == event.userId) it.copy(friendIsOnline = event.online) else it
                }
                _dms.value = _dms.value.map {
                    if (it.otherUserId == event.userId) it.copy(otherIsOnline = event.online) else it
                }
                _selectedDm.value = _selectedDm.value?.let {
                    if (it.otherUserId == event.userId) it.copy(otherIsOnline = event.online) else it
                }
            }
        }

        presenceEventsJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val token = SessionManager.getToken() ?: break
                    presenceWsClient.connectPresenceEvents(token)
                } catch (e: Exception) {
                    delay(2500)
                }
            }
        }
    }

    private fun connectProfileEvents() {
        if (profileEventsJob != null) return

        viewModelScope.launch {
            profileWsClient.profileEvents.collect { event ->
                applyProfileEvent(event)
            }
        }

        profileEventsJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val token = SessionManager.getToken() ?: break
                    profileWsClient.connectProfileEvents(token)
                } catch (e: Exception) {
                    delay(2500)
                }
            }
        }
    }

    private fun applyProfileEvent(event: ProfileEvent) {
        _profile.value = _profile.value?.let {
            if (it.id == event.userId) {
                val updated = it.copy(
                    username = event.username,
                    avatarUrl = event.avatarUrl,
                    profileBannerUrl = event.profileBannerUrl,
                    status = event.status,
                    profileColor = event.profileColor
                )
                SessionManager.getToken()?.let { token ->
                    SessionManager.saveSession(token, updated.username, updated.email, updated.id)
                }
                updated
            } else {
                it
            }
        }

        _viewedProfile.value = _viewedProfile.value?.let {
            if (it.id == event.userId) {
                it.copy(
                    username = event.username,
                    avatarUrl = event.avatarUrl,
                    profileBannerUrl = event.profileBannerUrl,
                    status = event.status,
                    profileColor = event.profileColor
                )
            } else {
                it
            }
        }

        _friends.value = _friends.value.map {
            if (it.friendId == event.userId) {
                it.copy(friendUsername = event.username, friendAvatar = event.avatarUrl)
            } else {
                it
            }
        }
        _pendingRequests.value = _pendingRequests.value.map {
            if (it.friendId == event.userId) {
                it.copy(friendUsername = event.username, friendAvatar = event.avatarUrl)
            } else {
                it
            }
        }
        _dms.value = _dms.value.map {
            if (it.otherUserId == event.userId) {
                it.copy(otherUsername = event.username, otherAvatar = event.avatarUrl)
            } else {
                it
            }
        }
        _selectedDm.value = _selectedDm.value?.let {
            if (it.otherUserId == event.userId) {
                it.copy(otherUsername = event.username, otherAvatar = event.avatarUrl)
            } else {
                it
            }
        }
    }

    private fun applyFriendshipEvent(type: String, friendship: FriendRequest) {
        when (type) {
            "friend_request_created" -> {
                _pendingRequests.value = (_pendingRequests.value.filterNot { it.id == friendship.id } + friendship)
            }
            "friend_request_accepted" -> {
                _pendingRequests.value = _pendingRequests.value.filterNot { it.id == friendship.id }
                _friends.value = (_friends.value.filterNot { it.id == friendship.id } + friendship)
            }
            "friend_request_rejected" -> {
                _pendingRequests.value = _pendingRequests.value.filterNot { it.id == friendship.id }
            }
            else -> loadAll()
        }
    }

    fun searchUser(username: String) {
        viewModelScope.launch {
            val token = SessionManager.getToken() ?: return@launch
            val cleanUsername = username.trim()
            if (cleanUsername.isBlank()) {
                _searchResult.value = null
                _error.value = "Escribe un nombre de usuario"
                return@launch
            }
            try {
                _searchResult.value = userApi.getUserByUsername(cleanUsername, token)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Usuario no encontrado"
                _searchResult.value = null
            }
        }
    }

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            val token = SessionManager.getToken() ?: return@launch
            try {
                _viewedProfile.value = userApi.getUserById(userId, token)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "No se pudo cargar el perfil"
            }
        }
    }

    fun clearViewedProfile() {
        _viewedProfile.value = null
    }

    fun sendFriendRequest(userId: String) {
        viewModelScope.launch {
            val token = SessionManager.getToken() ?: return@launch
            try {
                val request = friendApi.sendFriendRequest(userId, token)
                _pendingRequests.value = (_pendingRequests.value.filterNot { it.id == request.id } + request)
                _searchResult.value = null
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Error al enviar solicitud"
            }
        }
    }

    fun acceptRequest(friendshipId: String) {
        viewModelScope.launch {
            val token = SessionManager.getToken() ?: return@launch
            try {
                val accepted = friendApi.acceptRequest(friendshipId, token)
                _pendingRequests.value = _pendingRequests.value.filterNot { it.id == friendshipId }
                _friends.value = (_friends.value.filterNot { it.id == accepted.id } + accepted)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "No se pudo aceptar la solicitud"
                loadAll()
            }
        }
    }

    fun rejectRequest(friendshipId: String) {
        viewModelScope.launch {
            val token = SessionManager.getToken() ?: return@launch
            try {
                friendApi.rejectRequest(friendshipId, token)
                _pendingRequests.value = _pendingRequests.value.filterNot { it.id == friendshipId }
                _error.value = null
            } catch (e: Exception) {
                _error.value = "No se pudo rechazar la solicitud"
                loadAll()
            }
        }
    }

    fun openDm(otherUserId: String, onOpened: (DmConversation) -> Unit = {}) {
        viewModelScope.launch {
            val token = SessionManager.getToken() ?: return@launch
            try {
                val dm = friendApi.getOrCreateDm(otherUserId, token).copy(hasUnread = false)
                _selectedDm.value = dm
                _dms.value = (_dms.value.filterNot { it.id == dm.id } + dm)
                runCatching { friendApi.markDmRead(dm.id, token) }
                _error.value = null
                onOpened(dm)
            } catch (e: Exception) {
                _error.value = "No se pudo abrir el mensaje directo"
            }
        }
    }

    fun selectDm(dm: DmConversation) {
        val readDm = dm.copy(hasUnread = false)
        _selectedDm.value = readDm
        _dms.value = _dms.value.map { if (it.id == dm.id) readDm else it }
        viewModelScope.launch {
            val token = SessionManager.getToken() ?: return@launch
            runCatching { friendApi.markDmRead(dm.id, token) }
        }
    }

    fun clearSelectedDm() {
        _selectedDm.value = null
    }

    fun disconnectRealtime() {
        friendEventsJob?.cancel()
        presenceEventsJob?.cancel()
        profileEventsJob?.cancel()
        friendshipWsClient.disconnect()
        presenceWsClient.disconnect()
        profileWsClient.disconnect()
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            disconnectRealtime()
            onComplete()
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken() ?: return@launch
                authApi.changePassword(currentPassword, newPassword, token)
                _error.value = null
                onComplete()
            } catch (e: Exception) {
                _error.value = "No se pudo cambiar la contrasena"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnectRealtime()
    }
}
