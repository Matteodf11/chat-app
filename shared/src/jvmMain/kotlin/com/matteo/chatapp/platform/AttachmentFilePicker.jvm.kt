package com.matteo.chatapp.platform

import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.URI
import java.nio.file.Files

actual suspend fun pickAttachmentFile(): PickedAttachmentFile? {
    val dialog = FileDialog(null as Frame?, "Seleccionar archivo", FileDialog.LOAD)
    dialog.isVisible = true
    val directory = dialog.directory ?: return null
    val fileName = dialog.file ?: return null
    val file = File(directory, fileName)
    if (!file.isFile || file.length() <= 0 || file.length() > 25L * 1024L * 1024L) return null
    val contentType = detectContentType(file) ?: return null
    return PickedAttachmentFile(file.name, contentType, Files.readAllBytes(file.toPath()))
}

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
