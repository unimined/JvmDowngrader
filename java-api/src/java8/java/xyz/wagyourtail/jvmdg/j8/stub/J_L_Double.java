package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_Double {

    @Stub(ref = @Ref("java/lang/Double"))
    public static final int BYTES = Double.SIZE / Byte.SIZE;

    @Stub(ref = @Ref("java/lang/Double"))
    public static boolean isFinite(double d) {
        return Math.abs(d) <= Double.MAX_VALUE;
    }

    @Stub(ref = @Ref("java/lang/Double"))
    public static int hashCode(double value) {
        return J_L_Long.hashCode(Double.doubleToLongBits(value));
    }

    @Stub(ref = @Ref("java/lang/Double"))
    public static double sum(double a, double b) {
        return a + b;
    }

    @Stub(ref = @Ref("java/lang/Double"))
    public static double max(double a, double b) {
        return Math.max(a, b);
    }

    @Stub(ref = @Ref("java/lang/Double"))
    public static double min(double a, double b) {
        return Math.min(a, b);
    }
}
