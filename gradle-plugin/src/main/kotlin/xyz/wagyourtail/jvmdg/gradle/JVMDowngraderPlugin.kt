package xyz.wagyourtail.jvmdg.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import xyz.wagyourtail.jvmdg.gradle.flags.DefaultFlags

class JVMDowngraderPlugin: Plugin<Project> {

    override fun apply(project: Project) {
        project.gradle.sharedServices.registerIfAbsent("${project.path}:jvmdgDefaultFlags", DefaultFlags::class.java) {}
        project.extensions.create("jvmdg", JVMDowngraderExtension::class.java)
    }

}