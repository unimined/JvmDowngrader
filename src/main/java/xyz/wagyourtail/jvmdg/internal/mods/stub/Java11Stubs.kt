package xyz.wagyourtail.jvmdg.internal.mods.stub

import xyz.wagyourtail.jvmdg.internal.mods.MethodReplacer
import xyz.wagyourtail.jvmdg.internal.mods.stub._11.*
import xyz.wagyourtail.jvmdg.internal.mods.stub._11.httpclient.J_N_H_HttpClient
import xyz.wagyourtail.jvmdg.internal.mods.stub._12.J_N_F_Files

object Java11Stubs {
    fun apply() {
        MethodReplacer.apply {
            // -- java.base --
            // ChaCha20Cipher
            addClass(J_I_FileReader::class.java)
            addClass(J_I_FileWriter::class.java)
            addClass(J_I_InputStream::class.java)
            addClass(J_I_OutputStream::class.java)
            addClass(J_I_Reader::class.java)
            addClass(J_I_Writer::class.java)
            // AbstractStringBuilder
            // Character (more unicode spaces)
            addClass(J_L_CharSequence::class.java)
            // Class (nest stuff)
            addClass(J_L_String::class.java)
            addClass(J_L_StringBuilder::class.java)
            addClass(J_L_StringBuffer::class.java)
            // ConstantBootstraps
            // Reference
            addClass(J_N_C_SelectionKey::class.java)
            addClass(J_N_C_Selector::class.java)
            addClass(J_N_F_Files::class.java)
            addClass(J_N_F_Path::class.java)
            // RSAKey
            // XECKey
            // XECPrivateKey
            // XECPublicKey
            // NamedParameterSpec
            // PSSParameterSpec
            // RSAKeyGenParameterSpec
            // RSAMultiPrimePrivateCrtKeySpec
            // RSAPrivateCrtKeySpec
            // RSAPrivateKeySpec
            // RSAPublicKeySpec
            // XECPrivateKeySpec
            // XECPublicKeySpec
            addClass(J_U_C_TimeUnit::class.java)
            addClass(J_U_F_Predicate::class.java)
            addClass(J_U_Z_Deflater::class.java)
            addClass(J_U_Z_Inflater::class.java)
            // ChaCha20ParameterSpec
            // Container
            // Metrics

            // -- java.compiler --
            // SourceVersion

            // -- java.desktop --
            // DialogOwner
            // ListSelectionModel
            // SwingUtilities2

            // -- java.net.http --
            //TODO:
            // addClass(J_N_H_HttpClient::class.java)
            // HttpConnectTimeoutException
            // HttpRequest
            // HttpResponse
            // HttpTimeoutException
            // WebSocket
            // WebSocketHandshakeException

            // -- java.xml.crypto --
            // DigestMethod
            // SignatureMethod

            // -- jdk.jshell --
            // EvalException
            // RemoteCodes

            // -- jdk.net --
            // ExtendedSocketOptions
            // Channels

            // -- jdk.unsupported.desktop --
            // DispatcherWrapper
            // DragSourceContextWrapper
            // DropTargetContextWrapper
            // LightweightContentWrapper
            // SwingInterOpUtils
            // InteropProviderImpl
        }
    }
}