package com.matteo.chatapp.platform

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual fun imageShareActionLabel(): String = "Copiar imagen"

actual suspend fun shareImage(url: String, fileName: String, contentType: String): ImageShareResult = withContext(Dispatchers.IO) {
    val file = prepareSharedImage(url, fileName, File(System.getProperty("java.io.tmpdir"), "chimera-shared-images"))
    // Include both an image and the original file: receivers choose the format they support.
    val image = ImageIO.read(file) ?: org.jetbrains.skia.Image.makeFromEncoded(file.readBytes()).use { source ->
        val png = source.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)?.bytes
            ?: error("No se pudo copiar la imagen")
        ImageIO.read(ByteArrayInputStream(png)) ?: error("No se pudo copiar la imagen")
    }
    val transferable = object : Transferable {
        override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor, DataFlavor.javaFileListFlavor)
        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == DataFlavor.imageFlavor || flavor == DataFlavor.javaFileListFlavor
        override fun getTransferData(flavor: DataFlavor): Any = when (flavor) {
            DataFlavor.imageFlavor -> image
            DataFlavor.javaFileListFlavor -> listOf(file)
            else -> throw UnsupportedFlavorException(flavor)
        }
    }
    Toolkit.getDefaultToolkit().systemClipboard.setContents(transferable, null)
    ImageShareResult.COPIED
}
