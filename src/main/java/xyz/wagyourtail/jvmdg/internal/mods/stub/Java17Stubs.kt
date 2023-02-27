package xyz.wagyourtail.jvmdg.internal.mods.stub

import xyz.wagyourtail.jvmdg.internal.mods.MethodReplacer
import xyz.wagyourtail.jvmdg.internal.mods.stub._17.*

object Java17Stubs {
    fun apply() {
        MethodReplacer.apply {
            // -- java.base --
            addClass(J_I_Console::class.java)
            // ObjectInputFilter
            addClass(J_L_Class::class.java)
            addClass(J_L_Process::class.java)
            // SwitchBootstraps
            // InstantSource
            // HexFormat
            addClass(`J_U_Map$Entry`::class.java)
            // SplittableRandom
            // ForkJoinPool
            // RandomGenerator
            // RandomGeneratorFactory

            // -- java.compiler --
            // RandomSupport
            // SourceVersion
            // Modifier
            // TypeElement
            // Elements

            // -- java.desktop --
            // FileSystemView
            // DMarlinRenderingEngine
            // RenderingEngine

            // -- java.xml.crypto --
            // SignatureMethod
            // RSAPSSParameterSpec

            // -- jdk.compiler --
            // CaseLabelTree
            // CaseTree
            // ClassTree
            // CompilationUnitTree
            // DefaultCaseLabelTree
            // GuardedPatternTree
            // ParenthesizedPatternTree
            // Tree
            // TreeVisitor
            // SimpleTreeVisitor
            // TreeScanner

            // -- jdk.httpserver --
            addClass(C_S_N_H_Filter::class.java)

            // -- jdk.javadoc --
            // Reporter
            // StandardDoclet

            // -- jdk.jfr --
            // RecordingStream

            // -- jdk.jshell --
            // JavaShellToolBuilder

            // -- jdk.management.jfr --
            // RemoteRecordingStream
        }
    }
}