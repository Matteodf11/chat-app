package com.matteo.chatapp.platform

import java.io.File

/** Only direct children of our dedicated temporary directories; never follows links outside them. */
internal class ManagedMediaFiles(private val directories: List<File>, private val now: () -> Long = System::currentTimeMillis) {
    private fun files() = directories.flatMap { directory ->
        val root = directory.canonicalFile
        directory.listFiles().orEmpty().filter { it.isFile && it.canonicalFile.parentFile == root }
    }
    private fun old(file: File) = file.lastModified() > 0 && file.lastModified() < now() - 24L * 60 * 60 * 1000
    fun stats(): TemporaryMediaStats {
        val files = files()
        return TemporaryMediaStats(files.sumOf { it.length() }, files.filter(::old).sumOf { it.length() })
    }
    fun clearOld() { files().filter(::old).forEach { check(it.delete() || !it.exists()) { "No se pudo limpiar un archivo temporal" } } }
}
