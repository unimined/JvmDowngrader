@file:Suppress("LeakingThis")

package xyz.wagyourtail.jvmdg.gradle.task

import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import xyz.wagyourtail.jvmdg.ClassDowngrader
import xyz.wagyourtail.jvmdg.compile.ApiShader
import xyz.wagyourtail.jvmdg.compile.ZipDowngrader
import xyz.wagyourtail.jvmdg.gradle.flags.FlagsConvention
import xyz.wagyourtail.jvmdg.gradle.flags.ShadeFlags
import xyz.wagyourtail.jvmdg.gradle.flags.toFlags
import xyz.wagyourtail.jvmdg.util.FinalizeOnRead
import xyz.wagyourtail.jvmdg.util.deleteIfExists
import xyz.wagyourtail.jvmdg.util.readZipInputStreamFor
import java.io.File
import java.nio.file.StandardOpenOption
import javax.inject.Inject
import kotlin.io.path.outputStream

abstract class ShadeJar: Jar(), ShadeFlags, FlagsConvention {

    @get:Inject
    abstract val archive: ArchiveOperations

    @get:Internal
    val version by FinalizeOnRead(ShadeJar::class.java.`package`.implementationVersion ?: "0.7.0")

    @get:Internal
    val isRefreshDependencies = project.gradle.startParameter.isRefreshDependencies

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

        val downgradeApis = mutableSetOf<File>()
        for (path in apiJar.get()) {
            val downgraded = path.resolveSibling(path.nameWithoutExtension + "-downgraded-${version}.jar")
            if (!downgraded.exists() || isRefreshDependencies) {
                ClassDowngrader.downgradeTo(this.toFlags()).use {
                    ZipDowngrader.downgradeZip(it, path.toPath(), emptySet(), downgraded.toPath())
                }
            }
            downgradeApis.add(downgraded)
        }
        downgradeApis

        ApiShader.shadeApis(
            this.toFlags(),
            shadePath.get().invoke(archiveFileName.get()),
            inputFile.asFile.get(),
            tempOutput,
            downgradeApis
        )

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

        from(archive.zipTree(tempOutput))
        copy()
    }

}
