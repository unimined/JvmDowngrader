package xyz.wagyourtail.jvmdg.j9.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_Integer {

    @Stub(ref = @Ref("java/lang/Integer"))
    public static int parseInt(CharSequence s, int begin, int end, int radix) {
        return Integer.parseInt(s.subSequence(begin, end).toString(), radix);
    }

    @Stub(ref = @Ref("java/lang/Integer"))
    public static int parseUnsignedInt(CharSequence s, int begin, int end, int radix) {
        return Integer.parseUnsignedInt(s.subSequence(begin, end).toString(), radix);
    }
}
