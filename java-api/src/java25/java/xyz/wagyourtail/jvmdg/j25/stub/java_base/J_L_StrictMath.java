package xyz.wagyourtail.jvmdg.j25.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_StrictMath {

    @Stub(ref = @Ref("Ljava/lang/StrictMath;"))
    public static int powExact(int x, int n) {
        return J_L_Math.powExact(x, n);
    }

    @Stub(ref = @Ref("Ljava/lang/StrictMath;"))
    public static long powExact(long x, int n) {
        return J_L_Math.powExact(x, n);
    }

    @Stub(ref = @Ref("Ljava/lang/StrictMath;"))
    public static int unsignedMultiplyExact(int x, int y) {
        return J_L_Math.unsignedMultiplyExact(x, y);
    }

    @Stub(ref = @Ref("Ljava/lang/StrictMath;"))
    public static long unsignedMultiplyExact(long x, int y) {
        return J_L_Math.unsignedMultiplyExact(x, y);
    }

    @Stub(ref = @Ref("Ljava/lang/StrictMath;"))
    public static long unsignedMultiplyExact(long x, long y) {
        return J_L_Math.unsignedMultiplyExact(x, y);
    }

    @Stub(ref = @Ref("Ljava/lang/StrictMath;"))
    public static int unsignedPowExact(int x, int n) {
        return J_L_Math.unsignedPowExact(x, n);
    }

    @Stub(ref = @Ref("Ljava/lang/StrictMath;"))
    public static long unsignedPowExact(long x, int n) {
        return J_L_Math.unsignedPowExact(x, n);
    }

}
