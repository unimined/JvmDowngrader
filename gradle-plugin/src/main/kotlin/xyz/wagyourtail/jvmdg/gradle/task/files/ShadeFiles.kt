package xyz.wagyourtail.jvmdg.gradle.task.files

import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import xyz.wagyourtail.jvmdg.ClassDowngrader
import xyz.wagyourtail.jvmdg.compile.ApiShader
import xyz.wagyourtail.jvmdg.compile.ZipDowngrader
import xyz.wagyourtail.jvmdg.gradle.flags.DefaultFlags
import xyz.wagyourtail.jvmdg.gradle.flags.FlagsConvention
import xyz.wagyourtail.jvmdg.gradle.flags.ShadeFlags
import xyz.wagyourtail.jvmdg.gradle.flags.toFlags
import xyz.wagyourtail.jvmdg.util.FinalizeOnRead
import xyz.wagyourtail.jvmdg.util.MustSet
import xyz.wagyourtail.jvmdg.util.Utils
import java.io.File
import java.nio.file.FileSystem
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

abstract class ShadeFiles: ConventionTask(), ShadeFlags, FlagsConvention {

    @get:Internal
    val version by FinalizeOnRead(ShadeFiles::class.java.`package`.implementationVersion ?: "0.7.0")

    @get:Internal
    val isRefreshDependencies = project.gradle.startParameter.isRefreshDependencies

    @get:InputFiles
    var inputCollection: FileCollection by FinalizeOnRead(MustSet())

    @get:Internal
    val outputMap: Map<File, File>
        get() = inputCollection.associateWith { temporaryDir.resolve(it.name) }

    /**
     * this is the true output, gradle just doesn't have a
     * \@OutputDirectoriesAndFiles
     */
    @get:Internal
    val outputCollection: FileCollection by lazy {
        val fd = inputCollection.map { it to temporaryDir.resolve(it.name) }

        outputs.dirs(*fd.filter { it.first.isDirectory }.map { it.second }.toTypedArray())
        outputs.files(*fd.filter { it.first.isFile }.map { it.second }.toTypedArray())

        outputs.files
    }

//    fun setShadePath(path: String) {
//        shadePath.set { path }
//    }


    @get:ServiceReference("jvmdgDefaultFlags")
    abstract val defaultFlags: Property<DefaultFlags>

    init {
        group = "JVMDowngrader"
        convention(defaultFlags.get().parameters)
    }

    @TaskAction
    fun doDowngrade() {
        val toDowngrade = inputCollection.map { it.toPath() }.filter { it.exists() }
        val fileSystems = mutableSetOf<FileSystem>()


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

        try {

            outputs.files.forEach { it.deleteRecursively() }

            val downgraded = toDowngrade.map { temporaryDir.resolve(it.name) }.map {
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
            for (i in toDowngradePaths.indices) {
                val toDowngradeFile = toDowngradePaths[i]
                val downgradedFile = downgraded[i]
                ApiShader.shadeApis(
                    this.toFlags(),
                    shadePath.get().invoke(toDowngrade[i].name),
                    toDowngradeFile,
                    downgradedFile,
                    downgradeApis
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
