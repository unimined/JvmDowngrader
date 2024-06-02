package xyz.wagyourtail.jvmdg.gradle.flags

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

interface ShadeFlags : DowngradeFlags {

    @get:Input
    @get:Optional
    val shadePath : Property<(fileName: String) -> String>

}