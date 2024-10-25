package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.j23.stub.java_base.J_I_Console;
import xyz.wagyourtail.jvmdg.j23.stub.java_base.J_L_R_ExactConversionsSupport;
import xyz.wagyourtail.jvmdg.j23.stub.java_base.J_T_Instant;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

public class Java23Downgrader extends VersionProvider {

    public Java23Downgrader() {
        super(Opcodes.V23, Opcodes.V22, 0);
    }

    @Override
    public void init() {
        // -- java.base --
        stub(J_I_Console.class);
        stub(J_L_R_ExactConversionsSupport.class);
        stub(J_T_Instant.class);
    }

}
