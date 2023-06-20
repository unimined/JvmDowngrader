@file:Suppress("LeakingThis")

package xyz.wagyourtail.jvmdg.gradle

import org.gradle.api.JavaVersion
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar

abstract class JVMDowngraderTask(val jvmdg: JVMDowngraderExtension) : Jar() {

    @get:Input
    @get:Optional
    abstract val downgradeTo: Property<JavaVersion>

    @get:InputFile
    abstract val inputFile: RegularFileProperty

    init {
        group = "JVMDowngrader"
        description = "Downgrades the jar to the specified version"
        downgradeTo.convention(JavaVersion.VERSION_1_8)
    }

    @TaskAction
    fun doDowngrade() {
        val result = project.javaexec {
            it.mainClass.set("xyz.wagyourtail.jvmdg.compile.ZipDowngrader")
            it.args = listOf(
                jvToOpc(downgradeTo.get()).toString(),
                inputFile.get().asFile.absolutePath,
                archiveFile.get().asFile.absolutePath
            )
            it.workingDir = project.buildDir
            it.classpath = jvmdg.core
            it.jvmArgs = listOf("-Djvmdg.java-api=${jvmdg.api.resolve().first { it.extension == "jar" }.absolutePath}")
        }
        if (result.exitValue != 0) {
            throw Exception("Failed to downgrade jar")
        }
    }

}
