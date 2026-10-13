package com.matteo.chatapp.platform

internal data class TemporaryMediaStats(val bytes: Long = 0, val clearableBytes: Long = 0)
internal expect suspend fun temporaryMediaStats(): TemporaryMediaStats
internal expect suspend fun clearOldTemporaryMedia()
