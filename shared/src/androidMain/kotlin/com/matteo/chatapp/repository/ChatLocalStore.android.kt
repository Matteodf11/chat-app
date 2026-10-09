package com.matteo.chatapp.repository

import java.io.File
import com.matteo.chatapp.platform.AndroidPlatform

actual fun platformChatLocalStore(account: String): ChatLocalStore =
    FileChatLocalStore(File(AndroidPlatform.context.noBackupFilesDir, "chat-state"), account)

internal actual fun atomicReplaceChatFile(from: String, to: String) { android.system.Os.rename(from, to) }
