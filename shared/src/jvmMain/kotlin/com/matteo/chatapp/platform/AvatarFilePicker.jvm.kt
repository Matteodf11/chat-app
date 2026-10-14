package com.matteo.chatapp.platform

import java.awt.FileDialog
import java.awt.Frame
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal const val MAX_PROFILE_IMAGE_BYTES = 2 * 1024 * 1024

actual suspend fun pickAvatarFile(): PickedAvatarFile? {
    return pickProfileImageFile("Seleccionar avatar")
}

actual suspend fun pickProfileImageFile(title: String): PickedAvatarFile? = pickDesktopProfileImage(title)

internal suspend fun pickDesktopProfileImage(
    title: String,
    chooseFile: (String) -> File? = ::showProfileImageDialog,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): PickedAvatarFile? = withContext(dispatcher) {
    // A native modal dialog must not run inside Compose's flushing dispatcher:
    // its nested event loop can re-enter the coroutine processing the click.
    val file = chooseFile(title) ?: return@withContext null
    currentCoroutineContext().ensureActive()
    require(file.isFile && file.length() > 0) { "No se puede leer la imagen o está vacía" }
    require(file.length() <= MAX_PROFILE_IMAGE_BYTES) { "Elige una imagen de hasta 2 MB" }
    val contentType = when (file.extension.lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "jpg", "jpeg" -> "image/jpeg"
        else -> throw IllegalArgumentException("Selecciona una imagen JPG, PNG o WEBP")
    }
    PickedAvatarFile(file.name, contentType, file.inputStream().use { it.readProfileImageBytes() })
}

private fun showProfileImageDialog(title: String): File? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    try {
        dialog.file = "*.jpg;*.jpeg;*.png;*.webp"
        dialog.isVisible = true
        val directory = dialog.directory ?: return null
        val fileName = dialog.file ?: return null
        return File(directory, fileName)
    } finally {
        dialog.dispose()
    }
}

internal suspend fun InputStream.readProfileImageBytes(): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    while (true) {
        currentCoroutineContext().ensureActive()
        val count = read(buffer)
        if (count < 0) break
        require(output.size().toLong() + count <= MAX_PROFILE_IMAGE_BYTES) { "Elige una imagen de hasta 2 MB" }
        output.write(buffer, 0, count)
    }
    require(output.size() > 0) { "La imagen está vacía" }
    return output.toByteArray()
}
