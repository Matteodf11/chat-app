package com.matteo.chatapp.ui.components

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.matteo.chatapp.platform.horizontallyFlippedOrientation
import java.awt.image.BufferedImage
import kotlin.test.*

class ImageOrientationTest {
    @Test fun everyExifRotationRendersCorrectlyAndFlippingMirrorsTheDisplayedRows() {
        val source = BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB)
        for (y in 0..1) for (x in 0..2) source.setRGB(x, y, 0xff000000.toInt() or ((y * 3 + x + 1) * 0x222222))
        val bitmap = source.toComposeImageBitmap()
        val expected = listOf(
            listOf(1,2,3,4,5,6), listOf(3,2,1,6,5,4),
            listOf(6,5,4,3,2,1), listOf(4,5,6,1,2,3),
            listOf(1,4,2,5,3,6), listOf(4,1,5,2,6,3),
            listOf(6,3,5,2,4,1), listOf(3,6,2,5,1,4))
        for (orientation in 1..8) {
            val image = orientPickedImage(bitmap, metadata(orientation)).toAwtImage()
            val width = if (orientation >= 5) 2 else 3
            assertEquals(width, image.width)
            assertEquals(if (orientation >= 5) 3 else 2, image.height)
            fun values(image: BufferedImage) = (0 until image.height).flatMap { y ->
                (0 until image.width).map { x -> (image.getRGB(x, y) and 0xffffff) / 0x222222 }
            }
            assertEquals(expected[orientation - 1], values(image), "Orientation $orientation")
            val flipped = orientPickedImage(bitmap, metadata(horizontallyFlippedOrientation(orientation))).toAwtImage()
            assertEquals(expected[orientation - 1].chunked(width).flatMap { it.reversed() }, values(flipped),
                "Selfie flip must preserve the upright rotation for orientation $orientation")
        }
    }

    private fun metadata(orientation: Int): ByteArray = byteArrayOf(
        -1,-40,-1,-31,0,34,69,120,105,102,0,0,73,73,42,0,8,0,0,0,
        1,0,18,1,3,0,1,0,0,0,orientation.toByte(),0,0,0,0,0,0,0)
}
