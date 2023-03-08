package xyz.wagyourtail.jvmdg.internal

import org.objectweb.asm.Opcodes
import xyz.wagyourtail.jvmdg.VersionProvider
import xyz.wagyourtail.jvmdg.standalone.ClassDowngrader
import xyz.wagyourtail.jvmdg.standalone.classloader.DowngradingClassLoader
import xyz.wagyourtail.jvmdg.test.JavaRunner
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.nameWithoutExtension
import kotlin.test.Test
import kotlin.test.assertEquals


class JvmDowngraderTest {

    val original = Path.of("../downgradetest/build/libs/downgradetest-1.0.0.jar")

    val downgraded by lazy {
        if (!original.parent.resolve(original.nameWithoutExtension + "-downgraded-8.jar").exists()) {
            original.parent.resolve(original.nameWithoutExtension + "-downgraded-8.jar").apply {
                val downgrader = ClassDowngrader(Opcodes.V1_8)
                downgrader.downgradeZip(original.toFile(), this.toFile())
            }
        }
        original.parent.resolve(original.nameWithoutExtension + "-downgraded-8.jar")
    }

    val jvmdowngrader = Path.of("./build/libs/jvmdowngrader-standalone-0.0.1.jar")

    val downgradedJvmDowngrader by lazy {
        if (!jvmdowngrader.parent.resolve(jvmdowngrader.nameWithoutExtension + "-downgraded-8-jvmdowngrader.jar").exists()) {
            jvmdowngrader.parent.resolve(jvmdowngrader.nameWithoutExtension + "-downgraded-8-jvmdowngrader.jar").apply {
                val downgrader = ClassDowngrader(Opcodes.V1_8)
                downgrader.downgradeZip(jvmdowngrader.toFile(), this.toFile())
            }
        }
        jvmdowngrader.parent.resolve(jvmdowngrader.nameWithoutExtension + "-downgraded-8-jvmdowngrader.jar")
    }

    fun cleanup() {
        if (downgraded.exists()) {
            downgraded.toFile().delete()
        }
    }

    fun testDowngrade(mainClass: String) {
        println()
        println("Original: ")

        val originalLog = StringBuilder()
        val ret2 = JavaRunner.runJarInSubprocess(
            original,
            mainClass = mainClass,
            javaVersion = VersionProvider.getCurrentClassVersion(),
            output = {
                originalLog.append(it).append("\n")
                println(it)
            }
        )

        println()
        println("Downgraded: ")

        val downgradedLog = StringBuilder()
        val ret = JavaRunner.runJarInSubprocess(
            downgraded,
            mainClass = "xyz.wagyourtail.jvmdg.test.Bootstrap",
            classPath = setOf(downgradedJvmDowngrader),
            javaVersion = Opcodes.V1_8,
            output = {
                downgradedLog.append(it).append("\n")
                println(it)
            },
            args = arrayOf(mainClass)
        )

        if (ret2 != 0) {
            throw Exception("Original jar did not return 0")
        }
        if (ret != 0) {
            throw Exception("Downgraded jar did not return 0")
        }


        assertEquals(originalLog.toString(), downgradedLog.toString())
    }

    @Test
    fun testDowngradeRecord() {
        testDowngrade("xyz.wagyourtail.downgradetest.TestRecord")
    }

    @Test
    fun testDowngradeString() {
        testDowngrade("xyz.wagyourtail.downgradetest.TestString")
    }

    @Test
    fun testDowngradeInterface() {
        testDowngrade("xyz.wagyourtail.downgradetest.TestInterface")
    }

    @Test
    fun testSeal() {
        testDowngrade("xyz.wagyourtail.downgradetest.TestSeal")
    }

    @Test
    fun testFilter() {
        testDowngrade("xyz.wagyourtail.downgradetest.TestFilter")
    }

    @Test
    fun testException() {
        testDowngrade("xyz.wagyourtail.downgradetest.TestException")
    }

    @Test
    fun testStream() {
        testDowngrade("xyz.wagyourtail.downgradetest.TestStream")
    }

    @Test
    fun testClass() {
        testDowngrade("xyz.wagyourtail.downgradetest.TestClass")
    }

    @Test
    fun testFuture() {
        testDowngrade("xyz.wagyourtail.downgradetest.TestFuture")
    }

    @Test
    fun testFile() {
        testDowngrade("xyz.wagyourtail.downgradetest.TestFile")
    }

    @Test
    fun testTime() {
        testDowngrade("xyz.wagyourtail.downgradetest.TestTime")
    }

    @Test
    fun testCollection() {
        testDowngrade("xyz.wagyourtail.downgradetest.TestCollection")
    }

    @Test
    fun testBuffer() {
        testDowngrade("xyz.wagyourtail.downgradetest.TestBuffer")
    }

    @Test
    fun testMatch() {
        testDowngrade("xyz.wagyourtail.downgradetest.TestMatch")
    }
}