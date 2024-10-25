package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.j18.stub.java_base.*;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

public class Java18Downgrader extends VersionProvider {
    public Java18Downgrader() {
        super(Opcodes.V18, Opcodes.V17, 0);
    }

    public void init() {
        // -- java.base --
        // GaloisCounterMode$GCMOperation
        stub(J_I_PrintStream.class);
        stub(J_L_Math.class);
        stub(J_L_StrictMath.class);
        stub(J_L_System.class);
        stub(J_N_C_Charset.class);
        // InetAddressResolver
        // InetAddressResolverProvider
        // KeyStore
        // KeyStoreSpi
        stub(J_T_Duration.class);
        // Subject

        // -- java.compiler --

    }

}