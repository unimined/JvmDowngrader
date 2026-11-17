package xyz.wagyourtail.jvmdg.gradle.flags

import org.gradle.api.JavaVersion
import org.gradle.api.provider.HasMultipleValues
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import xyz.wagyourtail.jvmdg.cli.Flags
import xyz.wagyourtail.jvmdg.config.FlagBindings
import xyz.wagyourtail.jvmdg.config.FlagParser
import xyz.wagyourtail.jvmdg.config.JvmdgConfig
import xyz.wagyourtail.jvmdg.config.ManifestConfigReader
import xyz.wagyourtail.jvmdg.util.toOpcode

/**
 * The flags interfaces a DSL object may implement — a bound `gradle` name
 * resolving a property on none of them is mirror drift (the flag names a
 * DSL property that was never declared) and fails fast.
 */
private val FLAGS_INTERFACES = listOf(DowngradeFlags::class.java, ShadeFlags::class.java)

/**
 * Defaults &lt; embedded rules &lt; config files &lt; sysprops &lt; DSL —
 * the Flags ctor already applied defaults+sysprops, config files apply next
 * (skipping sysprop-set fields), then DSL `orNull` values win outright.
 *
 * The DSL→Flags mapping is driven by the bound-flag table, not a per-flag
 * map: each `gradle` name resolves the like-named property on this object
 * (`get<Name>`), an unset property defers to config, and each value is
 * converted by [dslValue] — `JavaVersion` is the only DSL-side type, strings
 * parse through the flag's own `BoundFlag.form` parser, and map-typed
 * fields accumulate entries through [FlagBindings.Binding.apply].
 */
fun DowngradeFlags.toFlags(sources: MutableMap<String, FlagBindings.Source>? = null): Flags {
    val flags = Flags()
    // config files apply in supplied order — later files win per key
    for (file in config.files) {
        JvmdgConfig.apply(ManifestConfigReader.read(file.toPath()), flags, sources)
    }
    for (b in FlagBindings.ALL) {
        val name = b.metas.firstOrNull { it.gradle.isNotEmpty() }?.gradle ?: continue
        val prop = dslProp(this, name)
        if (prop == null) {
            val getter = "get" + name.replaceFirstChar { it.uppercase() }
            if (FLAGS_INTERFACES.none { iface -> iface.methods.any { it.name == getter } }) {
                throw IllegalStateException(
                    "bound flag '${b.field.name}' names Gradle property '$name' but no 'val $name' " +
                        "exists on DowngradeFlags/ShadeFlags — the DSL mirror drifted")
            }
            continue // declared on another flags type (e.g. shadeInlining on ShadeFlags)
        }
        val value = when (prop) {
            is Property<*> -> prop.orNull
            is HasMultipleValues<*> -> (prop as Provider<*>).orNull
            else -> null
        } ?: continue
        applyDsl(b, flags, value)
        sources?.put(b.field.name, FlagBindings.Source.GRADLE)
    }
    assertDslMirror()
    return flags
}

/** DSL-side properties that aren't bound flags — the config-file input. */
private val NON_FLAG_PROPS = setOf("config")

private fun dslPropNames(): Set<String> {
    val names = linkedSetOf<String>()
    for (iface in FLAGS_INTERFACES) {
        for (m in iface.methods) {
            if (m.parameterCount != 0 || m.returnType == Void.TYPE) continue
            if (!m.name.startsWith("get") && !m.name.startsWith("is")) continue
            names += (m.name.removePrefix("get").removePrefix("is"))
                .replaceFirstChar { it.lowercase() }
        }
    }
    return names
}

/**
 * Reverse mirror check: every `val` on DowngradeFlags/ShadeFlags must be
 * reachable through some binding's `gradle` name — otherwise `toFlags()`
 * silently drops the property's value (forward check only catches
 * `gradle` names that *don't* resolve).
 */
private fun assertDslMirror() {
    val bound = FlagBindings.ALL
        .flatMap { b -> b.metas.map { it.gradle } }
        .filterTo(linkedSetOf()) { it.isNotEmpty() }
    for (name in dslPropNames()) {
        check(name in bound || name in NON_FLAG_PROPS) {
            "DSL property '$name' has no @BoundFlag gradle binding — it would be silently ignored"
        }
    }
}

private fun applyDsl(b: FlagBindings.Binding, f: Flags, value: Any) {
    if (Map::class.java.isAssignableFrom(b.field.type)) {
        // map-typed flags accumulate — each DSL element merges through the
        // binding's own parser so config-file entries survive a DSL add
        for (e in value as Iterable<*>) b.apply(f, e.toString(), true, true)
        return
    }
    if (value is Iterable<*>) {
        val elems = java.util.LinkedHashSet<Any?>()
        value.mapTo(elems) { dslValue(b, it) }
        b.field.set(f,
            if (Set::class.java.isAssignableFrom(b.field.type)) elems else ArrayList(elems))
    } else {
        b.field.set(f, dslValue(b, value))
    }
}

/** DSL element → field element: `JavaVersion` is the only DSL-side type;
 *  strings parse through the flag's own [FlagParser]; anything else is
 *  already field-typed. */
private fun dslValue(b: FlagBindings.Binding, v: Any?): Any? = when (v) {
    null -> null
    is JavaVersion -> v.toOpcode()
    is String -> b.parser().parseScalar(v)
    else -> v
}

/** `val <name>` on a managed flags object — `get<Name>` invoked reflectively;
 *  null when this object doesn't implement the declaring interface. */
private fun dslProp(obj: DowngradeFlags, name: String): Provider<*>? {
    val getter = "get" + name.replaceFirstChar { it.uppercase() }
    return runCatching { obj.javaClass.getMethod(getter).invoke(obj) as? Provider<*> }.getOrNull()
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
        val sp = dslProp(self, name)
        val tp = dslProp(target, name)
        if (sp != null && tp != null) {
            @Suppress("UNCHECKED_CAST")
            when (sp) {
                is Property<*> -> (sp as Property<Any>).convention(tp as Property<Any>)
                is HasMultipleValues<*> -> (sp as HasMultipleValues<Any>).convention(tp as Provider<Iterable<Any>>)
            }
        }
    }
    if (self is EnabledFlag && target is EnabledFlag) {
        self.enabled.convention(target.enabled)
    }
}

fun DowngradeFlags.convention(target: DowngradeFlags) {
    convention(this, target)
}

interface FlagsConvention : DowngradeFlags {

    /**
     * Conventions this flags carrier's unset properties from
     * [target] — the binding mechanism for spec-scoped flags:
     * `convention(jvmdg.specs["name"])` inside a task makes the
     * spec's values win while unset flags keep the current
     * conventions.
     */
    fun convention(target: DowngradeFlags) {
        convention(this, target)
    }

}
