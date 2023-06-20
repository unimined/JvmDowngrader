package xyz.wagyourtail.jvmdg.gradle

import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar

abstract class JVMDowngraderExtension(val project: Project) {

    val version = JVMDowngraderPlugin::class.java.`package`.implementationVersion

    val defaultTask = project.tasks.register("downgradeJar", JVMDowngraderTask::class.java) {
        val jar = (project.tasks.findByName("shadowJar") ?: project.tasks.getByName("jar")) as Jar
        it.inputFile.set(jar.archiveFile)
    }

    val core = project.configurations.detachedConfiguration(project.dependencies.create("xyz.wagyourtail.jvmdowngrader:jvmdowngrader:${version}:all"))

    val api = project.configurations.detachedConfiguration(project.dependencies.create("xyz.wagyourtail.jvmdowngrader:jvmdowngrader-java-api:${version}"))
}
