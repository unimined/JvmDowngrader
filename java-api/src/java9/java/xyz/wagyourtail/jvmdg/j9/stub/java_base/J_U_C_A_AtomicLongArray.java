package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import sun.misc.Unsafe;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.concurrent.atomic.AtomicLongArray;

public class J_U_C_A_AtomicLongArray {
    private static final Unsafe unsafe = Utils.getUnsafe();

    @Stub
    public static long compareAndExchange(AtomicLongArray ref, int index, long expected, long newValue) {
        long v;
        do {
            v = ref.get(index);
            if (v != expected) return v;
        } while (!ref.compareAndSet(index, expected, newValue));
        return expected;
    }

    @Stub
    public static long compareAndExchangeAcquire(AtomicLongArray ref, int index, long expected, long newValue) {
        long r = compareAndExchange(ref, index, expected, newValue);
        unsafe.fullFence();
        return r;
    }

    @Stub
    public static long compareAndExchangeRelease(AtomicLongArray ref, int index, long expected, long newValue) {
        unsafe.fullFence();
        return compareAndExchange(ref, index, expected, newValue);
    }

    @Stub
    public static long getAcquire(AtomicLongArray ref, int index) {
        long r = ref.get(index);
        unsafe.fullFence();
        return r;
    }

    @Stub
    public static long getOpaque(AtomicLongArray ref, int index) {
        return ref.get(index);
    }

    @Stub
    public static long getPlain(AtomicLongArray ref, int index) {
        return ref.get(index);
    }

    @Stub
    public static void setOpaque(AtomicLongArray ref, int index, long newValue) {
        ref.set(index, newValue);
    }

    @Stub
    public static void setPlain(AtomicLongArray ref, int index, long newValue) {
        ref.set(index, newValue);
    }

    @Stub
    public static void setRelease(AtomicLongArray ref, int index, long newValue) {
        unsafe.fullFence();
        ref.set(index, newValue);
    }

    @Stub
    public static boolean weakCompareAndSetAcquire(AtomicLongArray ref, int index, long expected, long newValue) {
        boolean r = ref.weakCompareAndSet(index, expected, newValue);
        unsafe.fullFence();
        return r;
    }

    @Stub
    public static boolean weakCompareAndSetPlain(AtomicLongArray ref, int index, long expected, long newValue) {
        return ref.weakCompareAndSet(index, expected, newValue);
    }

    @Stub
    public static boolean weakCompareAndSetRelease(AtomicLongArray ref, int index, long expected, long newValue) {
        unsafe.fullFence();
        return ref.weakCompareAndSet(index, expected, newValue);
    }

    @Stub
    public static boolean weakCompareAndSetVolatile(AtomicLongArray ref, int index, long expected, long newValue) {
        return ref.compareAndSet(index, expected, newValue);
    }

}
