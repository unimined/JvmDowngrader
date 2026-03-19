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

fun JavaVersion.toOpcode(): Int = if (this == JavaVersion.VERSION_1_1) Opcodes.V1_1 else Opcodes.V1_2 - 2 + this.majorVersion.toInt()

operator fun JavaVersion.rangeTo(that: JavaVersion): Array<JavaVersion> {
    return JavaVersion.entries.toTypedArray().copyOfRange(this.ordinal, that.ordinal + 1)
}

operator fun JavaVersion.plus(int: Int): JavaVersion {
    return JavaVersion.entries[this.ordinal + int]
}

val CONSTANT_TIME_FOR_ZIP_ENTRIES = GregorianCalendar(1980, Calendar.FEBRUARY, 1, 0, 0, 0).timeInMillis

fun safeName(name: String): String = name.replace(Regex("[.;\\[/]"), "-")