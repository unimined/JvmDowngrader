package xyz.wagyourtail.jvmdg.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import xyz.wagyourtail.jvmdg.gradle.task.DowngradeJar
import xyz.wagyourtail.jvmdg.gradle.task.ShadeAPI

class JVMDowngraderPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.extensions.create("jvmdg", JVMDowngraderExtension::class.java, project)

        // add default
        project.tasks.register("downgradeJar", DowngradeJar::class.java) {
            it.inputFile.set((project.tasks.getByName("jar") as Jar).archiveFile)
        }

        project.tasks.register("shadeDowngradedApi", ShadeAPI::class.java) {
            it.from((project.tasks.getByName("downgradeJar") as Jar).archiveFile)
        }
    }
}