package xyz.wagyourtail.jvmdg.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File
import java.nio.file.Path

class TestDowngrade {

    @Test
    fun testDowngrade() {
        val result = GradleRunner.create()
            .withProjectDir(File("test-downgrade"))
            .withArguments("clean", "build", "--stacktrace", "-PrunningTest")
            .withPluginClasspath()
            .build()

        assert(result.output.contains("BUILD SUCCESSFUL"))
    }

}
