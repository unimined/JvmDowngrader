package xyz.wagyourtail.jvmdg.internal.mods.stub

import xyz.wagyourtail.jvmdg.internal.mods.MethodReplacer
import xyz.wagyourtail.jvmdg.internal.mods.stub._15.*

object Java15Stubs {
    fun apply() {
        MethodReplacer.apply {
            // -- java.base --
            // Boolean
            // Character$UnicodeBlock (more unicode spaces)
            // CharSequence
            // Class
            addClass(J_L_Math::class.java)
            // Short
            addClass(J_L_StrictMath::class.java)
            addClass(J_L_String::class.java)
            // ConstantDescs
            // ConstantBootstraps
            addClass(`J_L_I_MethodHandles$Lookup`::class.java)
            addClass(J_N_C_ServerSocketChannel::class.java)
            addClass(J_N_C_SocketChannel::class.java)
            // EdECKey
            // EdECPrivateKey
            // EdECPublicKey
            // EdDSAParameterSpec
            // EdECPoint
            // EdECPrivateKeySpec
            // EdECPublicKeySpec
            // NamedParameterSpec
            // DecimalFormatSymbols
            addClass(J_U_NoSuchElementException::class.java)

            // -- java.compiler --
            // SourceVersion


            // -- jdk.compiler --
            // DocTrees

            // -- jdk.net --
            // ExtendedSocketOptions
        }
    }
}