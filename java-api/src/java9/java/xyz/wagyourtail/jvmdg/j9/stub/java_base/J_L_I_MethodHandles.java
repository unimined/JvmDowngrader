package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.exc.MissingStubError;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.JEP;
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
        if (arrayClass == null || !arrayClass.isArray()) {
            throw new IllegalArgumentException("not an array class: " + arrayClass);
        }
        return MethodHandles.insertArguments(ArrayCtor, 0, arrayClass);
    }


    @Stub(ref = @Ref("java/lang/invoke/MethodHandles"))
    public static MethodHandles.Lookup privateLookupIn(Class<?> cls, MethodHandles.Lookup lookup) throws Throwable {
        Objects.requireNonNull(cls);
        Objects.requireNonNull(lookup);
        if ((lookup.lookupModes() & MethodHandles.Lookup.PRIVATE) == 0) {
            throw new IllegalArgumentException(lookup + " does not have private access");
        }
        if (cls.isPrimitive() || cls.isArray()) {
            throw new IllegalArgumentException(cls + " is not a class with members");
        }
        // emulate the module system's refusal to private-lookup into packages
        // that are not open to the caller (all java.* packages on pre-module
        // runtimes)
        Package pkg = cls.getPackage();
        String pkgName = pkg != null ? pkg.getName() : "";
        if (cls.getClassLoader() == null ||
            pkgName.startsWith("java.") || pkgName.startsWith("jdk.") || pkgName.startsWith("sun.")) {
            throw new IllegalAccessException("module java.base does not open " + pkgName + " to unnamed module");
        }
        return (MethodHandles.Lookup) LookupCtor.invokeExact(cls);
    }


    @JEP(193)
    @Stub(ref = @Ref("java/lang/invoke/MethodHandles"))
    public static J_L_I_VarHandle arrayElementVarHandle(Class<?> cls) {
        Objects.requireNonNull(cls);
        if (!cls.isArray()) {
            throw new IllegalArgumentException();
        }
        return J_L_I_VarHandle.ofArray(cls);
    }

    @JEP(193)
    @Stub(ref = @Ref("java/lang/invoke/MethodHandles"))
    public static J_L_I_VarHandle byteArrayViewVarHandle(Class<?> viewClass, ByteOrder byteOrder) {
        Objects.requireNonNull(viewClass);
        Objects.requireNonNull(byteOrder);
        if (!viewClass.isArray()) {
            throw new IllegalArgumentException("not an array: " + viewClass);
        }
        Class<?> componentType = viewClass.getComponentType();
        if (!componentType.isPrimitive() || componentType == byte.class || componentType == boolean.class) {
            throw new IllegalArgumentException("not a primitive type array: " + viewClass);
        }
        return J_L_I_VarHandle.ofByteArrayView(viewClass, byteOrder != ByteOrder.nativeOrder());
    }

    @JEP(193)
    @Stub(ref = @Ref("java/lang/invoke/MethodHandles"))
    public static J_L_I_VarHandle byteBufferViewVarHandle(Class<?> viewClass, ByteOrder byteOrder) {
        Objects.requireNonNull(viewClass);
        Objects.requireNonNull(byteOrder);
        if (!viewClass.isArray()) {
            throw new IllegalArgumentException("not an array: " + viewClass);
        }
        Class<?> componentType = viewClass.getComponentType();
        if (!componentType.isPrimitive() || componentType == byte.class || componentType == boolean.class) {
            throw new IllegalArgumentException("not a primitive type array: " + viewClass);
        }
        return J_L_I_VarHandle.ofByteBufferView(viewClass, byteOrder != ByteOrder.nativeOrder());
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

        @Stub
        @JEP(193)
        public static J_L_I_VarHandle findVarHandle(MethodHandles.Lookup lookup, Class<?> owner, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            Objects.requireNonNull(owner);
            Objects.requireNonNull(name);
            Objects.requireNonNull(type);
            for (Field declaredField : owner.getDeclaredFields()) {
                if (declaredField.getName().equals(name) && declaredField.getType().equals(type)) {
                    if ((declaredField.getModifiers() & Opcodes.ACC_STATIC) != 0) {
                        throw new IllegalAccessException();
                    }
                    return J_L_I_VarHandle.of(declaredField);
                }
            }
            throw new NoSuchFieldException();
        }

        @Stub
        @JEP(193)
        public static J_L_I_VarHandle findStaticVarHandle(MethodHandles.Lookup lookup, Class<?> owner, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            Objects.requireNonNull(owner);
            Objects.requireNonNull(name);
            Objects.requireNonNull(type);
            for (Field declaredField : owner.getDeclaredFields()) {
                if (declaredField.getName().equals(name) && declaredField.getType().equals(type)) {
                    if ((declaredField.getModifiers() & Opcodes.ACC_STATIC) == 0) {
                        throw new IllegalAccessException();
                    }
                    return J_L_I_VarHandle.of(declaredField);
                }
            }
            throw new NoSuchFieldException();
        }

        @Stub
        @JEP(193)
        public static J_L_I_VarHandle unreflectVarHandle(MethodHandles.Lookup lookup, Field field) {
            Objects.requireNonNull(field);
            return J_L_I_VarHandle.of(field);
        }
        @Stub
        public static Class<?> defineClass(MethodHandles.Lookup lookup, byte[] bytes) throws Throwable {
            Objects.requireNonNull(bytes);
            DefiningLoader loader = new DefiningLoader(lookup.lookupClass().getClassLoader());
            // check if classdowngrader is available
            try {
                Class.forName("xyz.wagyourtail.jvmdg.ClassDowngrader");
            } catch (ClassNotFoundException e) {
                return loader.defineClass0(bytes, 0, bytes.length);
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
                    c = loader.defineClass0(entry.getValue(), 0, entry.getValue().length);
                } else {
                    loader.defineClass0(entry.getValue(), 0, entry.getValue().length);
                }
            }
            if (c == null) throw new IllegalStateException("class " + name + " not found in outputs");
            return c;
        }

        /**
         * Defines classes through a {@link ClassLoader} subclass because
         * {@code defineClass} is protected; the deprecated
         * {@code ClassLoader#defineClass(byte[], int, int)} that was used
         * before no longer exists on JDK 11+.
         */
        private static class DefiningLoader extends ClassLoader {
            DefiningLoader(ClassLoader parent) {
                super(parent);
            }

            final Class<?> defineClass0(byte[] b, int off, int len) {
                return super.defineClass(null, b, off, len);
            }
        }

    }

}
