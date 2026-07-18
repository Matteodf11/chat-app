package com.matteo.chatapp.repository

object SessionManager {
    private var token: String? = null
    private var refreshToken: String? = null
    private var username: String? = null
    private var email: String? = null
    private var userId: String? = null
    private var remembered = false

    init {
        SessionStorage.load()?.let { stored ->
            refreshToken = stored.refreshToken
            username = stored.username
            email = stored.email
            userId = stored.userId
            remembered = true
        }
    }

    fun saveAuthSession(
        token: String,
        refreshToken: String,
        username: String,
        email: String,
        remember: Boolean
    ) {
        this.token = token
        this.refreshToken = refreshToken
        this.username = username
        this.email = email
        this.remembered = remember
        persistIfRemembered()
        if (!remember) SessionStorage.clear()
    }

    fun saveSession(token: String, username: String, email: String, userId: String? = null) {
        this.token = token
        this.username = username
        this.email = email
        if (userId != null) this.userId = userId
        persistIfRemembered()
    }

    fun updateTokens(token: String, refreshToken: String) {
        this.token = token
        this.refreshToken = refreshToken
        persistIfRemembered()
    }

    fun getToken(): String? = token
    fun getRefreshToken(): String? = refreshToken
    fun getUsername(): String? = username
    fun getEmail(): String? = email
    fun getUserId(): String? = userId
    fun saveUserId(userId: String) {
        this.userId = userId
        persistIfRemembered()
    }
    fun isLoggedIn(): Boolean = token != null
    fun hasRememberedSession(): Boolean = remembered && refreshToken != null
    fun isRemembered(): Boolean = remembered

    fun clearSession() {
        token = null
        refreshToken = null
        username = null
        email = null
        userId = null
        remembered = false
        SessionStorage.clear()
    }

    private fun persistIfRemembered() {
        val storedRefreshToken = refreshToken ?: return
        if (!remembered) return
        SessionStorage.save(RememberedSession(storedRefreshToken, username.orEmpty(), email.orEmpty(), userId))
    }
}
