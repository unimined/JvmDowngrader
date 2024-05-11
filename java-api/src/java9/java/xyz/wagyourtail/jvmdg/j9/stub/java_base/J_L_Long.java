package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_Long {

    @Stub(ref = @Ref("java/lang/Long"))
    public static long parseLong(CharSequence s, int begin, int end, int radix) {
        return Long.parseLong(s.subSequence(begin, end).toString(), radix);
    }

    @Stub(ref = @Ref("java/lang/Long"))
    public static long parseUnsignedLong(CharSequence s, int begin, int end, int radix) {
        return Long.parseUnsignedLong(s.subSequence(begin, end).toString(), radix);
    }
}
