package xyz.wagyourtail.jvmdg.gradle

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import xyz.wagyourtail.jvmdg.gradle.task.DowngradeJar
import xyz.wagyourtail.jvmdg.util.FinalizeOnRead
import xyz.wagyourtail.jvmdg.util.defaultedMapOf
import java.io.File

abstract class JVMDowngraderExtension(val project: Project) {

    var version by FinalizeOnRead(JVMDowngraderPlugin::class.java.`package`.implementationVersion)

    val defaultTask = project.tasks.register("downgradeJar", DowngradeJar::class.java) {
        val jar = (project.tasks.findByName("shadowJar") ?: project.tasks.getByName("jar")) as Jar
        it.inputFile.set(jar.archiveFile)
    }

    val core = project.configurations.detachedConfiguration(project.dependencies.create("xyz.wagyourtail.jvmdowngrader:jvmdowngrader:${version}:all"))

    val api = project.configurations.detachedConfiguration(project.dependencies.create("xyz.wagyourtail.jvmdowngrader:jvmdowngrader-java-api:${version}"))

    val downgradedApi = defaultedMapOf<JavaVersion, File> { version ->
        // if it's 8 or 11, premade exists, grab off maven
        if (version.isJava8 || version.isJava11) {
            project.logger.lifecycle("Using pre-downgraded api for ${version.majorVersion}")
            return@defaultedMapOf project.configurations.detachedConfiguration(project.dependencies.create("xyz.wagyourtail.jvmdowngrader:jvmdowngrader-java-api:${this.version}:downgraded-$version")).resolve().first { it.extension == "jar" }
        }
        project.logger.lifecycle("Generating downgraded api for ${version.majorVersion}")
        // else, generate it
        val downgradedApi = project.buildDir.resolve("jvmdg").resolve("java-api-${this.version}-downgraded-${version.majorVersion}.jar")
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
        downgradedApi
    }
}
