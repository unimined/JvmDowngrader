package xyz.wagyourtail.jvmdg.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File
import kotlin.test.BeforeTest

class TestDowngrade {

    @BeforeTest
    fun clean() {
        File("test-downgrade/.gradle/jvmdg").deleteRecursively()
    }

    @Test
    fun testDowngrade() {
        fun runner(vararg args: String) = GradleRunner.create()
            .withProjectDir(File("test-downgrade"))
            .withArguments(*args, "-PrunningTest", "--stacktrace", "--configuration-cache")
            .withPluginClasspath()
            .build()

        val first = runner("clean", "build")
        assert(first.output.contains("BUILD SUCCESSFUL"))

        // the api-jar extraction provider records "file absent" on the
        // first store then creates the file — that self-invalidates the
        // entry once; the next run re-stores, and the one after must reuse
        runner("clean", "build")
        val third = runner("clean", "build")
        assert(third.output.contains("BUILD SUCCESSFUL"))
        assert(third.output.contains("Configuration cache entry reused"))
    }

}
