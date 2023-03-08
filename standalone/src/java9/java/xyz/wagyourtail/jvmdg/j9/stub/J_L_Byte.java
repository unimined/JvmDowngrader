package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

public class J_L_Byte {

    @Stub(javaVersion = Opcodes.V9, ref = @Ref("Ljava/lang/Byte;"))
    public static int compareUnsigned(byte x, byte y) {
        return Byte.toUnsignedInt(x) - Byte.toUnsignedInt(y);
    }

}
