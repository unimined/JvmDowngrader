package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.j24.stub.J_I_Reader;
import xyz.wagyourtail.jvmdg.j24.stub.J_L_C_ClassDesc;
import xyz.wagyourtail.jvmdg.j24.stub.J_L_Process;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

public class Java24Downgrader extends VersionProvider {

    public Java24Downgrader() {
        super(Opcodes.V24, Opcodes.V23, 0);
    }

    @Override
    public void init() {
        // -- java.base --
        stub(J_I_Reader.class);
        stub(J_L_Process.class);
        stub(J_L_C_ClassDesc.class);

    }

}
