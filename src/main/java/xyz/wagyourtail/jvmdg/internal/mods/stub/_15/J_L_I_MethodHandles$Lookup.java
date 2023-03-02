package xyz.wagyourtail.jvmdg.internal.mods.stub._15;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.UnsafeAccess;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

public class J_L_I_MethodHandles$Lookup {

    @Stub(value = JavaVersion.VERSION_15, include = {UnsafeAccess.class})
    public static Class<?> ensureInitialized(Class<?> c) {
        if (c.isPrimitive()) {
            throw new IllegalArgumentException(c + " is a primitive class");
        } else if (c.isArray()) {
            throw new IllegalArgumentException(c + " is an array class");
        } else {
            UnsafeAccess.ensureClassInitialized(c);
        }
        return c;
    }
}
