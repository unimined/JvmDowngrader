package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.j26.stub.java_base.*;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

public class Java26Downgrader extends VersionProvider {

    public Java26Downgrader() {
        super(Opcodes.V26, Opcodes.V25, 0);
    }

    @Override
    public void init() {
        stub(J_L_Process.class);
        stub(J_L_String.class);
    }

}
