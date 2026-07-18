package com.matteo.chatapp.repository

import java.util.prefs.Preferences

actual object SessionStorage {
    private val preferences = Preferences.userRoot().node("com/matteo/chatapp/session")

    actual fun load(): RememberedSession? {
        val refreshToken = preferences.get("refresh_token", null) ?: return null
        return RememberedSession(
            refreshToken = refreshToken,
            username = preferences.get("username", ""),
            email = preferences.get("email", ""),
            userId = preferences.get("user_id", null)
        )
    }

    actual fun save(session: RememberedSession) {
        preferences.put("refresh_token", session.refreshToken)
        preferences.put("username", session.username)
        preferences.put("email", session.email)
        session.userId?.let { preferences.put("user_id", it) } ?: preferences.remove("user_id")
    }

    actual fun clear() = preferences.clear()
}
