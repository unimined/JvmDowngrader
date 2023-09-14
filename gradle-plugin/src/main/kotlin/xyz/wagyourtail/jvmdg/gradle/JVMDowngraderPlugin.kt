package xyz.wagyourtail.jvmdg.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import xyz.wagyourtail.jvmdg.gradle.task.DowngradeJar
import xyz.wagyourtail.jvmdg.gradle.task.ShadeAPI

class JVMDowngraderPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.extensions.create("jvmdg", JVMDowngraderExtension::class.java, project)
    }
}