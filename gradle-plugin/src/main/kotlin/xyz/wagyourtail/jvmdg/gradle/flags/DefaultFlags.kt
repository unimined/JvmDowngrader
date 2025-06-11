package xyz.wagyourtail.jvmdg.gradle.flags

import groovy.lang.Closure
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class DefaultFlags : BuildService<DefaultFlags.ShadeBuildServiceFlags> {

    abstract class ShadeBuildServiceFlags : BuildServiceParameters, ShadeFlags {

        override fun shadePath(
            @ClosureParams(
                value = SimpleType::class,
                options = [
                    "java.lang.String"
                ]
            )
            action: Closure<String>
        ) {
            shadePath.set {
                action.call(it)
            }
        }

    }
}
