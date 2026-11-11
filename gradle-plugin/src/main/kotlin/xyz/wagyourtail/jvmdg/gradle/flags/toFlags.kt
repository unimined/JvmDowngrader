package xyz.wagyourtail.jvmdg.gradle.flags

import org.gradle.api.JavaVersion
import org.gradle.api.provider.HasMultipleValues
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import xyz.wagyourtail.jvmdg.cli.Flags
import xyz.wagyourtail.jvmdg.config.FlagBindings
import xyz.wagyourtail.jvmdg.config.JvmdgConfig
import xyz.wagyourtail.jvmdg.config.ManifestConfigReader
import xyz.wagyourtail.jvmdg.logging.Logger
import xyz.wagyourtail.jvmdg.util.toOpcode
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc

/**
 * Per-[BoundFlag.gradle] name → value adapter. The property plumbing (find the
 * property, read `orNull`, apply) is generated in [toFlags]; this map only
 * carries the type conversion each DSL property needs. A bound flag naming a
 * gradle property with no entry here throws — the mirror can't drift.
 */
private val GRADLE_APPLIERS: Map<String, DowngradeFlags.(Flags) -> Boolean> = mapOf(
    "downgradeTo" to { f -> downgradeTo.orNull?.let { f.classVersion = it.toOpcode(); true } ?: false },
    "apiJar" to { f -> apiJar.orNull?.let { f.api = it; true } ?: false },
    "quiet" to { f -> quiet.orNull?.let { f.quiet = it; true } ?: false },
    "logAnsiColors" to { f -> logAnsiColors.orNull?.let { f.logAnsiColors = it; true } ?: false },
    "logLevel" to { f -> logLevel.orNull?.let { f.logLevel = Logger.Level.valueOf(it.uppercase()); true } ?: false },
    "ignoreWarningsIn" to { f -> ignoreWarningsIn.getOrElse(emptyList()).takeIf { it.isNotEmpty() }?.let { v -> v.forEach(f::addIgnore); true } ?: false },
    "debug" to { f -> debug.orNull?.let { f.printDebug = it; true } ?: false },
    "debugSkipStub" to { f -> debugSkipStub.orNull?.let { f.debugSkipStub = it.map(FullyQualifiedMemberNameAndDesc::of).toSet(); true } ?: false },
    "debugSkipStubs" to { f -> debugSkipStubs.orNull?.let { f.debugSkipStubs = it.map(JavaVersion::toOpcode).toSet(); true } ?: false },
    "debugSkipJeps" to { f -> debugSkipJeps.orNull?.let { f.debugSkipJeps = it; true } ?: false },
    "debugDumpClasses" to { f -> debugDumpClasses.orNull?.let { f.debugDumpClasses = it; true } ?: false },
    "debugNoSynthetic" to { f -> debugNoSynthetic.orNull?.let { f.debugNoSynthetic = it; true } ?: false },
    "debugNoFuse" to { f -> debugNoFuse.orNull?.let { f.debugNoFuse = it; true } ?: false },
    "debugNoStubIdx" to { f -> debugNoStubIdx.orNull?.let { f.debugNoStubIdx = it; true } ?: false },
    "multiReleaseOriginal" to { f -> multiReleaseOriginal.orNull?.let { f.multiReleaseOriginal = it; true } ?: false },
    "multiReleaseVersions" to { f -> multiReleaseVersions.orNull?.let { f.multiReleaseVersions = it.map(JavaVersion::toOpcode).toSet(); true } ?: false },
    "downgradeFromMultiReleases" to { f -> downgradeFromMultiReleases.orNull?.let { f.downgradeFromMultiReleases = it; true } ?: false },
    "allowMaven" to { f -> allowMaven.orNull?.let { f.allowMaven = it; true } ?: false },
    "ignoreEmbeddedRules" to { f -> ignoreEmbeddedRules.orNull?.let { f.ignoreEmbeddedRules = it; true } ?: false },
    "keepEmbeddedRules" to { f -> keepEmbeddedRules.orNull?.let { f.keepEmbeddedRules = it; true } ?: false },
    "shadePrefix" to { f -> shadePrefix.orNull?.let { f.shadePrefix = it; true } ?: false },
    "shadeInlining" to { f -> (this as? ShadeFlags)?.shadeInlining?.orNull?.let { f.shadeInlining = it; true } ?: false },
)

/**
 * Defaults &lt; embedded rules &lt; config files &lt; sysprops &lt; DSL —
 * the Flags ctor already applied defaults+sysprops, config files apply next
 * (skipping sysprop-set fields), then DSL `orNull` values win outright.
 */
fun DowngradeFlags.toFlags(sources: MutableMap<String, FlagBindings.Source>? = null): Flags {
    val flags = Flags()
    // config files apply in supplied order — later files win per key
    for (file in config.files) {
        JvmdgConfig.apply(ManifestConfigReader.read(file.toPath()), flags, sources)
    }
    for (b in FlagBindings.ALL) {
        val name = b.metas.firstOrNull { it.gradle.isNotEmpty() }?.gradle ?: continue
        val apply = GRADLE_APPLIERS[name]
            ?: throw IllegalStateException("bound flag '$name' has no Gradle applier — add one to GRADLE_APPLIERS")
        if (apply(this, flags)) {
            sources?.put(b.field.name, FlagBindings.Source.GRADLE)
        }
    }
    return flags
}

fun DowngradeFlags.toFlags(): Flags = toFlags(null)

/**
 * convention() — generic same-named Property copy driven by the bound flag
 * surface, so a new bound field picks up convention chaining automatically.
 */
@JvmName("conventionImpl")
fun convention(self: DowngradeFlags, target: DowngradeFlags) {
    self.config.from(target.config)
    for (b in FlagBindings.ALL) {
        val name = b.metas.firstOrNull { it.gradle.isNotEmpty() }?.gradle ?: continue
        val getter = "get" + name.replaceFirstChar { it.uppercase() }
        val sp = runCatching { self.javaClass.getMethod(getter).invoke(self) as? Provider<*> }.getOrNull()
        val tp = runCatching { target.javaClass.getMethod(getter).invoke(target) as? Provider<*> }.getOrNull()
        if (sp != null && tp != null) {
            @Suppress("UNCHECKED_CAST")
            when (sp) {
                is Property<*> -> (sp as Property<Any>).convention(tp as Property<Any>)
                is HasMultipleValues<*> -> (sp as HasMultipleValues<Any>).convention(tp as Provider<Iterable<Any>>)
            }
        }
    }
}

fun DowngradeFlags.convention(target: DowngradeFlags) {
    convention(this, target)
}

interface FlagsConvention : DowngradeFlags {

    @Deprecated("convention(jvmdg) is no longer nececairy when specifying a custom task")
    fun convention(target: DowngradeFlags) {
        convention(this, target)
    }

}
