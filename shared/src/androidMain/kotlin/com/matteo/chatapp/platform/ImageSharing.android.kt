package com.matteo.chatapp.platform

import android.content.ClipData
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChimeraFileProvider : FileProvider()

actual fun imageShareActionLabel(): String = "Compartir"

actual suspend fun shareImage(url: String, fileName: String, contentType: String): ImageShareResult {
    val context = AndroidPlatform.context
    val file = withContext(Dispatchers.IO) {
        prepareSharedImage(url, fileName, File(context.cacheDir, "shared-images"))
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.shared-images", file, attachmentSaveName(fileName))
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = contentType
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, fileName, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir imagen").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    return ImageShareResult.CHOOSER_OPENED
}
