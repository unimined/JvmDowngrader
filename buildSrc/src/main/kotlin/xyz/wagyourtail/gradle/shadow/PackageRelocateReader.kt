package xyz.wagyourtail.gradle.shadow

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import xyz.wagyourtail.gradle.MustSet
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FilterReader
import java.io.Reader
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets

class PackageRelocateReader(input: Reader): FilterReader(input) {

    var remapper: PackageRelocator by MustSet()

    val contents = ByteArrayOutputStream().use { out ->
        out.writer(StandardCharsets.ISO_8859_1).use { writer ->
            input.copyTo(writer)
        }
        input.close()
        out.toByteArray()
    }
    
    val changedContents: Reader by lazy {
        val reader = ClassReader(contents)
        val writer = ClassWriter(0)
        reader.accept(ClassRemapper(writer, remapper), 0)
        ByteArrayInputStream(writer.toByteArray()).bufferedReader(StandardCharsets.ISO_8859_1)
    }

    override fun read(): Int {
        return changedContents.read()
    }

    override fun read(cbuf: CharArray, off: Int, len: Int): Int {
        return changedContents.read(cbuf, off, len)
    }

    override fun skip(n: Long): Long {
        return changedContents.skip(n)
    }

    override fun ready(): Boolean {
        return changedContents.ready()
    }

    override fun markSupported(): Boolean {
        return changedContents.markSupported()
    }

    override fun mark(readAheadLimit: Int) {
        changedContents.mark(readAheadLimit)
    }

    override fun reset() {
        changedContents.reset()
    }

    override fun close() {
        changedContents.close()
    }

}
