package xyz.wagyourtail.jvmdg.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * `jvmdgExtractApiJar` — materializes the plugin-bundled
 * `META-INF/lib/java-api.jar` resource into the build dir so
 * [xyz.wagyourtail.jvmdg.gradle.flags.DowngradeFlags.apiJar] conventions a
 * tracked task output rather than a provider that performs I/O during
 * fingerprinting (which self-invalidated the configuration-cache entry
 * once per store).
 */
@DisableCachingByDefault
abstract class ExtractApiJar : DefaultTask() {

    /** jvmdg version string — an input so a plugin upgrade re-extracts. */
    @get:Input
    abstract val apiVersion: Property<String>

    /** `--refresh-dependencies` at configuration time — forces re-extraction
     *  for same-version snapshot content. */
    @get:Input
    abstract val refreshDependencies: Property<Boolean>

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    init {
        group = "JVMDowngrader"
        description = "Extracts the bundled jvmdowngrader-java-api jar for downgrade use"
        outputJar.convention(
            project.layout.buildDirectory.dir("jvmdg-api").map { it.file("java-api.jar") }
        )
    }

    @TaskAction
    fun extract() {
        val out = outputJar.get().asFile
        out.parentFile.mkdirs()
        ExtractApiJar::class.java.getResourceAsStream("/META-INF/lib/java-api.jar").use { stream ->
            checkNotNull(stream) { "java-api.jar not found in plugin resources" }
            out.outputStream().use { os -> stream.copyTo(os) }
        }
    }

}
