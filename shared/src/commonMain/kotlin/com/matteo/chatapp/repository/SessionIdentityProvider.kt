package com.matteo.chatapp.repository

interface SessionIdentityProvider {
    fun currentUserId(): String?

    fun currentUsername(): String?
}

object CurrentSessionIdentityProvider : SessionIdentityProvider {
    override fun currentUserId(): String? = SessionManager.getUserId()

    override fun currentUsername(): String? = SessionManager.getUsername()
}
