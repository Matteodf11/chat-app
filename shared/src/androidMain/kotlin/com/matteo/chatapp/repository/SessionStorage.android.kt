package com.matteo.chatapp.repository

import android.content.Context
import com.matteo.chatapp.platform.AndroidPlatform

actual object SessionStorage {
    private val preferences
        get() = AndroidPlatform.context.getSharedPreferences("chat_app_session", Context.MODE_PRIVATE)

    actual fun load(): RememberedSession? {
        val refreshToken = preferences.getString("refresh_token", null) ?: return null
        return RememberedSession(
            refreshToken = refreshToken,
            username = preferences.getString("username", "").orEmpty(),
            email = preferences.getString("email", "").orEmpty(),
            userId = preferences.getString("user_id", null)
        )
    }

    actual fun save(session: RememberedSession) {
        preferences.edit()
            .putString("refresh_token", session.refreshToken)
            .putString("username", session.username)
            .putString("email", session.email)
            .apply {
                if (session.userId != null) putString("user_id", session.userId) else remove("user_id")
            }
            .apply()
    }

    actual fun clear() {
        preferences.edit().clear().apply()
    }
}
