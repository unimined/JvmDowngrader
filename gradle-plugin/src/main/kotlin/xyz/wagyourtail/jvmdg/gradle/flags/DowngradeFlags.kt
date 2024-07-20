package xyz.wagyourtail.jvmdg.gradle.flags

import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import xyz.wagyourtail.jvmdg.cli.Flags
import xyz.wagyourtail.jvmdg.logging.Logger
import xyz.wagyourtail.jvmdg.util.toOpcode
import java.io.File

interface DowngradeFlags : TransformParameters {

    /**
     * sets the target class version to downgrade to,
     * default is [JavaVersion.VERSION_1_8]
     */
    @get:Input
    @get:Optional
    val downgradeTo: Property<JavaVersion>

    /**
     * sets the api jar to use for downgrading
     * default is null
     */
    @get:InputFiles
    @get:Optional
    var apiJar: ListProperty<File>

    /**
     * sets the log level to [Logger.Level.FATAL]
     * @deprecated use [logLevel], if this is true, it will override [logLevel] to [Logger.Level.FATAL]
     */
    @get:Input
    @get:Optional
    @get:Deprecated(message = "use logLevel", replaceWith = ReplaceWith("logLevel = LogLevel.DEBUG"))
    val quiet: Property<Boolean>

    /**
     * sets if the logger should use ansi colors for the console to look pretty
     */
    @get:Input
    @get:Optional
    val logAnsiColors: Property<Boolean>


    /**
     * sets the log level, default is [Logger.Level.INFO] as a string
     */
    @get:Input
    @get:Optional
    val logLevel: Property<String>

    /**
     * sets if any classes should be set to ignore missing class/member warnings
     * this will prevent [xyz.wagyourtail.jvmdg.version.VersionProvider.printWarnings] for
     * any classes in this set
     *
     * This set also allows for packages to be ignored, by ending with a `*` or `**` to ignore all sub-packages
     * @since 0.9.0
     */
    @get:Input
    @get:Optional
    val ignoreWarningsIn: ListProperty<String>

    /**
     * sets if the logger should print debug messages
     * @deprecated use [logLevel], if this is true, it will override [logLevel] to [Logger.Level.DEBUG]
     */
    @get:Input
    @get:Optional
    @get:Deprecated(message = "use logLevel", replaceWith = ReplaceWith("logLevel = LogLevel.DEBUG"))
    val debug: Property<Boolean>

    /**
     * this skips applying stubs for the specified input class version, this will still apply the
     * [xyz.wagyourtail.jvmdg.version.VersionProvider.otherTransforms]
     * such as `INVOKE_INTERFACE` -> `INVOKE_SPECIAL` for private interface methods in java 9 -> 8
     */
    @get:Input
    @get:Optional
    val debugSkipStubs: SetProperty<JavaVersion>

    /**
     * sets if classes should be dumped to the [xyz.wagyourtail.jvmdg.Constants.DEBUG_DIR] directory
     * @since 0.9.0
     */
    @get:Input
    @get:Optional
    val debugDumpClasses: Property<Boolean>

}

fun DowngradeFlags.toFlags(): Flags {
    val flags = Flags()
    flags.api = apiJar.orNull?.toSet()
    flags.quiet = quiet.getOrElse(false)
    flags.logAnsiColors = logAnsiColors.getOrElse(true)
    flags.logLevel = Logger.Level.valueOf(logLevel.getOrElse("INFO").uppercase())
    flags.printDebug = debug.getOrElse(false)
    flags.classVersion = downgradeTo.getOrElse(JavaVersion.VERSION_1_8).toOpcode()
    flags.debugSkipStubs = debugSkipStubs.getOrElse(emptySet()).map { it.toOpcode() }.toSet()
    ignoreWarningsIn.getOrElse(emptyList()).forEach { flags.addIgnore(it) }
    flags.debugDumpClasses = debugDumpClasses.getOrElse(false)
    return flags
}
