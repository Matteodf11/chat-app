package com.matteo.chatapp.platform

import android.media.ExifInterface
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@androidx.compose.runtime.Composable
actual fun PrepareCameraCapture() = Unit

actual suspend fun flipCameraPhoto(file: PickedAttachmentFile): PickedAttachmentFile = withContext(Dispatchers.IO) {
    require(file.contentType == "image/jpeg" || file.contentType == "image/jpg") { "La captura debe ser una foto JPEG" }
    val directory = File(AndroidPlatform.context.cacheDir, "camera-captures").apply { mkdirs() }
    val temporary = File.createTempFile("capture-flip-", ".jpg", directory)
    try {
        temporary.writeBytes(file.bytes)
        val exif = ExifInterface(temporary.path)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, horizontallyFlippedOrientation(orientation).toString())
        // Update metadata only: the camera's JPEG pixels are not recompressed.
        exif.saveAttributes()
        file.copy(bytes = temporary.inputStream().use { it.readAttachmentBytes() })
            .also { validatePhotoSelection(listOf(it)) }
    } finally { temporary.delete() }
}
