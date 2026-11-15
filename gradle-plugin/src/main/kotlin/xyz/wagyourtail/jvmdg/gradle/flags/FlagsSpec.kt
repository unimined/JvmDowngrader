package xyz.wagyourtail.jvmdg.gradle.flags

import org.gradle.api.Named
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import javax.inject.Inject

/**
 * A standalone [ShadeFlags] container — a named bundle of downgrade
 * settings that is not tied to a task or the `jvmdg` extension.
 *
 * Gradle generates implementations for every flag property, so specs
 * can be created ad hoc with `objects.newInstance(FlagsSpec::class, "name")`
 * (for plugin authors building their own scoped specs) or managed
 * through [xyz.wagyourtail.jvmdg.gradle.JVMDowngraderExtension.specs]
 * (`jvmdg.spec("name") { }` / `jvmdg.specs.create("name")`), where
 * each spec conventions the global extension's values.
 *
 * Chaining is the same as for tasks: `spec.convention(other)` —
 * including another spec — flows every unset flag through, so
 * `spec("b") { convention(spec("a")) }` builds inheritance on
 * top of the container.
 *
 * @since 2.1.0
 */
abstract class FlagsSpec @Inject constructor(
    @get:Internal
    private val name: String
) : ShadeFlags, Named, EnabledFlag {

    override fun getName(): String = name

    /**
     * Whether this spec is active — `true` by convention. Consumers
     * (and tasks conventioned from a spec) are expected to check this
     * before doing downgrade work, so a spec can be declared without
     * being applied. Has no effect on [toFlags] itself.
     *
     * @since 2.1.0
     */
    @get:Input
    @get:Optional
    abstract override val enabled: Property<Boolean>

    init {
        enabled.convention(true)
    }

}
