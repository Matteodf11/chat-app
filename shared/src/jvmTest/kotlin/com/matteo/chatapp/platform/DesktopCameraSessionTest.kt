package com.matteo.chatapp.platform

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopCameraSessionTest {
    @Test fun bundledDriverLoadsWithoutJavaFxOrLegacyBridj() {
        val driver = com.github.eduramiba.webcamcapture.drivers.NativeDriver()
        assertEquals("com.github.eduramiba.webcamcapture.drivers.NativeDriver", driver.javaClass.name)
    }
    @Test fun switchingClosesPreviousAndLateCleanupCannotCloseNewCamera() = runTest {
        val first = FakeCamera(); val second = FakeCamera()
        val session = DesktopCameraSession(StandardTestDispatcher(testScheduler))
        val firstOwner = Any(); val secondOwner = Any()
        session.open(first, firstOwner)
        session.open(second, secondOwner)
        assertEquals(1, first.closed)
        session.close(firstOwner)
        assertEquals(0, second.closed)
        assertNull(session.frame(firstOwner))
        assertNotNull(session.frame(secondOwner))
        session.close(secondOwner); session.close()
        assertEquals(1, second.closed)
    }
    @Test fun failedOpenReleasesPartiallyOpenedDeviceAndAllowsRetry() = runTest {
        val camera = FakeCamera().apply { fail = true }
        val session = DesktopCameraSession(StandardTestDispatcher(testScheduler))
        assertFailsWith<IllegalStateException> { session.open(camera) }
        assertEquals(1, camera.closed)
        camera.fail = false
        session.open(camera); assertNotNull(session.frame()); session.close()
        assertEquals(2, camera.closed)
    }
    @Test fun disposePreventsLateOpensAndFramesAreIndependentSnapshots() = runTest {
        val camera = FakeCamera()
        val session = DesktopCameraSession(StandardTestDispatcher(testScheduler))
        session.open(camera)
        val snapshot = session.frame()!!
        camera.image.setRGB(0, 0, Color.BLUE.rgb)
        assertEquals(Color.RED.rgb, snapshot.getRGB(0, 0))
        session.dispose(); session.close()
        assertNull(session.frame())
        assertFailsWith<IllegalStateException> { session.open(camera) }
        assertEquals(1, camera.closed)
    }
    @Test fun jpegPreservesOrientationAndExplicitFlipMirrorsThePhoto() = runTest {
        val image = BufferedImage(80, 40, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = Color.RED; graphics.fillRect(0, 0, 40, 40)
        graphics.color = Color.BLUE; graphics.fillRect(40, 0, 40, 40); graphics.dispose()
        val photo = cameraJpeg(image)
        assertEquals("image/jpeg", photo.contentType)
        val original = ImageIO.read(ByteArrayInputStream(photo.bytes))
        assertTrue(Color(original.getRGB(10, 20)).red > 200)
        val flipped = flipCameraPhoto(photo)
        val decoded = ImageIO.read(ByteArrayInputStream(flipped.bytes))
        assertTrue(Color(decoded.getRGB(10, 20)).blue > 200)
        assertEquals(photo.fileName, flipped.fileName)
        assertEquals(80, decoded.width); assertEquals(40, decoded.height)
    }
    private class FakeCamera : DesktopCameraDevice {
        override val name = "Test camera"
        var closed = 0; var fail = false
        val image = BufferedImage(4, 2, BufferedImage.TYPE_INT_RGB).apply { setRGB(0, 0, Color.RED.rgb) }
        override fun open() { if (fail) error("busy") }
        override fun frame() = image
        override fun close() { closed++ }
    }
}
