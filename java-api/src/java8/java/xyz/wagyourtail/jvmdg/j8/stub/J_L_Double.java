package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

public class J_L_Double {

     @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/lang/Double"))
    public static final int BYTES = Double.SIZE / Byte.SIZE;

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/lang/Double"))
    public static boolean isFinite(double d) {
        return Math.abs(d) <= Double.MAX_VALUE;
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/lang/Double"))
    public static int hashCode(double value) {
        return J_L_Long.hashCode(Double.doubleToLongBits(value));
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/lang/Double"))
    public static double sum(double a, double b) {
        return a + b;
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/lang/Double"))
    public static double max(double a, double b) {
        return Math.max(a, b);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/lang/Double"))
    public static double min(double a, double b) {
        return Math.min(a, b);
    }
}
