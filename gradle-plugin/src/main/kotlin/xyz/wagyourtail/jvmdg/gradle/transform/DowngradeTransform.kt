package xyz.wagyourtail.jvmdg.gradle.transform

import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.InputArtifactDependencies
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import xyz.wagyourtail.jvmdg.ClassDowngrader
import xyz.wagyourtail.jvmdg.compile.ZipDowngrader

@CacheableTransform
abstract class DowngradeTransform : TransformAction<DowngradeFlags> {

    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputArtifact
    abstract val input: Provider<FileSystemLocation>

    @get:Classpath
    @get:InputArtifactDependencies
    abstract val dependencies: FileCollection

    override fun transform(outputs: TransformOutputs) {
        val input = input.get().asFile
        val flags = parameters
        val output = outputs.file("${input.nameWithoutExtension}-downgraded-${flags.downgradeTo.get()}.${input.extension}")
        val classpath = dependencies.files

        System.out.println(this.input.get().javaClass)

        ClassDowngrader.downgradeTo(flags.toFlags()).use {
            ZipDowngrader.downgradeZip(it, input.toPath(), classpath.map { it.toURI().toURL() }.toSet(), output.toPath())
        }
    }

}