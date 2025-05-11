package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import sun.misc.Unsafe;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class J_U_C_A_AtomicIntegerArray {
    private static final Unsafe unsafe = Utils.getUnsafe();

    @Stub
    public static int compareAndExchange(AtomicIntegerArray ref, int index, int expected, int newValue) {
        int v;
        do {
            v = ref.get(index);
            if (v != expected) return v;
        } while (!ref.compareAndSet(index, expected, newValue));
        return expected;
    }

    @Stub
    public static int compareAndExchangeAcquire(AtomicIntegerArray ref, int index, int expected, int newValue) {
        int r = compareAndExchange(ref, index, expected, newValue);
        unsafe.fullFence();
        return r;
    }

    @Stub
    public static int compareAndExchangeRelease(AtomicIntegerArray ref, int index, int expected, int newValue) {
        unsafe.fullFence();
        return compareAndExchange(ref, index, expected, newValue);
    }

    @Stub
    public static int getAcquire(AtomicIntegerArray ref, int index) {
        int r = ref.get(index);
        unsafe.fullFence();
        return r;
    }

    @Stub
    public static int getOpaque(AtomicIntegerArray ref, int index) {
        return ref.get(index);
    }

    @Stub
    public static int getPlain(AtomicIntegerArray ref, int index) {
        return ref.get(index);
    }

    @Stub
    public static void setOpaque(AtomicIntegerArray ref, int index, int newValue) {
        ref.set(index, newValue);
    }

    @Stub
    public static void setPlain(AtomicIntegerArray ref, int index, int newValue) {
        ref.set(index, newValue);
    }

    @Stub
    public static void setRelease(AtomicIntegerArray ref, int index, int newValue) {
        unsafe.fullFence();
        ref.set(index, newValue);
    }

    @Stub
    public static boolean weakCompareAndSetAcquire(AtomicIntegerArray ref, int index, int expected, int newValue) {
        boolean r = ref.weakCompareAndSet(index, expected, newValue);
        unsafe.fullFence();
        return r;
    }

    @Stub
    public static boolean weakCompareAndSetPlain(AtomicIntegerArray ref, int index, int expected, int newValue) {
        return ref.weakCompareAndSet(index, expected, newValue);
    }

    @Stub
    public static boolean weakCompareAndSetRelease(AtomicIntegerArray ref, int index, int expected, int newValue) {
        unsafe.fullFence();
        return ref.weakCompareAndSet(index, expected, newValue);
    }

    @Stub
    public static boolean weakCompareAndSetVolatile(AtomicIntegerArray ref, int index, int expected, int newValue) {
        return ref.compareAndSet(index, expected, newValue);
    }

}
