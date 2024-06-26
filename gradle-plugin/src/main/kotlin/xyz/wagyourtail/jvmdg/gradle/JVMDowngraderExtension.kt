package xyz.wagyourtail.jvmdg.gradle

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.tasks.Internal
import org.gradle.jvm.tasks.Jar
import xyz.wagyourtail.jvmdg.ClassDowngrader
import xyz.wagyourtail.jvmdg.compile.ZipDowngrader
import xyz.wagyourtail.jvmdg.gradle.flags.DowngradeFlags
import xyz.wagyourtail.jvmdg.gradle.flags.ShadeFlags
import xyz.wagyourtail.jvmdg.gradle.flags.toFlags
import xyz.wagyourtail.jvmdg.gradle.task.DowngradeJar
import xyz.wagyourtail.jvmdg.gradle.task.ShadeJar
import xyz.wagyourtail.jvmdg.gradle.transform.DowngradeTransform
import xyz.wagyourtail.jvmdg.gradle.transform.ShadeTransform
import xyz.wagyourtail.jvmdg.util.FinalizeOnRead
import xyz.wagyourtail.jvmdg.util.defaultedMapOf
import java.io.File
import javax.inject.Inject

abstract class JVMDowngraderExtension @Inject constructor(@get:Internal val project: Project) : ShadeFlags {
    @get:Internal
    val version by FinalizeOnRead(JVMDowngraderPlugin::class.java.`package`.implementationVersion ?: "0.7.0")

    @get:Internal
    val defaultTask = project.tasks.register("downgradeJar", DowngradeJar::class.java).apply {
        configure {
            val jar = (project.tasks.findByName("shadowJar") ?: project.tasks.getByName("jar")) as Jar
            it.inputFile.set(jar.archiveFile)
            it.archiveClassifier.set("downgraded")
        }
    }

    @get:Internal
    val defaultShadeTask = project.tasks.register("shadeDowngradedApi", ShadeJar::class.java).apply {
        configure {
            it.inputFile.set(defaultTask.get().archiveFile)
            it.archiveClassifier.set("downgraded-shaded")
        }
    }

    init {
        downgradeTo.convention(JavaVersion.VERSION_1_8).finalizeValueOnRead()
        apiJar.convention(project.provider {
            val apiJar = project.file(".gradle").resolve("jvmdg/java-api-${version}.jar")
            if (!apiJar.exists() || project.gradle.startParameter.isRefreshDependencies) {
                apiJar.parentFile.mkdirs()
                JVMDowngraderExtension::class.java.getResourceAsStream("/META-INF/lib/java-api.jar").use { stream ->
                    if (stream == null) throw IllegalStateException("java-api.jar not found in resources")
                    apiJar.outputStream().use { os ->
                        stream.copyTo(os)
                    }
                }
            }
            apiJar
        }).finalizeValueOnRead()
        quiet.convention(false).finalizeValueOnRead()
        debug.convention(false).finalizeValueOnRead()
        debugSkipStubs.convention(emptySet()).finalizeValueOnRead()
        shadePath.convention { it.substringBefore(".").substringBeforeLast("-").replace(Regex("[.;\\[/]"), "-") + "/" }
    }

    @get:Internal
    internal val downgradedApis = defaultedMapOf<JavaVersion, File> { version ->
        val downgradedPath = apiJar.get().resolveSibling("java-api-${version}-downgraded.jar")

        ClassDowngrader.downgradeTo(this.toFlags()).use {
            ZipDowngrader.downgradeZip(it, apiJar.get().toPath(), emptySet(), downgradedPath.toPath())
        }
        downgradedPath
    }

    fun getDowngradedApi(version: JavaVersion): File = downgradedApis[version]

    @JvmOverloads
    fun dg(dep: Configuration, shade: Boolean = true, config: DowngradeFlags.() -> Unit = {}) {
        val artifactType = Attribute.of("artifactType", String::class.java)
        val downgradeAttr = Attribute.of("jvmdg.dg.${dep.name}", Boolean::class.javaObjectType)
        val shadeAttr = Attribute.of("jvmdg.shade.${dep.name}", Boolean::class.javaObjectType)
        val javaVersionAttr = Attribute.of("org.gradle.jvm.version", Int::class.javaObjectType)

        lateinit var javaVersion: JavaVersion

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
                    it.downgradeTo.set(downgradeTo)
                    it.apiJar.set(apiJar)
                    it.quiet.set(quiet)
                    it.debug.set(debug)
                    it.debugSkipStubs.set(debugSkipStubs)
                    config(it)
                    javaVersion = it.downgradeTo.get()
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
                        it.downgradeTo.set(downgradeTo)
                        it.apiJar.set(project.provider {
                            downgradedApis[it.downgradeTo.get()]
                        })
                        it.quiet.set(quiet)
                        it.debug.set(debug)
                        it.debugSkipStubs.set(debugSkipStubs)
                        it.shadePath.set(shadePath)
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
