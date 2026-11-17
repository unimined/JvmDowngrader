package xyz.wagyourtail.jvmdg.gradle.task.files

import groovy.lang.Closure
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import xyz.wagyourtail.jvmdg.ClassDowngrader
import xyz.wagyourtail.jvmdg.compile.ApiShader
import xyz.wagyourtail.jvmdg.compile.ZipDowngrader
import xyz.wagyourtail.jvmdg.gradle.flags.FlagsConvention
import xyz.wagyourtail.jvmdg.gradle.flags.ShadeFlags
import xyz.wagyourtail.jvmdg.gradle.flags.toFlags
import xyz.wagyourtail.jvmdg.util.FinalizeOnRead
import xyz.wagyourtail.jvmdg.util.Utils
import java.io.File
import java.nio.file.FileSystem
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

@DisableCachingByDefault
abstract class ShadeFiles: ConventionTask(), ShadeFlags, FlagsConvention {

    @get:Internal
    val version by FinalizeOnRead(ShadeFiles::class.java.`package`.implementationVersion ?: "0.7.0")

    @get:Internal
    val isRefreshDependencies = project.gradle.startParameter.isRefreshDependencies

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract var inputCollection: FileCollection

    @get:Internal
    val outputMap: Map<File, File>
        get() = inputCollection.mappedOutputs()

    /**
     * Disambiguated output paths — inputs sharing a file name get
     * `-1`, `-2`, … suffixes so their outputs cannot overwrite
     * each other.
     */
    private fun FileCollection.mappedOutputs(): Map<File, File> {
        val seen = mutableMapOf<String, Int>()
        return associateWith { f ->
            val idx = seen.getOrDefault(f.name, 0)
            seen[f.name] = idx + 1
            temporaryDir.resolve(if (idx == 0) f.name else "${f.name}-$idx")
        }
    }

    /**
     * this is the true output, gradle just doesn't have a
     * \@OutputDirectoriesAndFiles
     */
    @get:Internal
    val outputCollection: FileCollection by lazy {
        val fd = inputCollection.mappedOutputs()

        outputs.dirs(*fd.filter { it.key.isDirectory }.values.toTypedArray())
        outputs.files(*fd.filter { it.key.isFile }.values.toTypedArray())

        outputs.files
    }

//    fun setShadePath(path: String) {
//        shadePath.set { path }
//    }

    init {
        group = "JVMDowngrader"
        convention(project.gradle.sharedServices.registrations.getByName("${project.path}:jvmdgDefaultFlags").parameters as ShadeFlags)
    }

    /**
     * Bind a [org.gradle.api.tasks.SourceSet]'s compiled class dirs as
     * inputs — typically chained on a `DowngradeFiles` task over the
     * same source set.
     *
     * @since 2.1.0
     */
    fun sourceSet(name: String) {
        inputCollection = project.extensions.getByType(SourceSetContainer::class.java)
            .getByName(name).output.classesDirs
    }

    @TaskAction
    fun doDowngrade() {
        val toDowngrade = inputCollection.map { it.toPath() }.filter { it.exists() }
        val fileSystems = mutableSetOf<FileSystem>()

        try {

            outputs.files.forEach { it.deleteRecursively() }

            val downgraded = toDowngrade.map { outputMap[it.toFile()] ?: temporaryDir.resolve(it.name) }.map {
                if (it.extension == "jar" || it.extension == "zip") {
                    val fs = Utils.openZipFileSystem(it.toPath(), true)
                    fileSystems.add(fs)
                    fs.getPath("/")
                } else it.toPath()
            }

            val toDowngradePaths = toDowngrade.map {
                if (it.isDirectory()) it else run {
                    val fs = Utils.openZipFileSystem(it, false)
                    fileSystems.add(fs)
                    fs.getPath("/")
                }
            }
            val fl = this.toFlags()
            for (i in toDowngradePaths.indices) {
                val toDowngradeFile = toDowngradePaths[i]
                val downgradedFile = downgraded[i]
                ApiShader.shadeApis(
                    fl,
                    fl.shadePrefix ?: toDowngrade[i].name.substringBefore(".").substringBeforeLast("-").replace(Regex("[.;\\[/]"), "-") + "/",
                    toDowngradeFile,
                    downgradedFile,
                    apiJar.get().toSet()
                )
            }
        } finally {
            fileSystems.forEach { it.close() }
        }
    }

    fun forInputs(files: Set<File>): FileCollection {
        return project.files(outputMap.filterKeys { it in files }.values)
    }

}
