package xyz.wagyourtail.jvmdg.util;

import org.objectweb.asm.Opcodes;
import sun.misc.Unsafe;
import sun.reflect.ReflectionFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipOutputStream;

public class Utils {
    private static volatile Unsafe cachedUnsafe;
    private static volatile MethodHandles.Lookup cachedImplLookup;

    public static Unsafe getUnsafe() {
        if (cachedUnsafe != null) {
            return cachedUnsafe;
        }
        synchronized (Utils.class) {
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
    }

    public static MethodHandles.Lookup getImplLookup() {
        if (cachedImplLookup != null) {
            return cachedImplLookup;
        }
        synchronized (Utils.class) {
            if (cachedImplLookup != null) {
                return cachedImplLookup;
            }
            return cachedImplLookup = getImplLookup0();
        }
    }

    private static MethodHandles.Lookup getImplLookup0() {

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
        return lookup;
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

    private static volatile MethodHandle hasStaticInitializerHandle;

    /**
     * whether the class has a static initializer ({@code <clinit>}), via
     * {@link java.io.ObjectStreamClass}'s internal check since reflection
     * cannot see it. Returns false when unavailable.
     */
    public static boolean hasStaticInitializer(Class<?> clazz) {
        MethodHandle handle = hasStaticInitializerHandle;
        if (handle == null) {
            synchronized (Utils.class) {
                handle = hasStaticInitializerHandle;
                if (handle == null) {
                    try {
                        handle = getImplLookup().findStatic(
                            java.io.ObjectStreamClass.class, "hasStaticInitializer",
                            java.lang.invoke.MethodType.methodType(boolean.class, Class.class));
                    } catch (Throwable ignored) {
                    }
                    hasStaticInitializerHandle = handle;
                }
            }
        }
        if (handle == null) {
            return false;
        }
        try {
            return (boolean) handle.invokeExact(clazz);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static FileSystem openZipFileSystem(Path path, boolean create) throws IOException {
        if (create && !Files.exists(path)) {
            new ZipOutputStream(Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)).close();
        }
        return FileSystems.newFileSystem(path, (ClassLoader) null);
    }

    /**
     * Open {@code url} bypassing the JVM-wide {@link URLConnection} cache —
     * the JDK caches {@code JarFile} handles by URL, so a jar rewritten
     * in-place (Gradle daemon/worker reuse across rebuilds) would otherwise
     * be read through stale offsets ("invalid LOC header").
     */
    public static InputStream openUncached(URL url) throws IOException {
        URLConnection conn = url.openConnection();
        conn.setUseCaches(false);
        return conn.getInputStream();
    }

    /**
     * {@link #openUncached(URL)} on the resource URL, falling back to
     * {@code getResourceAsStream} when the loader answers streams but not
     * URLs (custom loaders may override only the stream method).
     */
    public static InputStream openResource(ClassLoader loader, String name) throws IOException {
        URL url = loader == null ? ClassLoader.getSystemResource(name) : loader.getResource(name);
        if (url != null) return openUncached(url);
        return loader == null ? ClassLoader.getSystemResourceAsStream(name) : loader.getResourceAsStream(name);
    }

    /**
     * {@link #openResource(ClassLoader, String)} resolved relative to
     * {@code base}'s package, same stream-only fallback.
     */
    public static InputStream openResource(Class<?> base, String name) throws IOException {
        URL url = base.getResource(name);
        if (url != null) return openUncached(url);
        return base.getResourceAsStream(name);
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

    public static int classVersionToMajorVersion(int version) {
        if (version == Opcodes.V1_1) return 1;
        else return (version & 0xFFFF) - Opcodes.V1_2 + 2;
    }

    /**
     * The class-file version masked to its major — {@code ClassNode.version}
     * carries {@code minor << 16 | major} as read by ASM, and
     * {@code javac --enable-preview} sets the minor to {@code 0xFFFF}, so
     * raw {@code ==} comparisons against provider versions only work after
     * masking.
     */
    public static int majorClassVersion(int version) {
        return version & 0xFFFF;
    }

    /** a preview-compiled class file — its minor version is {@code 0xFFFF} (the only nonzero minor the JVMS allows) */
    public static boolean isPreviewVersion(int version) {
        return (version >>> 16) == 0xFFFF;
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

    /**
     * the class of the frame that invoked the caller of this method, skipping
     * reflection dispatch frames. Stack layout is
     * {@code [getStackTrace, getCaller, <invoker>, ...]} — starting the scan at
     * the invoker's frame would return the invoker's own class (e.g. the stub
     * method) rather than its caller.
     */
    public static Class<?> getCaller(MethodHandles.Lookup lookup) throws ClassNotFoundException {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        // locate this method's frame instead of assuming a fixed index: some
        // VMs omit the getStackTrace frame itself. The frame after getCaller
        // is its invoker, and we want the caller of *that*.
        int start = 3;
        for (int j = 0; j < stack.length; j++) {
            if (stack[j].getClassName().equals(Utils.class.getName())) {
                start = j + 2;
                break;
            }
        }
        for (int i = start; i < stack.length; i++) {
            String className = stack[i].getClassName();
            if (!isReflectionFrame(className)) {
                try {
                    return Class.forName(className, false, lookup.lookupClass().getClassLoader());
                } catch (ClassNotFoundException e) {
                    // the caller may not be visible to the lookup's loader;
                    // retry against the context loader before giving up
                    ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                    if (tccl != null && tccl != lookup.lookupClass().getClassLoader()) {
                        return Class.forName(className, false, tccl);
                    }
                    throw e;
                }
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

    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String getSha1(byte[] bytes) throws NoSuchAlgorithmException {
        return toHex(MessageDigest.getInstance("SHA-1").digest(bytes));
    }

    public static String getSha1(InputStream is) throws IOException, NoSuchAlgorithmException {
        MessageDigest digestSha1 = MessageDigest.getInstance("SHA-1");
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) != -1) {
            digestSha1.update(buffer, 0, read);
        }
        return toHex(digestSha1.digest());
    }

    public static String getSha1(Path path) throws IOException, NoSuchAlgorithmException {
        try (InputStream is = Files.newInputStream(path)) {
            return getSha1(is);
        }
    }

    public static String getSha1(File file) throws IOException, NoSuchAlgorithmException {
        return getSha1(file.toPath());
    }

    public static String getShortSha1(Path path) throws IOException, NoSuchAlgorithmException {
        return getSha1(path).substring(0, 7);
    }

    public static String getShortSha1(File file) throws IOException, NoSuchAlgorithmException {
        return getSha1(file).substring(0, 7);
    }

}
