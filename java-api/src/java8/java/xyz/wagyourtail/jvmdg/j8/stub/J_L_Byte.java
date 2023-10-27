package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_Byte {

    @Stub(ref = @Ref("java/lang/Byte"))
    public static int hashCode(byte value) {
        return value;
    }

    @Stub(ref = @Ref("java/lang/Byte"))
    public static int toUnsignedInt(byte value) {
        return value & 0xFF;
    }

    @Stub(ref = @Ref("java/lang/Byte"))
    public static long toUnsignedLong(byte value) {
        return value & 0xFFL;
    }


}
