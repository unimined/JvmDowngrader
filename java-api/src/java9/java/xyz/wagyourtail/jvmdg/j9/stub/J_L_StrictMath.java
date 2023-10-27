package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_StrictMath {

    @Stub(ref = @Ref("Ljava/lang/StrictMath;"))
    public static long multiplyExact(long x, int y) {
        return J_L_Math.multiplyExact(x, y);
    }

    @Stub(ref = @Ref("Ljava/lang/StrictMath;"))
    public static long multiplyFull(int x, int y) {
        return (long) x * (long) y;
    }

    @Stub(ref = @Ref("Ljava/lang/StrictMath;"))
    public static long multiplyHigh(long x, long y) {
        return J_L_Math.multiplyHigh(x, y);
    }

    @Stub(ref = @Ref("Ljava/lang/StrictMath;"))
    public static long floorDiv(long x, int y) {
        return Math.floorDiv(x, (long) y);
    }

    @Stub(ref = @Ref("Ljava/lang/StrictMath;"))
    public static int floorMod(long x, int y) {
        return (int) Math.floorMod(x, (long) y);
    }

    @Stub(ref = @Ref("Ljava/lang/StrictMath;"))
    public static double fma(double x, double y, double z) {
        return J_L_Math.fma(x, y, z);
    }

    @Stub(ref = @Ref("Ljava/lang/StrictMath;"))
    public static float fma(float x, float y, float z) {
        return J_L_Math.fma(x, y, z);
    }

}
