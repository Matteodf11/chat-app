package com.matteo.chatapp.platform

import kotlin.test.*

class PhotoSelectionTest {
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
