package com.matteo.chatapp.platform

data class PickedAttachmentFile(
    val fileName: String,
    val contentType: String,
    val bytes: ByteArray
) {
    val size: Long get() = bytes.size.toLong()
}

expect suspend fun pickAttachmentFile(): PickedAttachmentFile?

expect fun openExternalUrl(url: String)
