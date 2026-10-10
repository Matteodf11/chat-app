package com.matteo.chatapp.platform

import com.matteo.chatapp.network.BASE_URL
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.io.File
import java.util.UUID
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal const val MAX_ATTACHMENT_BYTES = 25 * 1024 * 1024

internal suspend fun InputStream.readAttachmentBytes(): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    while (true) {
        currentCoroutineContext().ensureActive()
        val count = read(buffer)
        if (count < 0) break
        require(output.size().toLong() + count <= MAX_ATTACHMENT_BYTES) { "El archivo supera el límite de 25 MB" }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

internal suspend fun downloadAttachmentBytes(url: String): ByteArray {
    val connection = URL(if (url.startsWith("/")) "$BASE_URL$url" else url).openConnection() as HttpURLConnection
    connection.connectTimeout = 15000
    connection.readTimeout = 20000
    try {
        check(connection.responseCode in 200..299) { "No se pudo descargar el archivo. Inténtalo de nuevo." }
        require(connection.contentLengthLong <= MAX_ATTACHMENT_BYTES) { "El archivo supera el límite de 25 MB" }
        return connection.inputStream.use { it.readAttachmentBytes() }
    } finally { connection.disconnect() }
}

internal fun attachmentSaveName(name: String): String = name.substringAfterLast('/').substringAfterLast('\\')
    .replace(Regex("[\\p{Cntrl}<>:\"|?*]"), "_").trim(' ', '.').take(160).ifBlank { "archivo" }

internal suspend fun prepareSharedImage(url: String, name: String, directory: File): File {
    check(directory.isDirectory || directory.mkdirs()) { "No se pudo preparar la imagen" }
    val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
    directory.listFiles()?.filter { it.isFile && it.name.startsWith("chimera-") && it.lastModified() < cutoff }
        ?.forEach { it.delete() }
    val bytes = downloadAttachmentBytes(url)
    val file = File(directory, "chimera-${UUID.randomUUID()}-${attachmentSaveName(name)}")
    try {
        file.writeBytes(bytes)
        currentCoroutineContext().ensureActive()
        return file
    } catch (e: Exception) { file.delete(); throw e }
}

actual suspend fun downloadAttachmentFile(url: String, fileName: String, contentType: String): PickedAttachmentFile =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        PickedAttachmentFile(attachmentSaveName(fileName), contentType, downloadAttachmentBytes(url))
            .also { require(it.bytes.isNotEmpty()) { "El archivo está vacío" } }
    }
