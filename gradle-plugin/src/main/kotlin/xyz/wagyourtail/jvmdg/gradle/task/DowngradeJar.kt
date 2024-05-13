@file:Suppress("LeakingThis")

package xyz.wagyourtail.jvmdg.gradle.task

import org.apache.commons.io.output.NullOutputStream
import org.gradle.api.JavaVersion
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.configuration.ShowStacktrace
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.Jar
import xyz.wagyourtail.jvmdg.gradle.JVMDowngraderExtension
import xyz.wagyourtail.jvmdg.gradle.deleteIfExists
import xyz.wagyourtail.jvmdg.gradle.jvToOpc
import xyz.wagyourtail.jvmdg.gradle.readZipInputStreamFor
import xyz.wagyourtail.jvmdg.util.FinalizeOnRead
import xyz.wagyourtail.jvmdg.util.LazyMutable
import java.io.File
import java.nio.file.StandardOpenOption
import kotlin.io.path.outputStream

abstract class DowngradeJar : Jar() {

    private val jvmdg by lazy {
        project.extensions.getByType(JVMDowngraderExtension::class.java)
    }

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

        project.javaexec { spec ->
            spec.mainClass.set("xyz.wagyourtail.jvmdg.compile.ZipDowngrader")
            spec.args = listOf(
                jvToOpc(downgradeTo).toString(),
                inputFile.get().asFile.absolutePath,
                tempOutput.absolutePath,
                sourceSet.compileClasspath.files.joinToString(File.pathSeparator) { it.absolutePath }
            )
            spec.workingDir = temporaryDir
            spec.classpath = jvmdg.core
            spec.jvmArgs = listOf("-Djvmdg.java-api=${jvmdg.api.resolve().first { it.extension == "jar" }.absolutePath}")

            if (project.gradle.startParameter.logLevel < LogLevel.LIFECYCLE) {
                spec.standardOutput = System.out
            } else {
                spec.standardOutput = NullOutputStream.NULL_OUTPUT_STREAM
            }
            if (project.gradle.startParameter.logLevel < LogLevel.LIFECYCLE || project.gradle.startParameter.showStacktrace != ShowStacktrace.INTERNAL_EXCEPTIONS) {
                spec.errorOutput = System.err
            } else {
                spec.errorOutput = NullOutputStream.NULL_OUTPUT_STREAM
            }
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
