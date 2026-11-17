package xyz.wagyourtail.jvmdg.gradle.flags

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class DefaultFlags : BuildService<DefaultFlags.ShadeBuildServiceFlags> {

    abstract class ShadeBuildServiceFlags : BuildServiceParameters, ShadeFlags {

    }
}
