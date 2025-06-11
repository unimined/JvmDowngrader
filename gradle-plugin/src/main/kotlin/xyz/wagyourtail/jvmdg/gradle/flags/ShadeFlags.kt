package xyz.wagyourtail.jvmdg.gradle.flags

import groovy.lang.Closure
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

interface ShadeFlags: DowngradeFlags {

    @get:Internal
    val shadePath: Property<(fileName: String) -> String>

    /**
     *
     * @since 1.2.0
     */
    @get:Input
    @get:Optional
    val shadeInlining: Property<Boolean>

    /**
     * @since 1.3.4
     */
    fun shadePath(
        @ClosureParams(
            value = SimpleType ::class,
            options = [
                "java.lang.String"
            ]
        )
        action: Closure<String>
    )

}
