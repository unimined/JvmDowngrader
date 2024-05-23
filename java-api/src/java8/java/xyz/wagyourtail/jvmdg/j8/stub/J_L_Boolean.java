package xyz.wagyourtail.jvmdg.j8.stub;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_Boolean {

    @Stub(ref = @Ref("java/lang/Boolean"))
    public static int hashCode(boolean value) {
        return value ? 1231 : 1237;
    }

    @Stub(ref = @Ref("java/lang/Boolean"))
    public static boolean logicalAnd(boolean a, boolean b) {
        return a && b;
    }

    @Stub(ref = @Ref("java/lang/Boolean"))
    public static boolean logicalOr(boolean a, boolean b) {
        return a || b;
    }

    @Stub(ref = @Ref("java/lang/Boolean"))
    public static boolean logicalXor(boolean a, boolean b) {
        return a ^ b;
    }

}
