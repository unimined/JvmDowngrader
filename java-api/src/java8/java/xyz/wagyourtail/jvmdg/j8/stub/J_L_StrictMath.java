package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_StrictMath {

    @Stub(ref = @Ref("java/lang/StrictMath"))
    public static int addExact(int x, int y) {
        return J_L_Math.addExact(x, y);
    }

    @Stub(ref = @Ref("java/lang/StrictMath"))
    public static long addExact(long x, long y) {
        return J_L_Math.addExact(x, y);
    }

    @Stub(ref = @Ref("java/lang/StrictMath"))
    public static int subtractExact(int x, int y) {
        return J_L_Math.subtractExact(x, y);
    }

    @Stub(ref = @Ref("java/lang/StrictMath"))
    public static long subtractExact(long x, long y) {
        return J_L_Math.subtractExact(x, y);
    }

    @Stub(ref = @Ref("java/lang/StrictMath"))
    public static int multiplyExact(int x, int y) {
        return J_L_Math.multiplyExact(x, y);
    }

    @Stub(ref = @Ref("java/lang/StrictMath"))
    public static long multiplyExact(long x, long y) {
        return J_L_Math.multiplyExact(x, y);
    }

    @Stub(ref = @Ref("java/lang/StrictMath"))
    public static int toIntExact(long a) {
        return J_L_Math.toIntExact(a);
    }

    @Stub(ref = @Ref("java/lang/StrictMath"))
    public static int floorDiv(int x, int y) {
        return J_L_Math.floorDiv(x, y);
    }

    @Stub(ref = @Ref("java/lang/StrictMath"))
    public static long floorDiv(long x, long y) {
        return J_L_Math.floorDiv(x, y);
    }

    @Stub(ref = @Ref("java/lang/StrictMath"))
    public static int floorMod(int x, int y) {
        return J_L_Math.floorMod(x, y);
    }

    @Stub(ref = @Ref("java/lang/StrictMath"))
    public static long floorMod(long x, long y) {
        return J_L_Math.floorMod(x, y);
    }

    @Stub(ref = @Ref("java/lang/StrictMath"))
    public static double nextDown(double d) {
        return J_L_Math.nextDown(d);
    }

    @Stub(ref = @Ref("java/lang/StrictMath"))
    public static float nextDown(float f) {
        return J_L_Math.nextDown(f);
    }

}
