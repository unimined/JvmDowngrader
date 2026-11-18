package xyz.wagyourtail.jvmdg.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class TestDowngrade {

    @Test
    fun testDowngrade() {
        fun runner(vararg args: String) = GradleRunner.create()
            .withProjectDir(File("test-downgrade"))
            .withArguments(*args, "-PrunningTest", "--stacktrace", "--configuration-cache")
            .withPluginClasspath()
            .build()

        val first = runner("clean", "build")
        assertTrue(first.output.contains("BUILD SUCCESSFUL"))

        // the api-jar extraction is a tracked task output now, so the
        // configuration-cache fingerprint is stable across the store —
        // the second run must reuse the entry
        val second = runner("clean", "build")
        assertTrue(second.output.contains("BUILD SUCCESSFUL"))
        assertTrue(second.output.contains("Configuration cache entry reused"))
    }

}
