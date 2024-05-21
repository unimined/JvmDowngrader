package xyz.wagyourtail.jvmdg.gradle

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.jetbrains.annotations.ApiStatus
import xyz.wagyourtail.jvmdg.ClassDowngrader
import xyz.wagyourtail.jvmdg.cli.Flags
import xyz.wagyourtail.jvmdg.compile.ApiShader
import xyz.wagyourtail.jvmdg.compile.ZipDowngrader
import xyz.wagyourtail.jvmdg.gradle.task.DowngradeJar
import xyz.wagyourtail.jvmdg.gradle.task.ShadeAPI
import xyz.wagyourtail.jvmdg.util.FinalizeOnRead
import xyz.wagyourtail.jvmdg.util.LazyMutable
import xyz.wagyourtail.jvmdg.util.defaultedMapOf
import xyz.wagyourtail.jvmdg.util.toOpcode
import java.io.File
import java.nio.file.StandardOpenOption
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.io.path.outputStream

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
        val downgradedPath = apiJar.resolveSibling("java-api-${version}-${version.majorVersion}-downgraded.jar")

        val flags = Flags()
        flags.api = apiJar
        flags.printDebug = false
        flags.classVersion = version.toOpcode()
        flags.debugSkipStubs = shadeDebugSkipStubs.toSet()

        ZipDowngrader.downgradeZip(ClassDowngrader.downgradeTo(flags), flags.findJavaApi(), emptySet(), downgradedPath.toPath())
        downgradedPath
    }
}
