package com.matteo.chatapp.platform

import kotlin.test.*

class LocationInputTest {
    private val point = SharedLocation(38.345, -0.481)
    @Test fun acceptsCoordinatePairsAndSpanishDecimalCommas() {
        for (input in listOf("38.345, -0.481", "38.345 -0.481", "38,345; -0,481", " 38.345;-0.481 "))
            assertEquals(point, locationFromInput(input), input)
        assertEquals(SharedLocation(0.0, 0.0), locationFromInput("0, 0"))
    }
    @Test fun googleMapsLinksResolveToCanonicalCoordinates() {
        assertEquals(point, locationFromInput(point.mapUrl))
        assertEquals(point, locationFromInput("https://maps.google.com/?q=38.345%2C-0.481"))
        assertEquals(point, locationFromInput("https://www.google.com/maps?q=38.345,-0.481"))
    }
    @Test fun rejectsAddressesViewportCentersShortLinksAndLookalikeHosts() {
        for (input in listOf("Alicante", "https://www.google.com/maps/@38.345,-0.481,15z",
            "https://maps.app.goo.gl/example", "https://evil.example/maps?q=38.345,-0.481",
            "https://www.google.com.evil.example/maps?q=38.345,-0.481",
            "https://user@www.google.com/maps?q=38.345,-0.481", "NaN,0", "91,0", "0,-181", "1,2,3",
            "https://www.google.com/maps?q=1,2&q=3,4", "https://www.google.com/maps?q=Alicante"))
            assertNull(locationFromInput(input), input)
    }
}
