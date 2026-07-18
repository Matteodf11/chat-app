package com.matteo.chatapp.platform

actual fun copyTextToClipboard(text: String) {
    val clipboard = AndroidPlatform.context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Mensaje", text))
}
