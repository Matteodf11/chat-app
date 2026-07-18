package com.matteo.chatapp.repository

data class RememberedSession(
    val refreshToken: String,
    val username: String,
    val email: String,
    val userId: String?
)

expect object SessionStorage {
    fun load(): RememberedSession?
    fun save(session: RememberedSession)
    fun clear()
}
