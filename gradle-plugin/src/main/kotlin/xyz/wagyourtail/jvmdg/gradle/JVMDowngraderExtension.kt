package xyz.wagyourtail.jvmdg.gradle

import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar

abstract class JVMDowngraderExtension(project: Project) {

    val defaultTask = project.tasks.register("downgradeJar", JVMDowngraderTask::class.java) {
        val jar = (project.tasks.findByName("shadowJar") ?: project.tasks.getByName("jar")) as Jar
        it.inputFile.set(jar.archiveFile)
    }

}
