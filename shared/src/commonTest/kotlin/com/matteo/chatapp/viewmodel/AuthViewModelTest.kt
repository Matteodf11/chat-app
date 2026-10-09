package com.matteo.chatapp.viewmodel

import com.matteo.chatapp.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @Test
    fun loginDelegatesCredentialsAndPublishesSuccess() = runViewModelTest {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.login(
            email = "matteo@example.com",
            password = "super-secret",
            rememberMe = true
        )
        advanceUntilIdle()

        assertEquals(
            LoginCall("matteo@example.com", "super-secret", true),
            repository.loginCall
        )
        assertEquals(AuthState.Success, viewModel.state.value)
    }

    @Test
    fun registerNormalizesInputBeforeDelegating() = runViewModelTest {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.register(
            username = "  Matteo  ",
            email = "  matteo@example.com  ",
            password = "super-secret"
        )
        advanceUntilIdle()

        assertEquals(
            RegisterCall("Matteo", "matteo@example.com", "super-secret"),
            repository.registerCall
        )
        assertEquals(AuthState.Success, viewModel.state.value)
    }

    @Test
    fun invalidRegistrationStopsBeforeCallingRepository() = runViewModelTest {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.register(
            username = "ab",
            email = "not-an-email",
            password = "short"
        )
        advanceUntilIdle()

        assertNull(repository.registerCall)
        assertIs<AuthState.Error>(viewModel.state.value)
    }

    @Test
    fun repositoryFailureBecomesUiError() = runViewModelTest {
        val repository = FakeAuthRepository(
            failure = IllegalStateException("Servidor no disponible")
        )
        val viewModel = AuthViewModel(repository)

        viewModel.login(
            email = "matteo@example.com",
            password = "super-secret",
            rememberMe = false
        )
        advanceUntilIdle()

        assertEquals(
            AuthState.Error("Servidor no disponible"),
            viewModel.state.value
        )
    }

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

private data class LoginCall(
    val email: String,
    val password: String,
    val rememberMe: Boolean
)

private data class RegisterCall(
    val username: String,
    val email: String,
    val password: String
)

private class FakeAuthRepository(
    private val failure: Exception? = null
) : AuthRepository {
    var loginCall: LoginCall? = null
        private set

    var registerCall: RegisterCall? = null
        private set

    override suspend fun login(email: String, password: String, rememberMe: Boolean) {
        loginCall = LoginCall(email, password, rememberMe)
        failure?.let { throw it }
    }

    override suspend fun register(username: String, email: String, password: String) {
        registerCall = RegisterCall(username, email, password)
        failure?.let { throw it }
    }

    override suspend fun logout(refreshToken: String) {
        failure?.let { throw it }
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String) {
        failure?.let { throw it }
    }
}
