package xyz.wagyourtail.jvmdg.gradle.task.files

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
import xyz.wagyourtail.jvmdg.compile.PathDowngrader
import xyz.wagyourtail.jvmdg.gradle.flags.DowngradeFlags
import xyz.wagyourtail.jvmdg.gradle.flags.FlagsConvention
import xyz.wagyourtail.jvmdg.gradle.flags.toFlags
import xyz.wagyourtail.jvmdg.util.FinalizeOnRead
import xyz.wagyourtail.jvmdg.util.MustSet
import xyz.wagyourtail.jvmdg.util.Utils
import java.io.File
import java.nio.file.FileSystem
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

@DisableCachingByDefault
abstract class DowngradeFiles: ConventionTask(), DowngradeFlags, FlagsConvention {

    private var _inputCollection: FileCollection by FinalizeOnRead(MustSet())

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    var inputCollection: FileCollection
        get() = _inputCollection
        set(value) {
            _inputCollection = value

            // use _inputCollection to finalize it immediately
            val fd = _inputCollection.mappedOutputs()
            outputs.dirs(*fd.filter { it.key.isDirectory }.values.toTypedArray())
            outputs.files(*fd.filter { it.key.isFile }.values.toTypedArray())

        }

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract var classpath: FileCollection

    @get:Internal
    val outputMap: Map<File, File>
        get() = inputCollection.mappedOutputs()

    /**
     * Disambiguated output paths — inputs sharing a file name (e.g.
     * two `main` dirs from different source sets) get `-1`, `-2`, …
     * suffixes so their outputs cannot overwrite each other.
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
        outputs.files
    }

    init {
        group = "JVMDowngrader"
        convention(project.gradle.sharedServices.registrations.getByName("${project.path}:jvmdgDefaultFlags").parameters as DowngradeFlags)
        classpath = project.extensions.findByType(SourceSetContainer::class.java)
            ?.findByName("main")?.runtimeClasspath ?: project.files()
    }

    /**
     * Bind a [org.gradle.api.tasks.SourceSet]'s compiled class dirs as
     * inputs and its `runtimeClasspath` for member resolution — the
     * "downgrade a source set's output" case.
     *
     * @since 2.1.0
     */
    fun sourceSet(name: String) {
        val sourceSet = project.extensions.getByType(SourceSetContainer::class.java).getByName(name)
        inputCollection = sourceSet.output.classesDirs
        classpath = sourceSet.runtimeClasspath
    }

    @TaskAction
    fun doDowngrade() {
        var toDowngrade = inputCollection.map { it.toPath() }.filter { it.exists() }
        val classpath = classpath.files

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

            toDowngrade = toDowngrade.map {
                if (it.isDirectory()) it else run {
                    val fs = Utils.openZipFileSystem(it, false)
                    fileSystems.add(fs)
                    fs.getPath("/")
                }
            }
            ClassDowngrader.downgradeTo(this.toFlags()).use {
                PathDowngrader.downgradePaths(it, toDowngrade, downgraded, classpath.map { it.toURI().toURL() }.toSet())
            }
        } finally {
            fileSystems.forEach { it.close() }
        }
    }

    fun forInputs(files: Set<File>): FileCollection {
        return project.files(outputMap.filterKeys { it in files }.values)
    }

}
