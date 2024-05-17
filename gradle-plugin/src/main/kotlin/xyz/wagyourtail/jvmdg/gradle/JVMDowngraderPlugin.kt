package xyz.wagyourtail.jvmdg.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class JVMDowngraderPlugin: Plugin<Project> {

    override fun apply(project: Project) {
        project.extensions.create("jvmdg", JVMDowngraderExtension::class.java, project)
    }

}