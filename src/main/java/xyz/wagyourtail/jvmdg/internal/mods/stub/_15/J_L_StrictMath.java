package xyz.wagyourtail.jvmdg.internal.mods.stub._15;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

public class J_L_StrictMath {

    @Stub(value = JavaVersion.VERSION_15, desc = "Ljava/lang/StrictMath;")
    public static int absEaxct(int a) {
        return Math.absExact(a);
    }

    @Stub(value = JavaVersion.VERSION_15, desc = "Ljava/lang/StrictMath;")
    public static long absExact(long a) {
        return Math.absExact(a);
    }
}
