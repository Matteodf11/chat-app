package com.matteo.chatapp.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint

internal expect suspend fun decodePickedImage(bytes: ByteArray, maxEdge: Int): ImageBitmap?

/** Read only the bounded JPEG APP1/TIFF orientation; malformed metadata is ignored. */
internal fun jpegOrientation(bytes: ByteArray): Int = runCatching {
    fun u(i: Int) = bytes[i].toInt() and 255
    if (bytes.size < 4 || u(0) != 255 || u(1) != 216) return 1
    var pos = 2
    while (pos + 4 <= bytes.size && u(pos) == 255) {
        val marker = u(pos + 1)
        if (marker == 218 || marker == 217) break
        if (marker == 255) { pos++; continue }
        val length = (u(pos + 2) shl 8) or u(pos + 3)
        val end = pos + 2 + length
        if (length < 2 || end > bytes.size) break
        val start = pos + 4
        if (marker == 225 && length >= 16 && bytes.copyOfRange(start, start + 6).contentEquals(byteArrayOf(69,120,105,102,0,0))) {
            val base = start + 6
            val little = u(base) == 73 && u(base + 1) == 73
            if (!little && !(u(base) == 77 && u(base + 1) == 77)) return 1
            fun word(i: Int): Int {
                require(i >= base && i + 2 <= end)
                return if (little) u(i) or (u(i+1) shl 8) else (u(i) shl 8) or u(i+1)
            }
            fun dword(i: Int): Long = if (little) word(i).toLong() or (word(i+2).toLong() shl 16)
                else (word(i).toLong() shl 16) or word(i+2).toLong()
            if (word(base+2) != 42) return 1
            val offset = dword(base+4)
            if (offset > (end - base - 2).toLong()) return 1
            val table = base + offset.toInt()
            val count = word(table)
            for (index in 0 until count) {
                val item = table + 2 + index * 12
                require(item + 12 <= end)
                if (word(item) == 274 && word(item+2) == 3 && dword(item+4) == 1L)
                    return word(item+8).takeIf { it in 1..8 } ?: 1
            }
        }
        pos = end
    }
    1
}.getOrDefault(1)

internal fun orientPickedImage(image: ImageBitmap, bytes: ByteArray): ImageBitmap {
    val orientation = jpegOrientation(bytes)
    if (orientation == 1) return image
    val swap = orientation >= 5
    val result = ImageBitmap(if (swap) image.height else image.width, if (swap) image.width else image.height)
    val canvas = Canvas(result)
    val w = image.width.toFloat()
    val h = image.height.toFloat()
    when (orientation) {
        2 -> { canvas.translate(w, 0f); canvas.scale(-1f, 1f) }
        3 -> { canvas.translate(w, h); canvas.rotate(180f) }
        4 -> { canvas.translate(0f, h); canvas.scale(1f, -1f) }
        5 -> { canvas.rotate(90f); canvas.scale(1f, -1f) }
        6 -> { canvas.translate(h, 0f); canvas.rotate(90f) }
        7 -> { canvas.translate(h, w); canvas.rotate(90f); canvas.scale(-1f, 1f) }
        8 -> { canvas.translate(0f, w); canvas.rotate(-90f) }
    }
    canvas.drawImage(image, androidx.compose.ui.geometry.Offset.Zero, Paint())
    return result
}
