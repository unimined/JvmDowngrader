package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.j27.stub.java_base.J_L_Math;
import xyz.wagyourtail.jvmdg.j27.stub.java_base.J_L_StrictMath;
import xyz.wagyourtail.jvmdg.j27.stub.java_base.J_L_String;

public class Java27Downgrader extends AllVersionDowngrader {

    public Java27Downgrader() {
        super(Opcodes.V27, Opcodes.V26, 0);
    }

    @Override
    public void init() {
        stub(J_L_Math.class);
        stub(J_L_StrictMath.class);
        stub(J_L_String.class);
    }

}
