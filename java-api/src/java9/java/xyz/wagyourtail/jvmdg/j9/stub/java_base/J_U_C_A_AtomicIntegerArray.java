package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.concurrent.atomic.AtomicIntegerArray;

public class J_U_C_A_AtomicIntegerArray {


    private static AtomicIntegerArray checkIndex(AtomicIntegerArray x, int i) {
        // VarHandle-style array ops throw ArrayIndexOutOfBoundsException,
        // not the plain IndexOutOfBoundsException the cast path throws
        if (i < 0 || i >= x.length()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return x;
    }
    @Stub
    public static int compareAndExchange(AtomicIntegerArray ref, int index, int expected, int newValue) {
        int v;
        do {
            v = checkIndex(ref, index).get(index);
            if (v != expected) return v;
        } while (!checkIndex(ref, index).compareAndSet(index, expected, newValue));
        return expected;
    }

    @Stub
    public static int compareAndExchangeAcquire(AtomicIntegerArray ref, int index, int expected, int newValue) {
        return compareAndExchange(ref, index, expected, newValue);
    }

    @Stub
    public static int compareAndExchangeRelease(AtomicIntegerArray ref, int index, int expected, int newValue) {
        return compareAndExchange(ref, index, expected, newValue);
    }

    @Stub
    public static int getAcquire(AtomicIntegerArray ref, int index) {
        return checkIndex(ref, index).get(index);
    }

    @Stub
    public static int getOpaque(AtomicIntegerArray ref, int index) {
        return checkIndex(ref, index).get(index);
    }

    @Stub
    public static int getPlain(AtomicIntegerArray ref, int index) {
        return checkIndex(ref, index).get(index);
    }

    @Stub
    public static void setOpaque(AtomicIntegerArray ref, int index, int newValue) {
        checkIndex(ref, index).set(index, newValue);
    }

    @Stub
    public static void setPlain(AtomicIntegerArray ref, int index, int newValue) {
        checkIndex(ref, index).set(index, newValue);
    }

    @Stub
    public static void setRelease(AtomicIntegerArray ref, int index, int newValue) {
        checkIndex(ref, index).set(index, newValue);
    }

    @Stub
    public static boolean weakCompareAndSetAcquire(AtomicIntegerArray ref, int index, int expected, int newValue) {
        return checkIndex(ref, index).weakCompareAndSet(index, expected, newValue);
    }

    @Stub
    public static boolean weakCompareAndSetPlain(AtomicIntegerArray ref, int index, int expected, int newValue) {
        return checkIndex(ref, index).weakCompareAndSet(index, expected, newValue);
    }

    @Stub
    public static boolean weakCompareAndSetRelease(AtomicIntegerArray ref, int index, int expected, int newValue) {
        return checkIndex(ref, index).weakCompareAndSet(index, expected, newValue);
    }

    @Stub
    public static boolean weakCompareAndSetVolatile(AtomicIntegerArray ref, int index, int expected, int newValue) {
        return checkIndex(ref, index).compareAndSet(index, expected, newValue);
    }

}
