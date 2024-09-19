package xyz.wagyourtail.gradle.shadow

import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import java.nio.charset.StandardCharsets

abstract class ShadowJar: Jar() {

    @get:Internal
    abstract val shadowContents: ListProperty<FileCollection>

    @get:Input
    @get:Optional
    abstract val relocatePackages: MapProperty<String, String>

    init {
        group = "Shadow"
        description = "Shadow the jar with the specified configurations"

        shadowContents.convention(mutableListOf()).finalizeValueOnRead()
        relocatePackages.convention(mutableMapOf()).finalizeValueOnRead()
        archiveClassifier.convention("all")
    }

    fun relocate(from: String, to: String) {
        relocatePackages.put(from, to)
    }

    @TaskAction
    fun runTask() {
        for (fileCollection in shadowContents.get()) {
            for (file in fileCollection) {
                if (!file.exists()) continue
                if (file.isDirectory) {
                    // copy directory
                    from(file)
                } else {
                    // copy file
                    from(project.zipTree(file))
                }
            }
        }

        filteringCharset = StandardCharsets.ISO_8859_1.name()
        includeEmptyDirs = false

        if (relocatePackages.getOrElse(emptyMap()).isNotEmpty()) {
            val map = relocatePackages.get()
                .mapKeys { it.key.replace('.', '/') }
                .mapKeys { if (!it.key.endsWith("/")) it.key + "/" else it.key }
                .mapValues { it.value.replace('.', '/') }
                .mapValues { if (!it.value.endsWith("/")) it.value + "/" else it.value }
            val rel = PackageRelocator(map)
            eachFile {
                if (!it.path.endsWith(".class")) return@eachFile
                it.path = rel.map(it.path)
                it.filter(mapOf("remapper" to rel), PackageRelocateReader::class.java)
            }
        }

        // call super
        copy()
    }

}
