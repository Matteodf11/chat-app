package com.matteo.chatapp.viewmodel

import com.matteo.chatapp.model.DmConversation
import com.matteo.chatapp.model.FriendRequest
import com.matteo.chatapp.model.FriendshipEvent
import com.matteo.chatapp.model.PresenceEvent
import com.matteo.chatapp.model.ProfileEvent
import com.matteo.chatapp.model.UserProfile
import com.matteo.chatapp.network.ApiException
import kotlinx.coroutines.CompletableDeferred
import com.matteo.chatapp.platform.PickedAvatarFile
import com.matteo.chatapp.repository.AccessTokenProvider
import com.matteo.chatapp.repository.AuthRepository
import com.matteo.chatapp.repository.FriendRepository
import com.matteo.chatapp.repository.FriendSnapshot
import com.matteo.chatapp.repository.FriendshipEventsSource
import com.matteo.chatapp.repository.PresenceEventsSource
import com.matteo.chatapp.repository.ProfileEventsSource
import com.matteo.chatapp.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FriendViewModelTest {
    @Test fun failedBlockingKeepsTheFriendAndAllowsRetry() = runViewModelTest {
        val friend = friendship("relation", "friend")
        val repository = FakeFriendRepository(snapshot = FriendSnapshot(listOf(friend), emptyList(), emptyList()))
        repository.contactHandler = { throw ApiException("No se pudo bloquear", 503) }
        val vm = createViewModel(repository, FakeProfileRepository(profile("me", "Me")))
        try {
            advanceUntilIdle()
            vm.blockUser("friend")
            advanceUntilIdle()
            assertEquals(listOf(friend), vm.uiState.value.friends)
            assertTrue(vm.uiState.value.blockedUsers.isEmpty())
            assertEquals("No se pudo bloquear", vm.uiState.value.error)
            assertFalse(vm.uiState.value.contactActionLoading)
        } finally { vm.disconnectRealtime() }
    }

    @Test fun blockingWaitsForServerAndUpdatesTheOpenChatWithoutDuplicateActions() = runViewModelTest {
        val friend = friendship("relation", "friend")
        val conversation = dm("dm", "friend")
        val repository = FakeFriendRepository(snapshot = FriendSnapshot(listOf(friend), emptyList(), listOf(conversation)))
        val completed = CompletableDeferred<Unit>()
        var calls = 0
        repository.contactHandler = {
            calls++
            completed.await()
            repository.snapshot = FriendSnapshot(emptyList(), emptyList(), listOf(conversation.copy(canMessage = false)),
                listOf(com.matteo.chatapp.model.BlockedUser("friend", "Friend")))
        }
        val vm = createViewModel(repository, FakeProfileRepository(profile("me", "Me")))
        try {
            advanceUntilIdle()
            vm.selectDm(conversation)
            vm.blockUser("friend")
            vm.blockUser("friend")
            advanceUntilIdle()
            assertEquals(1, calls)
            assertTrue(vm.uiState.value.contactActionLoading)
            assertEquals(listOf(friend), vm.uiState.value.friends)
            completed.complete(Unit)
            advanceUntilIdle()
            assertTrue(vm.uiState.value.friends.isEmpty())
            assertEquals("friend", vm.uiState.value.blockedUsers.single().id)
            assertFalse(assertNotNull(vm.uiState.value.selectedDm).canMessage)
            assertFalse(vm.uiState.value.contactActionLoading)
        } finally { vm.disconnectRealtime() }
    }

    @Test
    fun passwordChangeShowsServerErrorAndAllowsRetryWithoutDuplicateRequests() = runViewModelTest {
        val response = CompletableDeferred<Unit>()
        var calls = 0
        val repository = NoOpAuthRepository {
            calls++
            response.await()
        }
        val viewModel = createViewModel(FakeFriendRepository(), FakeProfileRepository(profile("me", "Matteo")), authRepository = repository)
        var completed = false
        try {
            advanceUntilIdle()
            viewModel.changePassword("old-password", "new-password") { completed = true }
            viewModel.changePassword("old-password", "new-password") { completed = true }
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.changingPassword)
            assertEquals(1, calls)
            response.completeExceptionally(ApiException("La contraseña actual no es correcta", 400))
            advanceUntilIdle()
            assertFalse(completed)
            assertFalse(viewModel.uiState.value.changingPassword)
            assertEquals("La contraseña actual no es correcta", viewModel.uiState.value.passwordError)
        } finally {
            viewModel.disconnectRealtime()
        }
    }

    @Test
    fun passwordChangeOnlyCompletesAfterRepositorySuccess() = runViewModelTest {
        val viewModel = createViewModel(FakeFriendRepository(), FakeProfileRepository(profile("me", "Matteo")))
        var completed = false
        try {
            advanceUntilIdle()
            viewModel.changePassword("old-password", "new-password") { completed = true }
            assertFalse(completed)
            advanceUntilIdle()
            assertTrue(completed)
            assertFalse(viewModel.uiState.value.changingPassword)
            assertEquals(null, viewModel.uiState.value.passwordError)
        } finally {
            viewModel.disconnectRealtime()
        }
    }

    @Test
    fun initializationLoadsProfileAndFriendSnapshot() = runViewModelTest {
        val profile = profile(id = "me", username = "Matteo")
        val friend = friendship(id = "friendship-1", friendId = "friend-1")
        val dm = dm(id = "dm-1", otherUserId = "friend-1")
        val friendRepository = FakeFriendRepository(
            snapshot = FriendSnapshot(
                friends = listOf(friend),
                pendingRequests = emptyList(),
                dms = listOf(dm)
            )
        )
        val profileRepository = FakeProfileRepository(profile)
        val viewModel = createViewModel(friendRepository, profileRepository)

        try {
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(profile, state.profile)
            assertEquals(listOf(friend), state.friends)
            assertEquals(listOf(dm), state.dms)
        } finally {
            viewModel.disconnectRealtime()
        }
    }

    @Test
    fun openingDmWaitsForVisibleMessagesToMarkReadAndPresenceEventUpdatesIt() = runViewModelTest {
        val openedDm = dm(
            id = "dm-2",
            otherUserId = "friend-2",
            hasUnread = true
        )
        val friendRepository = FakeFriendRepository(openedDm = openedDm)
        val presenceSource = FakePresenceEventsSource()
        val viewModel = createViewModel(
            friendRepository = friendRepository,
            profileRepository = FakeProfileRepository(profile("me", "Matteo")),
            presenceEventsSource = presenceSource
        )

        try {
            advanceUntilIdle()
            viewModel.openDm("friend-2")
            advanceUntilIdle()

            assertTrue(friendRepository.markedAsRead.isEmpty())
            assertFalse(assertNotNull(viewModel.uiState.value.selectedDm).hasUnread)

            presenceSource.emit(
                PresenceEvent(
                    userId = "friend-2",
                    username = "Amiga",
                    online = true
                )
            )
            advanceUntilIdle()

            assertTrue(assertNotNull(viewModel.uiState.value.selectedDm).otherIsOnline)
        } finally {
            viewModel.disconnectRealtime()
        }
    }

    private fun createViewModel(
        friendRepository: FakeFriendRepository,
        profileRepository: FakeProfileRepository,
        presenceEventsSource: FakePresenceEventsSource = FakePresenceEventsSource(),
        authRepository: AuthRepository = NoOpAuthRepository()
    ) = FriendViewModel(
        friendRepository = friendRepository,
        profileRepository = profileRepository,
        authRepository = authRepository,
        accessTokenProvider = EmptyAccessTokenProvider,
        friendshipEventsSource = FakeFriendshipEventsSource(),
        presenceEventsSource = presenceEventsSource,
        profileEventsSource = FakeProfileEventsSource()
    )

    private fun runViewModelTest(
        block: suspend TestScope.() -> Unit
    ) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }
}

private object EmptyAccessTokenProvider : AccessTokenProvider {
    override fun currentToken(): String? = null
}

private class FakeFriendRepository(
    var snapshot: FriendSnapshot = FriendSnapshot(
        friends = emptyList(),
        pendingRequests = emptyList(),
        dms = emptyList()
    ),
    private val openedDm: DmConversation = dm("dm", "friend")
) : FriendRepository {
    var contactHandler: suspend (String) -> Unit = {}
    override suspend fun blockUser(userId: String) = contactHandler(userId)
    override suspend fun unblockUser(userId: String) = contactHandler(userId)
    override suspend fun removeFriend(userId: String) = contactHandler(userId)
    val markedAsRead = mutableListOf<String>()

    override suspend fun loadSnapshot(): FriendSnapshot = snapshot

    override suspend fun sendFriendRequest(userId: String): FriendRequest =
        friendship("sent", userId)

    override suspend fun acceptRequest(friendshipId: String): FriendRequest =
        friendship(friendshipId, "friend")

    override suspend fun rejectRequest(friendshipId: String) = Unit

    override suspend fun openDm(otherUserId: String): DmConversation = openedDm

    override suspend fun markDmRead(dmId: String) {
        markedAsRead += dmId
    }
}

private class FakeProfileRepository(
    private val ownProfile: UserProfile
) : ProfileRepository {
    override suspend fun loadMyProfile(): UserProfile = ownProfile

    override suspend fun saveProfile(
        username: String,
        status: String,
        bio: String,
        avatarFile: PickedAvatarFile?,
        bannerFile: PickedAvatarFile?
    ): UserProfile = ownProfile.copy(username = username, status = status, bio = bio)

    override suspend fun findUser(username: String): UserProfile =
        ownProfile.copy(username = username)

    override suspend fun getUser(userId: String): UserProfile =
        ownProfile.copy(id = userId)

    override suspend fun getCurrentUserId(): String = ownProfile.id

    override fun cacheCurrentProfile(profile: UserProfile) = Unit
}

private class FakeFriendshipEventsSource : FriendshipEventsSource {
    override val events: Flow<FriendshipEvent> = MutableSharedFlow()

    override suspend fun connect(token: String, userId: String) = Unit

    override fun disconnect() = Unit
}

private class FakePresenceEventsSource : PresenceEventsSource {
    private val mutableEvents = MutableSharedFlow<PresenceEvent>()
    override val events: Flow<PresenceEvent> = mutableEvents

    suspend fun emit(event: PresenceEvent) {
        mutableEvents.emit(event)
    }

    override suspend fun connect(token: String) = Unit

    override fun disconnect() = Unit
}

private class FakeProfileEventsSource : ProfileEventsSource {
    override val events: Flow<ProfileEvent> = MutableSharedFlow()

    override suspend fun connect(token: String) = Unit

    override fun disconnect() = Unit
}

private class NoOpAuthRepository(private val onChangePassword: suspend () -> Unit = {}) : AuthRepository {
    override suspend fun login(email: String, password: String, rememberMe: Boolean) = Unit

    override suspend fun register(username: String, email: String, password: String) = Unit

    override suspend fun logout(refreshToken: String) = Unit

    override suspend fun changePassword(currentPassword: String, newPassword: String) = onChangePassword()
}

private fun profile(id: String, username: String) = UserProfile(
    id = id,
    username = username,
    email = "$username@example.com"
)

private fun friendship(id: String, friendId: String) = FriendRequest(
    id = id,
    friendId = friendId,
    friendUsername = "Amiga",
    status = "ACCEPTED"
)

private fun dm(
    id: String,
    otherUserId: String,
    hasUnread: Boolean = false
) = DmConversation(
    id = id,
    otherUserId = otherUserId,
    otherUsername = "Amiga",
    hasUnread = hasUnread
)
