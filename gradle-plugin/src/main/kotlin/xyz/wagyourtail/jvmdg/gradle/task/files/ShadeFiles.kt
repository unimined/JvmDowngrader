package xyz.wagyourtail.jvmdg.gradle.task.files

import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.*
import xyz.wagyourtail.jvmdg.compile.ApiShader
import xyz.wagyourtail.jvmdg.gradle.JVMDowngraderExtension
import xyz.wagyourtail.jvmdg.gradle.flags.ShadeFlags
import xyz.wagyourtail.jvmdg.gradle.flags.toFlags
import xyz.wagyourtail.jvmdg.util.*
import java.io.File
import java.nio.file.FileSystem
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

abstract class ShadeFiles : ConventionTask(), ShadeFlags {

    @get:Internal
    protected val jvmdg by lazy {
        project.extensions.getByType(JVMDowngraderExtension::class.java)
    }

    @get:InputFiles
    open var inputCollection: FileCollection by FinalizeOnRead(MustSet())

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

    init {
        downgradeTo.convention(jvmdg.downgradeTo).finalizeValueOnRead()
        apiJar.convention(jvmdg.apiJar).finalizeValueOnRead()
        quiet.convention(jvmdg.quiet).finalizeValueOnRead()
        debug.convention(jvmdg.debug).finalizeValueOnRead()
        debugSkipStubs.convention(jvmdg.debugSkipStubs).finalizeValueOnRead()
        shadePath.convention(jvmdg.shadePath).finalizeValueOnRead()
    }

    @TaskAction
    fun doDowngrade() {
        val toDowngrade = inputCollection.map { it.toPath() }.filter { it.exists() }
        val fileSystems = mutableSetOf<FileSystem>()

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
                ApiShader.shadeApis(this.toFlags(), shadePath.get().invoke(toDowngrade[i].name), toDowngradeFile, downgradedFile, jvmdg.downgradedApis[downgradeTo.get()])
            }
        } finally {
            fileSystems.forEach { it.close() }
        }
    }

    fun forInputs(files: Set<File>): FileCollection {
        return project.files(outputMap.filterKeys { it in files }.values)
    }

}
