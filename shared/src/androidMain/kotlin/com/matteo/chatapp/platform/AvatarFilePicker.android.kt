package com.matteo.chatapp.platform

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

internal object AndroidProfileImagePicker {
    private var launcher: ActivityResultLauncher<String>? = null
    private var pendingResult: ((Uri?) -> Unit)? = null

    fun initialize(activity: ComponentActivity) {
        launcher = activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            pendingResult?.invoke(uri)
            pendingResult = null
        }
    }

    suspend fun pick(): PickedAvatarFile? {
        val uri = suspendCancellableCoroutine<Uri?> { continuation ->
            val activeLauncher = launcher
            if (activeLauncher == null || pendingResult != null) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }
            pendingResult = { result ->
                if (continuation.isActive) continuation.resume(result)
            }
            continuation.invokeOnCancellation { pendingResult = null }
            activeLauncher.launch("image/*")
        } ?: return null

        return withContext(Dispatchers.IO) {
            val resolver = AndroidPlatform.context.contentResolver
            val contentType = resolver.getType(uri)?.lowercase() ?: return@withContext null
            if (contentType !in setOf("image/jpeg", "image/jpg", "image/png", "image/webp")) return@withContext null
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
            if (bytes.isEmpty() || bytes.size > 2 * 1024 * 1024) return@withContext null
            PickedAvatarFile(
                fileName = displayName(uri) ?: "profile-image.${extensionFor(contentType)}",
                contentType = contentType,
                bytes = bytes
            )
        }
    }

    private fun displayName(uri: Uri): String? {
        val resolver = AndroidPlatform.context.contentResolver
        return resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) null else cursor.getString(0)
        }
    }

    private fun extensionFor(contentType: String): String = when (contentType) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> "jpg"
    }
}

actual suspend fun pickAvatarFile(): PickedAvatarFile? = AndroidProfileImagePicker.pick()

actual suspend fun pickProfileImageFile(title: String): PickedAvatarFile? = AndroidProfileImagePicker.pick()
