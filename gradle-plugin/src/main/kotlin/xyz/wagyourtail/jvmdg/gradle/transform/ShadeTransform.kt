package xyz.wagyourtail.jvmdg.gradle.transform

import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import xyz.wagyourtail.jvmdg.compile.ApiShader
import xyz.wagyourtail.jvmdg.gradle.flags.ShadeFlags
import xyz.wagyourtail.jvmdg.gradle.flags.toFlags

@CacheableTransform
abstract class ShadeTransform : TransformAction<ShadeFlags> {

    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputArtifact
    abstract val input: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = input.get().asFile
        val flags = parameters
        val output = outputs.file("${input.nameWithoutExtension}-shaded-${flags.downgradeTo.get()}.${input.extension}")

        ApiShader.shadeApis(flags.toFlags(), flags.shadePath.get().invoke(input.nameWithoutExtension), input, output, flags.apiJar.get().toSet())
    }

}