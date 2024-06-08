package xyz.wagyourtail.jvmdg.gradle.flags

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal

interface ShadeFlags : DowngradeFlags {

    @get:Internal
    val shadePath : Property<(fileName: String) -> String>

//    fun setShadePath(
//        @ClosureParams(
//            value = SimpleType::class,
//            options = [
//                "java.lang.String"
//            ]
//        )
//        action: Closure<String>
//    ) {
//        shadePath.set { fileName -> action.call(fileName) }
//    }
}