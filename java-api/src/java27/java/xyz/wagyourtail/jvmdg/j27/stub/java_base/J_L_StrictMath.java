package xyz.wagyourtail.jvmdg.j27.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_StrictMath {

    @Stub(ref = @Ref("java/lang/StrictMath"))
    public static double acosh(double x) {
        return J_L_Math.acosh(x);
    }

    @Stub(ref = @Ref("java/lang/StrictMath"))
    public static double asinh(double x) {
        return J_L_Math.asinh(x);
    }

    @Stub(ref = @Ref("java/lang/StrictMath"))
    public static double atanh(double x) {
        return J_L_Math.atanh(x);
    }

}
