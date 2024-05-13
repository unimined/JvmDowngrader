package xyz.wagyourtail.jvmdg.gradle

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.file.FileCollection
import org.gradle.jvm.tasks.Jar
import xyz.wagyourtail.jvmdg.gradle.task.DowngradeJar
import xyz.wagyourtail.jvmdg.gradle.task.ShadeAPI
import xyz.wagyourtail.jvmdg.util.FinalizeOnRead
import xyz.wagyourtail.jvmdg.util.defaultedMapOf
import java.io.File

abstract class JVMDowngraderExtension(val project: Project) {

    var defaultMavens: Boolean by FinalizeOnRead(true)

    var group by FinalizeOnRead("xyz.wagyourtail.jvmdowngrader")

    val coreArchiveName by FinalizeOnRead("jvmdowngrader")
    var apiArchiveName by FinalizeOnRead("jvmdowngrader-java-api")

    var version by FinalizeOnRead(JVMDowngraderPlugin::class.java.`package`.implementationVersion ?: "0.2.2")

    var asmVersion by FinalizeOnRead("9.7")

    val defaultTask = project.tasks.register("downgradeJar", DowngradeJar::class.java).apply {
        configure {
            val jar = (project.tasks.findByName("shadowJar") ?: project.tasks.getByName("jar")) as Jar
            it.inputFile.set(jar.archiveFile)
            it.archiveClassifier.set("downgraded")
        }
    }

    val defaultShadeTask = project.tasks.register("shadeDowngradedApi", ShadeAPI::class.java).apply {
        configure {
            it.inputFile.set(defaultTask.get().archiveFile)
            it.archiveClassifier.set("downgraded-shaded")
        }
    }

    val core by lazy {
        if (this.defaultMavens) {
            project.logger.info("Adding $defaultMavenResolver")
        }
        project.configurations.detachedConfiguration(
            project.dependencies.create("$group:$coreArchiveName:${version}"),
            project.dependencies.create("org.ow2.asm:asm:$asmVersion"),
            project.dependencies.create("org.ow2.asm:asm-commons:$asmVersion"),
            project.dependencies.create("org.ow2.asm:asm-tree:$asmVersion"),
            project.dependencies.create("org.ow2.asm:asm-util:$asmVersion"),
        )
    }

    val api by lazy {
        if (this.defaultMavens) {
            project.logger.info("Adding $defaultMavenResolver")
        }
        project.configurations.detachedConfiguration(
            project.dependencies.create("$group:$apiArchiveName:${version}")
        )
    }

    private val downgradedApis = defaultedMapOf<JavaVersion, Dependency> { version ->
        // if it's 8 or 11, premade exists, grab off maven
        if (version.isJava8 || version.isJava11 && defaultMavens) {
            project.logger.lifecycle("Using pre-downgraded api for ${version.majorVersion}")
            project.logger.info("Adding $defaultMavenResolver")
            return@defaultedMapOf project.dependencies.create("$group:$apiArchiveName:${this.version}:downgraded-${version.majorVersion}")
        }
        project.logger.lifecycle("Generating downgraded api for ${version.majorVersion}")
        // else, generate it
        val jvmdg = project.layout.buildDirectory.get().asFile.resolve("jvmdg")
        jvmdg.mkdirs()
        val downgradedApi = jvmdg.resolve("java-api-${this.version}-downgraded-${version.majorVersion}.jar")
        if (!downgradedApi.exists()) {
            val result = project.javaexec {
                it.mainClass.set("xyz.wagyourtail.jvmdg.compile.ZipDowngrader")
                it.args = listOf(
                    jvToOpc(version).toString(),
                    api.resolve().first { it.extension == "jar" }.absolutePath,
                    downgradedApi.absolutePath
                )
                it.workingDir = project.buildDir
                it.classpath = core
                it.jvmArgs = listOf("-Djvmdg.java-api=${api.resolve().first { it.extension == "jar" }.absolutePath}")
            }
            if (result.exitValue != 0) {
                throw Exception("Failed to downgrade jar")
            }
        }
        project.dependencies.create(project.files(downgradedApi))
    }

    fun downgradeDirectories(version: JavaVersion, inputs: List<File>, outputs: List<File>, classpath: FileCollection) {
        val tempDir = project.layout.buildDirectory.get().asFile.resolve("jvmdg")
        tempDir.mkdirs()
        project.javaexec { spec ->
            spec.mainClass.set("xyz.wagyourtail.jvmdg.compile.PathDowngrader")
            spec.args = listOf(
                jvToOpc(version).toString(),
                inputs.joinToString(File.pathSeparator) { it.absolutePath },
                outputs.joinToString(File.pathSeparator) { it.absolutePath },
                classpath.files.joinToString(File.pathSeparator) { it.absolutePath }
            )
            spec.workingDir = tempDir
            spec.classpath = core
            spec.jvmArgs = listOf("-Djvmdg.java-api=${api.resolve().first { it.extension == "jar" }.absolutePath}")
        }.assertNormalExitValue().rethrowFailure()
    }

    fun getDowngradedApi(version: JavaVersion): Dependency {
        return downgradedApis[version]
    }

    private val defaultMavenResolver by lazy {
        insertMaven {  }
        "Wagyourtail's Mavens"
    }

    fun insertMaven(action: MavenArtifactRepository.() -> Unit) {
        project.repositories.maven {
            it.name = "WagYourTail (Releases)"
            it.url = project.uri("https://maven.wagyourtail.xyz/releases/")
            it.metadataSources { ms ->
                ms.mavenPom()
                ms.artifact()
            }
        }

        project.repositories.maven {
            it.name = "WagYourTail (Snapshots)"
            it.url = project.uri("https://maven.wagyourtail.xyz/snapshots/")
            it.metadataSources { ms ->
                ms.mavenPom()
                ms.artifact()
            }
        }
    }

}
