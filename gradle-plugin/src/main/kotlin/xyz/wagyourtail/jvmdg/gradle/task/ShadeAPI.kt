@file:Suppress("LeakingThis")

package xyz.wagyourtail.jvmdg.gradle.task

import org.gradle.api.JavaVersion
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import xyz.wagyourtail.jvmdg.gradle.JVMDowngraderExtension
import xyz.wagyourtail.jvmdg.gradle.deleteIfExists
import xyz.wagyourtail.jvmdg.gradle.readZipInputStreamFor
import xyz.wagyourtail.jvmdg.util.FinalizeOnRead
import xyz.wagyourtail.jvmdg.util.LazyMutable
import java.nio.file.StandardOpenOption
import kotlin.io.path.outputStream

abstract class ShadeAPI : Jar() {

    private val jvmdg = project.extensions.getByType(JVMDowngraderExtension::class.java)

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

        project.javaexec { spec ->
            spec.mainClass.set("xyz.wagyourtail.jvmdg.compile.ApiShader")
            spec.args = listOf(
                project.configurations.detachedConfiguration(jvmdg.getDowngradedApi(downgradeTo)).resolve().first { it.extension == "jar" }.absolutePath,
                shadePath,
                inputFile.get().asFile.absolutePath,
                tempOutput.absolutePath,
            )
            spec.workingDir = temporaryDir
            spec.classpath = jvmdg.core
        }.assertNormalExitValue().rethrowFailure()

        inputFile.asFile.get().toPath().readZipInputStreamFor("META-INF/MANIFEST.MF", false) { inp ->
            // write to temp file
            val inpTmp = temporaryDir.toPath().resolve("input-manifest.MF")
            inpTmp.outputStream(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use { out ->
                inp.copyTo(out)
            }
            this.manifest {
                it.from(inpTmp)
            }
        }

        from(project.zipTree(tempOutput))
        copy()
    }

}
