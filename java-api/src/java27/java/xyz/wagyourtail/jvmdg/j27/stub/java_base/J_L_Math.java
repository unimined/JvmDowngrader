package xyz.wagyourtail.jvmdg.j27.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_Math {

    @Stub(ref = @Ref("java/lang/Math"))
    public static double acosh(double x) {
        if (x < 1.0) {
            return Double.NaN; // acosh is only defined for x >= 1
        }
        // Formula: ln(x + sqrt(x^2 - 1))
        return Math.log(x + Math.sqrt(x * x - 1.0));
    }

    @Stub(ref = @Ref("java/lang/Math"))
    public static double asinh(double x) {
        if (Double.isInfinite(x) || Double.isNaN(x)) {
            return x;
        }
        // Formula: ln(x + sqrt(x^2 + 1))
        return Math.log(x + Math.sqrt(x * x + 1.0));
    }

    @Stub(ref = @Ref("java/lang/Math"))
    public static double atanh(double x) {
        if (Math.abs(x) > 1.0) {
            return Double.NaN; // Undefined outside (-1, 1)
        }
        if (x == 1.0) {
            return Double.POSITIVE_INFINITY;
        }
        if (x == -1.0) {
            return Double.NEGATIVE_INFINITY;
        }
        // Formula: 0.5 * ln((1 + x) / (1 - x))
        return 0.5 * Math.log((1.0 + x) / (1.0 - x));
    }

}
