package xyz.wagyourtail.jvmdg.internal.mods.stub._15;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

public class J_L_Math {

    @Stub(value = JavaVersion.VERSION_15, desc = "Ljava/lang/Math;")
    public static int absExact(int a) {
        if (a == Integer.MIN_VALUE) {
            throw new ArithmeticException("Overflow to represent absolute value of Integer.MIN_VALUE");
        }
        return Math.abs(a);
    }

    @Stub(value = JavaVersion.VERSION_15, desc = "Ljava/lang/Math;")
    public static long absExact(long a) {
        if (a == Long.MIN_VALUE) {
            throw new ArithmeticException("Overflow to represent absolute value of Long.MIN_VALUE");
        }
        return Math.abs(a);
    }
}
