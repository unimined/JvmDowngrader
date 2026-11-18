package xyz.wagyourtail.jvmdg.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import xyz.wagyourtail.jvmdg.config.FlagBindings
import xyz.wagyourtail.jvmdg.gradle.JVMDowngraderExtension
import xyz.wagyourtail.jvmdg.gradle.flags.DowngradeFlags
import xyz.wagyourtail.jvmdg.gradle.flags.FlagsConvention
import xyz.wagyourtail.jvmdg.gradle.flags.toFlags

/**
 * `jvmdgPrintConfig` — dumps the effective merged configuration (bound flag
 * values + scoped rules) with the source each value came from.
 */
@DisableCachingByDefault
abstract class PrintConfig : DefaultTask(), DowngradeFlags, FlagsConvention {

    init {
        group = "JVMDowngrader"
        description = "Prints the effective merged jvmdg configuration and where each value came from"
        convention(project.extensions.getByType(JVMDowngraderExtension::class.java))
    }

    @TaskAction
    fun printConfig() {
        val sources = mutableMapOf<String, FlagBindings.Source>()
        val flags = toFlags(sources)
        println(FlagBindings.printConfig(flags, sources))
    }

}
