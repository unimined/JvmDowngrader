package xyz.wagyourtail.jvmdg.util

import org.apache.commons.compress.archivers.zip.ZipFile
import org.gradle.api.JavaVersion
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.InputStream
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.zip.ZipOutputStream
import kotlin.Pair
import kotlin.io.path.exists
import kotlin.io.path.outputStream

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

fun JavaVersion.sym() = this.majorVersion.toInt().toString(36).uppercase()

fun Path.openZipFileSystem(vararg args: Pair<String, Any>): FileSystem {
    return openZipFileSystem(args.associate { it })
}

fun Path.openZipFileSystem(args: Map<String, *> = mapOf<String, Any>()): FileSystem {
    if (!exists() && args["create"] == true) {
        ZipOutputStream(outputStream()).use { stream ->
            stream.closeEntry()
        }
    }

    return FileSystems.newFileSystem(URI.create("jar:${toUri()}"), args, null)
}

fun InputStream.readClass(flags: Int = ClassReader.SKIP_CODE): ClassNode {
    return use {
        val cr = ClassReader(it)
        val cn = ClassNode()
        cr.accept(cn, flags)
        cn
    }
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
    JavaVersion.VERSION_23 -> Opcodes.V23
    else -> throw IllegalArgumentException("Unsupported Java Version: $this")
}

operator fun JavaVersion.rangeTo(that: JavaVersion): Array<JavaVersion> {
    return JavaVersion.entries.toTypedArray().copyOfRange(this.ordinal, that.ordinal + 1)
}

val CONSTANT_TIME_FOR_ZIP_ENTRIES = GregorianCalendar(1980, Calendar.FEBRUARY, 1, 0, 0, 0).timeInMillis


fun safeName(name: String): String = name.replace(Regex("[.;\\[/]"), "-")