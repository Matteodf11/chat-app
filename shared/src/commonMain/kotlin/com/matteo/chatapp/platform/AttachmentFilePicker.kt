package com.matteo.chatapp.platform

data class PickedAttachmentFile(
    val fileName: String,
    val contentType: String,
    val bytes: ByteArray
) {
    val size: Long get() = bytes.size.toLong()
}

expect suspend fun pickAttachmentFile(): PickedAttachmentFile?
expect suspend fun pickPhotoAttachment(): PickedAttachmentFile?
expect suspend fun pickPhotoAttachments(): List<PickedAttachmentFile>
expect fun supportsPhotoCapture(): Boolean
expect suspend fun capturePhoto(): PickedAttachmentFile?
@androidx.compose.runtime.Composable
expect fun PrepareCameraCapture()
expect suspend fun flipCameraPhoto(file: PickedAttachmentFile): PickedAttachmentFile

/** Mirror the displayed image horizontally, including portraits with rotated pixels. */
internal fun horizontallyFlippedOrientation(orientation: Int): Int = when (orientation) {
    1 -> 2
    2 -> 1
    3 -> 4
    4 -> 3
    5 -> 6
    6 -> 5
    7 -> 8
    8 -> 7
    else -> 2
}

const val MAX_SELECTED_PHOTOS = 10
const val MAX_PHOTO_SELECTION_BYTES = 50L * 1024 * 1024
fun validatePhotoSelection(files: List<PickedAttachmentFile>) {
    require(files.size in 1..MAX_SELECTED_PHOTOS) { "Selecciona entre 1 y 10 fotos" }
    require(files.all { it.contentType in setOf("image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif") && it.size in 1..(25L * 1024 * 1024) }) {
        "Usa imágenes JPG, PNG, WebP o GIF de hasta 25 MB"
    }
    require(files.sumOf { it.size } <= MAX_PHOTO_SELECTION_BYTES) { "Las fotos seleccionadas superan 50 MB" }
}

expect suspend fun downloadAttachmentFile(url: String, fileName: String, contentType: String): PickedAttachmentFile

expect suspend fun saveAttachment(url: String, fileName: String): Boolean
expect fun imageShareActionLabel(): String
enum class ImageShareResult { CHOOSER_OPENED, COPIED }
expect suspend fun shareImage(url: String, fileName: String, contentType: String): ImageShareResult

expect fun supportsCurrentLocation(): Boolean
expect suspend fun currentLocation(): SharedLocation
expect fun openLocationSettings()
expect fun supportsLocationMap(): Boolean
expect suspend fun chooseLocationOnMap(initial: SharedLocation? = null): SharedLocation?

data class SharedLocation(val latitude: Double, val longitude: Double) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0 && longitude.isFinite() && longitude in -180.0..180.0) {
            "Introduce unas coordenadas válidas"
        }
    }
    val mapUrl: String get() = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
    fun message(): String = "Ubicación compartida\n$mapUrl"
}

fun sharedLocationFromMessage(content: String): SharedLocation? {
    val prefix = "Ubicación compartida\nhttps://www.google.com/maps/search/?api=1&query="
    if (!content.startsWith(prefix)) return null
    val parts = content.removePrefix(prefix).split(',')
    if (parts.size != 2) return null
    return runCatching { SharedLocation(parts[0].toDouble(), parts[1].toDouble()) }.getOrNull()
}

expect fun openExternalUrl(url: String)
