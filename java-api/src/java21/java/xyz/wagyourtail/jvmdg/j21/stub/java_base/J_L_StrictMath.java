package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_StrictMath {

    @Stub(ref = @Ref("java/lang/StrictMath"))
    public static double clamp(double value, double min, double max) {
        return Math.min(Math.max(value, min), max);
    }

    @Stub(ref = @Ref("java/lang/StrictMath"))
    public static float clamp(float value, float min, float max) {
        return Math.min(Math.max(value, min), max);
    }

    @Stub(ref = @Ref("java/lang/StrictMath"))
    public static int clamp(long value, int min, int max) {
        return (int) Math.min(Math.max(value, min), max);
    }

    @Stub(ref = @Ref("java/lang/StrictMath"))
    public static long clamp(long value, long min, long max) {
        return Math.min(Math.max(value, min), max);
    }

}
