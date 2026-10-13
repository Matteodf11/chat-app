package com.matteo.chatapp.viewmodel

import com.matteo.chatapp.model.*
import com.matteo.chatapp.platform.PickedAvatarFile
import com.matteo.chatapp.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ServerViewModelTest {
    private val server = Server("s1", "Equipo", ownerUsername = "Ana", myRole = "owner")
    private fun vm(repository: TestServerRepository) = ServerViewModel(repository,
        object : AccessTokenProvider { override fun currentToken(): String? = null },
        object : ServerEventsSource {
            override val events = emptyFlow<ServerEvent>()
            override suspend fun connect(token: String) = Unit
            override fun disconnect() = Unit
        })
    private fun check(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try { block() } finally { Dispatchers.resetMain() }
    }

    @Test fun failedServerListCanBeRetriedWithoutLookingLikeAnEmptyAccount() = check {
        val repository = TestServerRepository(server)
        repository.list = { error("offline") }
        val vm = vm(repository)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.serversError)
        assertFalse(vm.uiState.value.isLoading)
        repository.list = { listOf(server) }
        vm.loadServers()
        advanceUntilIdle()
        assertNull(vm.uiState.value.serversError)
        assertEquals(listOf(server), vm.uiState.value.servers)
    }

    @Test fun failedSaveDoesNotDismissOrReplaceServerAndAllowsRetry() = check {
        val repository = TestServerRepository(server)
        val vm = vm(repository)
        vm.selectServer(server)
        advanceUntilIdle()
        var saved = 0
        repository.save = { error("offline") }
        vm.updateSelectedServer("Nuevo", "Descripción", null) { saved++ }
        assertEquals("save", vm.uiState.value.action)
        advanceUntilIdle()
        assertEquals(0, saved)
        assertEquals("Equipo", vm.uiState.value.selectedServer?.name)
        assertNotNull(vm.uiState.value.error)
        assertNull(vm.uiState.value.action)
        repository.save = { server.copy(name = "Nuevo", description = "Descripción") }
        vm.updateSelectedServer("Nuevo", "Descripción", null) { saved++ }
        advanceUntilIdle()
        assertEquals(1, saved)
        assertEquals("Nuevo", vm.uiState.value.selectedServer?.name)
        assertEquals("Cambios guardados", vm.uiState.value.notice)
        assertNull(vm.uiState.value.error)
    }

    @Test fun duplicateInvitationIsBlockedAndOnlySuccessClearsTheForm() = check {
        val repository = TestServerRepository(server)
        val vm = vm(repository)
        vm.selectServer(server)
        advanceUntilIdle()
        val gate = CompletableDeferred<Unit>()
        repository.invite = { gate.await() }
        var cleared = 0
        vm.inviteMember("Ana") { cleared++ }
        vm.inviteMember("Ana") { cleared++ }
        advanceUntilIdle()
        assertEquals(1, repository.invites)
        gate.completeExceptionally(IllegalStateException("offline"))
        advanceUntilIdle()
        assertEquals(0, cleared)
        assertNull(vm.uiState.value.action)
        repository.invite = {}
        vm.inviteMember("Ana") { cleared++ }
        advanceUntilIdle()
        assertEquals(1, cleared)
        assertEquals("Invitación enviada", vm.uiState.value.notice)
    }

    @Test fun channelErrorsAreRecoverableAndCreationCallbackOnlyRunsOnSuccess() = check {
        val repository = TestServerRepository(server)
        repository.channels = { error("offline") }
        val vm = vm(repository)
        vm.selectServer(server)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.channelsError)
        assertFalse(vm.uiState.value.isLoadingChannels)
        repository.channels = { listOf(Channel("c1", "general", "text", server.id)) }
        vm.reloadSelectedServer()
        advanceUntilIdle()
        assertNull(vm.uiState.value.channelsError)
        assertEquals(1, vm.uiState.value.channels.size)
        var cleared = 0
        repository.create = { error("offline") }
        vm.createChannel("fotos") { cleared++ }
        advanceUntilIdle()
        assertEquals(0, cleared)
        repository.create = { Channel("c2", "fotos", "text", server.id) }
        vm.createChannel("fotos") { cleared++ }
        advanceUntilIdle()
        assertEquals(1, cleared)
        assertEquals(listOf("general", "fotos"), vm.uiState.value.channels.map { it.name })
        assertEquals("Canal creado", vm.uiState.value.notice)
    }

    @Test fun activityAppliesToChannelsLoadedLaterAndSurvivesNetworkFailure() = check {
        val repository = TestServerRepository(server)
        repository.activity = { listOf(ChannelActivity("c1", server.id, 3, 1)) }
        repository.channels = { listOf(Channel("c1", "general", "text", server.id)) }
        val vm = vm(repository)
        advanceUntilIdle()
        assertEquals(3L, vm.uiState.value.servers.single().unreadCount)
        vm.selectServer(server)
        advanceUntilIdle()
        assertEquals(1L, vm.uiState.value.channels.single().mentionCount)
        repository.activity = { error("offline") }
        vm.refreshActivity(); advanceUntilIdle()
        assertEquals(3L, vm.uiState.value.channels.single().unreadCount)
        repository.activity = { listOf(ChannelActivity("c1", server.id, 0, 0)) }
        vm.markChannelRead("c1", "message")
        advanceUntilIdle()
        vm.markChannelRead("c1", "message")
        advanceUntilIdle()
        assertEquals(1, repository.reads)
        assertEquals(0L, vm.uiState.value.servers.single().unreadCount)
    }

    @Test fun eventArrivingDuringRefreshGetsAnAuthoritativeSecondRefresh() = check {
        val repository = TestServerRepository(server)
        val vm = vm(repository)
        advanceUntilIdle()
        val gate = CompletableDeferred<List<ChannelActivity>>()
        repository.activity = { gate.await() }
        vm.refreshActivity(); advanceUntilIdle()
        vm.refreshActivity()
        repository.activity = { listOf(ChannelActivity("c1", server.id, 2, 1)) }
        gate.complete(listOf(ChannelActivity("c1", server.id, 1, 0)))
        advanceUntilIdle()
        assertEquals(2L, vm.uiState.value.servers.single().unreadCount)
        assertEquals(1L, vm.uiState.value.servers.single().mentionCount)
    }
}

private class TestServerRepository(private val server: Server) : ServerRepository {
    var activity: suspend () -> List<ChannelActivity> = { emptyList() }
    var reads = 0
    override suspend fun getActivity() = activity()
    override suspend fun markChannelRead(channelId: String, messageId: String) { reads++ }
    var list: suspend () -> List<Server> = { listOf(server) }
    var save: suspend () -> Server = { server }
    var invite: suspend () -> Unit = {}
    var channels: suspend () -> List<Channel> = { emptyList() }
    var create: suspend () -> Channel = { error("not configured") }
    var invites = 0
    override suspend fun getServers() = list()
    override suspend fun getChannels(serverId: String) = channels()
    override suspend fun getMembers(serverId: String) = emptyList<ServerMember>()
    override suspend fun getPendingInvitations() = emptyList<ServerInvitation>()
    override suspend fun getAllChannels(serverIds: List<String>) = emptyList<Channel>()
    override suspend fun createServer(name: String, description: String, iconFile: PickedAvatarFile?) = server
    override suspend fun updateServer(serverId: String, name: String, description: String, iconFile: PickedAvatarFile?) = save()
    override suspend fun inviteMember(serverId: String, username: String) { invites++; invite() }
    override suspend fun updateMemberRole(serverId: String, userId: String, role: String): ServerMember = error("not configured")
    override suspend fun removeMember(serverId: String, userId: String) = Unit
    override suspend fun acceptInvitation(invitationId: String) = server
    override suspend fun rejectInvitation(invitationId: String) = Unit
    override suspend fun leaveServer(serverId: String) = Unit
    override suspend fun deleteServer(serverId: String) = Unit
    override suspend fun createChannel(serverId: String, name: String) = create()
}
