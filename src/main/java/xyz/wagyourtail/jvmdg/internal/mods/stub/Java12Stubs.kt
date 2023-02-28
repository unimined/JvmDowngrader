package xyz.wagyourtail.jvmdg.internal.mods.stub

import xyz.wagyourtail.jvmdg.internal.mods.MethodReplacer
import xyz.wagyourtail.jvmdg.internal.mods.stub._12.*

object Java12Stubs {
    fun apply() {
        MethodReplacer.apply {
            // -- java.base --
            addClass(J_I_InputStream::class.java)
            // Character$UnicodeBlock (more unicode spaces)
            addClass(J_L_Class::class.java)
            // Double
            // Enum
            // Float
            // Integer
            // Long
            // String
            // ClassDesc
            // ConstantDesc
            // ConstantDescs
            // DirectMethodHandleDesc
            // DynamicCallSiteDesc
            // DynamicConstantDesc
            // MethodHandleDesc
            // MethodTypeDesc
            // MethodHandle
            // MethodType
            // TypeDescriptor
            // VarHandle
            // SecureCacheResponse
            // ServerSocket
            addClass(J_N_F_Files::class.java)
            // CompactNumberFormat
            // NumberFormat
            // NumberFormatProvider
            addClass(J_U_C_CompletionStage::class.java)
            addClass(J_U_S_Collectors::class.java)
            // HttpsURLConnection
            // NumberFormatProviderImpl

            // -- java.compiler --
            // SourceVersion

            // -- java.desktop --
            // FileSystemView

            // -- java.naming --
            // LdapDnsProvider
            // LdapDnsProviderResult

            // -- jdk.compiler --
            // DocTreeView
            // SystemPropertyTree
            // CaseTree
            // SwitchExpressionTree
            // Tree
            // TreeVisitor
            // DocTreeFactory
            // DocTreeScanner
            // SimpleDocTreeVisitor
        }
    }
}