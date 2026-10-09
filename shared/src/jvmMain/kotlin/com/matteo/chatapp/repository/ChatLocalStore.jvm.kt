package com.matteo.chatapp.repository

import java.io.File

actual fun platformChatLocalStore(account: String): ChatLocalStore =
    FileChatLocalStore(File(System.getProperty("user.home"), ".chimera/chat-state"), account)

internal actual fun atomicReplaceChatFile(from: String, to: String) {
    java.nio.file.Files.move(File(from).toPath(), File(to).toPath(),
        java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
}
