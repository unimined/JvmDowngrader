package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.*;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class J_L_I_MethodHandles {
    private static final MethodHandles.Lookup IMPL_LOOKUP = Utils.getImplLookup();
    private static final MethodHandle LookupCtor;
    private static final MethodHandle ArrayCtor;

    static {
        try {
            LookupCtor = IMPL_LOOKUP.findConstructor(MethodHandles.Lookup.class, MethodType.methodType(void.class, Class.class));
            ArrayCtor = IMPL_LOOKUP.findStatic(Array.class, "newInstance", MethodType.methodType(Object.class, Class.class, int.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Stub(ref = @Ref("java/lang/invoke/MethodHandles"))
    public static MethodHandle arrayConstructor(Class<?> arrayClass) {
        return MethodHandles.insertArguments(ArrayCtor, 0, arrayClass);
    }


    @Stub(ref = @Ref("java/lang/invoke/MethodHandles"))
    public static MethodHandles.Lookup privateLookupIn(Class<?> cls, MethodHandles.Lookup lookup) throws Throwable {
        return (MethodHandles.Lookup) LookupCtor.invokeExact(cls);
    }

    public static class Lookup {
        private static final MethodHandle DefineClass;

        static {
            try {
                DefineClass = IMPL_LOOKUP.findVirtual(ClassLoader.class, "defineClass", MethodType.methodType(Class.class, byte[].class, int.class, int.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Stub(requiresRuntime = true)
        public static Class<?> defineClass(MethodHandles.Lookup lookup, byte[] bytes) throws Throwable {
            Objects.requireNonNull(bytes);
            ClassLoader loader = lookup.lookupClass().getClassLoader();
            // check if classdowngrader is available
            try {
                Class.forName("xyz.wagyourtail.jvmdg.ClassDowngrader");
            } catch (ClassNotFoundException e) {
                return (Class<?>) DefineClass.invokeExact(loader, bytes, 0, bytes.length);
            }
            AtomicReference<String> name = new AtomicReference<>(null);
            Map<String, byte[]> classBytes = ClassDowngrader.getCurrentVersionDowngrader().downgrade(name, bytes, true, (s) -> {
                URL url = loader.getResource(s + ".class");
                if (url == null) return null;
                try {
                    return Utils.readAllBytes(url.openStream());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            Class<?> c = null;
            for (Map.Entry<String, byte[]> entry : classBytes.entrySet()) {
                if (Objects.equals(entry.getKey(), name.get())) {
                    c = (Class<?>) DefineClass.invokeExact(loader, entry.getValue(), 0, entry.getValue().length);
                } else {
                    DefineClass.invokeExact(loader, entry.getValue(), 0, entry.getValue().length);
                }
            }
            if (c == null) throw new IllegalStateException("class " + name + " not found in outputs");
            return c;
        }

    }
}
