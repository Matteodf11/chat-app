package com.matteo.chatapp.platform

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun managedMedia() = ManagedMediaFiles(listOf("shared-images", "camera-captures").map { File(AndroidPlatform.context.cacheDir, it) })
internal actual suspend fun temporaryMediaStats() = withContext(Dispatchers.IO) { managedMedia().stats() }
internal actual suspend fun clearOldTemporaryMedia() = withContext(Dispatchers.IO) { managedMedia().clearOld() }
