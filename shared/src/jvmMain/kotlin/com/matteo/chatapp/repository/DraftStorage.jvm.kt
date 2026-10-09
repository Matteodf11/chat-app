package com.matteo.chatapp.repository

import java.util.prefs.Preferences
import java.security.MessageDigest

actual object DraftStorage {
    private val preferences = Preferences.userRoot().node("com/matteo/chatapp/drafts")
    private fun key(account: String, conversation: String) = MessageDigest.getInstance("SHA-256")
        .digest("$account|$conversation".toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    actual fun load(account: String, conversation: String): String = preferences.get(key(account, conversation), "")
    actual fun save(account: String, conversation: String, text: String) {
        if (text.isEmpty()) preferences.remove(key(account, conversation)) else preferences.put(key(account, conversation), text)
    }
}
