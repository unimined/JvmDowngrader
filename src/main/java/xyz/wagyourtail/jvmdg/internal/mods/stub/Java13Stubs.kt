package xyz.wagyourtail.jvmdg.internal.mods.stub

import xyz.wagyourtail.jvmdg.internal.mods.MethodReplacer
import xyz.wagyourtail.jvmdg.internal.mods.stub._13.*

object Java13Stubs {
    fun apply() {
        MethodReplacer.apply {
            // -- java.base --
            // Character$UnicodeBlock (more unicode spaces)
            addClass(J_N_Buffer::class.java)
            // MappedByteBuffer
            addClass(J_N_F_FileSystems::class.java)
            // Signature
            // DecimalFormatSymbols
            // JapaneseEra
            // Unsafe
            // UnsafeConstants
            // GCMParameters

            // -- java.compiler --
            // ProcessingEnvironment
            // SourceVersion
            // StandardJavaFileManager

            // -- java.security --
            // KerberosPrincipal

            // -- java.xml.crypto --
            // CanonicalizationMethod
            // DocumentBuilderFactory
            // SAXParserFactory

            // -- jdk.compiler --
            // Tree
            // TreeVisitor
            // YieldTree
            // JavacTask
            // ParameterNameProvider
        }
    }
}