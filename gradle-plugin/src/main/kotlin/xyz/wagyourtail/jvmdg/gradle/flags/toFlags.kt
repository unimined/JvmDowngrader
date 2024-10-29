package xyz.wagyourtail.jvmdg.gradle.flags

import org.gradle.api.JavaVersion
import xyz.wagyourtail.jvmdg.cli.Flags
import xyz.wagyourtail.jvmdg.logging.Logger
import xyz.wagyourtail.jvmdg.util.toOpcode

fun DowngradeFlags.toFlags(): Flags {
    val flags = Flags()
    flags.api = apiJar.orNull
    flags.quiet = quiet.getOrElse(false)
    flags.logAnsiColors = logAnsiColors.getOrElse(true)
    flags.logLevel = Logger.Level.valueOf(logLevel.getOrElse("INFO").uppercase())
    flags.printDebug = debug.getOrElse(false)
    flags.classVersion = downgradeTo.getOrElse(JavaVersion.VERSION_1_8).toOpcode()
    flags.debugSkipStubs = debugSkipStubs.getOrElse(emptySet()).map { it.toOpcode() }.toSet()
    ignoreWarningsIn.getOrElse(emptyList()).forEach { flags.addIgnore(it) }
    flags.debugDumpClasses = debugDumpClasses.getOrElse(false)
    flags.multiReleaseOriginal = multiReleaseOriginal.getOrElse(false)
    flags.multiReleaseVersions = multiReleaseVersions.getOrElse(emptySet()).map { it.toOpcode() }.toSet()
    flags.downgradeFromMultiReleases = downgradeFromMultiReleases.getOrElse(false)
    if (this is ShadeFlags) {
        flags.shadeInlining = shadeInlining.get()
    }
    return flags
}