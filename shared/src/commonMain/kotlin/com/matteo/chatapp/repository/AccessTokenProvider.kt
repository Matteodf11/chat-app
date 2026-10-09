package com.matteo.chatapp.repository

interface AccessTokenProvider {
    fun currentToken(): String?

    fun requireToken(): String = checkNotNull(currentToken()) {
        "La sesión ha caducado"
    }
}

object SessionAccessTokenProvider : AccessTokenProvider {
    override fun currentToken(): String? = SessionManager.getToken()
}
