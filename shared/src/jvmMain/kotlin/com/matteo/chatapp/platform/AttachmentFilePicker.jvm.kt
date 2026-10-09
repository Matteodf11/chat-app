package com.matteo.chatapp.platform

import kotlinx.coroutines.ensureActive
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.URI
import java.nio.file.Files

actual suspend fun pickAttachmentFile(): PickedAttachmentFile? = pickDesktopAttachment(false)
actual suspend fun pickPhotoAttachment(): PickedAttachmentFile? = pickDesktopAttachment(true)

private suspend fun pickDesktopAttachment(photo: Boolean): PickedAttachmentFile? {
    val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val dialog = FileDialog(null as Frame?, if (photo) "Seleccionar imagen" else "Seleccionar archivo", FileDialog.LOAD)
        if (photo) {
            dialog.directory = File(System.getProperty("user.home"), "Pictures").path
            dialog.setFilenameFilter { _, name -> name.substringAfterLast('.').lowercase() in setOf("jpg", "jpeg", "png", "webp", "gif") }
        }
        try {
            dialog.isVisible = true
            dialog.file?.let { File(dialog.directory, it) }
        } finally { dialog.dispose() }
    } ?: return null
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        require(file.isFile && file.length() > 0) { "No se puede leer el archivo" }
        require(file.length() <= MAX_ATTACHMENT_BYTES) { "El archivo supera el límite de 25 MB" }
        val contentType = detectContentType(file) ?: error("Formato de archivo no compatible")
        require(!photo || contentType.startsWith("image/")) { "Selecciona una imagen" }
        PickedAttachmentFile(file.name, contentType, file.inputStream().use { it.readAttachmentBytes() })
    }
}

actual suspend fun saveAttachment(url: String, fileName: String): Boolean {
    val target = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val dialog = FileDialog(null as Frame?, "Guardar archivo", FileDialog.SAVE)
        dialog.file = attachmentSaveName(fileName)
        try {
            dialog.isVisible = true
            dialog.file?.let { File(dialog.directory, it).toPath() }
        } finally { dialog.dispose() }
    } ?: return false
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val bytes = downloadAttachmentBytes(url)
        val temporary = Files.createTempFile(target.toAbsolutePath().parent, ".chimera-", ".download")
        try {
            Files.write(temporary, bytes)
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            try { Files.move(temporary, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING) }
            catch (_: java.nio.file.AtomicMoveNotSupportedException) { Files.move(temporary, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING) }
            true
        } finally { Files.deleteIfExists(temporary) }
    }
}

actual fun supportsCurrentLocation(): Boolean = false
actual suspend fun currentLocation(): SharedLocation = error("Introduce las coordenadas del lugar que quieras compartir")

actual fun openExternalUrl(url: String) {
    if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI(url))
}

private fun detectContentType(file: File): String? {
    val probed = runCatching { Files.probeContentType(file.toPath()) }.getOrNull()?.lowercase()
    if (probed in allowedContentTypes) return probed
    return when (file.extension.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        "csv" -> "text/csv"
        "zip" -> "application/zip"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        else -> null
    }
}

private val allowedContentTypes = setOf(
    "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif",
    "application/pdf", "text/plain", "text/csv", "application/zip",
    "application/x-zip-compressed", "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation"
)

actual fun supportsPhotoCapture(): Boolean = false
actual suspend fun capturePhoto(): PickedAttachmentFile? = null
actual suspend fun pickPhotoAttachments(): List<PickedAttachmentFile> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    val dialog = FileDialog(null as Frame?, "Seleccionar fotos", FileDialog.LOAD)
    dialog.isMultipleMode = true
    dialog.directory = File(System.getProperty("user.home"), "Pictures").path
    dialog.setFilenameFilter { _, name -> name.substringAfterLast('.').lowercase() in setOf("jpg", "jpeg", "png", "webp", "gif") }
    val files = try { dialog.isVisible = true; dialog.files.toList() } finally { dialog.dispose() }
    if (files.isEmpty()) return@withContext emptyList()
    require(files.size <= MAX_SELECTED_PHOTOS) { "Puedes seleccionar hasta 10 fotos" }
    require(files.all { it.isFile && it.length() in 1..MAX_ATTACHMENT_BYTES }) { "Cada foto debe ocupar entre 1 byte y 25 MB" }
    require(files.sumOf { it.length() } <= MAX_PHOTO_SELECTION_BYTES) { "Las fotos seleccionadas superan 50 MB" }
    var total = 0L
    files.map { file ->
        val type = detectContentType(file) ?: error("Formato de imagen no compatible")
        require(type.startsWith("image/")) { "Selecciona imágenes" }
        val bytes = file.inputStream().use { it.readAttachmentBytes() }
        total += bytes.size
        require(total <= MAX_PHOTO_SELECTION_BYTES) { "Las fotos seleccionadas superan 50 MB" }
        PickedAttachmentFile(file.name, type, bytes)
    }.also(::validatePhotoSelection)
}
