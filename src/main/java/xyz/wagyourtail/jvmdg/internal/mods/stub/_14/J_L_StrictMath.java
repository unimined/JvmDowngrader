package xyz.wagyourtail.jvmdg.internal.mods.stub._14;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

public class J_L_StrictMath {

    @Stub(value = JavaVersion.VERSION_14, desc = "Ljava/lang/StrictMath;")
    public static int incrementExact(int i) {
        return Math.incrementExact(i);
    }

    @Stub(value = JavaVersion.VERSION_14, desc = "Ljava/lang/StrictMath;")
    public static long incrementExact(long i) {
        return Math.incrementExact(i);
    }

    @Stub(value = JavaVersion.VERSION_14, desc = "Ljava/lang/StrictMath;")
    public static int decrementExact(int i) {
        return Math.decrementExact(i);
    }

    @Stub(value = JavaVersion.VERSION_14, desc = "Ljava/lang/StrictMath;")
    public static long decrementExact(long i) {
        return Math.decrementExact(i);
    }

    @Stub(value = JavaVersion.VERSION_14, desc = "Ljava/lang/StrictMath;")
    public static int negateExact(int i) {
        return Math.negateExact(i);
    }

    @Stub(value = JavaVersion.VERSION_14, desc = "Ljava/lang/StrictMath;")
    public static long negateExact(long i) {
        return Math.negateExact(i);
    }
}
