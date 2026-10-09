package com.matteo.chatapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matteo.chatapp.model.DmConversation
import com.matteo.chatapp.model.FriendRequest
import com.matteo.chatapp.model.ProfileEvent
import com.matteo.chatapp.network.ApiException
import com.matteo.chatapp.platform.PickedAvatarFile
import com.matteo.chatapp.repository.AccessTokenProvider
import com.matteo.chatapp.repository.AuthRepository
import com.matteo.chatapp.repository.FriendRepository
import com.matteo.chatapp.repository.FriendshipEventsSource
import com.matteo.chatapp.repository.PresenceEventsSource
import com.matteo.chatapp.repository.ProfileEventsSource
import com.matteo.chatapp.repository.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FriendViewModel(
    private val friendRepository: FriendRepository,
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val accessTokenProvider: AccessTokenProvider,
    private val friendshipEventsSource: FriendshipEventsSource,
    private val presenceEventsSource: PresenceEventsSource,
    private val profileEventsSource: ProfileEventsSource
) : ViewModel() {
    private var friendEventsJob: Job? = null
    private var presenceEventsJob: Job? = null
    private var profileEventsJob: Job? = null
    private var friendEventsCollectJob: Job? = null
    private var presenceEventsCollectJob: Job? = null
    private var profileEventsCollectJob: Job? = null
    private var profileOperationJob: Job? = null
    private var loadAllJob: Job? = null
    private var userSearchJob: Job? = null
    private var viewedProfileJob: Job? = null
    private var openDmJob: Job? = null

    private val _uiState = MutableStateFlow(FriendUiState())
    val uiState: StateFlow<FriendUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        loadAll()
        connectFriendEvents()
        connectPresenceEvents()
        connectProfileEvents()
    }

    fun loadProfile() {
        if (_uiState.value.savingProfile) return
        profileOperationJob?.cancel()
        profileOperationJob = viewModelScope.launch {
            try {
                val profile = profileRepository.loadMyProfile()
                _uiState.update { it.copy(profile = profile, error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo cargar tu perfil") }
            }
        }
    }

    fun saveProfile(username: String, status: String, bio: String, avatarFile: PickedAvatarFile?, bannerFile: PickedAvatarFile?, onComplete: () -> Unit = {}) {
        if (_uiState.value.savingProfile) return
        _uiState.update { it.copy(savingProfile = true, error = null) }
        profileOperationJob?.cancel()
        profileOperationJob = viewModelScope.launch {
            try {
                val updated = profileRepository.saveProfile(
                    username = username,
                    status = status,
                    bio = bio,
                    avatarFile = avatarFile,
                    bannerFile = bannerFile
                )
                _uiState.update { it.copy(profile = updated, error = null) }
                loadAll()
                onComplete()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message?.takeIf(String::isNotBlank) ?: "No se pudo guardar el perfil")
                }
            } finally {
                _uiState.update { it.copy(savingProfile = false) }
            }
        }
    }

    fun loadAll() {
        loadAllJob?.cancel()
        loadAllJob = viewModelScope.launch {
            try {
                val snapshot = friendRepository.loadSnapshot()
                _uiState.update {
                    it.copy(
                        friends = snapshot.friends,
                        pendingRequests = snapshot.pendingRequests,
                        dms = snapshot.dms,
                        error = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudieron cargar amigos y solicitudes") }
            }
        }
    }

    private fun connectFriendEvents() {
        if (friendEventsJob != null) return

        friendEventsCollectJob = viewModelScope.launch {
            friendshipEventsSource.events.collect { event ->
                applyFriendshipEvent(event.type, event.friendship)
            }
        }

        friendEventsJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val token = accessTokenProvider.currentToken() ?: break
                    val userId = profileRepository.getCurrentUserId()
                    friendshipEventsSource.connect(token, userId)
                } catch (e: Exception) {
                    delay(2500)
                }
            }
        }
    }

    private fun connectPresenceEvents() {
        if (presenceEventsJob != null) return

        presenceEventsCollectJob = viewModelScope.launch {
            presenceEventsSource.events.collect { event ->
                _uiState.update { it.withPresenceEvent(event) }
            }
        }

        presenceEventsJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val token = accessTokenProvider.currentToken() ?: break
                    presenceEventsSource.connect(token)
                } catch (e: Exception) {
                    delay(2500)
                }
            }
        }
    }

    private fun connectProfileEvents() {
        if (profileEventsJob != null) return

        profileEventsCollectJob = viewModelScope.launch {
            profileEventsSource.events.collect { event ->
                applyProfileEvent(event)
            }
        }

        profileEventsJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val token = accessTokenProvider.currentToken() ?: break
                    profileEventsSource.connect(token)
                } catch (e: Exception) {
                    delay(2500)
                }
            }
        }
    }

    private fun applyProfileEvent(event: ProfileEvent) {
        _uiState.update { it.withProfileEvent(event) }
        _uiState.value.profile
            ?.takeIf { it.id == event.userId }
            ?.let(profileRepository::cacheCurrentProfile)
    }

    private fun applyFriendshipEvent(type: String, friendship: FriendRequest) {
        when (type) {
            "friend_request_created" -> {
                _uiState.update {
                    it.copy(pendingRequests = it.pendingRequests.upsertFriendship(friendship))
                }
            }
            "friend_request_accepted" -> {
                _uiState.update {
                    it.copy(
                        pendingRequests = it.pendingRequests.filterNot { request ->
                            request.id == friendship.id
                        },
                        friends = it.friends.upsertFriendship(friendship)
                    )
                }
            }
            "friend_request_rejected" -> {
                _uiState.update {
                    it.copy(
                        pendingRequests = it.pendingRequests.filterNot { request ->
                            request.id == friendship.id
                        }
                    )
                }
            }
            else -> loadAll()
        }
    }

    fun searchUser(username: String) {
        userSearchJob?.cancel()
        userSearchJob = viewModelScope.launch {
            val cleanUsername = username.trim()
            if (cleanUsername.isBlank()) {
                _uiState.update {
                    it.copy(searchResult = null, error = "Escribe un nombre de usuario")
                }
                return@launch
            }
            _uiState.update { it.copy(isSearchingUser = true, searchResult = null, error = null) }
            try {
                val result = profileRepository.findUser(cleanUsername)
                _uiState.update { it.copy(searchResult = result, error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(searchResult = null, error =
                    if (e is ApiException) e.message else "No se pudo buscar el usuario. Comprueba tu conexión.") }
            } finally {
                _uiState.update { it.copy(isSearchingUser = false) }
            }
        }
    }

    fun loadUserProfile(userId: String) {
        viewedProfileJob?.cancel()
        viewedProfileJob = viewModelScope.launch {
            try {
                val profile = profileRepository.getUser(userId)
                _uiState.update { it.copy(viewedProfile = profile, error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo cargar el perfil") }
            }
        }
    }

    fun clearViewedProfile() {
        viewedProfileJob?.cancel()
        viewedProfileJob = null
        _uiState.update { it.copy(viewedProfile = null) }
    }

    fun sendFriendRequest(userId: String) {
        viewModelScope.launch {
            try {
                val request = friendRepository.sendFriendRequest(userId)
                _uiState.update {
                    it.copy(
                        pendingRequests = it.pendingRequests.upsertFriendship(request),
                        searchResult = null,
                        error = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al enviar solicitud") }
            }
        }
    }

    fun acceptRequest(friendshipId: String) {
        viewModelScope.launch {
            try {
                val accepted = friendRepository.acceptRequest(friendshipId)
                _uiState.update {
                    it.copy(
                        pendingRequests = it.pendingRequests.filterNot { request ->
                            request.id == friendshipId
                        },
                        friends = it.friends.upsertFriendship(accepted),
                        error = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo aceptar la solicitud") }
                loadAll()
            }
        }
    }

    fun rejectRequest(friendshipId: String) {
        viewModelScope.launch {
            try {
                friendRepository.rejectRequest(friendshipId)
                _uiState.update {
                    it.copy(
                        pendingRequests = it.pendingRequests.filterNot { request ->
                            request.id == friendshipId
                        },
                        error = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo rechazar la solicitud") }
                loadAll()
            }
        }
    }

    fun openDm(otherUserId: String, onOpened: (DmConversation) -> Unit = {}) {
        openDmJob?.cancel()
        openDmJob = viewModelScope.launch {
            try {
                val dm = friendRepository.openDm(otherUserId).copy(hasUnread = false)
                _uiState.update {
                    it.copy(selectedDm = dm, dms = it.dms.upsertDm(dm), error = null)
                }
                openDmJob = null
                onOpened(dm)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo abrir el mensaje directo") }
            }
        }
    }

    fun selectDm(dm: DmConversation) {
        openDmJob?.cancel()
        openDmJob = null
        val readDm = dm.copy(hasUnread = false)
        _uiState.update {
            it.copy(selectedDm = readDm, dms = it.dms.upsertDm(readDm))
        }
    }

    fun clearSelectedDm() {
        openDmJob?.cancel()
        openDmJob = null
        _uiState.update { it.copy(selectedDm = null) }
    }

    fun disconnectRealtime() {
        profileOperationJob?.cancel()
        loadAllJob?.cancel()
        userSearchJob?.cancel()
        viewedProfileJob?.cancel()
        openDmJob?.cancel()
        friendEventsJob?.cancel()
        presenceEventsJob?.cancel()
        profileEventsJob?.cancel()
        friendEventsCollectJob?.cancel()
        presenceEventsCollectJob?.cancel()
        profileEventsCollectJob?.cancel()
        friendshipEventsSource.disconnect()
        presenceEventsSource.disconnect()
        profileEventsSource.disconnect()
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            disconnectRealtime()
            onComplete()
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, onComplete: () -> Unit) {
        if (_uiState.value.changingPassword) return
        _uiState.update { it.copy(changingPassword = true, passwordError = null) }
        viewModelScope.launch {
            try {
                authRepository.changePassword(currentPassword, newPassword)
                onComplete()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = if (e is ApiException) e.message else
                    "No se pudo conectar con el servidor. Inténtalo de nuevo."
                _uiState.update { it.copy(passwordError = message) }
            } finally {
                _uiState.update { it.copy(changingPassword = false) }
            }
        }
    }

    private suspend fun markDmRead(dmId: String) {
        try {
            friendRepository.markDmRead(dmId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // El estado local ya se ha actualizado; el servidor podra sincronizarse despues.
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnectRealtime()
    }
}
