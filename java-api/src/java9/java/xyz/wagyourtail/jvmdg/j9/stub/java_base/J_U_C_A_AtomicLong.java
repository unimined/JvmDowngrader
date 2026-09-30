package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.concurrent.atomic.AtomicLong;

public class J_U_C_A_AtomicLong {

    @Stub
    public static long compareAndExchange(AtomicLong ref, long expected, long newValue) {
        long v;
        do {
            v = ref.get();
            if (v != expected) return v;
        } while (!ref.compareAndSet(expected, newValue));
        return expected;
    }

    @Stub
    public static long compareAndExchangeAcquire(AtomicLong ref, long expected, long newValue) {
        return compareAndExchange(ref, expected, newValue);
    }

    @Stub
    public static long compareAndExchangeRelease(AtomicLong ref, long expected, long newValue) {
        return compareAndExchange(ref, expected, newValue);
    }

    @Stub
    public static long getAcquire(AtomicLong ref) {
        return ref.get();
    }

    @Stub
    public static long getOpaque(AtomicLong ref) {
        return ref.get();
    }

    @Stub
    public static long getPlain(AtomicLong ref) {
        return ref.get();
    }

    @Stub
    public static void setOpaque(AtomicLong ref, long newValue) {
        ref.set(newValue);
    }

    @Stub
    public static void setPlain(AtomicLong ref, long newValue) {
        ref.set(newValue);
    }

    @Stub
    public static void setRelease(AtomicLong ref, long newValue) {
        ref.set(newValue);
    }

    @Stub
    public static boolean weakCompareAndSetAcquire(AtomicLong ref, long expected, long newValue) {
        return ref.weakCompareAndSet(expected, newValue);
    }

    @Stub
    public static boolean weakCompareAndSetPlain(AtomicLong ref, long expected, long newValue) {
        return ref.weakCompareAndSet(expected, newValue);
    }

    @Stub
    public static boolean weakCompareAndSetRelease(AtomicLong ref, long expected, long newValue) {
        return ref.weakCompareAndSet(expected, newValue);
    }

    @Stub
    public static boolean weakCompareAndSetVolatile(AtomicLong ref, long expected, long newValue) {
        return ref.compareAndSet(expected, newValue);
    }

}
