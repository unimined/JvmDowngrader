package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import sun.misc.Unsafe;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.concurrent.atomic.AtomicInteger;

public class J_U_C_A_AtomicInteger {
    private static final Unsafe unsafe = Utils.getUnsafe();

    @Stub
    public static int compareAndExchange(AtomicInteger ref, int expected, int newValue) {
        int v;
        do {
            v = ref.get();
            if (v != expected) return v;
        } while (!ref.compareAndSet(expected, newValue));
        return expected;
    }

    @Stub
    public static int compareAndExchangeAcquire(AtomicInteger ref, int expected, int newValue) {
        int r = compareAndExchange(ref, expected, newValue);
        unsafe.fullFence();
        return r;
    }

    @Stub
    public static int compareAndExchangeRelease(AtomicInteger ref, int expected, int newValue) {
        unsafe.fullFence();
        return compareAndExchange(ref, expected, newValue);
    }

    @Stub
    public static int getAcquire(AtomicInteger ref) {
        int r = ref.get();
        unsafe.fullFence();
        return r;
    }

    @Stub
    public static int getOpaque(AtomicInteger ref) {
        return ref.get();
    }

    @Stub
    public static int getPlain(AtomicInteger ref) {
        return ref.get();
    }

    @Stub
    public static void setOpaque(AtomicInteger ref, int newValue) {
        ref.set(newValue);
    }

    @Stub
    public static void setPlain(AtomicInteger ref, int newValue) {
        ref.set(newValue);
    }

    @Stub
    public static void setRelease(AtomicInteger ref, int newValue) {
        unsafe.fullFence();
        ref.set(newValue);
    }

    @Stub
    public static boolean weakCompareAndSetAcquire(AtomicInteger ref, int expected, int newValue) {
        boolean r = ref.weakCompareAndSet(expected, newValue);
        unsafe.fullFence();
        return r;
    }

    @Stub
    public static boolean weakCompareAndSetPlain(AtomicInteger ref, int expected, int newValue) {
        return ref.weakCompareAndSet(expected, newValue);
    }

    @Stub
    public static boolean weakCompareAndSetRelease(AtomicInteger ref, int expected, int newValue) {
        unsafe.fullFence();
        return ref.weakCompareAndSet(expected, newValue);
    }

    @Stub
    public static boolean weakCompareAndSetVolatile(AtomicInteger ref, int expected, int newValue) {
        return ref.compareAndSet(expected, newValue);
    }

}
