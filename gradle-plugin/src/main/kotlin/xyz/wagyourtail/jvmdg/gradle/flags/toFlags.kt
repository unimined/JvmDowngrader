package xyz.wagyourtail.jvmdg.gradle.flags

import org.gradle.api.JavaVersion
import xyz.wagyourtail.jvmdg.ClassDowngrader.downgradeTo
import xyz.wagyourtail.jvmdg.cli.Flags
import xyz.wagyourtail.jvmdg.logging.Logger
import xyz.wagyourtail.jvmdg.util.toOpcode
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc

fun DowngradeFlags.toFlags(): Flags {
    val flags = Flags()
    flags.api = apiJar.orNull
    flags.quiet = quiet.getOrElse(flags.quiet)
    flags.logAnsiColors = logAnsiColors.getOrElse(flags.logAnsiColors)
    flags.logLevel = Logger.Level.valueOf(logLevel.getOrElse(flags.logLevel.name).uppercase())
    flags.printDebug = debug.getOrElse(flags.printDebug)
    flags.classVersion = downgradeTo.getOrElse(JavaVersion.forClassVersion(flags.classVersion)).toOpcode()
    flags.debugSkipStub = debugSkipStub.getOrElse(flags.debugSkipStub.map { it.toString() }.toSet()).map { FullyQualifiedMemberNameAndDesc.of(it) }.toSet()
    flags.debugSkipStubs = debugSkipStubs.getOrElse(emptySet()).map { it.toOpcode() }.toSet()
    ignoreWarningsIn.getOrElse(flags.ignoreWarningsIn.map { it.key + "*".repeat(it.value.ordinal) }).forEach { flags.addIgnore(it) }
    flags.debugDumpClasses = debugDumpClasses.getOrElse(flags.debugDumpClasses)
    flags.debugNoSynthetic = debugNoSynthetic.getOrElse(flags.debugNoSynthetic)
    flags.multiReleaseVersions = multiReleaseVersions.getOrElse(flags.multiReleaseVersions.map { JavaVersion.forClassVersion(it) }.toSet()).map { it.toOpcode() }.toSet()
    flags.downgradeFromMultiReleases = downgradeFromMultiReleases.getOrElse(flags.downgradeFromMultiReleases)
    if (this is ShadeFlags) {
        flags.shadeInlining = shadeInlining.getOrElse(flags.shadeInlining)
    }
    return flags
}

fun DowngradeFlags.convention(target: DowngradeFlags) {
    apiJar.convention(target.apiJar)
    quiet.convention(target.quiet)
    logAnsiColors.convention(target.logAnsiColors)
    logLevel.convention(target.logLevel)
    debug.convention(target.debug)
    downgradeTo.convention(target.downgradeTo)
    debugSkipStub.convention(target.debugSkipStub)
    debugSkipStubs.convention(target.debugSkipStubs)
    ignoreWarningsIn.convention(target.ignoreWarningsIn)
    debugDumpClasses.convention(target.debugDumpClasses)
    debugNoSynthetic.convention(target.debugNoSynthetic)
    multiReleaseOriginal.convention(target.multiReleaseOriginal)
    multiReleaseVersions.convention(target.multiReleaseVersions)
    downgradeFromMultiReleases.convention(target.downgradeFromMultiReleases)
    if (this is ShadeFlags && target is ShadeFlags) {
        shadeInlining.convention(target.shadeInlining)
        shadePath.convention(target.shadePath)
    }
}

interface FlagsConvention : DowngradeFlags {

    fun convention(target: DowngradeFlags) {
        apiJar.convention(target.apiJar)
        quiet.convention(target.quiet)
        logAnsiColors.convention(target.logAnsiColors)
        logLevel.convention(target.logLevel)
        debug.convention(target.debug)
        downgradeTo.convention(target.downgradeTo)
        debugSkipStub.convention(target.debugSkipStub)
        debugSkipStubs.convention(target.debugSkipStubs)
        ignoreWarningsIn.convention(target.ignoreWarningsIn)
        debugDumpClasses.convention(target.debugDumpClasses)
        debugNoSynthetic.convention(target.debugNoSynthetic)
        multiReleaseOriginal.convention(target.multiReleaseOriginal)
        multiReleaseVersions.convention(target.multiReleaseVersions)
        downgradeFromMultiReleases.convention(target.downgradeFromMultiReleases)
        if (this is ShadeFlags && target is ShadeFlags) {
            shadeInlining.convention(target.shadeInlining)
            shadePath.convention(target.shadePath)
        }
    }

}