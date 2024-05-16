package xyz.wagyourtail.jvmdg.gradle

import org.apache.commons.compress.archivers.zip.ZipFile
import org.gradle.api.JavaVersion
import org.objectweb.asm.Opcodes
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

fun File.deleteIfExists() {
    if (exists()) {
        delete()
    }
}

fun <T> Path.readZipInputStreamFor(path: String, throwIfMissing: Boolean = true, action: (InputStream) -> T): T {
    Files.newByteChannel(this).use {
        ZipFile.builder().setIgnoreLocalFileHeader(true).setSeekableByteChannel(it).get().use { zip ->
            val entry = zip.getEntry(path.replace("\\", "/"))
            if (entry != null) {
                return zip.getInputStream(entry).use(action)
            } else {
                if (throwIfMissing) {
                    throw IllegalArgumentException("Missing file $path in $this")
                }
            }
        }
    }
    return null as T
}

fun JavaVersion.toOpcode(): Int = when (this) {
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
    JavaVersion.VERSION_18 -> Opcodes.V18
    JavaVersion.VERSION_19 -> Opcodes.V19
    JavaVersion.VERSION_20 -> Opcodes.V20
    JavaVersion.VERSION_21 -> Opcodes.V21
    JavaVersion.VERSION_22 -> Opcodes.V22
    else -> throw IllegalArgumentException("Unsupported Java Version: $this")
}