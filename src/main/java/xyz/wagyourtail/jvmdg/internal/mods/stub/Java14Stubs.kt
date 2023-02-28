package xyz.wagyourtail.jvmdg.internal.mods.stub

import xyz.wagyourtail.jvmdg.internal.mods.MethodReplacer
import xyz.wagyourtail.jvmdg.internal.mods.stub._14.*

object Java14Stubs {
    fun apply() {
        MethodReplacer.apply {
            // -- java.base --
            addClass(J_I_PrintStream::class.java)
            // Serial
            addClass(J_L_StrictMath::class.java)
            // MethodHandles
            // CompactNumberFormat
            addClass(J_U_C_L_LockSupport::class.java)
            // PreviewFeature
            // Unsafe

            // -- java.compiler --
            // SourceVersion
            // AbstractAnnotationValueVisitor14
            // AbstractElementVisitor6
            // AbstractTypeVisitor14
            // ElementKindVisitor6
            // SimpleAnnotationValueVisitor14
            // SimpleTypeVisitor14
            // TypeKindVisitor14

            // -- java.xml --
            // ContentHandler

            // -- jdk.compiler --
            // SimpleTreeVisitor
            // TreeScanner

            // -- jdk.jfr --
            // Recording
            // EventStream
            // RecordingStream

            // -- jdk.jshell --
            // Snippet

            // -- jdk.management --
            // OperatingSystemMXBean
            // ThreadMXBean

            // -- jdk.nio.mapmode --
            // ExtendedMapMode
        }
    }
}