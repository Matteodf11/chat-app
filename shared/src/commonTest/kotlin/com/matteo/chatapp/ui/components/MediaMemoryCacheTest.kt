package com.matteo.chatapp.ui.components

import kotlinx.coroutines.test.runTest
import kotlin.test.*

class MediaMemoryCacheTest {
    @Test fun leastRecentlyViewedImageIsEvictedWithinBudget() = runTest {
        val cache = MediaMemoryCache<String>(10)
        val generation = cache.generation()
        cache.put("a", "A", 4, generation)
        cache.put("b", "B", 4, generation)
        assertEquals("A", cache.get("a"))
        cache.put("c", "C", 4, generation)
        assertNull(cache.get("b"))
        assertNull(cache.peek("b"))
        assertEquals("A", cache.get("a"))
        assertEquals("C", cache.peek("c"))
        assertEquals("C", cache.get("c"))
        assertEquals(8L, cache.bytes.value)
        cache.put("large", "Too big", 11, generation)
        assertNull(cache.get("large"))
        assertEquals(8L, cache.bytes.value)
    }
    @Test fun cleaningDuringDownloadDoesNotRepopulateWithOldWork() = runTest {
        val cache = MediaMemoryCache<String>(10)
        val before = cache.generation()
        cache.put("a", "A", 4, before)
        cache.clear()
        cache.put("late", "Stale", 4, before)
        assertNull(cache.get("late"))
        assertNull(cache.peek("a"))
        assertNull(cache.peek("late"))
        assertEquals(0L, cache.bytes.value)
        cache.put("fresh", "New", 3, cache.generation())
        assertEquals("New", cache.get("fresh"))
        assertEquals(3L, cache.bytes.value)
    }
}
