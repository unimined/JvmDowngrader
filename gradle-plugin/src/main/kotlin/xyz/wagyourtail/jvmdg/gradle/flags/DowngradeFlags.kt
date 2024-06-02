package xyz.wagyourtail.jvmdg.gradle.flags

import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import xyz.wagyourtail.jvmdg.cli.Flags
import xyz.wagyourtail.jvmdg.util.toOpcode
import java.io.File

interface DowngradeFlags : TransformParameters {

    @get:Input
    @get:Optional
    val downgradeTo: Property<JavaVersion>

    @get:Input
    @get:Optional
    val apiJar: Property<File>

    @get:Input
    @get:Optional
    val quiet: Property<Boolean>

    @get:Input
    @get:Optional
    val debug: Property<Boolean>

    @get:Input
    @get:Optional
    val debugSkipStubs: SetProperty<JavaVersion>
}

fun DowngradeFlags.toFlags(): Flags {
    val flags = Flags()
    flags.api = apiJar.get()
    flags.printDebug = debug.get()
    flags.quiet = quiet.get()
    flags.classVersion = downgradeTo.get().toOpcode()
    flags.debugSkipStubs = debugSkipStubs.get().map { it.toOpcode() }.toSet()
    return flags
}