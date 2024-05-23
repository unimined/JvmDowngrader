package xyz.wagyourtail.jvmdg.j8.stub;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_Short {

    @Stub(ref = @Ref("java/lang/Short"))
    public static final int BYTES = Short.SIZE / Byte.SIZE;

    @Stub(ref = @Ref("java/lang/Short"))
    public static int hashCode(short value) {
        return value;
    }

    @Stub(ref = @Ref("java/lang/Short"))
    public static int toUnsignedInt(short value) {
        return value & 0xFFFF;
    }

    @Stub(ref = @Ref("java/lang/Short"))
    public static long toUnsignedLong(short value) {
        return value & 0xFFFFL;
    }

}
