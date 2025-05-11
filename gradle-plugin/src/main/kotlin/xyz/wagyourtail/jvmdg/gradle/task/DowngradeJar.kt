@file:Suppress("LeakingThis")

package xyz.wagyourtail.jvmdg.gradle.task

import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.Jar
import xyz.wagyourtail.jvmdg.ClassDowngrader
import xyz.wagyourtail.jvmdg.compile.ZipDowngrader
import xyz.wagyourtail.jvmdg.gradle.flags.DowngradeFlags
import xyz.wagyourtail.jvmdg.gradle.flags.FlagsConvention
import xyz.wagyourtail.jvmdg.gradle.flags.toFlags
import xyz.wagyourtail.jvmdg.util.FinalizeOnRead
import xyz.wagyourtail.jvmdg.util.LazyMutable
import xyz.wagyourtail.jvmdg.util.deleteIfExists
import xyz.wagyourtail.jvmdg.util.readZipInputStreamFor
import java.nio.file.StandardOpenOption
import javax.inject.Inject
import kotlin.io.path.outputStream

abstract class DowngradeJar: Jar(), DowngradeFlags, FlagsConvention {

    @get:Inject
    abstract val archive: ArchiveOperations

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

        tempOutput.toPath().readZipInputStreamFor("META-INF/MANIFEST.MF", false) { inp ->
            // write to temp file
            val inpTmp = temporaryDir.toPath().resolve("input-manifest.MF")
            inpTmp.outputStream(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use { out ->
                inp.copyTo(out)
            }
            this.manifest {
                it.from(inpTmp)
            }
        }

        from(archive.zipTree(tempOutput))
        copy()
    }

}
