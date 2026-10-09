package com.matteo.chatapp.repository

import android.content.Context
import com.matteo.chatapp.platform.AndroidPlatform

actual object DraftStorage {
    private val preferences get() = AndroidPlatform.context.getSharedPreferences("chat_drafts", Context.MODE_PRIVATE)
    private fun key(account: String, conversation: String) = "$account|$conversation"
    actual fun load(account: String, conversation: String): String = preferences.getString(key(account, conversation), "").orEmpty()
    actual fun save(account: String, conversation: String, text: String) {
        preferences.edit().apply {
            if (text.isEmpty()) remove(key(account, conversation)) else putString(key(account, conversation), text)
        }.apply()
    }
}
