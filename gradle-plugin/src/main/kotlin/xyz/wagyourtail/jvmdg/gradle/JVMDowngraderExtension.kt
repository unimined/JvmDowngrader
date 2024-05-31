package xyz.wagyourtail.jvmdg.gradle

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.jvm.tasks.Jar
import org.jetbrains.annotations.ApiStatus
import xyz.wagyourtail.jvmdg.ClassDowngrader
import xyz.wagyourtail.jvmdg.cli.Flags
import xyz.wagyourtail.jvmdg.compile.ZipDowngrader
import xyz.wagyourtail.jvmdg.gradle.task.DowngradeJar
import xyz.wagyourtail.jvmdg.gradle.task.ShadeAPI
import xyz.wagyourtail.jvmdg.gradle.transform.DowngradeFlags
import xyz.wagyourtail.jvmdg.gradle.transform.DowngradeTransform
import xyz.wagyourtail.jvmdg.gradle.transform.ShadeTransform
import xyz.wagyourtail.jvmdg.util.FinalizeOnRead
import xyz.wagyourtail.jvmdg.util.LazyMutable
import xyz.wagyourtail.jvmdg.util.defaultedMapOf
import xyz.wagyourtail.jvmdg.util.toOpcode
import java.io.File

abstract class JVMDowngraderExtension(val project: Project) {

    var version by FinalizeOnRead(JVMDowngraderPlugin::class.java.`package`.implementationVersion ?: "0.3.0")

    var asmVersion by FinalizeOnRead("9.7")

    var shadeDebugSkipStubs = mutableListOf<Int>()

    var apiJar by FinalizeOnRead(LazyMutable {
        JVMDowngraderExtension::class.java.getResourceAsStream("/META-INF/lib/java-api.jar").use {
            val apiJar = project.layout.buildDirectory.get().asFile.resolve("jvmdg/java-api-${version}.jar")
            if (!apiJar.exists() || project.gradle.startParameter.isRefreshDependencies) {
                apiJar.parentFile.mkdirs()
                apiJar.outputStream().use { os ->
                    it.copyTo(os)
                }
            }
            apiJar
        }
    })

    val defaultTask = project.tasks.register("downgradeJar", DowngradeJar::class.java).apply {
        configure {
            val jar = (project.tasks.findByName("shadowJar") ?: project.tasks.getByName("jar")) as Jar
            it.inputFile.set(jar.archiveFile)
            it.archiveClassifier.set("downgraded")
        }
    }

    val defaultShadeTask = project.tasks.register("shadeDowngradedApi", ShadeAPI::class.java).apply {
        configure {
            it.inputFile.set(defaultTask.get().archiveFile)
            it.archiveClassifier.set("downgraded-shaded")
        }
    }

    @get:ApiStatus.Internal
    internal val downgradedApis = defaultedMapOf<JavaVersion, File> { version ->
        val downgradedPath = apiJar.resolveSibling("java-api-${version}-downgraded.jar")

        val flags = Flags()
        flags.api = apiJar
        flags.printDebug = false
        flags.classVersion = version.toOpcode()
        flags.debugSkipStubs = shadeDebugSkipStubs.toSet()

        ClassDowngrader.downgradeTo(flags).use {
            ZipDowngrader.downgradeZip(it, flags.findJavaApi().toPath(), emptySet(), downgradedPath.toPath())
        }
        downgradedPath
    }

    fun getDowngradedApi(version: JavaVersion): File = downgradedApis[version]

    var depDgVersion by FinalizeOnRead(JavaVersion.VERSION_1_8)

    @JvmOverloads
    fun dg(dep: Configuration, shade: Boolean = true, config: DowngradeFlags.() -> Unit = {}) {
        val artifactType = Attribute.of("artifactType", String::class.java)
        val downgradeAttr = Attribute.of("jvmdg.dg.${dep.name}", Boolean::class.javaObjectType)
        val shadeAttr = Attribute.of("jvmdg.shade.${dep.name}", Boolean::class.javaObjectType)
//        val javaVersion = Attribute.of("org.gradle.jvm.version", Int::class.javaObjectType)

        project.dependencies.apply {
            attributesSchema {
                it.attribute(downgradeAttr)
            }
            artifactTypes.getByName("jar") {
                it.attributes.attribute(downgradeAttr, false)
            }
            registerTransform(DowngradeTransform::class.java) { spec ->
                spec.from.attribute(artifactType, "jar").attribute(downgradeAttr, false).attribute(shadeAttr, false)
                spec.to.attribute(artifactType, "jar").attribute(downgradeAttr, true).attribute(shadeAttr, false)

                spec.parameters {
                    it.downgradeTo.set(depDgVersion)
                    it.apiJar.set(apiJar)
                    config(it)
                }
            }
        }

        if (shade) {
            project.dependencies.apply {
                attributesSchema {
                    it.attribute(shadeAttr)
                }
                artifactTypes.getByName("jar") {
                    it.attributes.attribute(shadeAttr, false)
                }
                registerTransform(ShadeTransform::class.java) { spec ->
                    spec.from.attribute(artifactType, "jar").attribute(shadeAttr, false).attribute(downgradeAttr, true)
                    spec.to.attribute(artifactType, "jar").attribute(shadeAttr, true).attribute(downgradeAttr, true)

                    spec.parameters {
                        it.downgradeTo.set(depDgVersion)
                        it.apiJar.set(downgradedApis[depDgVersion])
                        config(it)
                    }
                }
            }
        }

        dep.attributes {
            it.attribute(downgradeAttr, true)
            if (shade) {
                it.attribute(shadeAttr, true)
            }
        }
    }

}
