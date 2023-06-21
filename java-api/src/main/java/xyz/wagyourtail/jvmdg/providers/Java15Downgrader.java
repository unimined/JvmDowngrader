package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.j15.stub.*;

public class Java15Downgrader extends VersionProvider {
    public Java15Downgrader() {
        super(Opcodes.V15, Opcodes.V14);
    }

    public void init() {
            // -- java.base --
            // Boolean
            // Character$UnicodeBlock (more unicode spaces);
            // CharSequence
            // Class
            stub(J_L_Math.class);
            // Short
            stub(J_L_StrictMath.class);
            stub(J_L_String.class);
            // ConstantDescs
            // ConstantBootstraps
            stub(J_L_I_MethodHandles$Lookup.class);
            stub(J_L_I_MethodHandles$Lookup$ClassOption.class);
            stub(J_N_C_ServerSocketChannel.class);
            stub(J_N_C_SocketChannel.class);
            // EdECKey
            // EdECPrivateKey
            // EdECPublicKey
            // EdDSAParameterSpec
            // EdECPoint
            // EdECPrivateKeySpec
            // EdECPublicKeySpec
            // NamedParameterSpec
            // DecimalFormatSymbols
            stub(J_U_NoSuchElementException.class);

            // -- java.compiler --
            // SourceVersion


            // -- jdk.compiler --
            // DocTrees

            // -- jdk.net --
            // ExtendedSocketOptions
    }
}