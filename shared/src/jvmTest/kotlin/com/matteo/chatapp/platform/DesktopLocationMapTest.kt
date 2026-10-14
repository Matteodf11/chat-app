package com.matteo.chatapp.platform

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class DesktopLocationMapTest {
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()

    @Test fun pageStartsWithoutInventingLocationAndIncludesMapAttribution() {
        DesktopLocationMap(null, false).use { session ->
            val response = get(session.url)
            assertEquals(200, response.statusCode())
            assertTrue(response.body().contains("navigator.geolocation.getCurrentPosition"))
            assertTrue(response.body().contains("OpenStreetMap</a> contributors"))
            assertTrue(response.body().contains("if (false) locate()"))
            assertFalse(response.body().contains("__TOKEN__"))
            assertFalse(session.result.isCompleted)
        }
    }

    @Test fun validPointIsReturnedToAppAndCannotBeSubmittedTwice(): Unit = runBlocking {
        DesktopLocationMap(null, false).use { session ->
            assertEquals(200, post(session, "38.345|-0.481").statusCode())
            assertEquals(SharedLocation(38.345, -0.481), session.result.await())
            assertEquals(409, post(session, "0|0").statusCode())
        }
    }

    @Test fun invalidOrNonFiniteCoordinatesDoNotCompleteSelection() {
        DesktopLocationMap(null, false).use { session ->
            for (body in listOf("NaN|0", "91|0", "0|Infinity", "0|181", "0|0|0", "a|b")) {
                assertEquals(400, post(session, body).statusCode())
                assertFalse(session.result.isCompleted)
            }
        }
    }

    @Test fun unrelatedWebPagesCannotSubmitOrReadThePicker() {
        DesktopLocationMap(null, false).use { session ->
            assertEquals(403, post(session, "0|0", origin = "https://example.com").statusCode())
            assertEquals(403, post(session, "0|0", token = "wrong").statusCode())
            assertEquals(404, get(session.url.resolve("/unknown/")).statusCode())
            assertFalse(session.result.isCompleted)
        }
    }

    @Test fun cancelReturnsNoPointAndCloseStopsTheListener(): Unit = runBlocking {
        val session = DesktopLocationMap(null, false)
        val url = session.url
        assertEquals(200, post(session, "cancel").statusCode())
        assertNull(session.result.await())
        session.close(); session.close()
        assertFails { get(url) }
    }

    @Test fun initialPointAndAutomaticLookupAreExplicitAndSessionsAreIsolated() {
        DesktopLocationMap(SharedLocation(12.5, -1.25), true).use { first ->
            DesktopLocationMap(null, false).use { second ->
                assertNotEquals(first.url, second.url)
                val html = get(first.url).body()
                assertTrue(html.contains("setView([12.5, -1.25], true ? 15 : 5)"))
                assertTrue(html.contains("if (true) locate()"))
                assertEquals(404, get(URI(second.url.toString().replace(second.url.path, first.url.path))).statusCode())
            }
        }
    }

    private fun get(url: URI) = client.send(HttpRequest.newBuilder(url).timeout(Duration.ofSeconds(3)).GET().build(), HttpResponse.BodyHandlers.ofString())

    private fun post(session: DesktopLocationMap, body: String,
        origin: String = "http://${session.url.authority}", token: String = session.url.path.trim('/')) =
        client.send(HttpRequest.newBuilder(session.url.resolve("select")).timeout(Duration.ofSeconds(3))
            .header("Origin", origin).header("X-Chimera-Selection", token)
            .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString())
}
