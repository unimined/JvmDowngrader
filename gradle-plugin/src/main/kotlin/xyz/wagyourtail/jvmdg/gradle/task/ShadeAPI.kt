@file:Suppress("LeakingThis")

package xyz.wagyourtail.jvmdg.gradle.task

import org.gradle.api.JavaVersion
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.Jar
import xyz.wagyourtail.jvmdg.gradle.JVMDowngraderExtension
import xyz.wagyourtail.jvmdg.gradle.deleteIfExists
import xyz.wagyourtail.jvmdg.util.FinalizeOnRead
import xyz.wagyourtail.jvmdg.util.LazyMutable
import javax.inject.Inject

abstract class ShadeAPI @Inject constructor(@Internal val jvmdg: JVMDowngraderExtension): Jar() {

    @get:Input
    @get:Optional
    var downgradeTo by FinalizeOnRead(JavaVersion.VERSION_1_8)

    @get:Input
    @get:Optional
    var shadePath by FinalizeOnRead(LazyMutable { archiveBaseName.get().replace(Regex("[ -]"), "_") + "/jvmdg/api" })

    /**
     * must already be downgraded
     */
    @get:InputFile
    abstract val inputFile: RegularFileProperty

    init {
        group = "JVMDowngrader"
        description = "Downgrades the jar to the specified version"
    }

    @TaskAction
    fun doShade() {
        val tempOutput = temporaryDir.resolve("downgradedInput.jar")
        tempOutput.deleteIfExists()

        project.javaexec {
            it.mainClass.set("xyz.wagyourtail.jvmdg.compile.ApiShader")
            it.args = listOf(
                jvmdg.api.resolve().first { it.extension == "jar" }.absolutePath,
                shadePath,
                inputFile.get().asFile.absolutePath,
                tempOutput.absolutePath,
            )
            it.workingDir = temporaryDir
            it.classpath = jvmdg.core
        }.assertNormalExitValue().rethrowFailure()

        from(project.zipTree(tempOutput))
        copy()
    }

}
