package xyz.wagyourtail.gradle.coverage

import org.gradle.api.JavaVersion
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.ConventionTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService

abstract class CoverageRunTask : ConventionTask() {

    @get:InputFile
    abstract val apiJar: RegularFileProperty

    @get:Internal
    abstract var classpath: FileCollection

    @get:InputFile
    abstract val ctSym: RegularFileProperty

    @get:Input
    abstract val javaVersion: Property<JavaVersion>

    @get:OutputDirectory
    @get:Optional
    abstract var coverageReports: FileCollection


    init {
        group = "jvmdg"
        coverageReports = project.files(temporaryDir.resolve("coverage"))
    }

    @TaskAction
    fun run() {
        val toolchains = project.extensions.getByType(JavaToolchainService::class.java)

        project.javaexec { spec ->
            spec.executable = toolchains.launcherFor {
                it.languageVersion.set(JavaLanguageVersion.of(javaVersion.get().majorVersion))
            }.get().executablePath.asFile.absolutePath

            spec.workingDir = temporaryDir
            spec.mainClass.set("xyz.wagyourtail.jvmdg.coverage.ApiCoverageChecker")
            spec.classpath = classpath
            spec.jvmArgs("-Djvmdg.java-api=${apiJar.get().asFile.absolutePath}", "-Djvmdg.quiet=true")
            spec.args(ctSym.get().asFile.absolutePath)

        }.assertNormalExitValue().rethrowFailure()

    }


}