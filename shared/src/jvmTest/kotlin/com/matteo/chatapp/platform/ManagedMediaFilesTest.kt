package com.matteo.chatapp.platform

import java.nio.file.Files
import kotlin.test.*

class ManagedMediaFilesTest {
    @Test fun cleaningOnlyRemovesAgedDirectTemporaryFiles() {
        val root = Files.createTempDirectory("chimera-cache-test").toFile()
        try {
            val cacheDir = root.resolve("cache").apply { mkdirs() }
            val now = System.currentTimeMillis()
            val old = cacheDir.resolve("old.jpg").apply { writeBytes(ByteArray(10)); setLastModified(now - 2L * 86400000) }
            val fresh = cacheDir.resolve("fresh.jpg").apply { writeBytes(ByteArray(20)) }
            val nested = cacheDir.resolve("drafts/file.bin").apply { parentFile.mkdirs(); writeBytes(ByteArray(30)) }
            val outside = root.resolve("download.jpg").apply { writeBytes(ByteArray(40)) }
            val managed = ManagedMediaFiles(listOf(cacheDir)) { now }
            assertEquals(TemporaryMediaStats(30, 10), managed.stats())
            managed.clearOld()
            assertFalse(old.exists())
            assertTrue(fresh.exists())
            assertTrue(nested.exists())
            assertTrue(outside.exists())
            assertEquals(TemporaryMediaStats(20, 0), managed.stats())
        } finally { root.deleteRecursively() }
    }
}
