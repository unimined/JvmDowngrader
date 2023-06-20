package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_Byte {

    @Stub(opcVers = Opcodes.V9, ref = @Ref("Ljava/lang/Byte;"))
    public static int compareUnsigned(byte x, byte y) {
        return Byte.toUnsignedInt(x) - Byte.toUnsignedInt(y);
    }

}
