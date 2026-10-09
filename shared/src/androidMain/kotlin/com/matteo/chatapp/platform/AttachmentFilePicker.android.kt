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

    private var multipleLauncher: ActivityResultLauncher<PickVisualMediaRequest>? = null
    private var multipleResult: ((List<Uri>) -> Unit)? = null
    private var cameraLauncher: ActivityResultLauncher<Uri>? = null
    private var cameraResult: ((Boolean) -> Unit)? = null
    private var cameraFile: java.io.File? = null

    fun initialize(activity: ComponentActivity) {
        multipleLauncher = activity.registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_SELECTED_PHOTOS)) {
            val callback = multipleResult
            multipleResult = null
            callback?.invoke(it)
        }
        cameraLauncher = activity.registerForActivityResult(object : ActivityResultContracts.TakePicture() {
            override fun createIntent(context: android.content.Context, input: Uri): Intent =
                super.createIntent(context, input).apply {
                    clipData = android.content.ClipData.newRawUri("Foto", input)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
        }) { saved ->
            val callback = cameraResult
            cameraResult = null
            if (callback != null) callback(saved)
            else { cameraFile?.delete(); cameraFile = null }
        }
        java.io.File(AndroidPlatform.context.cacheDir, "camera-captures").listFiles()?.filter {
            it.name.startsWith("capture-") && it.lastModified() < System.currentTimeMillis() - 86_400_000
        }?.forEach { it.delete() }

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
            if (activeLauncher == null || pendingResult != null || multipleResult != null || cameraFile != null) {
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

    suspend fun photos(): List<PickedAttachmentFile> {
        val uris = suspendCancellableCoroutine<List<Uri>> { continuation ->
            val activeLauncher = multipleLauncher
            check(activeLauncher != null && pendingResult == null && multipleResult == null && cameraFile == null) { "Ya hay un selector abierto" }
            multipleResult = { if (continuation.isActive) continuation.resume(it) }
            continuation.invokeOnCancellation { multipleResult = null }
            try { activeLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            catch (e: Exception) { multipleResult = null; throw e }
        }
        if (uris.isEmpty()) return emptyList()
        require(uris.size <= MAX_SELECTED_PHOTOS) { "Puedes seleccionar hasta 10 fotos" }
        return withContext(Dispatchers.IO) {
            val resolver = AndroidPlatform.context.contentResolver
            var total = 0L
            uris.map { uri ->
                val type = resolver.getType(uri)?.lowercase().orEmpty()
                require(type in ALLOWED_TYPES && type.startsWith("image/")) { "Formato de imagen no compatible" }
                val bytes = resolver.openInputStream(uri)?.use { it.readAttachmentBytes() } ?: error("No se pudo leer la foto")
                total += bytes.size
                require(total <= MAX_PHOTO_SELECTION_BYTES) { "Las fotos seleccionadas superan 50 MB" }
                PickedAttachmentFile(displayName(uri) ?: "foto", type, bytes)
            }.also(::validatePhotoSelection)
        }
    }

    suspend fun camera(): PickedAttachmentFile? {
        val activeLauncher = cameraLauncher ?: error("No se pudo abrir la cámara")
        check(cameraFile == null && pendingResult == null && multipleResult == null) { "Ya hay un selector abierto" }
        val folder = java.io.File(AndroidPlatform.context.cacheDir, "camera-captures").apply { mkdirs() }
        val file = java.io.File.createTempFile("capture-", ".jpg", folder)
        cameraFile = file
        val saved = suspendCancellableCoroutine<Boolean> { continuation ->
            cameraResult = { if (continuation.isActive) continuation.resume(it) }
            continuation.invokeOnCancellation { cameraResult = null }
            try {
                val uri = androidx.core.content.FileProvider.getUriForFile(AndroidPlatform.context, "${AndroidPlatform.context.packageName}.shared-images", file)
                activeLauncher.launch(uri)
            } catch (e: Exception) {
                cameraResult = null; cameraFile = null; file.delete()
                continuation.resumeWith(Result.failure(IllegalStateException("No se pudo abrir una aplicación de cámara", e)))
            }
        }
        return try {
            if (!saved) null else withContext(Dispatchers.IO) {
                PickedAttachmentFile("foto-${System.currentTimeMillis()}.jpg", "image/jpeg", file.inputStream().use { it.readAttachmentBytes() })
                    .also { validatePhotoSelection(listOf(it)) }
            }
        } finally { cameraFile = null; file.delete() }
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

actual suspend fun pickPhotoAttachments(): List<PickedAttachmentFile> = AndroidAttachmentFilePicker.photos()
actual fun supportsPhotoCapture(): Boolean = true
actual suspend fun capturePhoto(): PickedAttachmentFile? = AndroidAttachmentFilePicker.camera()
