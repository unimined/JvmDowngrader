package xyz.wagyourtail.jvmdowngrader.internal

import org.gradle.api.JavaVersion
import xyz.wagyourtail.jvmdowngrader.test.JavaRunner
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.test.Test


class JvmDowngraderTest {

    val original = Path.of("./downgradetest/build/libs/downgradetest-1.0.0.jar")

    val downgraded by lazy {
        original.parent.resolve(original.nameWithoutExtension + "-downgraded-8.jar").apply {
            val downgrader = JvmDowngrader(original, "xyz.wagyourtail", JavaVersion.VERSION_1_8)
            downgrader.downgrade(this)
        }
    }

    @Test
    fun testDowngradeRecord() {
        println()
        println("Downgraded: ")

        val downgradedLog = StringBuilder()
        val ret = JavaRunner.runJarInSubprocess(
            downgraded,
            mainClass = "xyz.wagyourtail.downgradetest.TestRecord",
            javaVersion = JavaVersion.VERSION_1_8,
            output = {
                downgradedLog.append(it).append("\n")
                println(it)
            }
        )
        if (ret != 0) {
            throw Exception("Downgraded jar did not return 0")
        }

        println()
        println("Original: ")

        val originalLog = StringBuilder()
        val ret2 = JavaRunner.runJarInSubprocess(
            original,
            mainClass = "xyz.wagyourtail.downgradetest.TestRecord",
            javaVersion = JavaVersion.current(),
            output = {
                originalLog.append(it).append("\n")
                println(it)
            }
        )
        if (ret2 != 0) {
            throw Exception("Original jar did not return 0")
        }
        if (downgradedLog.toString() != originalLog.toString()) {
            throw Exception("Downgraded jar did not return the same output as the original")
        }
    }

    @Test
    fun testDowngradeString() {
        println()
        println("Downgraded: ")

        val downgradedLog = StringBuilder()
        val ret = JavaRunner.runJarInSubprocess(
            downgraded,
            mainClass = "xyz.wagyourtail.downgradetest.TestString",
            javaVersion = JavaVersion.VERSION_1_8,
            output = {
                downgradedLog.append(it).append("\n")
                println(it)
            }
        )
        if (ret != 0) {
            throw Exception("Downgraded jar did not return 0")
        }

        println()
        println("Original: ")

        val originalLog = StringBuilder()
        val ret2 = JavaRunner.runJarInSubprocess(
            original,
            mainClass = "xyz.wagyourtail.downgradetest.TestString",
            javaVersion = JavaVersion.current(),
            output = {
                originalLog.append(it).append("\n")
                println(it)
            }
        )
        if (ret2 != 0) {
            throw Exception("Original jar did not return 0")
        }
        if (downgradedLog.toString() != originalLog.toString()) {
            throw Exception("Downgraded jar did not return the same output as the original")
        }
    }
}