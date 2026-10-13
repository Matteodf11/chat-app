package com.matteo.chatapp.platform

import io.ktor.http.Url

/** Accept coordinates, never an address or the camera position of a map viewport. */
fun locationFromInput(raw: String): SharedLocation? {
    val text = raw.trim().takeIf { it.length in 1..2048 } ?: return null
    val coordinates = if (text.startsWith("https://", ignoreCase = true)) {
        val url = runCatching { Url(text) }.getOrNull() ?: return null
        val host = url.host.lowercase()
        if (host !in setOf("google.com", "www.google.com", "maps.google.com") ||
            !url.user.isNullOrEmpty() || !url.password.isNullOrEmpty() ||
            (host != "maps.google.com" && url.encodedPath != "/maps" && !url.encodedPath.startsWith("/maps/"))) return null
        (url.parameters.getAll("query").orEmpty() + url.parameters.getAll("q").orEmpty()).singleOrNull() ?: return null
    } else text
    val pair = if (';' in coordinates) coordinates.split(';').map { it.trim().replace(',', '.') }
        else coordinates.split(Regex("\\s*,\\s*|\\s+")).filter { it.isNotEmpty() }
    if (pair.size != 2) return null
    val latitude = pair[0].toDoubleOrNull() ?: return null
    val longitude = pair[1].toDoubleOrNull() ?: return null
    return runCatching { SharedLocation(latitude, longitude) }.getOrNull()
}
