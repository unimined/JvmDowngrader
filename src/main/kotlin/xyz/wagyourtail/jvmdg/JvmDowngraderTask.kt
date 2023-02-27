package xyz.wagyourtail.jvmdowngrader

import org.gradle.api.JavaVersion
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import xyz.wagyourtail.jvmdowngrader.internal.JvmDowngrader

@Suppress("LeakingThis")
abstract class JvmDowngraderTask : Jar() {

    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val targetVersion: Property<JavaVersion>

    @get:Input
    @get:Optional
    abstract val stubPkg: Property<String?>


    init {
        targetVersion.convention(JavaVersion.VERSION_1_8)
        stubPkg.convention(null as String?)
    }

    @TaskAction
    fun run() {
        val input = inputFile.get().asFile
        val output = archiveFile.get().asFile

        output.parentFile.mkdirs()

        val target = targetVersion.get()

        val jar = JvmDowngrader(input.toPath(), stubPkg.get() ?: project.group.toString(), target)

        jar.downgrade(output.toPath())
    }



}