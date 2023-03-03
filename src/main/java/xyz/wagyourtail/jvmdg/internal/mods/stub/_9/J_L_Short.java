package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

public class J_L_Short {
    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/lang/Short;")
    public static int compareUnsigned(short x, short y) {
        return Short.toUnsignedInt(x) - Short.toUnsignedInt(y);
    }
}
