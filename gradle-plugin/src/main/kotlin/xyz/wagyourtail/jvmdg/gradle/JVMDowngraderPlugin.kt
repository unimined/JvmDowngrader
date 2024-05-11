package xyz.wagyourtail.jvmdg.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class JVMDowngraderPlugin: Plugin<Project> {

    override fun apply(project: Project) {
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

        project.extensions.create("jvmdg", JVMDowngraderExtension::class.java, project)
    }
}