package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Objects;

public class J_L_Integer {

    @Stub(ref = @Ref("java/lang/Integer"))
    public static int parseInt(CharSequence s, int begin, int end, int radix) {
        Objects.requireNonNull(s);
        if (begin < 0 || begin > end || end > s.length()) {
            throw new IndexOutOfBoundsException("begin " + begin + ", end " + end + ", length " + s.length());
        }
        return Integer.parseInt(s.subSequence(begin, end).toString(), radix);
    }

    @Stub(ref = @Ref("java/lang/Integer"))
    public static int parseUnsignedInt(CharSequence s, int begin, int end, int radix) {
        Objects.requireNonNull(s);
        if (begin < 0 || begin > end || end > s.length()) {
            throw new IndexOutOfBoundsException("begin " + begin + ", end " + end + ", length " + s.length());
        }
        return Integer.parseUnsignedInt(s.subSequence(begin, end).toString(), radix);
    }

}
