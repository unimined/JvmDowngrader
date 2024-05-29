package xyz.wagyourtail.jvmdg.gradle.task

import org.gradle.api.JavaVersion
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.annotations.ApiStatus
import xyz.wagyourtail.jvmdg.ClassDowngrader
import xyz.wagyourtail.jvmdg.cli.Flags
import xyz.wagyourtail.jvmdg.compile.PathDowngrader
import xyz.wagyourtail.jvmdg.gradle.JVMDowngraderExtension
import xyz.wagyourtail.jvmdg.util.*
import java.io.File
import java.nio.file.FileSystem
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

abstract class DowngradeFiles : ConventionTask() {

    @get:Internal
    protected val jvmdg by lazy {
        project.extensions.getByType(JVMDowngraderExtension::class.java)
    }

    @get:Input
    @get:Optional
    var downgradeTo by FinalizeOnRead(JavaVersion.VERSION_1_8)

    @get:InputFiles
    open var toDowngrade: FileCollection by FinalizeOnRead(MustSet())

    @get:InputFiles
    var classpath: FileCollection by FinalizeOnRead(LazyMutable {
        project.extensions.getByType(SourceSetContainer::class.java).getByName("main").runtimeClasspath
    })

    @get:Internal
    val outputMap: Map<File, File>
        get() = toDowngrade.associateWith { temporaryDir.resolve(it.name) }

    /**
     * this is the true output, gradle just doesn't have a
     * \@OutputDirectoriesAndFiles
     */
    @get:Internal
    val outputCollection: FileCollection
        get() = project.files(toDowngrade.map { temporaryDir.resolve(it.name) })

    @get:OutputFiles
    @get:ApiStatus.Internal
    val outputFiles: FileCollection
        get() = outputCollection.filter { it.isFile }

    @get:OutputDirectories
    @get:ApiStatus.Internal
    val outputDirectories: FileCollection
        get() = outputCollection.filter { it.isDirectory }

    @get:Input
    @get:Optional
    @get:ApiStatus.Experimental
    abstract val debugSkipStubs: ListProperty<Int>

    @get:Input
    @get:Optional
    @get:ApiStatus.Experimental
    abstract val debugPrint: Property<Boolean>

    init {
        debugSkipStubs.convention(mutableListOf())
        debugPrint.convention(false)
    }

    @TaskAction
    fun doDowngrade() {
        var toDowngrade = toDowngrade.map { it.toPath() }.filter { it.exists() }
        val classpath = classpath.files

        val fileSystems = mutableSetOf<FileSystem>()

        val flags = Flags()
        flags.api = jvmdg.apiJar
        flags.printDebug = debugPrint.get()
        flags.classVersion = downgradeTo.toOpcode()
        flags.debugSkipStubs = debugSkipStubs.get().toSet()

        try {

            outputs.files.forEach { it.deleteRecursively() }

            val downgraded = toDowngrade.map { temporaryDir.resolve(it.name) }.map {
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
            ClassDowngrader.downgradeTo(flags).use {
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
