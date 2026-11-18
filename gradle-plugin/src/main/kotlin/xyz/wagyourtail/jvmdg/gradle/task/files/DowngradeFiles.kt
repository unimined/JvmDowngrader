package xyz.wagyourtail.jvmdg.gradle.task.files

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import xyz.wagyourtail.jvmdg.ClassDowngrader
import xyz.wagyourtail.jvmdg.compile.PathDowngrader
import xyz.wagyourtail.jvmdg.gradle.JVMDowngraderExtension
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
abstract class DowngradeFiles: DefaultTask(), DowngradeFlags, FlagsConvention {

    private var _inputCollection: FileCollection by FinalizeOnRead(MustSet())

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    var inputCollection: FileCollection
        get() = _inputCollection
        set(value) {
            _inputCollection = value
        }

    @get:Classpath
    abstract var classpath: FileCollection

    /**
     * Output root — each input maps to a child named after the input;
     * inputs sharing a file name get `-1`, `-2`, … suffixes (see
     * [outputMap]). A single declared root keeps the task's outputs a
     * normal `@OutputDirectory` instead of per-input declarations.
     */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Internal
    val outputMap: Map<File, File>
        get() = inputCollection.mappedOutputs()

    private fun FileCollection.mappedOutputs(): Map<File, File> {
        val root = outputDir.get().asFile
        val seen = mutableMapOf<String, Int>()
        return associateWith { f ->
            val idx = seen.getOrDefault(f.name, 0)
            seen[f.name] = idx + 1
            root.resolve(if (idx == 0) f.name else "${f.name}-$idx")
        }
    }

    /**
     * The mapped output paths (dirs stay dirs, jar inputs stay jars) —
     * rooted on [outputDir] so the collection carries this task as
     * producer and downstream `inputCollection = task.outputCollection`
     * wiring picks up the dependency.
     */
    @get:Internal
    val outputCollection: FileCollection
        get() = project.files(outputDir.map { outputMap.values })

    init {
        group = "JVMDowngrader"
        convention(project.extensions.getByType(JVMDowngraderExtension::class.java))
        classpath = project.extensions.findByType(SourceSetContainer::class.java)
            ?.findByName("main")?.runtimeClasspath ?: project.files()
        outputDir.convention(project.layout.buildDirectory.dir("jvmdg/files/$name"))
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

            outputDir.get().asFile.deleteRecursively()

            val downgraded = toDowngrade.map { outputMap[it.toFile()] ?: outputDir.get().asFile.resolve(it.name) }.map {
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
