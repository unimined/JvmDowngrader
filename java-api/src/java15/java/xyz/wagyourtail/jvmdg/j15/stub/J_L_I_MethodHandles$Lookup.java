package xyz.wagyourtail.jvmdg.j15.stub;


import org.objectweb.asm.Opcodes;
import sun.misc.Unsafe;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Objects;

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

    @Stub(javaVersion = Opcodes.V15)
    public static Class<?> defineHiddenClass(MethodHandles.Lookup lookup, byte[] bytes, boolean initialize, J_L_I_MethodHandles$Lookup$ClassOption... options) {
        Objects.requireNonNull(bytes);
        Objects.requireNonNull(options);

        //TODO: runtime transform this class down
        return new HiddenClassLoader(lookup.lookupClass().getClassLoader()).defineClass0(bytes, 0, bytes.length);
    }

    static class HiddenClassLoader extends ClassLoader {

        public HiddenClassLoader(ClassLoader parent) {
            super(parent);
        }

        public final Class<?> defineClass0(byte[] b, int off, int len) {
            return super.defineClass(null, b, off, len);
        }
    }

}
