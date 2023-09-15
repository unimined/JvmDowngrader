package xyz.wagyourtail.jvmdg.cursed;

import java.lang.reflect.Field;

public class UnsafeWrapper {
    private static final Object unsafe;

    static {
        Object unsafe1;
        try {
            Field fd = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
            fd.setAccessible(true);
            unsafe1 = fd.get(null);
        } catch (Exception e) {
            unsafe1 = null;
        }
        unsafe = unsafe1;
    }

    public static Object getRawUnsafe() {
        return unsafe;
    }

    public static void putObject(Object obj, long offset, Object value) {
        if (unsafe == null) {
            throw new UnsupportedOperationException("Unsafe not found");
        }
        try {
            unsafe.getClass().getMethod("putObject", Object.class, long.class, Object.class).invoke(unsafe, obj, offset, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static long objectFieldOffset(Field field) {
        if (unsafe == null) {
            throw new UnsupportedOperationException("Unsafe not found");
        }
        try {
            return (long) unsafe.getClass().getMethod("objectFieldOffset", Field.class).invoke(unsafe, field);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
