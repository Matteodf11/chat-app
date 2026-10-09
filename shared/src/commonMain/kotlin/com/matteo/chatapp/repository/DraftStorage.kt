package com.matteo.chatapp.repository

expect object DraftStorage {
    fun load(account: String, conversation: String): String
    fun save(account: String, conversation: String, text: String)
}
