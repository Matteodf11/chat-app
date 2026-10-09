package com.matteo.chatapp.ui.components

import kotlin.test.*

class PickedImageTest {
    @Test fun jpegOrientationHandlesBothByteOrdersAndBrokenMetadata() {
        for (little in listOf(true, false)) for (orientation in 1..8) {
            val data = ByteArray(40)
            data[0] = 255.toByte(); data[1] = 216.toByte()
            data[2] = 255.toByte(); data[3] = 225.toByte()
            data[4] = 0; data[5] = 34
            byteArrayOf(69,120,105,102,0,0).copyInto(data, 6)
            val base = 12
            data[base] = (if (little) 73 else 77).toByte(); data[base+1] = data[base]
            fun word(offset: Int, value: Int) {
                data[base+offset] = (if (little) value else value shr 8).toByte()
                data[base+offset+1] = (if (little) value shr 8 else value).toByte()
            }
            word(2,42)
            word(if (little) 4 else 6,8)
            word(8,1)
            word(10,274); word(12,3); word(if (little) 14 else 16,1)
            word(18,orientation)
            assertEquals(orientation, jpegOrientation(data))
            assertEquals(1, jpegOrientation(data.copyOf(20)))
            data[base+4] = 127; data[base+5] = 127
            assertEquals(1, jpegOrientation(data))
        }
        assertEquals(1, jpegOrientation(byteArrayOf()))
    }
}
