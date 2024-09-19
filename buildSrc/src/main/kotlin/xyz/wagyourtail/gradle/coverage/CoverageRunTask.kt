package xyz.wagyourtail.gradle.coverage

import org.gradle.api.JavaVersion
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService

@CacheableTask
abstract class CoverageRunTask: JavaExec() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val apiJar: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val ctSym: RegularFileProperty

    @get:Suppress("ACCIDENTAL_OVERRIDE")
    var javaVersion: JavaVersion?
        get() = super.getJavaVersion()
        set(value) {
            val toolchains = project.extensions.getByType(JavaToolchainService::class.java)
            if (value == null) {
                javaLauncher.set(toolchains.launcherFor { })
            } else {
                javaLauncher.set(toolchains.launcherFor {
                    it.languageVersion.set(JavaLanguageVersion.of(value.majorVersion))
                })
            }
        }

    @get:OutputDirectory
    @get:Optional
    abstract var coverageReports: FileCollection

    init {
        group = "jvmdg"
        coverageReports = project.files(project.layout.buildDirectory.asFile.get().resolve("coverage"))
        mainClass.set("xyz.wagyourtail.jvmdg.coverage.ApiCoverageChecker")
    }

    @TaskAction
    override fun exec() {
        workingDir = project.layout.buildDirectory.asFile.get()
        jvmArgs("-Djvmdg.java-api=${apiJar.get().asFile.absolutePath}", "-Djvmdg.quiet=true")
        args(ctSym.get().asFile.absolutePath)

        super.exec()
    }

}