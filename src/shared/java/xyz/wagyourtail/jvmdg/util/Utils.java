package xyz.wagyourtail.jvmdg.util;

import org.objectweb.asm.Opcodes;
import sun.misc.Unsafe;
import sun.reflect.ReflectionFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipOutputStream;

public class Utils {
    private static Unsafe cachedUnsafe;
    private static MethodHandles.Lookup cachedImplLookup;

    public static Unsafe getUnsafe() {
        if (cachedUnsafe != null) {
            return cachedUnsafe;
        }
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return cachedUnsafe = (Unsafe) f.get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new UnsupportedOperationException("Unable to get Unsafe instance", e);
        }
    }

    public static MethodHandles.Lookup getImplLookup() {
        if (cachedImplLookup != null) {
            return cachedImplLookup;
        }

        // ensure lookup is initialized
        MethodHandles.lookup();

        // try to get lookup
        MethodHandles.Lookup lookup = getImplLookupWithUnsafe();
        if (lookup == null) {
            lookup = getImplLookupWithSerialization();
        }
        if (lookup == null) {
            lookup = constructImplLookupWithReflection();
        }
        if (lookup == null) {
            lookup = constructImplLookupWithSerialization();
        }
        if (lookup == null) {
            throw new UnsupportedOperationException("Unable to get or construct IMPL_LOOKUP");
        }
        return cachedImplLookup = lookup;
    }

    private static MethodHandles.Lookup getImplLookupWithUnsafe() {
        try {
            // get the impl_lookup field
            Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            Unsafe unsafe = getUnsafe();
            MethodHandles.Lookup IMPL_LOOKUP;
            IMPL_LOOKUP = (MethodHandles.Lookup) unsafe.getObject(MethodHandles.Lookup.class, unsafe.staticFieldOffset(implLookupField));
            return IMPL_LOOKUP;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static MethodHandles.Lookup constructImplLookupWithReflection() {
        try {
            Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
            constructor.setAccessible(true);
            return constructor.newInstance(Object.class, -1);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static MethodHandles.Lookup getImplLookupWithSerialization() {
        ReflectionFactory factory = ReflectionFactory.getReflectionFactory();
        try {
            // create constructor for lookup
            Constructor<MethodHandles.Lookup> constructor = (Constructor) factory.newConstructorForSerialization(MethodHandles.Lookup.class, MethodHandles.Lookup.class.getDeclaredConstructor(Class.class));
            // get private lookup for lookup class
            MethodHandles.Lookup lookup = constructor.newInstance(MethodHandles.Lookup.class);
            // use private lookup to access IMPL_LOOKUP field
            MethodHandle getter = lookup.findStaticGetter(MethodHandles.Lookup.class, "IMPL_LOOKUP", MethodHandles.Lookup.class);
            return (MethodHandles.Lookup) getter.invokeExact();
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static MethodHandles.Lookup constructImplLookupWithSerialization() {
        ReflectionFactory factory = ReflectionFactory.getReflectionFactory();
        try {
            // create constructor for lookup
            Constructor<MethodHandles.Lookup> constructor = (Constructor) factory.newConstructorForSerialization(MethodHandles.Lookup.class, MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class));
            return constructor.newInstance(Object.class, -1);
        } catch (Throwable ignored) {
        }
        return null;
    }
    public static FileSystem openZipFileSystem(Path path, boolean create) throws IOException {
        if (create && !Files.exists(path)) {
            new ZipOutputStream(Files.newOutputStream(path)).close();
        }
        return FileSystems.newFileSystem(path, null);
    }

    public static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    public static int getCurrentClassVersion() {
        String version = System.getProperty("java.class.version");
        if (version != null) {
            try {
                return Integer.parseInt(version.split("\\.")[0]);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        throw new UnsupportedOperationException("Unable to determine current class version");
    }

    public static int normalizeVersion(int version) {
        if (version == Opcodes.V1_1) return version;
        return version & 0xFFFF;
    }

    public static int classVersionToMajorVersion(int version) {
        if (version == Opcodes.V1_1) return 1;
        return (version & 0xFFFF) - Opcodes.V1_2 + 2;
    }

    public static int majorVersionToClassVersion(int version) {
        if (version == 1) return Opcodes.V1_1;
        else return version + Opcodes.V1_2 - 2;
    }

    public static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    public static String getDescForClass(Class<?> cls) {
        if (cls.isPrimitive()) {
            if (cls == void.class) return "V";
            if (cls == boolean.class) return "Z";
            if (cls == byte.class) return "B";
            if (cls == char.class) return "C";
            if (cls == short.class) return "S";
            if (cls == int.class) return "I";
            if (cls == long.class) return "J";
            if (cls == float.class) return "F";
            if (cls == double.class) return "D";
        }
        if (cls.isArray()) {
            return "[" + getDescForClass(cls.getComponentType());
        }
        return "L" + cls.getName().replace('.', '/') + ";";
    }

    public static Class<?> getClassForDesc(String desc) throws ClassNotFoundException {
        if (desc.length() == 1) {
            switch (desc) {
                case "Z":
                    return boolean.class;
                case "B":
                    return byte.class;
                case "C":
                    return char.class;
                case "S":
                    return short.class;
                case "I":
                    return int.class;
                case "J":
                    return long.class;
                case "F":
                    return float.class;
                case "D":
                    return double.class;
                case "V":
                    return void.class;
                default:
                    throw new ClassNotFoundException("Unable to determine class for " + desc);
            }
        }
        if (desc.startsWith("[")) {
            int dims = 0;
            for (int i = 0; i < desc.length(); i++) {
                if (desc.charAt(i) != '[') {
                    break;
                }
                dims++;
            }
            Class<?> type = getClassForDesc(desc.substring(dims));
            return Array.newInstance(type, new int[dims]).getClass();
        }
        return Class.forName(desc.substring(1, desc.length() - 1).replace('/', '.'));
    }

    public static Class<?> getBoxFor(Class<?> prim) {
        if (!prim.isPrimitive()) {
            throw new IllegalArgumentException("type " + prim + " is not a primitive");
        }
        switch (prim.getName()) {
            case "boolean":
                return Boolean.class;
            case "byte":
                return Byte.class;
            case "char":
                return Character.class;
            case "short":
                return Short.class;
            case "int":
                return Integer.class;
            case "long":
                return Long.class;
            case "float":
                return Float.class;
            case "double":
                return Double.class;
            case "void":
                return Void.class;
            default:
                throw new IllegalArgumentException("type " + prim + " not found");
        }
    }

    public static boolean isReflectionFrame(String className) {
        return className.equals(Method.class.getName()) ||
            className.equals(Constructor.class.getName()) ||
            className.startsWith("sun.reflect.") ||
            className.startsWith("jdk.internal.reflect.") ||
            className.startsWith("java.lang.invoke.LambdaForm");
    }

    public static Class<?> getCaller(MethodHandles.Lookup lookup) throws ClassNotFoundException {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (int i = 2; i < stack.length; i++) {
            String className = stack[i].getClassName();
            if (!isReflectionFrame(className)) {
                return Class.forName(className, false, lookup.lookupClass().getClassLoader());
            }
        }
        throw new ClassNotFoundException("Could not find caller class???");
    }

    public static boolean equals(
        byte[] a, int aFromIndex, int aToIndex,
        byte[] b, int bFromIndex, int bToIndex
    ) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength) {
            return false;
        }
        if (aLength == 0) {
            return true;
        }
        for (int i = 0; i < aLength; i++) {
            if (a[aFromIndex + i] != b[bFromIndex + i]) {
                return false;
            }
        }
        return true;
    }

}
