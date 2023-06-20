package xyz.wagyourtail.jvmdg.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class JVMDowngraderPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.extensions.create("jvmdg", JVMDowngraderExtension::class.java, project)

        // add default
        project.tasks.register("downgradeJar", JVMDowngraderTask::class.java) {
            it.group = "jvmDowngrader"
            it.description = "Downgrades the jar to the specified version"
            it.from(project.tasks.getByName("jar"))
        }
    }
}