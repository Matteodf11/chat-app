package com.matteo.chatapp.platform

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.file.Files

actual suspend fun pickAvatarFile(): PickedAvatarFile? {
    return pickProfileImageFile("Seleccionar avatar")
}

actual suspend fun pickProfileImageFile(title: String): PickedAvatarFile? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.file = "*.jpg;*.jpeg;*.png;*.webp"
    dialog.isVisible = true

    val directory = dialog.directory ?: return null
    val fileName = dialog.file ?: return null
    val file = File(directory, fileName)
    if (!file.exists() || !file.isFile) return null

    val extension = file.extension.lowercase()
    val contentType = when (extension) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "jpg", "jpeg" -> "image/jpeg"
        else -> return null
    }

    return PickedAvatarFile(
        fileName = file.name,
        contentType = contentType,
        bytes = Files.readAllBytes(file.toPath())
    )
}
