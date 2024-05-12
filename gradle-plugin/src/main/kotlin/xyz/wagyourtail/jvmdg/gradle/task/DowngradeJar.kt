@file:Suppress("LeakingThis")

package xyz.wagyourtail.jvmdg.gradle.task

import org.gradle.api.JavaVersion
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.Jar
import xyz.wagyourtail.jvmdg.gradle.JVMDowngraderExtension
import xyz.wagyourtail.jvmdg.gradle.deleteIfExists
import xyz.wagyourtail.jvmdg.gradle.jvToOpc
import xyz.wagyourtail.jvmdg.util.FinalizeOnRead
import xyz.wagyourtail.jvmdg.util.LazyMutable
import java.io.File
import javax.inject.Inject

abstract class DowngradeJar @Inject constructor(@Internal val jvmdg: JVMDowngraderExtension): Jar() {

    @get:Input
    @get:Optional
    var downgradeTo by FinalizeOnRead(JavaVersion.VERSION_1_8)


    @get:Internal
    var sourceSet: SourceSet by FinalizeOnRead(LazyMutable {
        project.extensions.getByType(SourceSetContainer::class.java).getByName("main")
    })

    @get:InputFile
    abstract val inputFile: RegularFileProperty

    init {
        group = "JVMDowngrader"
        description = "Downgrades the jar to the specified version"
    }

    @TaskAction
    fun doDowngrade() {
        val tempOutput = temporaryDir.resolve("downgradedInput.jar")
        tempOutput.deleteIfExists()

        project.javaexec {
            it.mainClass.set("xyz.wagyourtail.jvmdg.compile.ZipDowngrader")
            it.args = listOf(
                jvToOpc(downgradeTo).toString(),
                inputFile.get().asFile.absolutePath,
                tempOutput.absolutePath,
                sourceSet.compileClasspath.files.joinToString(File.pathSeparator) { it.absolutePath }
            )
            it.workingDir = temporaryDir
            it.classpath = jvmdg.core
            it.jvmArgs = listOf("-Djvmdg.java-api=${jvmdg.api.resolve().first { it.extension == "jar" }.absolutePath}")
        }.assertNormalExitValue().rethrowFailure()

        from(project.zipTree(tempOutput))
        copy()
    }

}
