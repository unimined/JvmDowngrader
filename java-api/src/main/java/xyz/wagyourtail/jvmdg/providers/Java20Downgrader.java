package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.j20.stub.java_base.*;
import xyz.wagyourtail.jvmdg.j20.stub.java_net_http.J_N_H_HttpClient;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

public class Java20Downgrader extends VersionProvider {
    public Java20Downgrader() {
        super(Opcodes.V20, Opcodes.V19);
    }

    public void init() {
        // -- java.base --
        stub(J_L_Class.class);
        stub(J_L_Float.class);
        stub(J_L_C_ClassDesc.class);
        stub(J_L_M_ModuleDescriptor.class);
        stub(J_L_R_AccessFlag.class);
        stub(J_L_R_ClassFileFormatVersion.class);
        stub(J_L_R_Executable.class);
        stub(J_L_R_Field.class);
        stub(J_L_R_Member.class);
        stub(J_L_R_Parameter.class);
        stub(J_N_F_S_FileSystemProvider.class);
        stub(J_N_URL.class);
        stub(J_S_InvalidParameterException.class);
        stub(J_U_C_ForkJoinPool.class);
        stub(J_U_C_ForkJoinWorkerThread.class);
        stub(J_U_R_MatchResult.class);
        stub(J_U_R_Matcher.class);
        // SSLParameters

        // -- java.net.http --
        stub(J_N_H_HttpClient.class);

        // -- java.compiler --
    }
}