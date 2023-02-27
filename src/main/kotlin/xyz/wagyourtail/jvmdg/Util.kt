package xyz.wagyourtail.jvmdowngrader

import java.io.InputStream
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

fun forEachInZip(zip: Path, action: (String, InputStream) -> Unit) {
    ZipInputStream(zip.inputStream()).use { stream ->
        var entry = stream.nextEntry
        while (entry != null) {
            if (entry.isDirectory) {
                entry = stream.nextEntry
                continue
            }
            action(entry.name, stream)
            entry = stream.nextEntry
        }
    }
}

fun openZipFileSystem(path: Path, args: Map<String, *> = mapOf<String, Any>()): FileSystem {
    if (!Files.exists(path) && args["create"] == true) {
        ZipOutputStream(path.outputStream()).use { stream ->
            stream.closeEntry()
        }
    }
    return FileSystems.newFileSystem(URI.create("jar:${path.toUri()}"), args, null)
}