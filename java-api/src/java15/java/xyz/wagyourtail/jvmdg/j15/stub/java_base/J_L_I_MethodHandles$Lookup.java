package xyz.wagyourtail.jvmdg.j15.stub.java_base;


import sun.misc.Unsafe;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.instrument.IllegalClassFormatException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

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

    @Stub
    public static Class<?> ensureInitialized(MethodHandles.Lookup lookup, Class<?> c) {
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

    @Stub(requiresRuntime = true)
    public static Class<?> defineHiddenClass(MethodHandles.Lookup lookup, byte[] bytes, boolean initialize, ClassOption... options) throws IllegalClassFormatException {
        Objects.requireNonNull(bytes);
        Objects.requireNonNull(options);
        HiddenClassLoader loader = new HiddenClassLoader(lookup.lookupClass().getClassLoader());
        // check if classdowngrader is available
        try {
            Class.forName("xyz.wagyourtail.jvmdg.ClassDowngrader");
        } catch (ClassNotFoundException e) {
            return loader.defineClass0(bytes, 0, bytes.length);
        }
        AtomicReference<String> name = new AtomicReference<>(null);
        Map<String, byte[]> classBytes = ClassDowngrader.getCurrentVersionDowngrader().downgrade(name, bytes, true, loader);
        Class<?> c = null;
        for (Map.Entry<String, byte[]> entry : classBytes.entrySet()) {
            if (entry.getKey().startsWith("META-INF/versions")) continue;
            if (Objects.equals(entry.getKey(), name.get())) {
                c = loader.defineClass0(entry.getValue(), 0, entry.getValue().length);
            } else {
                loader.defineClass0(entry.getValue(), 0, entry.getValue().length);
            }
        }
        if (c == null) throw new IllegalStateException("class " + name + " not found in outputs");
        return c;
    }

    @Adapter("Ljava/lang/invoke/MethodHandles$Lookup$ClassOption;")
    public enum ClassOption {
        NESTMATE(0x00000001),
        STRONG(0x00000004);

        private final int flag;

        ClassOption(int flag) {
            this.flag = flag;
        }
    }

    public static class HiddenClassLoader extends ClassLoader implements Function<String, byte[]> {

        public HiddenClassLoader(ClassLoader parent) {
            super(parent);
        }

        public final Class<?> defineClass0(byte[] b, int off, int len) {
            return super.defineClass(null, b, off, len);
        }

        @Override
        public byte[] apply(String s) {
            URL url = getResource(s + ".class");
            if (url == null) return null;
            try {
                return Utils.readAllBytes(url.openStream());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

}
