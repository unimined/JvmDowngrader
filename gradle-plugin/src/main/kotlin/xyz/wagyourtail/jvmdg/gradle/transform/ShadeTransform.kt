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
import xyz.wagyourtail.jvmdg.compile.ApiShader
import xyz.wagyourtail.jvmdg.compile.ZipDowngrader

@CacheableTransform
abstract class ShadeTransform : TransformAction<DowngradeFlags> {

    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputArtifact
    abstract val input: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = input.get().asFile
        val flags = parameters
        val output = outputs.file("${input.nameWithoutExtension}-shaded-${flags.downgradeTo.get()}.${input.extension}")

        ApiShader.shadeApis(flags.toFlags(), input.nameWithoutExtension, input, output, flags.apiJar.get())
    }

}