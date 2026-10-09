package com.matteo.chatapp.network

import com.matteo.chatapp.repository.SessionManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException

object SessionRefresher {
    private val refreshMutex = Mutex()
    private val refreshClient by lazy { createHttpClient(enableSessionRecovery = false) }

    suspend fun refreshSession(): String = refreshMutex.withLock {
        refreshLocked()
    }

    suspend fun recoverAfterRejectedToken(failedToken: String?): String? = refreshMutex.withLock {
        val currentToken = SessionManager.getToken()
        if (currentToken != null && currentToken != failedToken) return@withLock currentToken
        try {
            refreshLocked()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun refreshLocked(): String {
        val refreshToken = SessionManager.getRefreshToken()
            ?: throw IllegalStateException("No hay una sesion renovable")
        val response = AuthApi(refreshClient).refresh(refreshToken)
        SessionManager.updateTokens(response.token, response.refreshToken)
        SessionManager.saveSession(response.token, response.username, response.email)
        return response.token
    }
}
