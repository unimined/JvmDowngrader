package xyz.wagyourtail.jvmdg.gradle

import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.attributes.java.TargetJvmVersion
import javax.inject.Inject

/**
 * Rewrites a module's `org.gradle.jvm.version` to the downgrade
 * target on every variant, so consumers resolving at the emit level
 * pass variant compatibility against Gradle-Module-Metadata that
 * only publishes higher-jvm variants. Registered per `dg()` call on
 * the dep's `group:name`; transforms then bridge the `jvmdg.dg.*`
 * attribute as usual.
 *
 * @since 2.1.0
 */
abstract class DowngradeTargetJvmRule @Inject constructor(
    private val targetJvmVersion: Int
) : ComponentMetadataRule {

    override fun execute(context: ComponentMetadataContext) {
        context.details.allVariants { variant ->
            variant.attributes {
                it.attribute(
                    TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE,
                    targetJvmVersion
                )
            }
        }
    }
}
