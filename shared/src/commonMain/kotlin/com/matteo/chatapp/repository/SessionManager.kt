package com.matteo.chatapp.repository

object SessionManager {
    private var token: String? = null
    private var username: String? = null
    private var email: String? = null

    fun saveSession(token: String, username: String, email: String) {
        this.token = token
        this.username = username
        this.email = email
    }

    fun getToken(): String? = token
    fun getUsername(): String? = username
    fun getEmail(): String? = email
    fun isLoggedIn(): Boolean = token != null

    fun clearSession() {
        token = null
        username = null
        email = null
    }
}