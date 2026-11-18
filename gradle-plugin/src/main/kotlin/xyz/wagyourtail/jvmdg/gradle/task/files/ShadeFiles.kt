package xyz.wagyourtail.jvmdg.gradle.task.files

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import xyz.wagyourtail.jvmdg.compile.ApiShader
import xyz.wagyourtail.jvmdg.gradle.JVMDowngraderExtension
import xyz.wagyourtail.jvmdg.gradle.flags.FlagsConvention
import xyz.wagyourtail.jvmdg.gradle.flags.ShadeFlags
import xyz.wagyourtail.jvmdg.gradle.flags.toFlags
import xyz.wagyourtail.jvmdg.util.Utils
import xyz.wagyourtail.jvmdg.util.defaultShadePrefix
import java.io.File
import java.nio.file.FileSystem
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

@DisableCachingByDefault
abstract class ShadeFiles: DefaultTask(), ShadeFlags, FlagsConvention {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract var inputCollection: FileCollection

    /**
     * Output root — each input maps to a child named after the input;
     * inputs sharing a file name get `-1`, `-2`, … suffixes (see
     * [outputMap]).
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
     * producer for downstream wiring.
     */
    @get:Internal
    val outputCollection: FileCollection
        get() = project.files(outputDir.map { outputMap.values })

    init {
        group = "JVMDowngrader"
        convention(project.extensions.getByType(JVMDowngraderExtension::class.java))
        outputDir.convention(project.layout.buildDirectory.dir("jvmdg/files/$name"))
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
    fun doShade() {
        val toDowngrade = inputCollection.map { it.toPath() }.filter { it.exists() }
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
                    fl.shadePrefix ?: defaultShadePrefix(toDowngrade[i].name),
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
