@file:Suppress("LeakingThis")

package xyz.wagyourtail.jvmdg.gradle.task

import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.Jar
import xyz.wagyourtail.jvmdg.ClassDowngrader
import xyz.wagyourtail.jvmdg.compile.ZipDowngrader
import xyz.wagyourtail.jvmdg.gradle.flags.DowngradeFlags
import xyz.wagyourtail.jvmdg.gradle.JVMDowngraderExtension
import xyz.wagyourtail.jvmdg.gradle.flags.toFlags
import xyz.wagyourtail.jvmdg.util.*
import java.nio.file.StandardOpenOption
import kotlin.io.path.outputStream

abstract class DowngradeJar : Jar(), DowngradeFlags {

    private val jvmdg by lazy {
        project.extensions.getByType(JVMDowngraderExtension::class.java)
    }

    @get:InputFiles
    @get:Optional
    var classpath: FileCollection by FinalizeOnRead(LazyMutable {
        project.extensions.getByType(SourceSetContainer::class.java).getByName("main").compileClasspath
    })

    @get:InputFile
    abstract val inputFile: RegularFileProperty

    init {
        group = "JVMDowngrader"
        description = "Downgrades the jar to the specified version"

        downgradeTo.convention(jvmdg.downgradeTo).finalizeValueOnRead()
        apiJar.convention(jvmdg.apiJar).finalizeValueOnRead()
        quiet.convention(jvmdg.quiet).finalizeValueOnRead()
        debug.convention(jvmdg.debug).finalizeValueOnRead()
        debugSkipStubs.convention(jvmdg.debugSkipStubs).finalizeValueOnRead()

    }

    @TaskAction
    fun doDowngrade() {
        val tempOutput = temporaryDir.resolve("downgradedInput.jar")
        tempOutput.deleteIfExists()

        ClassDowngrader.downgradeTo(this.toFlags()).use {
            ZipDowngrader.downgradeZip(
                it,
                inputFile.asFile.get().toPath(),
                classpath.files.map { it.toURI().toURL() }.toSet(),
                tempOutput.toPath()
            )
        }

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
