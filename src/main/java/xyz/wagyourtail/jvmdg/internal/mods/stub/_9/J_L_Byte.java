package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

public class J_L_Byte {

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/lang/Byte;")
    public static int compareUnsigned(byte x, byte y) {
        return Byte.toUnsignedInt(x) - Byte.toUnsignedInt(y);
    }
}
