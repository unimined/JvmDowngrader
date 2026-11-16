package xyz.wagyourtail.jvmdg.gradle.flags

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * Marker for flag carriers that can be toggled off without unsetting
 * their other flags. [convention] flows [enabled] through when both
 * ends implement this interface, so a disabled spec disables any
 * spec chaining from it.
 *
 * Note: Gradle task types cannot implement this — [org.gradle.api.Task.setEnabled]
 * already occupies the name as a plain boolean. Gate bound tasks with
 * `onlyIf { spec.enabled.get() }` instead.
 *
 * @since 2.1.0
 */
interface EnabledFlag {

    /**
     * Whether this flag bundle is active — consumers are expected to
     * check this before doing downgrade work; it has no effect on
     * [toFlags] itself.
     */
    @get:Input
    @get:Optional
    val enabled: Property<Boolean>

}
