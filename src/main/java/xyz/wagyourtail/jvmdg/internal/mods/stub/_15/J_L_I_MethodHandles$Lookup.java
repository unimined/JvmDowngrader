package xyz.wagyourtail.jvmdg.internal.mods.stub._15;

import org.gradle.api.JavaVersion;
import sun.misc.Unsafe;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.lang.reflect.Field;

public class J_L_I_MethodHandles$Lookup {

    @Stub(JavaVersion.VERSION_15)
    public static Class<?> ensureInitialized(Class<?> c) {
        if (c.isPrimitive()) {
            throw new IllegalArgumentException(c + " is a primitive class");
        } else if (c.isArray()) {
            throw new IllegalArgumentException(c + " is an array class");
        } else {
            // aquire the unsafe
            Unsafe unsafe;
            try {
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                unsafe = (Unsafe) theUnsafe.get(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            // initialize the class
            //noinspection removal
            unsafe.ensureClassInitialized(c);
        }
        return c;
    }
}
