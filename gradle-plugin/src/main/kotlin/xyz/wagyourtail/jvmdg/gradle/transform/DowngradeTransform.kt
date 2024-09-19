package xyz.wagyourtail.jvmdg.gradle.transform

import org.gradle.api.artifacts.transform.*
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import xyz.wagyourtail.jvmdg.ClassDowngrader
import xyz.wagyourtail.jvmdg.compile.ZipDowngrader
import xyz.wagyourtail.jvmdg.gradle.flags.DowngradeFlags
import xyz.wagyourtail.jvmdg.gradle.flags.toFlags

@CacheableTransform
abstract class DowngradeTransform: TransformAction<DowngradeFlags> {

    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputArtifact
    abstract val input: Provider<FileSystemLocation>

    @get:Classpath
    @get:InputArtifactDependencies
    abstract val dependencies: FileCollection

    override fun transform(outputs: TransformOutputs) {
        val input = input.get().asFile
        val flags = parameters
        val output =
            outputs.file("${input.nameWithoutExtension}-downgraded-${flags.downgradeTo.get()}.${input.extension}")
        val classpath = dependencies.files

        ClassDowngrader.downgradeTo(flags.toFlags()).use {
            ZipDowngrader.downgradeZip(
                it,
                input.toPath(),
                classpath.map { it.toURI().toURL() }.toSet(),
                output.toPath()
            )
        }
    }

}
