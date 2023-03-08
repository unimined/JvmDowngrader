package xyz.wagyourtail.jvmdg.j15.stub;


import org.objectweb.asm.Opcodes;
import sun.misc.Unsafe;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.lang.reflect.Field;

public class J_L_I_MethodHandles$Lookup {

    private static final Unsafe unsafe;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe) theUnsafe.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Stub(javaVersion = Opcodes.V15)
    public static Class<?> ensureInitialized(Class<?> c) {
        if (c.isPrimitive()) {
            throw new IllegalArgumentException(c + " is a primitive class");
        } else if (c.isArray()) {
            throw new IllegalArgumentException(c + " is an array class");
        } else {
            //noinspection removal
            unsafe.ensureClassInitialized(c);
        }
        return c;
    }

}
