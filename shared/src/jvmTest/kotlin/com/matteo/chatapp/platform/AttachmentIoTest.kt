package com.matteo.chatapp.platform

import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class AttachmentIoTest {
    @Test fun streamLimitAlsoRejectsUnknownOrMisreportedSizes() = runTest {
        val input = object : InputStream() {
            var remaining = MAX_ATTACHMENT_BYTES + 1
            override fun read(): Int = if (remaining-- > 0) 1 else -1
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (remaining <= 0) return -1
                val count = minOf(length, remaining)
                remaining -= count
                return count
            }
        }
        assertFailsWith<IllegalArgumentException> { input.readAttachmentBytes() }
        assertContentEquals(byteArrayOf(1, 2, 3), ByteArrayInputStream(byteArrayOf(1, 2, 3)).readAttachmentBytes())
    }

    @Test fun suggestedFileNamesCannotEscapeTheSelectedDirectory() {
        assertEquals("foto.png", attachmentSaveName("../../foto.png"))
        assertEquals("foto.png", attachmentSaveName("C:\\secret\\foto.png"))
        assertEquals("archivo", attachmentSaveName(""))
        assertEquals("archivo", attachmentSaveName(".."))
        assertFalse(attachmentSaveName("test\nname?.png").contains('\n'))
        assertFalse(attachmentSaveName("test?.png").contains('?'))
    }
}
