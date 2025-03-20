package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import sun.misc.Unsafe;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.concurrent.atomic.AtomicBoolean;

public class J_U_C_A_AtomicBoolean {
    private static final Unsafe unsafe = Utils.getUnsafe();

    @Stub
    public static boolean compareAndExchange(AtomicBoolean ref, boolean expected, boolean newValue) {
        boolean v;
        do {
            v = ref.get();
            if (v != expected) return v;
        } while (!ref.compareAndSet(expected, newValue));
        return expected;
    }

    @Stub
    public static boolean compareAndExchangeAcquire(AtomicBoolean ref, boolean expected, boolean newValue) {
        boolean r = compareAndExchange(ref, expected, newValue);
        unsafe.fullFence();
        return r;
    }

    @Stub
    public static boolean compareAndExchangeRelease(AtomicBoolean ref, boolean expected, boolean newValue) {
        unsafe.fullFence();
        return compareAndExchange(ref, expected, newValue);
    }

    @Stub
    public static boolean getAcquire(AtomicBoolean ref) {
        boolean r = ref.get();
        unsafe.fullFence();
        return r;
    }

    @Stub
    public static boolean getOpaque(AtomicBoolean ref) {
        return ref.get();
    }

    @Stub
    public static boolean getPlain(AtomicBoolean ref) {
        return ref.get();
    }

    @Stub
    public static void setOpaque(AtomicBoolean ref, boolean newValue) {
        ref.set(newValue);
    }

    @Stub
    public static void setPlain(AtomicBoolean ref, boolean newValue) {
        ref.set(newValue);
    }

    @Stub
    public static void setRelease(AtomicBoolean ref, boolean newValue) {
        unsafe.fullFence();
        ref.set(newValue);
    }

    @Stub
    public static boolean weakCompareAndSetAcquire(AtomicBoolean ref, boolean expected, boolean newValue) {
        boolean r = ref.weakCompareAndSet(expected, newValue);
        unsafe.fullFence();
        return r;
    }

    @Stub
    public static boolean weakCompareAndSetPlain(AtomicBoolean ref, boolean expected, boolean newValue) {
        return ref.weakCompareAndSet(expected, newValue);
    }

    @Stub
    public static boolean weakCompareAndSetRelease(AtomicBoolean ref, boolean expected, boolean newValue) {
        unsafe.fullFence();
        return ref.weakCompareAndSet(expected, newValue);
    }

    @Stub
    public static boolean weakCompareAndSetVolatile(AtomicBoolean ref, boolean expected, boolean newValue) {
        return ref.compareAndSet(expected, newValue);
    }

}
