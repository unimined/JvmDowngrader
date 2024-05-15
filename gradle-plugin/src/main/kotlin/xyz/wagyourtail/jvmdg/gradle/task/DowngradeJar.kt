@file:Suppress("LeakingThis")

package xyz.wagyourtail.jvmdg.gradle.task

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.apache.commons.io.output.NullOutputStream
import org.gradle.api.JavaVersion
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.configuration.ShowStacktrace
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.Jar
import org.gradle.process.JavaExecSpec
import org.jetbrains.annotations.ApiStatus
import xyz.wagyourtail.jvmdg.gradle.JVMDowngraderExtension
import xyz.wagyourtail.jvmdg.gradle.deleteIfExists
import xyz.wagyourtail.jvmdg.gradle.jvToOpc
import xyz.wagyourtail.jvmdg.gradle.readZipInputStreamFor
import xyz.wagyourtail.jvmdg.util.FinalizeOnRead
import xyz.wagyourtail.jvmdg.util.LazyMutable
import java.io.File
import java.nio.file.StandardOpenOption
import kotlin.io.path.outputStream

abstract class DowngradeJar : Jar() {

    private val jvmdg by lazy {
        project.extensions.getByType(JVMDowngraderExtension::class.java)
    }

    @get:Input
    @get:Optional
    var downgradeTo by FinalizeOnRead(JavaVersion.VERSION_1_8)


    @get:Internal
    var classpath: FileCollection by FinalizeOnRead(LazyMutable {
        project.extensions.getByType(SourceSetContainer::class.java).getByName("main").compileClasspath
    })

    @get:Input
    @get:Optional
    @get:ApiStatus.Experimental
    abstract val debugSkipStubs: ListProperty<Int>

    @get:Input
    @get:Optional
    @get:ApiStatus.Experimental
    abstract val debugPrint: Property<Boolean>

    @get:InputFile
    abstract val inputFile: RegularFileProperty

    init {
        group = "JVMDowngrader"
        description = "Downgrades the jar to the specified version"
    }

    private var configureDowngrade: JavaExecSpec.() -> Unit = {}

    fun configureDowngrade(spec: JavaExecSpec.() -> Unit) {
        val old = configureDowngrade
        configureDowngrade = {
            old()
            spec()
        }
    }

    fun configureDowngrade(
        @DelegatesTo(
            JavaExecSpec::class,
            strategy = Closure.DELEGATE_FIRST
        )
        closure: Closure<*>
    ) {
        configureDowngrade {
            closure.delegate = this
            closure.call(this)
        }
    }

    @TaskAction
    fun doDowngrade() {
        val tempOutput = temporaryDir.resolve("downgradedInput.jar")
        tempOutput.deleteIfExists()

        project.javaexec { spec ->
            spec.mainClass.set("xyz.wagyourtail.jvmdg.cli.Main")
            val args = mutableListOf<String>(
                "-a",
                jvmdg.api.resolve().first { it.extension == "jar" }.absolutePath,
                "-c"
            )
            if (debugSkipStubs.get().isNotEmpty() || debugPrint.get()) {
                args.add("debug")
                for (i in debugSkipStubs.get()) {
                    args.add("-s")
                    args.add(i.toString())
                }
                if (debugPrint.get()) {
                    args.add("-p")
                }
            }
            args.addAll(listOf(
                jvToOpc(downgradeTo).toString(),
                "downgrade",
                "-t",
                inputFile.get().asFile.absolutePath,
                tempOutput.absolutePath,
                "-cp",
                classpath.files.joinToString(File.pathSeparator) { it.absolutePath }
            ))

            spec.args = args
            spec.workingDir = temporaryDir
            spec.classpath = jvmdg.core

            if (project.gradle.startParameter.logLevel < LogLevel.LIFECYCLE) {
                spec.standardOutput = System.out
            } else {
                spec.standardOutput = NullOutputStream.NULL_OUTPUT_STREAM
            }
            if (project.gradle.startParameter.logLevel < LogLevel.LIFECYCLE || project.gradle.startParameter.showStacktrace != ShowStacktrace.INTERNAL_EXCEPTIONS) {
                spec.errorOutput = System.err
            } else {
                spec.errorOutput = NullOutputStream.NULL_OUTPUT_STREAM
            }
            configureDowngrade(spec)
        }.assertNormalExitValue().rethrowFailure()

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

        from(project.zipTree(tempOutput))
        copy()
    }

}
