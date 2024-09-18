package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.j22.stub.java_base.*;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

public class Java22Downgrader extends VersionProvider {

    public Java22Downgrader() {
        super(Opcodes.V22, Opcodes.V21, 0);
    }

    @Override
    public void init() {
        // -- java.base --
        stub(J_I_Console.class);
        stub(J_L_Class.class);
        stub(J_L_Module.class);
        stub(J_N_F_Path.class);

    }

}
