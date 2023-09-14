package xyz.wagyourtail.jvmdg.gradle

import org.gradle.api.JavaVersion
import org.objectweb.asm.Opcodes
import java.io.File
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


fun File.deleteIfExists() {
    if (exists()) {
        delete()
    }
}

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

fun jvToOpc(vers: JavaVersion): Int = when (vers) {
    JavaVersion.VERSION_1_1 -> Opcodes.V1_1
    JavaVersion.VERSION_1_2 -> Opcodes.V1_2
    JavaVersion.VERSION_1_3 -> Opcodes.V1_3
    JavaVersion.VERSION_1_4 -> Opcodes.V1_4
    JavaVersion.VERSION_1_5 -> Opcodes.V1_5
    JavaVersion.VERSION_1_6 -> Opcodes.V1_6
    JavaVersion.VERSION_1_7 -> Opcodes.V1_7
    JavaVersion.VERSION_1_8 -> Opcodes.V1_8
    JavaVersion.VERSION_1_9 -> Opcodes.V9
    JavaVersion.VERSION_1_10 -> Opcodes.V10
    JavaVersion.VERSION_11 -> Opcodes.V11
    JavaVersion.VERSION_12 -> Opcodes.V12
    JavaVersion.VERSION_13 -> Opcodes.V13
    JavaVersion.VERSION_14 -> Opcodes.V14
    JavaVersion.VERSION_15 -> Opcodes.V15
    JavaVersion.VERSION_16 -> Opcodes.V16
    JavaVersion.VERSION_17 -> Opcodes.V17
    else -> throw IllegalArgumentException("Unsupported Java Version: $vers")
}