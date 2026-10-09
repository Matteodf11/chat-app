package com.matteo.chatapp.platform

import kotlin.test.*

class SharedLocationTest {
    @Test fun locationRoundTripsWithNegativeCoordinates() {
        val point = SharedLocation(40.4168, -3.7038)
        assertEquals(point, sharedLocationFromMessage(point.message()))
        assertTrue(point.mapUrl.startsWith("https://www.google.com/maps/"))
    }

    @Test fun invalidCoordinatesAndLookalikeLinksDoNotBecomeLocations() {
        for ((lat, lon) in listOf(91.0 to 0.0, 0.0 to -181.0, Double.NaN to 0.0, 0.0 to Double.POSITIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> { SharedLocation(lat, lon) }
        }
        assertNull(sharedLocationFromMessage("Ubicación compartida\nhttps://example.com/?query=1,2"))
        assertNull(sharedLocationFromMessage(SharedLocation(1.0, 2.0).message() + "&redirect=evil"))
        assertNull(sharedLocationFromMessage("Ubicación compartida\nhttps://www.google.com/maps/search/?api=1&query=100,2"))
    }
}
