package xyz.wagyourtail.jvmdg.j11;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.VersionProvider;
import xyz.wagyourtail.jvmdg.j11.stub.*;
import xyz.wagyourtail.jvmdg.j11.stub.httpclient.J_N_H_HttpClient;
import xyz.wagyourtail.jvmdg.j11.stub.httpclient.J_N_H_HttpRequest;

public class Java11Downgrader extends VersionProvider {
        public Java11Downgrader() {
            super(Opcodes.V11);
        }

        public void init() {
            // -- java.base --
            // ChaCha20Cipher
            stub(J_I_FileReader.class);
            stub(J_I_FileWriter.class);
            stub(J_I_InputStream.class);
            stub(J_I_OutputStream.class);
            stub(J_I_Reader.class);
            stub(J_I_Writer.class);
            // AbstractStringBuilder
            // Character (more unicode spaces);
            stub(J_L_CharSequence.class);
            // Class (nest stuff)
            stub(J_L_String.class);
            stub(J_L_StringBuilder.class);
            stub(J_L_StringBuffer.class);
            // ConstantBootstraps
            // Reference
            stub(J_N_C_SelectionKey.class);
            stub(J_N_C_Selector.class);
            stub(J_N_F_Files.class);
            stub(J_N_F_Path.class);
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
            stub(J_U_C_TimeUnit.class);
            stub(J_U_F_Predicate.class);
            stub(J_U_Z_Deflater.class);
            stub(J_U_Z_Inflater.class);
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
             stub(J_N_H_HttpClient.class);
            // HttpConnectTimeoutException
             stub(J_N_H_HttpRequest.class);
             stub(J_N_H_HttpRequest.Builder.class);
             stub(J_N_H_HttpRequest.BodyPublisher.class);
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