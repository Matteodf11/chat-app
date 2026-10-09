package com.matteo.chatapp.platform

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

internal object AndroidAttachmentFilePicker {
    private var launcher: ActivityResultLauncher<Array<String>>? = null
    private var photoLauncher: ActivityResultLauncher<PickVisualMediaRequest>? = null
    private var pendingResult: ((Uri?) -> Unit)? = null

    fun initialize(activity: ComponentActivity) {
        photoLauncher = activity.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            val callback = pendingResult
            pendingResult = null
            callback?.invoke(uri)
        }
        launcher = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val callback = pendingResult
            pendingResult = null
            callback?.invoke(uri)
        }
    }

    suspend fun pick(photo: Boolean = false): PickedAttachmentFile? {
        val uri = suspendCancellableCoroutine<Uri?> { continuation ->
            val activeLauncher = launcher
            if (activeLauncher == null || pendingResult != null) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }
            pendingResult = { result -> if (continuation.isActive) continuation.resume(result) }
            continuation.invokeOnCancellation { pendingResult = null }
            if (photo) photoLauncher?.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            else activeLauncher.launch(ALLOWED_TYPES.toTypedArray())
        } ?: return null

        return withContext(Dispatchers.IO) {
            val resolver = AndroidPlatform.context.contentResolver
            val contentType = resolver.getType(uri)?.lowercase() ?: return@withContext null
            require(contentType in ALLOWED_TYPES) { "Formato no compatible. Usa JPG, PNG, WebP o GIF para las imágenes." }
            val bytes = resolver.openInputStream(uri)?.use { it.readAttachmentBytes() } ?: return@withContext null
            require(bytes.isNotEmpty()) { "El archivo está vacío" }
            PickedAttachmentFile(displayName(uri) ?: "archivo", contentType, bytes)
        }
    }

    private fun displayName(uri: Uri): String? {
        val resolver = AndroidPlatform.context.contentResolver
        return resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) null else cursor.getString(0)
        }
    }

    private val ALLOWED_TYPES = setOf(
        "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif",
        "application/pdf", "text/plain", "text/csv", "application/zip",
        "application/x-zip-compressed", "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    )
}

actual suspend fun pickAttachmentFile(): PickedAttachmentFile? = AndroidAttachmentFilePicker.pick()

actual fun openExternalUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    AndroidPlatform.context.startActivity(intent)
}

actual suspend fun pickPhotoAttachment(): PickedAttachmentFile? = AndroidAttachmentFilePicker.pick(photo = true)
