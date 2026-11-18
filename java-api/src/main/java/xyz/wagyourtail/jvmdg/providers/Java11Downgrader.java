package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.j11.stub.java_base.*;
import xyz.wagyourtail.jvmdg.j11.stub.java_net_http.J_N_H_HttpClient;
import xyz.wagyourtail.jvmdg.j11.stub.java_net_http.J_N_H_HttpHeaders;
import xyz.wagyourtail.jvmdg.j11.stub.java_net_http.J_N_H_HttpRequest;
import xyz.wagyourtail.jvmdg.j11.stub.java_net_http.J_N_H_HttpResponse;

public class Java11Downgrader extends AllVersionDowngrader {
    public Java11Downgrader() {
        super(Opcodes.V11, Opcodes.V10, 0);
    }

    public void init() {
        // -- java.base --
        // ChaCha20Cipher
        stub(J_I_ByteArrayOutputStream.class);
        stub(J_I_FileReader.class);
        stub(J_I_FileWriter.class);
        stub(J_I_InputStream.class);
        stub(J_I_OutputStream.class);
        stub(J_I_Reader.class);
        stub(J_I_Writer.class);
        // AbstractStringBuilder
        stub(J_L_Character.class);
        stub(J_L_CharSequence.class);
        stub(J_L_Class.class);
        stub(J_L_String.class);
        stub(J_L_StringBuffer.class);
        stub(J_L_StringBuilder.class);
        stub(J_L_I_ConstantBootstraps.class);
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
        stub(J_U_Collection.class);
        stub(J_U_Optional.class);
        stub(J_U_OptionalDouble.class);
        stub(J_U_OptionalInt.class);
        stub(J_U_OptionalLong.class);
        stub(J_U_C_TimeUnit.class);
        stub(J_U_F_Predicate.class);
        stub(J_U_R_Pattern.class);
        stub(J_U_Z_Deflater.class);
        stub(J_U_Z_Inflater.class);
        // ZipInputStream -- handled by InputStream
        // ChaCha20ParameterSpec
        // Container
        // Metrics
        // MGF1
        // RSAPSSSignature
        // RSAUtil
        // SignatureUtil

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
        stub(J_N_H_HttpHeaders.class);
        stub(J_N_H_HttpRequest.class);
        stub(J_N_H_HttpResponse.class);
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
