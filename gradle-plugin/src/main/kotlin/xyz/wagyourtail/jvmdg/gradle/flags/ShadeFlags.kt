package xyz.wagyourtail.jvmdg.gradle.flags

import groovy.lang.Closure
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import java.io.Serializable

interface ShadeFlags: DowngradeFlags {

    /**
     *
     * @since 1.2.0
     */
    @get:Input
    @get:Optional
    val shadeInlining: Property<Boolean>

    /**
     * which `reflection.idx` entries a shaded output ships for dynamic
     * reflection sites — `none|usedOwners|all`. `usedOwners` (default) roots
     * the impl classes of stub members on every JDK owner the input
     * references.
     * @since 3.1.0
     */
    @get:Input
    @get:Optional
    val shadeReflection: Property<String>

    /**
     * extra `reflection.idx` entries to keep under
     * `shadeReflection = usedOwners` — `owner.member` patterns such as
     * `java/util/List.*`
     * @since 3.1.0
     */
    @get:Input
    @get:Optional
    val shadeKeepReflection: ListProperty<String>

}
