package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

public class J_L_StrictMath {

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/lang/StrictMath;")
    public static long multiplyExact(long x, int y) {
        return Math.multiplyExact(x, y);
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/lang/StrictMath;")
    public static long multiplyFull(int x, int y) {
        return (long) x * (long) y;
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/lang/StrictMath;")
    public static long multiplyHigh(long x, long y) {
        return Math.multiplyHigh(x, y);
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/lang/StrictMath;")
    public static long floorDiv(long x, int y) {
        return Math.floorDiv(x, (long) y);
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/lang/StrictMath;")
    public static int floorMod(long x, int y) {
        return (int) Math.floorMod(x, (long) y);
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/lang/StrictMath;")
    public static double fma(double x, double y, double z) {
        return Math.fma(x, y, z);
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/lang/StrictMath;")
    public static float fma(float x, float y, float z) {
        return Math.fma(x, y, z);
    }
}
