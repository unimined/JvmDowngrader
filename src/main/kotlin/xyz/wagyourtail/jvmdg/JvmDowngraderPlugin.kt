package xyz.wagyourtail.jvmdowngrader

import org.gradle.api.Plugin
import org.gradle.api.Project

class JvmDowngraderPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        configureJvmDowngraderTask(target)
    }

    protected fun configureJvmDowngraderTask(project: Project) {
        project.tasks.register("downgradeJar", JvmDowngraderTask::class.java) {
            it.group = "jvmDowngrader"
            it.description = "Downgrades the jar to the specified version"
        }
    }

}