package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

public class J_L_Byte {

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/lang/Byte"))
    public static int hashCode(byte value) {
        return value;
    }

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/lang/Byte"))
    public static int toUnsignedInt(byte value) {
        return value & 0xFF;
    }

    @Stub(javaVersion = Opcodes.V1_8, ref = @Ref("java/lang/Byte"))
    public static long toUnsignedLong(byte value) {
        return value & 0xFFL;
    }


}
