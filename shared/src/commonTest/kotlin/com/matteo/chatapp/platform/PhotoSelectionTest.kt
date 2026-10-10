package com.matteo.chatapp.platform

import kotlin.test.*

class PhotoSelectionTest {
    @Test fun selfieFlipPreservesRotationAndCanBeUndoneForEveryExifOrientation() {
        val expected = listOf(2, 1, 4, 3, 6, 5, 8, 7)
        for (orientation in 1..8) {
            val flipped = horizontallyFlippedOrientation(orientation)
            assertEquals(expected[orientation - 1], flipped)
            assertEquals(orientation, horizontallyFlippedOrientation(flipped))
            assertEquals(orientation >= 5, flipped >= 5)
        }
        assertEquals(2, horizontallyFlippedOrientation(0))
    }

    @Test fun rejectsInvalidCountsFormatsAndEmptyImages() {
        val photo = PickedAttachmentFile("foto.jpg", "image/jpeg", byteArrayOf(1))
        validatePhotoSelection(List(10) { photo })
        assertFailsWith<IllegalArgumentException> { validatePhotoSelection(emptyList()) }
        assertFailsWith<IllegalArgumentException> { validatePhotoSelection(List(11) { photo }) }
        assertFailsWith<IllegalArgumentException> { validatePhotoSelection(listOf(photo.copy(contentType = "application/pdf"))) }
        assertFailsWith<IllegalArgumentException> { validatePhotoSelection(listOf(photo.copy(bytes = byteArrayOf()))) }
    }
    @Test fun rejectsSelectionWhoseCombinedSizeExceedsLimit() {
        val photo = PickedAttachmentFile("foto.jpg", "image/jpeg", ByteArray(18 * 1024 * 1024))
        validatePhotoSelection(listOf(photo, photo))
        assertFailsWith<IllegalArgumentException> { validatePhotoSelection(listOf(photo, photo, photo)) }
    }
}
