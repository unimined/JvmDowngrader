@file:Suppress("LeakingThis")

package xyz.wagyourtail.jvmdg.gradle.task

import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import org.gradle.work.DisableCachingByDefault
import xyz.wagyourtail.jvmdg.compile.ApiShader
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

@DisableCachingByDefault
abstract class ShadeJar: Jar(), ShadeFlags, FlagsConvention {

    @get:Inject
    abstract val archive: ArchiveOperations

    @get:Internal
    val version by FinalizeOnRead(ShadeJar::class.java.`package`.implementationVersion ?: "0.7.0")

    @get:Internal
    val isRefreshDependencies = project.gradle.startParameter.isRefreshDependencies

    @get:Optional
    @get:Input
    abstract val shadePath: Property<String>

    /**
     * must already be downgraded
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val inputFile: RegularFileProperty

    init {
        group = "JVMDowngrader"
        description = "Downgrades the jar to the specified version"
        convention(project.gradle.sharedServices.registrations.getByName("${project.path}:jvmdgDefaultFlags").parameters as ShadeFlags)
    }

    fun shadePath(path: String) {
        this.shadePath.set(path)
    }

    @TaskAction
    fun doShade() {
        val tempOutput = temporaryDir.resolve("downgradedInput.jar")
        tempOutput.deleteIfExists()

        val fl = this.toFlags()
        ApiShader.shadeApis(
            fl,
            shadePath.getOrElse(fl.shadePrefix ?: archiveBaseName.get().replace(Regex("[.;\\[/]"), "-") + "/"),
            inputFile.asFile.get(),
            tempOutput,
            apiJar.get().toSet()
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
