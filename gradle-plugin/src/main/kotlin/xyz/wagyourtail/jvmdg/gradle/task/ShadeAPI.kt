@file:Suppress("LeakingThis")

package xyz.wagyourtail.jvmdg.gradle.task

import org.gradle.api.JavaVersion
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.Jar
import xyz.wagyourtail.jvmdg.gradle.JVMDowngraderExtension
import javax.inject.Inject

abstract class ShadeAPI @Inject constructor(@Internal val jvmdg: JVMDowngraderExtension) : Jar() {

    @get:Input
    @get:Optional
    abstract val downgradeTo: Property<JavaVersion>

    /**
     * must already be downgraded
     */
    @get:InputFile
    abstract val inputFile: RegularFileProperty

    init {
        group = "JVMDowngrader"
        description = "Downgrades the jar to the specified version"
        downgradeTo.convention(JavaVersion.VERSION_1_8)
    }

    @TaskAction
    fun doShade() {
        TODO()
    }

}
