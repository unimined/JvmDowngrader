@file:Suppress("LeakingThis")

package xyz.wagyourtail.jvmdg.gradle.task

import org.gradle.api.JavaVersion
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.Jar
import xyz.wagyourtail.jvmdg.gradle.JVMDowngraderExtension
import xyz.wagyourtail.jvmdg.gradle.jvToOpc
import javax.inject.Inject

abstract class DowngradeJar @Inject constructor(@Internal val jvmdg: JVMDowngraderExtension) : Jar() {

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
