package com.matteo.chatapp.platform

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class WindowsLocationTest {
    @Test fun parsesInvariantCoordinatesAndRejectsInvalidProviderResponses() {
        assertEquals(SharedLocation(38.345, -0.481), parseLocationResult("OK|38.345|-0.481\r\n"))
        for (output in listOf("", "OK|NaN|0", "OK|91|0", "OK|0|Infinity", "OK|38,3|-0,4", "DENIED", "UNAVAILABLE", "noise\nOK|1|2"))
            assertFailsWith<RuntimeException> { parseLocationResult(output) }
    }
    @Test fun providerFailureClosesProcessWithoutLeakingItsOutput() = runTest {
        val process = FakeProcess(false, "DENIED")
        val error = assertFailsWith<IllegalStateException> { awaitLocationProcess(process) }
        assertTrue(error.message.orEmpty().contains("Windows"))
        assertEquals(1, process.closes)
    }
    @Test fun cancellationStopsOutstandingLocationLookup() = runTest {
        val process = FakeProcess(true, "")
        val job = launch { awaitLocationProcess(process) }
        runCurrent(); job.cancelAndJoin()
        assertEquals(1, process.closes)
        assertFalse(process.isAlive)
    }
    @Test fun timeoutStopsLookupAndCompletedResultClosesItToo() = runTest {
        val stalled = FakeProcess(true, "")
        assertFailsWith<TimeoutCancellationException> { awaitLocationProcess(stalled, 250) }
        assertEquals(1, stalled.closes)
        val completed = FakeProcess(false, "OK|0|0")
        assertEquals(SharedLocation(0.0, 0.0), awaitLocationProcess(completed))
        assertEquals(1, completed.closes)
    }
    private class FakeProcess(override var isAlive: Boolean, private val text: String) : LocationProcess {
        var closes = 0
        override fun output() = text
        override fun close() { closes++; isAlive = false }
    }
}
