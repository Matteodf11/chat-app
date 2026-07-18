package com.matteo.chatapp.platform

data class PickedAvatarFile(
    val fileName: String,
    val contentType: String,
    val bytes: ByteArray
)

expect suspend fun pickAvatarFile(): PickedAvatarFile?

expect suspend fun pickProfileImageFile(title: String): PickedAvatarFile?
