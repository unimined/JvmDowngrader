package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.exc.MissingStubError;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.ByteOrder;
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


    @Stub(ref = @Ref("java/lang/invoke/MethodHandles"))
    public static J_L_I_VarHandle arrayElementVarHandle(Class<?> cls) {
        Objects.requireNonNull(cls);
        if (!cls.isArray()) {
            throw new IllegalArgumentException();
        }
        return new J_L_I_VarHandle(cls);
    }

    @Stub(ref = @Ref("java/lang/invoke/MethodHandles"))
    public static MethodHandle arrayLength(Class<?> arrayClass) throws NoSuchMethodException, IllegalAccessException {
        Objects.requireNonNull(arrayClass);
        if (!arrayClass.isArray()) {
            throw new IllegalArgumentException();
        }
        return IMPL_LOOKUP.findStatic(Array.class, "getLength", MethodType.methodType(int.class, Object.class)).asType(MethodType.methodType(int.class, arrayClass));
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

        @Stub
        public static J_L_I_VarHandle findVarHandle(MethodHandles.Lookup lookup, Class<?> owner, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            Objects.requireNonNull(owner);
            Objects.requireNonNull(name);
            Objects.requireNonNull(type);
            for (Field declaredField : owner.getDeclaredFields()) {
                if (declaredField.getName().equals(name) && declaredField.getType().equals(type)) {
                    if ((declaredField.getModifiers() & Opcodes.ACC_STATIC) != 0) {
                        throw new IllegalAccessException();
                    }
                    return new J_L_I_VarHandle(declaredField);
                }
            }
            throw new NoSuchFieldException();
        }

        @Stub
        public static J_L_I_VarHandle findStaticVarHandle(MethodHandles.Lookup lookup, Class<?> owner, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            Objects.requireNonNull(owner);
            Objects.requireNonNull(name);
            Objects.requireNonNull(type);
            for (Field declaredField : owner.getDeclaredFields()) {
                if (declaredField.getName().equals(name) && declaredField.getType().equals(type)) {
                    if ((declaredField.getModifiers() & Opcodes.ACC_STATIC) == 0) {
                        throw new IllegalAccessException();
                    }
                    return new J_L_I_VarHandle(declaredField);
                }
            }
            throw new NoSuchFieldException();
        }

        @Stub
        public static J_L_I_VarHandle unreflectVarHandle(MethodHandles.Lookup lookup, Field field) {
            Objects.requireNonNull(field);
            return new J_L_I_VarHandle(field);
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
                if (entry.getKey().startsWith("META-INF/versions")) continue;
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
