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

expect suspend fun saveAttachment(url: String, fileName: String): Boolean

expect fun supportsCurrentLocation(): Boolean
expect suspend fun currentLocation(): SharedLocation

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
