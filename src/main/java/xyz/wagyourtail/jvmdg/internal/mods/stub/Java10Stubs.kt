package xyz.wagyourtail.jvmdg.internal.mods.stub

import xyz.wagyourtail.jvmdg.internal.mods.MethodReplacer
import xyz.wagyourtail.jvmdg.internal.mods.stub._10.*

object Java10Stubs {
    fun apply() {
        MethodReplacer.apply {
            // -- java.base --
            addClass(J_I_PrintWriter::class.java)
            addClass(J_I_Reader::class.java)
            addClass(`J_L_Runtime$Version`::class.java)
            // StackWalker
            addClass(J_L_I_MethodType::class.java)
            addClass(J_N_URLDecoder::class.java)
            addClass(J_N_URLEncoder::class.java)
            // FileStore
            // DateTimeFormatter
            // DoubleSummaryStatistics
            // IntSummaryStatistics
            addClass(J_U_List::class.java)
            // LongSummaryStatistics
            addClass(J_U_Map::class.java)
            addClass(J_U_Optional::class.java)
            addClass(J_U_OptionalDouble::class.java)
            addClass(J_U_OptionalInt::class.java)
            addClass(J_U_OptionalLong::class.java)
            // Properties
            addClass(J_U_Scanner::class.java)
            addClass(J_U_Set::class.java)
            // SplittableRandom
            // StampedLock
            // JarEntry
            // JarFile
            // LocaleNameProvider
            addClass(J_U_S_Collectors::class.java)
            // ZipFile

            // -- java.compiler --
            // SourceVersion
            // TypeKindVisitor6
            // TypeKindVisitor9

            // -- java.desktop --
            // Toolkit
            // Path2D
            // ButtonModel

            // -- java.management --
            // RuntimeMXBean
            // ThreadMXBean

            // -- jdk.compiler --
            // DocCommentTree
            // DocTreeVisitor
            // DocTypeTree
            // SummaryTree
            // DocTreeFactory
            // DocTreeScanner
            // SimpleDocTreeVisitor

            // -- jdk.net --
            // ExtendedSocketOptions

            // -- jdk.unsupported --
            // ExtendedOpenOption
        }
    }
}