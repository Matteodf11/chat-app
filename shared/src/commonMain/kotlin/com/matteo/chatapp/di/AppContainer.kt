package com.matteo.chatapp.di

import com.matteo.chatapp.network.AuthApi
import com.matteo.chatapp.network.FriendApi
import com.matteo.chatapp.network.MessageApi
import com.matteo.chatapp.network.ServerApi
import com.matteo.chatapp.network.UserApi
import com.matteo.chatapp.network.WebSocketClient
import com.matteo.chatapp.network.createHttpClient
import com.matteo.chatapp.repository.AuthRepository
import com.matteo.chatapp.repository.AccessTokenProvider
import com.matteo.chatapp.repository.ChatRepository
import com.matteo.chatapp.repository.CurrentSessionIdentityProvider
import com.matteo.chatapp.repository.FriendRepository
import com.matteo.chatapp.repository.NetworkAuthRepository
import com.matteo.chatapp.repository.NetworkChatRepository
import com.matteo.chatapp.repository.NetworkFriendRepository
import com.matteo.chatapp.repository.NetworkProfileRepository
import com.matteo.chatapp.repository.NetworkServerRepository
import com.matteo.chatapp.repository.ProfileRepository
import com.matteo.chatapp.repository.ServerRepository
import com.matteo.chatapp.repository.SessionIdentityProvider
import com.matteo.chatapp.repository.SessionAccessTokenProvider
import com.matteo.chatapp.repository.WebSocketConversationEventsSource
import com.matteo.chatapp.repository.WebSocketDmInboxEventsSource
import com.matteo.chatapp.repository.WebSocketFriendshipEventsSource
import com.matteo.chatapp.repository.WebSocketPersonalMessagesSource
import com.matteo.chatapp.repository.WebSocketPresenceEventsSource
import com.matteo.chatapp.repository.WebSocketProfileEventsSource
import com.matteo.chatapp.repository.WebSocketServerEventsSource
import com.matteo.chatapp.viewmodel.AuthViewModel
import com.matteo.chatapp.viewmodel.ChatViewModel
import com.matteo.chatapp.viewmodel.FriendViewModel
import com.matteo.chatapp.viewmodel.ServerViewModel
import io.ktor.client.HttpClient

class AppContainer(
    private val httpClient: HttpClient = createHttpClient()
) {
    private val accessTokenProvider: AccessTokenProvider = SessionAccessTokenProvider
    private val sessionIdentityProvider: SessionIdentityProvider =
        CurrentSessionIdentityProvider
    private val authApi = AuthApi(httpClient)
    val authRepository: AuthRepository = NetworkAuthRepository(authApi, accessTokenProvider)
    private val friendApi = FriendApi(httpClient)
    private val friendRepository: FriendRepository =
        NetworkFriendRepository(friendApi, accessTokenProvider)
    private val messageApi = MessageApi(httpClient)
    private val chatRepository: ChatRepository =
        NetworkChatRepository(messageApi, friendApi, accessTokenProvider)
    private val serverApi = ServerApi(httpClient)
    private val serverRepository: ServerRepository =
        NetworkServerRepository(serverApi, accessTokenProvider)
    private val userApi = UserApi(httpClient)
    private val profileRepository: ProfileRepository =
        NetworkProfileRepository(userApi, accessTokenProvider)

    fun createAuthViewModel() = AuthViewModel(authRepository)

    fun createChatViewModel() = ChatViewModel(
        chatRepository = chatRepository,
        accessTokenProvider = accessTokenProvider,
        sessionIdentityProvider = sessionIdentityProvider,
        conversationEventsSource = WebSocketConversationEventsSource(newWebSocketClient()),
        profileEventsSource = WebSocketProfileEventsSource(newWebSocketClient()),
        personalMessagesSource = WebSocketPersonalMessagesSource(newWebSocketClient()),
        inboxEventsSource = WebSocketDmInboxEventsSource(newWebSocketClient())
    )

    fun createFriendViewModel() = FriendViewModel(
        friendRepository = friendRepository,
        profileRepository = profileRepository,
        authRepository = authRepository,
        accessTokenProvider = accessTokenProvider,
        friendshipEventsSource = WebSocketFriendshipEventsSource(newWebSocketClient()),
        presenceEventsSource = WebSocketPresenceEventsSource(newWebSocketClient()),
        profileEventsSource = WebSocketProfileEventsSource(newWebSocketClient())
    )

    fun createServerViewModel() = ServerViewModel(
        serverRepository = serverRepository,
        accessTokenProvider = accessTokenProvider,
        serverEventsSource = WebSocketServerEventsSource(newWebSocketClient())
    )

    fun close() {
        httpClient.close()
    }

    private fun newWebSocketClient() = WebSocketClient(httpClient)
}
