package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_Short {
    @Stub(opcVers = Opcodes.V9, ref = @Ref("Ljava/lang/Short;"))
    public static int compareUnsigned(short x, short y) {
        return Short.toUnsignedInt(x) - Short.toUnsignedInt(y);
    }

}
