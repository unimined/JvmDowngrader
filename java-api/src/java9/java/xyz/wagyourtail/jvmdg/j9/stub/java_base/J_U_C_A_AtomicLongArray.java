package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.concurrent.atomic.AtomicLongArray;

public class J_U_C_A_AtomicLongArray {


    private static AtomicLongArray checkIndex(AtomicLongArray x, int i) {
        // VarHandle-style array ops throw ArrayIndexOutOfBoundsException,
        // not the plain IndexOutOfBoundsException the cast path throws
        if (i < 0 || i >= x.length()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return x;
    }
    @Stub
    public static long compareAndExchange(AtomicLongArray ref, int index, long expected, long newValue) {
        long v;
        do {
            v = checkIndex(ref, index).get(index);
            if (v != expected) return v;
        } while (!checkIndex(ref, index).compareAndSet(index, expected, newValue));
        return expected;
    }

    @Stub
    public static long compareAndExchangeAcquire(AtomicLongArray ref, int index, long expected, long newValue) {
        return compareAndExchange(ref, index, expected, newValue);
    }

    @Stub
    public static long compareAndExchangeRelease(AtomicLongArray ref, int index, long expected, long newValue) {
        return compareAndExchange(ref, index, expected, newValue);
    }

    @Stub
    public static long getAcquire(AtomicLongArray ref, int index) {
        return checkIndex(ref, index).get(index);
    }

    @Stub
    public static long getOpaque(AtomicLongArray ref, int index) {
        return checkIndex(ref, index).get(index);
    }

    @Stub
    public static long getPlain(AtomicLongArray ref, int index) {
        return checkIndex(ref, index).get(index);
    }

    @Stub
    public static void setOpaque(AtomicLongArray ref, int index, long newValue) {
        checkIndex(ref, index).set(index, newValue);
    }

    @Stub
    public static void setPlain(AtomicLongArray ref, int index, long newValue) {
        checkIndex(ref, index).set(index, newValue);
    }

    @Stub
    public static void setRelease(AtomicLongArray ref, int index, long newValue) {
        checkIndex(ref, index).set(index, newValue);
    }

    @Stub
    public static boolean weakCompareAndSetAcquire(AtomicLongArray ref, int index, long expected, long newValue) {
        return checkIndex(ref, index).weakCompareAndSet(index, expected, newValue);
    }

    @Stub
    public static boolean weakCompareAndSetPlain(AtomicLongArray ref, int index, long expected, long newValue) {
        return checkIndex(ref, index).weakCompareAndSet(index, expected, newValue);
    }

    @Stub
    public static boolean weakCompareAndSetRelease(AtomicLongArray ref, int index, long expected, long newValue) {
        return checkIndex(ref, index).weakCompareAndSet(index, expected, newValue);
    }

    @Stub
    public static boolean weakCompareAndSetVolatile(AtomicLongArray ref, int index, long expected, long newValue) {
        return checkIndex(ref, index).compareAndSet(index, expected, newValue);
    }

}
